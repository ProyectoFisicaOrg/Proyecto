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
import org.example.animation.AnimationEngine   // Motor de animación.
import org.example.model.SimulationState       // Estado de la simulación.
import org.example.physics.MockPhysicsEngine   // Implementación de física mock.
import org.example.rendering.SceneConfig       // Configuración geométrica.
import org.example.rendering.SceneRenderer     // Orquestador de dibujo.
import org.example.ui.ControlPanel             // Panel de controles.

/**
 * Simulación de Cañón con JavaFX.
 *
 * Arquitectura orientada a objetos que sigue los principios SOLID:
 *  - SRP: cada clase tiene una única responsabilidad (controles, render,
 *    física, animación, estado y entrada).
 *  - OCP: extensible sin modificar el código existente (nuevos motores de
 *    física sin tocar el resto).
 *  - LSP: cualquier implementación de [PhysicsEngine] es intercambiable.
 *  - ISP: interfaces pequeñas y específicas ([PhysicsEngine] con un
 *    único método).
 *  - DIP: los módulos de alto nivel dependen de la abstracción
 *    [PhysicsEngine], no de una implementación concreta.
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

        // --- 2. Canvas y su contexto gráfico ---
        // Lienzo del tamaño definido en la configuración.
        val canvas = Canvas(cfg.canvasWidth, cfg.canvasHeight)
        // Contexto gráfico: permite dibujar sobre el canvas.
        val gc = canvas.graphicsContext2D

        // --- 3. Motor de render y motor de animación ---
        // El renderer se encarga de pintar cada fotograma.
        val sceneRenderer = SceneRenderer(gc, state, cfg)
        // El motor de animación consulta la física (como abstracción) y
        // repinta mediante el callback onUpdate; al terminar rearma los
        // controles vía onFinished.
        val animationEngine = AnimationEngine(
            state = state,                    // Estado que actualiza.
            physics = MockPhysicsEngine(),    // Abstracción PhysicalEngine (DIP).
            cfg = cfg,                        // Configuración para límites.
            onUpdate = { sceneRenderer.render() },       // Repinta el fotograma.
            onFinished = { controls.setAnimating(false) } // Habilita controles al terminar.
        )

        // --- 4. Panel de controles ---
        // Crea la UI (sliders + botón) y conecta sus callbacks con el motor.
        controls = ControlPanel(
            state = state,           // Estado que leen/actualizan los sliders.
            onFire = {
                // Al presionar DISPARAR: imprime los valores capturados.
                println("Disparando con: ángulo=${state.angle.toInt()}°, " +
                        "velocidad=${state.velocity.toInt()} m/s, " +
                        "gravedad=${"%.1f".format(state.gravity)} m/s², " +
                        "masa=${state.mass.toInt()} kg")
                // Deshabilita controles y botón mientras vuela el proyectil.
                controls.setAnimating(true)
                // Inicia la animación del proyectil.
                animationEngine.start()
            },
            onAngleChange = { sceneRenderer.render() }  // Repinta al mover el ángulo.
        )
        // Construye el panel de controles (VBox) listo para el layout.
        val controlsPanel = controls.build()

        // --- 5. Contenedor del canvas ---
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

        // --- 6. Layout principal ---
        // HBox: a la izquierda el panel de controles, a la derecha el canvas.
        val mainLayout = HBox(30.0, controlsPanel, canvasContainer)
        mainLayout.alignment = Pos.CENTER   // Centra el contenido.
        mainLayout.padding = Insets(20.0)   // Márgenes alrededor.
        // Permite que el contenedor del canvas ocupe el espacio sobrante.
        HBox.setHgrow(canvasContainer, Priority.ALWAYS)

        // --- 7. Fondo cielo/pasto para toda la ventana ---
        // Gradiente de fondo de la ventana (cielo a pasto).
        val skyGradient = LinearGradient(
            0.0, 0.0, 0.0, 1.0, true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(135, 206, 235)),  // Azul cielo arriba.
            Stop(0.6, Color.rgb(176, 224, 230)),  // Azul claro.
            Stop(1.0, Color.rgb(34, 139, 34))     // Verde pasto abajo.
        )
        // Aplica el gradiente como fondo del layout principal.
        mainLayout.background = Background(BackgroundFill(skyGradient, CornerRadii.EMPTY, Insets.EMPTY))

        // --- 8. Scene y Stage ---
        // Crea la escena con el tamaño del canvas más el ancho del panel.
        val scene = Scene(mainLayout, cfg.canvasWidth + 400.0, cfg.canvasHeight + 60.0)
        primaryStage.scene = scene      // Asigna la escena a la ventana.
        primaryStage.minWidth = 1000.0  // Ancho mínimo de la ventana.
        primaryStage.minHeight = 650.0  // Alto mínimo de la ventana.
        primaryStage.show()             // Muestra la ventana.

        // --- 9. Render inicial ---
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
