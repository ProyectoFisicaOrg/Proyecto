package org.example.rendering

// --- Imports de JavaFX y del modelo ---
import javafx.scene.canvas.GraphicsContext
import org.example.model.SimulationState

/**
 * Orquestador del renderizado de la escena (SRP).
 *
 * Su única responsabilidad es coordinar, en el orden correcto, la
 * pintura de fondo, suelo, cañón, estela, proyectil e indicador de
 * ángulo. Para ello crea y delega en los renderizadores especializados
 * [Scenery], [CannonRenderer] y [ProjectileRenderer] (composición,
 * orientado a objetos). No calcula física ni gestiona controles.
 *
 * @property gc Contexto gráfico sobre el que se dibuja todo.
 * @property state Estado de la simulación (se lee para dibujar).
 * @property cfg Configuración geométrica del escenario.
 * @property pathRenderer Renderizador de la estela continua.
 * @property targetRenderer Renderizador de la diana arrastrable.
 */
class SceneRenderer(
    private val gc: GraphicsContext,
    private val state: SimulationState,
    private val cfg: SceneConfig,
    private val pathRenderer: PathRenderer,
    private val targetRenderer: TargetRenderer,
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
     * cañón, proyectil, indicador de ángulo y diana.
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

        // 6. Dibuja el proyectil en su posición actual.
        projectileRenderer.drawProjectile(state.projectileX, state.projectileY)

        // 7. Dibuja el indicador del ángulo sobre la base del cañón.
        projectileRenderer.drawAngleIndicator(state.angle)

        // 8. Dibuja la diana (última para que quede encima de todo).
        targetRenderer.drawTarget(state.targetX)
    }
}
