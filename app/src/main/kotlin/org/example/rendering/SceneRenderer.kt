package org.example.rendering

import javafx.scene.canvas.GraphicsContext
import org.example.model.ISimulationState
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
 * Sub-renderizador cohesivo del entorno y actores principales inyectado por interfaces (SRP + DIP).
 */
class SceneEnvironmentPainter(
    gc: GraphicsContext,
    cfg: ISceneConfig,
    scenery: IScenery,
    cannonRenderer: ICannonRenderer,
    projectileRenderer: IProjectileRenderer
) : ISceneEnvironmentPainter {
    private val ctx: Pair<Pair<GraphicsContext, ISceneConfig>, Triple<IScenery, ICannonRenderer, IProjectileRenderer>> =
        Pair(Pair(gc, cfg), Triple(scenery, cannonRenderer, projectileRenderer))

    override fun clearAndDrawBackdrop() {
        val env = ctx.first
        env.first.clearRect(0.0, 0.0, env.second.width(), env.second.height())
        ctx.second.first.drawBackground()
        ctx.second.first.drawGround()
    }

    override fun drawCannonAndAngle(angle: Double) {
        ctx.second.second.drawCannon(angle)
        ctx.second.third.drawAngleIndicator(angle)
    }

    override fun drawProjectile(x: Double, y: Double) {
        ctx.second.third.drawProjectile(x, y)
    }
}

/**
 * Pintor dedicado a los efectos visuales interactivos, HUD de resultados y tooltip de scrubbing (SRP + DIP).
 */
class ScrubbingFeedbackPainter(
    resultsHud: IHudRenderer,
    tooltipRenderer: ITooltipRenderer,
    effectRenderer: IInteractiveProjectileRenderer
) : IScrubbingFeedbackPainter {
    private val deps: Triple<IHudRenderer, ITooltipRenderer, IInteractiveProjectileRenderer> =
        Triple(resultsHud, tooltipRenderer, effectRenderer)

    override fun renderScrubbing(gc: GraphicsContext, controller: IScrubbingController, coordsAndBounds: List<Double>) {
        if (!controller.checkEnabled()) return
        val px = coordsAndBounds[0]
        val py = coordsAndBounds[1]
        val dragging = controller.checkDragging()
        val hovered = controller.checkHovered()
        deps.third.drawInteractiveEffects(gc, InteractiveEffectState(px, py, dragging, hovered))
        controller.currentFlightSummary()?.let { deps.first.render(gc, it) }
        if (dragging || hovered) {
            controller.selectedTrajectoryPoint()?.let { point ->
                deps.second.render(gc, point, TooltipAnchor(px, py, coordsAndBounds[2], coordsAndBounds[3]))
            }
        }
    }
}

/**
 * Ensamblador predeterminado de feedback visual de scrubbing (DIP).
 */
object ScrubbingFeedbackWiring : IScrubbingFeedbackWiring {
    override fun createFeedbackPainter(): IScrubbingFeedbackPainter {
        return ScrubbingFeedbackPainter(ResultsHUD(24.0, 24.0), TooltipRenderer(), InteractiveProjectileRenderer())
    }
}

/**
 * Sub-renderizador cohesivo de las capas interactivas de distancia al objetivo y feedback de scrubbing (SRP + DIP).
 */
class SceneOverlayRenderer(
    gc: GraphicsContext,
    state: ISimulationState,
    cfg: ISceneConfig,
    targetDistanceHud: ITargetDistanceHud,
    feedback: IScrubbingFeedbackPainter = ScrubbingFeedbackWiring.createFeedbackPainter()
) : ISceneOverlayRenderer {
    private val ctx: Pair<Triple<GraphicsContext, ISimulationState, ISceneConfig>, Pair<ITargetDistanceHud, IScrubbingFeedbackPainter>> =
        Pair(Triple(gc, state, cfg), Pair(targetDistanceHud, feedback))

    override fun renderOverlay(controller: IScrubbingController?) {
        val env = ctx.first
        ctx.second.first.render(env.first, env.second.currentTargetX())
        val active = controller ?: return
        val proj = env.second.currentProjectileCoords()
        val bounds = listOf(proj.first, proj.second, env.third.width(), env.third.height())
        ctx.second.second.renderScrubbing(env.first, active, bounds)
    }
}

/**
 * Orquestador del renderizado de la escena 100% desacoplado por constructores e interfaces (SRP + OCP + DIP).
 */
