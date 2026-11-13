package org.example.rendering

import javafx.scene.canvas.GraphicsContext
import org.example.model.ISimulationState
import org.example.scrubbing.IScrubbingController

/**
 * Contratos de pintado ambiental, superposiciones interactivas y ensamblaje de escena (ISP + DIP).
 */
interface ISceneEnvironmentPainter {
    fun clearAndDrawBackdrop()
    fun drawCannonAndAngle(angle: Double)
    fun drawProjectile(x: Double, y: Double)
}

interface IScrubbingFeedbackPainter {
    fun renderScrubbing(gc: GraphicsContext, controller: IScrubbingController, coordsAndBounds: List<Double>)
}

interface IScrubbingFeedbackWiring {
    fun createFeedbackPainter(): IScrubbingFeedbackPainter
}

interface ISceneOverlayRenderer {
    fun renderOverlay(controller: IScrubbingController?)
}

interface ISceneRenderInput {
    fun graphics(): GraphicsContext
    fun sceneConfig(): ISceneConfig
    fun coordTransformer(): CoordinateTransformer
    fun stateModel(): ISimulationState
    fun path(): IPathRenderer
    fun target(): ITargetRenderer
}

interface ISceneRendererWiring {
    fun assemble(input: ISceneRenderInput): ISceneRenderer
}

interface ISceneRenderer {
    fun currentScrubbingController(): IScrubbingController?
    fun assignScrubbingController(controller: IScrubbingController?)
    fun render()
}

interface ISceneryBuilder {
    fun buildScenery(gc: GraphicsContext, cfg: ISceneConfig): IScenery
}

interface ICannonBuilder {
    fun buildCannon(gc: GraphicsContext, cfg: ISceneConfig): ICannonRenderer
}

interface ISceneEnvBuilder {
    fun buildEnvironment(gc: GraphicsContext, cfg: ISceneConfig): ISceneEnvironmentPainter
}

interface ISceneOverlayBuilder {
    fun buildOverlay(gc: GraphicsContext, state: ISimulationState, cfg: ISceneConfig, transformer: CoordinateTransformer): ISceneOverlayRenderer
}
