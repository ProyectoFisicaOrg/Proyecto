package org.example.rendering

import javafx.scene.canvas.GraphicsContext
import org.example.model.ISimulationState
import org.example.scrubbing.IHudRenderer
import org.example.scrubbing.IInteractiveEffectState
import org.example.scrubbing.IInteractiveProjectileRenderer
import org.example.scrubbing.IScrubbingController
import org.example.scrubbing.ITooltipAnchor
import org.example.scrubbing.ITooltipRenderer
import org.example.scrubbing.InteractiveEffectState
import org.example.scrubbing.InteractiveProjectileRenderer
import org.example.scrubbing.ResultsHUD
import org.example.scrubbing.TooltipAnchor
import org.example.scrubbing.TooltipRenderer

/**
 * Sub-renderizador cohesivo del entorno y actores principales inyectado por interfaces (SRP + DIP).
 */
class SceneEnvironmentPainter(
    private val gc: GraphicsContext,
    private val cfg: ISceneConfig,
    scenery: IScenery,
    cannonRenderer: ICannonRenderer,
    projectileRenderer: IProjectileRenderer
) : ISceneEnvironmentPainter {
    private val actors: Triple<IScenery, ICannonRenderer, IProjectileRenderer> =
        Triple(scenery, cannonRenderer, projectileRenderer)

    override fun clearAndDrawBackdrop() {
        gc.clearRect(0.0, 0.0, cfg.canvasWidth, cfg.canvasHeight)
        actors.first.drawBackground()
        actors.first.drawGround()
    }

    override fun drawCannonAndAngle(angle: Double) {
        actors.second.drawCannon(angle)
        actors.third.drawAngleIndicator(angle)
    }

    override fun drawProjectile(x: Double, y: Double) {
        actors.third.drawProjectile(x, y)
    }
}

/**
 * Sub-renderizador cohesivo de las capas interactivas de distancia al objetivo, scrubbing, HUD y tooltip (SRP + DIP).
 */
class SceneOverlayRenderer(
    gc: GraphicsContext,
    state: ISimulationState,
    cfg: ISceneConfig,
    targetDistanceHud: ITargetDistanceHud,
    resultsHud: IHudRenderer = ResultsHUD(24.0, 24.0),
    tooltipRenderer: ITooltipRenderer = TooltipRenderer(),
    effectRenderer: IInteractiveProjectileRenderer = InteractiveProjectileRenderer(),
    effectFactory: (Double, Double, Boolean, Boolean) -> IInteractiveEffectState = ::InteractiveEffectState,
    anchorFactory: (Double, Double, Double, Double) -> ITooltipAnchor = ::TooltipAnchor
) : ISceneOverlayRenderer {
    private val ctx: Triple<Triple<GraphicsContext, ISimulationState, ISceneConfig>, Triple<ITargetDistanceHud, IHudRenderer, ITooltipRenderer>, Triple<IInteractiveProjectileRenderer, (Double, Double, Boolean, Boolean) -> IInteractiveEffectState, (Double, Double, Double, Double) -> ITooltipAnchor>> =
        Triple(Triple(gc, state, cfg), Triple(targetDistanceHud, resultsHud, tooltipRenderer), Triple(effectRenderer, effectFactory, anchorFactory))

    override fun renderOverlay(controller: IScrubbingController?) {
        val gc = ctx.first.first
        val state = ctx.first.second
        val cfg = ctx.first.third
        ctx.second.first.render(gc, state.targetX)
        val active = controller ?: return
        if (!active.isEnabled) return
        val fxState = ctx.third.second(state.projectileX, state.projectileY, active.isDragging, active.isHovered)
        ctx.third.first.drawInteractiveEffects(gc, fxState)
        active.flightSummary?.let { ctx.second.second.render(gc, it) }
        if (active.isDragging || active.isHovered) {
            active.currentPoint?.let { point ->
                val anchor = ctx.third.third(state.projectileX, state.projectileY, cfg.canvasWidth, cfg.canvasHeight)
                ctx.second.third.render(gc, point, anchor)
            }
        }
    }
}

