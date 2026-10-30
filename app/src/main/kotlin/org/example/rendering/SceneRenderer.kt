package org.example.rendering

import javafx.scene.canvas.GraphicsContext
import org.example.model.SimulationState
import org.example.scrubbing.IHudRenderer
import org.example.scrubbing.IInteractiveProjectileRenderer
import org.example.scrubbing.IScrubbingController
import org.example.scrubbing.ITooltipRenderer
import org.example.scrubbing.InteractiveEffectState
import org.example.scrubbing.InteractiveProjectileRenderer
import org.example.scrubbing.ResultsHUD
import org.example.scrubbing.TooltipAnchor
import org.example.scrubbing.TooltipRenderer

/**
 * Contrato de renderizado para orquestadores gráficos de la escena (DIP + ISP).
 */
interface ISceneRenderer {
    var scrubbingController: IScrubbingController?
    fun render()
}

/**
 * Abstracción para el pintado del entorno base y actores principales (DIP).
 */
interface ISceneEnvironmentPainter {
    fun clearAndDrawBackdrop()
    fun drawCannonAndAngle(angle: Double)
    fun drawProjectile(x: Double, y: Double)
}

/**
 * Abstracción para el pintado de superposiciones interactivas (HUD y Tooltip) (DIP).
 */
interface ISceneOverlayRenderer {
    fun renderOverlay(controller: IScrubbingController?)
}

/**
 * Sub-renderizador del entorno y actores principales (fondo, suelo, cañón y proyectil) (SRP).
 */
class SceneEnvironmentPainter(
    private val gc: GraphicsContext,
    private val cfg: SceneConfig,
    private val scenery: IScenery = Scenery(gc, cfg),
    private val cannonRenderer: ICannonRenderer = CannonRenderer(gc, cfg),
    private val projectileRenderer: IProjectileRenderer = ProjectileRenderer(gc, cfg)
) : ISceneEnvironmentPainter {
    override fun clearAndDrawBackdrop() {
        gc.clearRect(0.0, 0.0, cfg.canvasWidth, cfg.canvasHeight)
        scenery.drawBackground()
        scenery.drawGround()
    }

    override fun drawCannonAndAngle(angle: Double) {
        cannonRenderer.drawCannon(angle)
        projectileRenderer.drawAngleIndicator(angle)
    }

    override fun drawProjectile(x: Double, y: Double) {
        projectileRenderer.drawProjectile(x, y)
    }
}

/**
 * Sub-renderizador cohesivo de las capas interactivas de distancia al objetivo, scrubbing, HUD y tooltip (SRP + DIP).
 */
class SceneOverlayRenderer(
    private val gc: GraphicsContext,
    private val state: SimulationState,
    private val cfg: SceneConfig,
    private val targetDistanceHud: ITargetDistanceHud = TargetDistanceHud(cfg, DefaultCoordinateTransformer(cfg)),
    private val resultsHud: IHudRenderer = ResultsHUD(x = 24.0, y = 24.0),
    private val tooltipRenderer: ITooltipRenderer = TooltipRenderer(),
    private val effectRenderer: IInteractiveProjectileRenderer = InteractiveProjectileRenderer()
) : ISceneOverlayRenderer {
    override fun renderOverlay(controller: IScrubbingController?) {
        targetDistanceHud.render(gc, state.targetX)
        val active = controller ?: return
        if (!active.isEnabled) return

        val fxState = InteractiveEffectState(state.projectileX, state.projectileY, active.isDragging, active.isHovered)
        effectRenderer.drawInteractiveEffects(gc, fxState)
        active.flightSummary?.let { resultsHud.render(gc, it) }

        if (active.isDragging || active.isHovered) {
            active.currentPoint?.let { point ->
                val anchor = TooltipAnchor(state.projectileX, state.projectileY, cfg.canvasWidth, cfg.canvasHeight)
                tooltipRenderer.render(gc, point, anchor)
            }
        }
    }
}

/**
 * Orquestador del renderizado de la escena desacoplado por constructores e interfaces (SRP + OCP + DIP).
 */
class SceneRenderer(
    private val state: SimulationState,
    private val pathRenderer: IPathRenderer,
    private val targetRenderer: ITargetRenderer,
    private val envPainter: ISceneEnvironmentPainter,
    private val overlayRenderer: ISceneOverlayRenderer
) : ISceneRenderer {

    constructor(
        gc: GraphicsContext,
        state: SimulationState,
        cfg: SceneConfig,
        pathRenderer: IPathRenderer,
        targetRenderer: ITargetRenderer,
        transformer: CoordinateTransformer = DefaultCoordinateTransformer(cfg)
    ) : this(
        state = state,
        pathRenderer = pathRenderer,
        targetRenderer = targetRenderer,
        envPainter = SceneEnvironmentPainter(gc, cfg),
        overlayRenderer = SceneOverlayRenderer(gc, state, cfg, TargetDistanceHud(cfg, transformer))
    )

    override var scrubbingController: IScrubbingController? = null

    override fun render() {
        envPainter.clearAndDrawBackdrop()
        pathRenderer.drawPath()
        envPainter.drawCannonAndAngle(state.angle)
        targetRenderer.drawTarget(state.targetX)
        envPainter.drawProjectile(state.projectileX, state.projectileY)
        overlayRenderer.renderOverlay(scrubbingController)
    }
}
