package org.example

// === Imports de JavaFX para construir la ventana y el layout ===
import javafx.application.Application      // Base de toda aplicación JavaFX.
import javafx.geometry.Insets              // Márgenes (padding) de los layouts.
import javafx.geometry.Pos                 // Alineación de los nodos.
import javafx.scene.Scene                  // Scene que contiene los nodos.
import javafx.scene.canvas.Canvas          // Lienzo donde se dibuja la simulación.
import javafx.scene.layout.Background      // Fondo de un layout.
import javafx.scene.layout.BackgroundFill  // Relleno sólido de un fondo.
import javafx.scene.layout.CornerRadii     // Esquinas redondeadas del fondo.
import javafx.scene.layout.HBox            // Layout horizontal (izquierda-derecha).
import javafx.scene.layout.Priority        // Controla cómo crece un nodo.
import javafx.scene.layout.VBox            // Layout vertical (arriba-abajo).
import javafx.scene.paint.Color            // Colores.
import javafx.scene.paint.CycleMethod      // Modo de repetición de un gradiente.
import javafx.scene.paint.LinearGradient   // Gradiente lineal.
import javafx.scene.paint.Stop             // Parada de un gradiente.
import javafx.stage.Stage                  // Ventana principal.

// === Imports de los componentes propios de la aplicación ===
import org.example.TrajectorySimulationFactory
import org.example.animation.AnimationEngine   // Motor de animación.
import org.example.collision.CollisionStatus
import org.example.model.SimulationState       // Estado de la simulación.
import org.example.rendering.DefaultCoordinateTransformer // Conversor de coordenadas modelo <-> canvas.
import org.example.rendering.PathRenderer      // Renderizador de estela continua.
import org.example.rendering.SceneConfig       // Configuración geométrica.
import org.example.rendering.SceneRenderer     // Orquestador de dibujo.
import org.example.rendering.TargetRenderer    // Renderizador de diana arrastrable.
import org.example.scrubbing.PhysicsTrajectoryInspectorAdapter // Adaptador de física para inspección (Issue 3.2).
import org.example.scrubbing.ScrubbingController // Controlador de interacción y arrastre sobre trayectoria (Issue 3.2).
import org.example.ui.ControlPanel             // Panel de controles.

/**
 * Simulación de Cañón con JavaFX.
 *
 * Arquitectura orientada a objetos que sigue los principios SOLID:
 *  - SRP: cada clase tiene una única responsabilidad (controles, render,
 *    física, animación, estado y entrada).
 *  - OCP: extensible sin modificar el código existente (nuevos generadores o detectores
 *    sin tocar la animación o el render).
 *  - LSP: cualquier implementación de [CollisionDetector] o [TrajectoryMatrixGenerator] es intercambiable.
 *  - ISP: interfaces pequeñas y específicas ([CollisionListener], [CoordinateTransformer]).
 *  - DIP: los módulos de alto nivel dependen de abstracciones,
 *    no de implementaciones concretas.
 *
 * Está clase ES EL PUNTO DE ENTRADA: su única responsabilidad es crear
 * los objetos del sistema, conectarlos entre sí y mostrar la ventana.
 */
class CannonSimulation : Application() {

    // Referencia al panel de controles, se completa dentro de start().
    // Se declara como propiedad de clase para resolver la dependencia
    // circular entre el panel (callback onFire) y el motor de animación
    // (callback onFinished).
    private lateinit var controls: ControlPanel