/**
 * Orquestador del renderizado de la escena 100% desacoplado por constructores e interfaces (SRP + OCP + DIP).
 */
class SceneRenderer(
    private val state: ISimulationState,
    private val pathRenderer: IPathRenderer,
    private val targetRenderer: ITargetRenderer,
    private val envPainter: ISceneEnvironmentPainter,
    private val overlayRenderer: ISceneOverlayRenderer
) : ISceneRenderer {
    private var activeScrubbing: IScrubbingController? = null

    override fun currentScrubbingController(): IScrubbingController? {
        return activeScrubbing
    }

    override fun assignScrubbingController(controller: IScrubbingController?) {
        activeScrubbing = controller
    }

    override fun render() {
        envPainter.clearAndDrawBackdrop()
        pathRenderer.drawPath()
        envPainter.drawCannonAndAngle(state.angle)
        targetRenderer.drawTarget(state.targetX)
        envPainter.drawProjectile(state.projectileX, state.projectileY)
        overlayRenderer.renderOverlay(activeScrubbing)
    }
}

/**
 * Constructores modulares de paisaje, cañón, entorno y superposiciones (SRP + DIP).
 */
class DefaultSceneryBuilder : ISceneryBuilder {

    override fun buildScenery(gc: GraphicsContext, cfg: ISceneConfig): IScenery {
        return Scenery(SkyBackdropPainter(gc, cfg), GroundFoliagePainter(gc, cfg))
    }
}

class DefaultCannonBuilder : ICannonBuilder {

    override fun buildCannon(gc: GraphicsContext, cfg: ISceneConfig): ICannonRenderer {
        return CannonRenderer(CannonBarrelPainter(gc, cfg), CannonMountPainter(gc, cfg))
    }
}

class DefaultSceneEnvBuilder(
    sceneryBuilder: ISceneryBuilder = DefaultSceneryBuilder(),
    cannonBuilder: ICannonBuilder = DefaultCannonBuilder()
) : ISceneEnvBuilder {
    private val builders: Pair<ISceneryBuilder, ICannonBuilder> = Pair(sceneryBuilder, cannonBuilder)

    override fun buildEnvironment(gc: GraphicsContext, cfg: ISceneConfig): ISceneEnvironmentPainter {
        val scenery = builders.first.buildScenery(gc, cfg)
        val cannon = builders.second.buildCannon(gc, cfg)
        return SceneEnvironmentPainter(gc, cfg, scenery, cannon, ProjectileRenderer(gc, cfg))
    }
}

class DefaultSceneOverlayBuilder : ISceneOverlayBuilder {

    override fun buildOverlay(gc: GraphicsContext, state: ISimulationState, cfg: ISceneConfig, transformer: CoordinateTransformer): ISceneOverlayRenderer {
        return SceneOverlayRenderer(gc, state, cfg, TargetDistanceHud(cfg, transformer))
    }
}

/**
 * Ensamblador de colaboradores gráficos de [ISceneRenderer] conforme a DIP (SRP + DIP).
 */
class SceneRendererAssembler(
    private val cfg: ISceneConfig,
    transformer: CoordinateTransformer = DefaultCoordinateTransformer(cfg),
    envBuilder: ISceneEnvBuilder = DefaultSceneEnvBuilder(),
    overlayBuilder: ISceneOverlayBuilder = DefaultSceneOverlayBuilder()
) : ICanvasBounds by cfg {
    private val deps: Triple<CoordinateTransformer, ISceneEnvBuilder, ISceneOverlayBuilder> =
        Triple(transformer, envBuilder, overlayBuilder)

    fun assemble(gc: GraphicsContext, state: ISimulationState, pathRenderer: IPathRenderer, targetRenderer: ITargetRenderer): ISceneRenderer {
        val env = deps.second.buildEnvironment(gc, cfg)
        val overlay = deps.third.buildOverlay(gc, state, cfg, deps.first)
        return SceneRenderer(state, pathRenderer, targetRenderer, env, overlay)
    }
}
