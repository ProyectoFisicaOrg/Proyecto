package org.example

import javafx.scene.canvas.Canvas
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.example.animation.IAnimationEngine
import org.example.animation.IFlightCompletionNotifier
import org.example.rendering.ISceneConfig
import org.example.rendering.ISceneRenderer
import org.example.rendering.ITargetRenderer
import org.example.ui.IControlPanel
import org.example.ui.ITargetDistanceController
import org.example.ui.ITargetInteractionCoordinator

/**
 * Contratos de ensamblaje de aplicación, objetivo, ventana y arranque JavaFX (ISP + DIP).
 */
interface ISimulationStageBuilder {
    fun mount(stage: Stage, controlsBox: VBox, canvasLayer: Pane, cfg: ISceneConfig)
}

interface IWorkspaceCoreBuilder {
    fun buildCore(canvas: Canvas, cfg: ISceneConfig): IWorkspaceCore
}

interface ITargetDistanceWiring {
    fun createPillAndController(core: IWorkspaceCore, onRepaint: () -> Unit): Pair<Pane, ITargetDistanceController>
}

interface ITargetSubsystemBuilder {
    fun buildTargetSubsystem(canvas: Canvas, core: IWorkspaceCore, onRepaint: () -> Unit): Pair<Pane, Pair<ITargetRenderer, ITargetInteractionCoordinator>>
}

interface ISceneWiring {
    fun buildScene(canvas: Canvas, core: IWorkspaceCore, target: ITargetRenderer, onRepaint: () -> Unit): ISceneRenderer
}

interface IWorkspaceVisualsWiring {
    fun buildVisuals(canvas: Canvas, core: IWorkspaceCore): IWorkspaceVisuals
}

interface ISimulationWorkspaceFactory {
    fun create(canvas: Canvas, cfg: ISceneConfig): ISimulationWorkspace
}

interface IFlightHooksWiring {
    fun createHooks(ws: ISimulationWorkspace, controlsProvider: () -> IControlPanel): IFlightCompletionNotifier
}

interface IFlightEngineWiring {
    fun createEngine(ws: ISimulationWorkspace, controlsProvider: () -> IControlPanel): IAnimationEngine
}

interface ISimControlsWiring {
    fun buildInteractiveControls(ws: ISimulationWorkspace): IControlPanel
}

interface ISimulationAppAssembler {
    fun defaultConfig(): ISceneConfig
    fun createWorkspace(canvas: Canvas, cfg: ISceneConfig): ISimulationWorkspace
    fun buildControls(ws: ISimulationWorkspace): IControlPanel
}

interface ISimulationBootstrapper {
    fun launchStage(primaryStage: Stage)
}
