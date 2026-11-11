package org.example

import javafx.scene.canvas.Canvas
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.example.animation.IAnimationEngine
import org.example.model.ISimulationState
import org.example.rendering.CoordinateTransformer
import org.example.rendering.ISceneConfig
import org.example.rendering.ITargetRenderer
import org.example.ui.IControlPanel
import org.example.ui.ITargetInteractionCoordinator

/**
 * Contratos de ensamblaje de aplicación, objetivo, ventana y arranque JavaFX (ISP + DIP).
 */
interface ISimulationStageBuilder {
    fun mount(stage: Stage, controlsBox: VBox, canvasLayer: Pane, cfg: ISceneConfig)
}

interface IWorkspaceCoreBuilder {
    fun buildCore(canvas: Canvas, cfg: ISceneConfig): Triple<ISimulationState, CoordinateTransformer, org.example.rendering.IPathRenderer>
}

interface ITargetSubsystemBuilder {
    fun buildTargetSubsystem(canvas: Canvas, cfg: ISceneConfig, state: ISimulationState, transformer: CoordinateTransformer): Triple<Pane, ITargetRenderer, ITargetInteractionCoordinator>
}

interface ISimulationWorkspaceFactory {
    fun create(canvas: Canvas, cfg: ISceneConfig): ISimulationWorkspace
}

interface ISimulationAppAssembler {
    fun createWorkspace(canvas: Canvas, cfg: ISceneConfig): ISimulationWorkspace
    fun createEngine(ws: ISimulationWorkspace, controlsProvider: () -> IControlPanel): IAnimationEngine
    fun createControls(ws: ISimulationWorkspace, engine: IAnimationEngine): IControlPanel
}

interface ISimulationBootstrapper {
    fun launchStage(primaryStage: Stage)
}