    /**
     * Método principal de JavaFX: configura y muestra la ventana.
     *
     * @param primaryStage Ventana principal que entrega JavaFX.
     */
    override fun start(primaryStage: Stage) {
        // Título de la ventana.
        primaryStage.title = "Simulación de Cañón"

        // --- 1. Crear los objetos del sistema (punto de composición) ---
        // Estado mutable compartido por toda la aplicación.
        val state = SimulationState()
        // Configuración geométrica (medidas del canvas y del cañón).
        val cfg = SceneConfig()
        // Transformador de coordenadas (metros físicos <-> píxeles canvas) (DIP).
        val transformer = DefaultCoordinateTransformer(cfg, pixelsPerMeter = 2.5)

        // --- 2. Canvas y su contexto gráfico ---
        // Lienzo del tamaño definido en la configuración.
        val canvas = Canvas(cfg.canvasWidth, cfg.canvasHeight)
        // Contexto gráfico: permite dibujar sobre el canvas.
        val gc = canvas.graphicsContext2D

        // --- 3. Renderizadores de estela y diana ---
        // Renderizador de la estela continua del proyectil (Issue 2.2).
        val pathRenderer = PathRenderer(gc)

        // Renderer orquestador (lateinit porque el callback de targetRenderer lo referencia).
        lateinit var sceneRenderer: SceneRenderer

        // Renderizador de la diana arrastrable con notificación de cambios (Issue 2.2).
        val targetRenderer = TargetRenderer(
            gc = gc,
            cfg = cfg,
            canvas = canvas,
            state = state,
            // Cada vez que el usuario mueve la diana, se actualiza el estado y se repinta.
            onTargetPositionChanged = { newX ->
                state.targetX = newX
                sceneRenderer.render()
            },
            // Cada fotograma del pulso de la diana repinta la escena completa.
            onRepaint = { sceneRenderer.render() }
        )

        // --- 4. Motor de render y dependencias de física ---
        // El renderer orquesta el dibujo de todos los componentes.
        sceneRenderer = SceneRenderer(gc, state, cfg, pathRenderer, targetRenderer)

        // Controlador de arrastre con mouse (Scrubbing) y HUD (Issue 3.2)
        val scrubbingController = ScrubbingController(
            state = state,
            transformer = transformer,
            cfg = cfg,
            onRepaint = { sceneRenderer.render() }
        )
        sceneRenderer.scrubbingController = scrubbingController

        // Abstracciones de física y colisiones entregadas por el módulo de Dev 1 (DIP)
        val trajectoryGenerator = TrajectorySimulationFactory.createGenerator()
        val collisionDetector = TrajectorySimulationFactory.createCollisionDetector()

        // Motor de animación de vuelo (Issue 2.2 + Issue 3.2):
        // Recorre de forma fluida la trayectoria entregada por Dev 1,
        // acumula el rastro continuo y valida colisiones al tocar el suelo.
        // Al finalizar, habilita la interacción de arrastre sobre la trayectoria (Issue 3.2).
        lateinit var animationEngine: AnimationEngine
        animationEngine = AnimationEngine(
            state = state,
            cfg = cfg,
            transformer = transformer,
            trajectoryGenerator = trajectoryGenerator,
            collisionDetector = collisionDetector,
            onUpdate = { sceneRenderer.render() },
            onFinished = { controls.setAnimating(false) },
            onTick = { x: Double, y: Double -> pathRenderer.addPoint(x, y) },
            collisionListener = { result ->
                // Notificación del validador de colisión del Issue 2.1 exactamente al impactar
                controls.showCollisionResult(result)
                println("=== RESULTADO DE IMPACTO (Issue 2.1) ===")
                println("Estado: ${result.status} | Mensaje: ${result.message}")
                result.impact?.let { impact ->
                    println("Detalles: x=%.2f m, tiempo=%.4f s, distCentro=%.2f m"
                        .format(impact.point.x, impact.impactTime, impact.distanceToCenter))
                }
            },
            onOutOfBounds = {
                // Si la pelota sale del cuadrado, reinicio automático
                scrubbingController.disable()
                pathRenderer.clear()
                controls.setAnimating(false)
                controls.showOutOfBoundsMessage()
                println("⚠️ El proyectil salió del cuadrado. Juego reiniciado.")
            },
            onFlightCompleted = { matrix ->
                // Al completar el vuelo e impactar el suelo: congela la trayectoria y habilita scrubbing (Issue 3.2)
                pathRenderer.freeze()
                val inspector = PhysicsTrajectoryInspectorAdapter(matrix, transformer)
                scrubbingController.enable(inspector)
                sceneRenderer.render()
            },
            timeScale = 1.85 // Vuelo más rápido y dinámico (1.85x velocidad real)
        )

        // --- 5. Panel de controles ---
        // Crea la UI (sliders + botones) y conecta sus callbacks con el motor.
        controls = ControlPanel(
            state = state,
            onFire = {
                println("Disparando con: ángulo=${state.angle.toInt()}°, " +
                        "velocidad=${state.velocity.toInt()} m/s, " +
                        "gravedad=${"%.1f".format(state.gravity)} m/s², " +
                        "masa=${state.mass.toInt()} kg")
                // Deshabilita scrubbing anterior y limpia la estela anterior
                scrubbingController.disable()
                pathRenderer.clear()
                pathRenderer.recordLaunch()
                controls.clearCollisionResult()
                controls.setAnimating(true)
                // Inicia la animación del proyectil recorriendo la trayectoria de Dev 1
                animationEngine.start()
            },
            onAngleChange = { sceneRenderer.render() },
            onReset = {
                // Reinicio manual solicitado desde el panel
                scrubbingController.disable()
                animationEngine.reset()
                pathRenderer.clear()
                controls.clearCollisionResult()
                sceneRenderer.render()
            }
        )
        // Construye el panel de controles (VBox) listo para el layout.
        val controlsPanel = controls.build()

        // --- 6. Contenedor del canvas ---
        // VBox que envuelve al canvas con estilo y borde.
        val canvasContainer = VBox(canvas).apply {
            alignment = Pos.CENTER  // Centra el canvas.
            style = """
                -fx-background-color: #0f0f23;
                -fx-background-radius: 12;
                -fx-border-color: #0f3460;
                -fx-border-width: 1;
                -fx-border-radius: 12;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 4);
            """.trimIndent()
        }

        // --- 7. Layout principal ---
        // HBox: a la izquierda el panel de controles, a la derecha el canvas.
        val mainLayout = HBox(30.0, controlsPanel, canvasContainer)
        mainLayout.alignment = Pos.CENTER   // Centra el contenido.
        mainLayout.padding = Insets(20.0)   // Márgenes alrededor.
        // Permite que el contenedor del canvas ocupe el espacio sobrante.
        HBox.setHgrow(canvasContainer, Priority.ALWAYS)

        // --- 8. Fondo cielo/pasto para toda la ventana ---
        // Gradiente de fondo de la ventana (cielo a pasto).
        val skyGradient = LinearGradient(
            0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(135, 206, 235)),  // Azul cielo arriba.
            Stop(0.6, Color.rgb(176, 224, 230)),  // Azul claro.
            Stop(1.0, Color.rgb(34, 139, 34))     // Verde pasto abajo.
        )
        // Aplica el gradiente como fondo del layout principal.
        mainLayout.background = Background(BackgroundFill(skyGradient, CornerRadii.EMPTY, Insets.EMPTY))