class SceneRenderer(
    state: ISimulationState,
    pathRenderer: IPathRenderer,
    targetRenderer: ITargetRenderer,
    envPainter: ISceneEnvironmentPainter,
    overlayRenderer: ISceneOverlayRenderer
) : ISceneRenderer {
    private var slot: Pair<Pair<ISimulationState, Pair<IPathRenderer, ITargetRenderer>>, Pair<Pair<ISceneEnvironmentPainter, ISceneOverlayRenderer>, IScrubbingController?>> =
        Pair(Pair(state, Pair(pathRenderer, targetRenderer)), Pair(Pair(envPainter, overlayRenderer), null))

    override fun currentScrubbingController(): IScrubbingController? {
        return slot.second.second
    }

    override fun assignScrubbingController(controller: IScrubbingController?) {
        slot = Pair(slot.first, Pair(slot.second.first, controller))
    }

    override fun render() {
        val st = slot.first.first
        val actors = slot.first.second
        val painters = slot.second.first
        painters.first.clearAndDrawBackdrop()
        actors.first.drawPath()
        painters.first.drawCannonAndAngle(st.currentAngle())
        actors.second.drawTarget(st.currentTargetX())
        val proj = st.currentProjectileCoords()
        painters.first.drawProjectile(proj.first, proj.second)
        painters.second.renderOverlay(slot.second.second)
    }
}

/**
 * Constructores modulares de paisaje, cañón, entorno y superposiciones (SRP + DIP).
 */
object DefaultSceneryBuilder : ISceneryBuilder {
    override fun buildScenery(gc: GraphicsContext, cfg: ISceneConfig): IScenery {
        return Scenery(SkyBackdropPainter(gc, cfg), GroundFoliagePainter(gc, cfg))
    }
}

object DefaultCannonBuilder : ICannonBuilder {
    override fun buildCannon(gc: GraphicsContext, cfg: ISceneConfig): ICannonRenderer {
        return CannonRenderer(CannonBarrelPainter(gc, cfg), CannonMountPainter(gc, cfg))
    }
}

object DefaultSceneEnvBuilder : ISceneEnvBuilder {
    override fun buildEnvironment(gc: GraphicsContext, cfg: ISceneConfig): ISceneEnvironmentPainter {
        val scenery = DefaultSceneryBuilder.buildScenery(gc, cfg)
        val cannon = DefaultCannonBuilder.buildCannon(gc, cfg)
        return SceneEnvironmentPainter(gc, cfg, scenery, cannon, ProjectileRenderer(gc, cfg))
    }
}

object DefaultSceneOverlayBuilder : ISceneOverlayBuilder {
    override fun buildOverlay(gc: GraphicsContext, state: ISimulationState, cfg: ISceneConfig, transformer: CoordinateTransformer): ISceneOverlayRenderer {
        return SceneOverlayRenderer(gc, state, cfg, TargetDistanceHud(cfg, transformer))
    }
}

/**
 * Datos encapsulados de entrada para ensamblar un [ISceneRenderer] (SRP).
 */
@ConsistentCopyVisibility
data class SceneRenderInput internal constructor(
    private val pack: Pair<Triple<GraphicsContext, ISceneConfig, CoordinateTransformer>, Triple<ISimulationState, IPathRenderer, ITargetRenderer>>
) : ISceneRenderInput {

    constructor(
        gc: GraphicsContext,
        cfg: ISceneConfig,
        transformer: CoordinateTransformer,
        state: ISimulationState,
        path: IPathRenderer,
        target: ITargetRenderer
    ) : this(Pair(Triple(gc, cfg, transformer), Triple(state, path, target)))

    override fun graphics(): GraphicsContext {
        return pack.first.first
    }

    override fun sceneConfig(): ISceneConfig {
        return pack.first.second
    }

    override fun coordTransformer(): CoordinateTransformer {
        return pack.first.third
    }

    override fun stateModel(): ISimulationState {
        return pack.second.first
    }

    override fun path(): IPathRenderer {
        return pack.second.second
    }

    override fun target(): ITargetRenderer {
        return pack.second.third
    }
}

/**
 * Ensamblador de colaboradores gráficos de [ISceneRenderer] conforme a DIP.
 */
object SceneRendererWiring : ISceneRendererWiring {
    override fun assemble(input: ISceneRenderInput): ISceneRenderer {
        val env = DefaultSceneEnvBuilder.buildEnvironment(input.graphics(), input.sceneConfig())
        val overlay = DefaultSceneOverlayBuilder.buildOverlay(input.graphics(), input.stateModel(), input.sceneConfig(), input.coordTransformer())
        return SceneRenderer(input.stateModel(), input.path(), input.target(), env, overlay)
    }
}
