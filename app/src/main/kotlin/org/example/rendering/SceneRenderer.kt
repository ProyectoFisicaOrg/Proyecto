package org.example.rendering

// --- Imports de JavaFX y del modelo ---
import javafx.scene.canvas.GraphicsContext
import org.example.model.SimulationState
import org.example.scrubbing.IHudRenderer
import org.example.scrubbing.ITooltipRenderer
import org.example.scrubbing.InteractiveProjectileRenderer
import org.example.scrubbing.ResultsHUD
import org.example.scrubbing.ScrubbingController
import org.example.scrubbing.TooltipRenderer

/**
 * Orquestador del renderizado de la escena (SRP + OCP + DIP).
 *
 * Su única responsabilidad es coordinar, en el orden correcto, la
 * pintura de fondo, suelo, cañón, estela, proyectil, indicador de
 * ángulo, diana, efectos interactivos de arrastre, HUD de resultados
 * y etiqueta flotante (tooltip).
 *
 * @property gc Contexto gráfico sobre el que se dibuja todo.
 * @property state Estado de la simulación (se lee para dibujar).
 * @property cfg Configuración geométrica del escenario.
 * @property pathRenderer Renderizador de la estela continua.
 * @property targetRenderer Renderizador de la diana arrastrable.
 * @property scrubbingController Controlador de interacción y arrastre sobre la trayectoria (Issue 3.2).
 * @property resultsHud Renderizador de la tarjeta de resultados finales (Issue 3.2).
 * @property tooltipRenderer Renderizador de la etiqueta flotante de telemetría (Issue 3.2).
 * @property interactiveProjectileRenderer Renderizador de halo y foco del proyectil interactivo.
 */
class SceneRenderer(
    private val gc: GraphicsContext,
    private val state: SimulationState,
    private val cfg: SceneConfig,
    private val pathRenderer: PathRenderer,
    private val targetRenderer: TargetRenderer,
    var scrubbingController: ScrubbingController? = null,
    val resultsHud: IHudRenderer = ResultsHUD(x = 20.0, y = 20.0),
    val tooltipRenderer: ITooltipRenderer = TooltipRenderer(),
    val interactiveProjectileRenderer: InteractiveProjectileRenderer = InteractiveProjectileRenderer(),
) {
    // Renderizador del fondo (cielo, sol, nube, colinas, pasto).
    private val scenery = Scenery(gc, cfg)
    // Renderizador del cañón (tubo, boca, recámara, base).
    private val cannonRenderer = CannonRenderer(gc, cfg)
    // Renderizador del proyectil e indicador de ángulo.
    private val projectileRenderer = ProjectileRenderer(gc, cfg)

    /**
     * Renderiza un fotograma completo leyendo el estado actual.
     * Orden de pintado: limpiar, fondo, suelo, estela continua,
     * cañón, indicador de ángulo, diana, proyectil (por delante del objetivo),
     * efectos interactivos, tarjeta HUD de resultados y etiqueta flotante (tooltip).
     */
    fun render() {
        // 1. Limpia el canvas para empezar un fotograma nuevo.
        gc.clearRect(0.0, 0.0, cfg.canvasWidth, cfg.canvasHeight)

        // 2. Dibuja el fondo: cielo y pasto.
        scenery.drawBackground()

        // 3. Dibuja la línea del suelo.
        scenery.drawGround()

        // 4. Dibuja la estela continua del proyectil (detrás del cañón).
        pathRenderer.drawPath()

        // 5. Dibuja el cañón rotado según el ángulo actual.
        cannonRenderer.drawCannon(state.angle)

        // 6. Dibuja el indicador del ángulo sobre la base del cañón.
        projectileRenderer.drawAngleIndicator(state.angle)

        // 7. Dibuja la diana en el suelo (detrás del proyectil para que el impacto quede al frente).
        targetRenderer.drawTarget(state.targetX)

        // 8. Dibuja el proyectil en su posición actual (por delante / arriba del objetivo).
        projectileRenderer.drawProjectile(state.projectileX, state.projectileY)

        // 9. Efectos visuales interactivos sobre el proyectil si el scrubbing está activo (Issue 3.2)
        val controller = scrubbingController
        if (controller?.isEnabled == true) {
            interactiveProjectileRenderer.drawInteractiveEffects(
                gc = gc,
                x = state.projectileX,
                y = state.projectileY,
                isDragging = controller.isDragging,
                isHovered = controller.isHovered
            )
        }

        // 10. Dibuja la tarjeta de resultados estática en pantalla si el vuelo finalizó (Issue 3.2)
        if (controller?.isEnabled == true) {
            controller.flightSummary?.let { summary ->
                resultsHud.render(gc, summary)
            }
        }

        // 11. Dibuja la etiqueta flotante (tooltip) en tiempo real siguiendo al proyectil (Issue 3.2)
        if (controller?.isEnabled == true && (controller.isDragging || controller.isHovered)) {
            controller.currentPoint?.let { point ->
                tooltipRenderer.render(
                    gc = gc,
                    point = point,
                    targetCanvasX = state.projectileX,
                    targetCanvasY = state.projectileY,
                    canvasWidth = cfg.canvasWidth,
                    canvasHeight = cfg.canvasHeight
                )
            }
        }
    }
}