        // --- 9. Scene y Stage ---
        // Crea la escena con el tamaño del canvas más el ancho del panel.
        val scene = Scene(mainLayout, cfg.canvasWidth + 400.0, cfg.canvasHeight + 60.0)
        primaryStage.scene = scene      // Asigna la escena a la ventana.
        primaryStage.minWidth = 1000.0  // Ancho mínimo de la ventana.
        primaryStage.minHeight = 650.0  // Alto mínimo de la ventana.
        primaryStage.show()             // Muestra la ventana.

        // --- 10. Instala el gesto de arrastre de la diana y el scrubbing sobre la trayectoria ---
        // Se instala primero el controlador de la capa frontal (proyectil / scrubbing)
        // para que reciba y consuma eventos con prioridad cuando esté al frente.
        scrubbingController.install(canvas)
        targetRenderer.installDrag()
        targetRenderer.startPulse()

        // --- 11. Render inicial ---
        // Dibuja el primer fotograma (fondo, cañón y proyectil en origen).
        sceneRenderer.render()
    }

    companion object {
        /**
         * Punto de entrada de la JVM. Lanza la aplicación JavaFX con
         * la clase CannonSimulation como ventana principal.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            // Inicia el ciclo de vida de JavaFX.
            Application.launch(CannonSimulation::class.java, *args)
        }
    }
}
