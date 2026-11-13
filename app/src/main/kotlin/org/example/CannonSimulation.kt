package org.example

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.example.animation.CollisionCallbackAdapter
import org.example.animation.DefaultAnimationEngineFactory
import org.example.animation.DefaultFlightPortFactory
import org.example.animation.FlightEventHooks
import org.example.animation.IAnimationEngine
import org.example.animation.IFlightCompletionNotifier
import org.example.model.ISimulationState
import org.example.model.ITrajectoryMatrixSummary
import org.example.model.SimulationState
import org.example.rendering.CoordinateTransformer
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.IPathRenderer
import org.example.rendering.ISceneConfig
import org.example.rendering.ISceneRenderer
import org.example.rendering.ITargetRenderer
import org.example.rendering.PathRenderer
import org.example.rendering.SceneConfig
import org.example.rendering.SceneRenderInput
import org.example.rendering.SceneRendererWiring
import org.example.rendering.TargetRenderer
import org.example.scrubbing.ITrajectoryInspector
import org.example.scrubbing.PhysicsTrajectoryInspectorAdapter
import org.example.scrubbing.ScrubbingController
import org.example.ui.ControlInitSpec
import org.example.ui.ControlPanelWiring
import org.example.ui.IControlPanel
import org.example.ui.ITargetDistanceController
import org.example.ui.ITargetInteractionCoordinator
import org.example.ui.JavaFxTargetOverlayPill
import org.example.ui.TargetDistanceController
import org.example.ui.TargetDistanceSpec

/**
 * Constructor cohesivo de la ventana principal y layout JavaFX (SRP + DIP).
 */
class SimulationStageBuilder : ISimulationStageBuilder {

    override fun mount(stage: Stage, controlsBox: VBox, canvasLayer: Pane, cfg: ISceneConfig) {
        stage.title = "Simulación de Cañón"
        val canvasContainer = createCanvasContainer(canvasLayer)
        val root = createMainLayout(controlsBox, canvasContainer)
        stage.scene = Scene(root, cfg.width() + 400.0, cfg.height() + 60.0)
        stage.minWidth = 1000.0
        stage.minHeight = 650.0
        stage.show()
    }

    private fun createCanvasContainer(canvasLayer: Pane): VBox {
        return VBox(canvasLayer).apply {
            style = "-fx-alignment: center; -fx-background-color: #0f0f23; -fx-background-radius: 12; -fx-border-color: #0f3460; -fx-border-width: 1; -fx-border-radius: 12;"
        }
    }

    private fun createMainLayout(controlsPanel: VBox, canvasContainer: VBox): HBox {
        val layout = HBox(30.0, controlsPanel, canvasContainer).apply {
            style = "-fx-alignment: center; -fx-padding: 20; -fx-background-color: linear-gradient(to bottom, rgb(135,206,235) 0%, rgb(176,224,230) 60%, rgb(34,139,34) 100%);"
        }
        HBox.setHgrow(canvasContainer, Priority.ALWAYS)
        return layout
    }
}

/**
 * Coordinador cohesivo del objetivo arrastrable y su campo editable de distancia (SRP + DIP).
 */
class TargetInteractionCoordinator(
    targetRenderer: ITargetRenderer,
    distanceController: ITargetDistanceController
) : ITargetInteractionCoordinator {
    private val pair: Pair<ITargetRenderer, ITargetDistanceController> = Pair(targetRenderer, distanceController)

    override fun install() {
        pair.first.installDrag()
        pair.first.startPulse()
    }

    override fun randomize() {
        pair.first.randomizePosition()
    }

    override fun prepareForLaunch() {
        pair.first.cancelDrag()
        pair.second.prepareForLaunch()
    }

    override fun unlockEditing() {
        pair.second.unlockForEditing()
    }
}

/**
 * Núcleo inmutable de estado, configuración, transformador y estela del espacio de trabajo (SRP).
 */
@ConsistentCopyVisibility
data class WorkspaceCore internal constructor(
    private val pack: Pair<Pair<ISimulationState, ISceneConfig>, Pair<CoordinateTransformer, IPathRenderer>>
) : IWorkspaceCore {

    constructor(
        state: ISimulationState,
        cfg: ISceneConfig,
        transformer: CoordinateTransformer,
        pathRenderer: IPathRenderer
    ) : this(Pair(Pair(state, cfg), Pair(transformer, pathRenderer)))

    override fun stateModel(): ISimulationState {
        return pack.first.first
    }

    override fun sceneConfig(): ISceneConfig {
        return pack.first.second
    }

    override fun coordTransformer(): CoordinateTransformer {
        return pack.second.first
    }

    override fun pathRenderer(): IPathRenderer {
        return pack.second.second
    }
}

/**
 * Contenedor inmutable de capas visuales y coordinadores de escena (SRP).
 */
@ConsistentCopyVisibility
data class WorkspaceVisuals internal constructor(
    private val pack: Triple<Pane, ITargetInteractionCoordinator, ISceneRenderer>
) : IWorkspaceVisuals {

    constructor(
        sceneLayer: Pane,
        targetCoordinator: ITargetInteractionCoordinator,
        sceneRenderer: ISceneRenderer
    ) : this(Triple(sceneLayer, targetCoordinator, sceneRenderer))

    override fun canvasLayer(): Pane {
        return pack.first
    }

    override fun targetCoordinator(): ITargetInteractionCoordinator {
        return pack.second
    }

    override fun sceneRenderer(): ISceneRenderer {
        return pack.third
    }
}

/**
 * Contexto encapsulado y cohesivo de los componentes visuales e interactivos de la escena (SRP + DIP).
 */
class SimulationWorkspace(
    core: IWorkspaceCore,
    visuals: IWorkspaceVisuals,
    inspectorFactory: (ITrajectoryMatrixSummary, CoordinateTransformer) -> ITrajectoryInspector = ::PhysicsTrajectoryInspectorAdapter
) : ISimulationWorkspace, IWorkspaceCore by core {
    private val ctx: Triple<IWorkspaceCore, IWorkspaceVisuals, (ITrajectoryMatrixSummary, CoordinateTransformer) -> ITrajectoryInspector> =
        Triple(core, visuals, inspectorFactory)

    override fun canvasLayer(): Pane {
        return ctx.second.canvasLayer()
    }

    override fun renderScene() {
        ctx.second.sceneRenderer().render()
    }

    override fun recordTrailPoint(x: Double, y: Double) {
        ctx.first.pathRenderer().addPoint(x, y)
    }

    override fun randomizeTarget() {
        ctx.second.targetCoordinator().randomize()
    }

    override fun installOn(canvas: Canvas) {
        ctx.second.sceneRenderer().currentScrubbingController()?.install(canvas)
        ctx.second.targetCoordinator().install()
    }

    override fun prepareForLaunch() {
        val core = ctx.first
        val vis = ctx.second
        vis.targetCoordinator().prepareForLaunch()
        vis.sceneRenderer().currentScrubbingController()?.disable()
        core.pathRenderer().clear()
        core.pathRenderer().recordLaunch()
    }

    override fun resetInteractionAndTrail() {
        val vis = ctx.second
        vis.sceneRenderer().currentScrubbingController()?.disable()
        ctx.first.pathRenderer().clear()
        vis.targetCoordinator().unlockEditing()
    }

    override fun onFlightFinishedWithImpact(matrix: ITrajectoryMatrixSummary) {
        val core = ctx.first
        val vis = ctx.second
        core.pathRenderer().freeze()
        vis.sceneRenderer().currentScrubbingController()?.enable(ctx.third(matrix, core.coordTransformer()))
        vis.sceneRenderer().render()
    }
}

/**
 * Constructores modulares de subsistemas de escena y espacio de trabajo conforme a DIP.
 */
object DefaultWorkspaceCoreBuilder : IWorkspaceCoreBuilder {
    override fun buildCore(canvas: Canvas, cfg: ISceneConfig): IWorkspaceCore {
        return WorkspaceCore(SimulationState(), cfg, DefaultCoordinateTransformer(cfg, 2.5), PathRenderer(canvas.graphicsContext2D))
    }
}

object TargetDistanceWiring : ITargetDistanceWiring {
    override fun createPillAndController(core: IWorkspaceCore, onRepaint: () -> Unit): Pair<Pane, ITargetDistanceController> {
        val st = core.stateModel()
        val tr = core.coordTransformer()
        val pill = JavaFxTargetOverlayPill(core.sceneConfig())
        val spec = TargetDistanceSpec(18.0, 312.0, tr.toPhysicsX(st.currentTargetX()))
        val ctrl = TargetDistanceController(spec, tr, pill, { newX -> st.changeTargetX(newX); onRepaint() })
        ctrl.bind()
        ctrl.syncFromCanvasX(st.currentTargetX())
        return Pair(pill.node(), ctrl)
    }
}

object DefaultTargetSubsystemBuilder : ITargetSubsystemBuilder {
    override fun buildTargetSubsystem(canvas: Canvas, core: IWorkspaceCore, onRepaint: () -> Unit): Pair<Pane, Pair<ITargetRenderer, ITargetInteractionCoordinator>> {
        val st = core.stateModel()
        val pillCtrl = TargetDistanceWiring.createPillAndController(core, onRepaint)
        val target = TargetRenderer(
            canvas.graphicsContext2D, core.sceneConfig(), canvas, st,
            onTargetPositionChanged = { x -> st.changeTargetX(x); pillCtrl.second.syncFromCanvasX(x); onRepaint() },
            onRepaint = onRepaint
        )
        val w = core.sceneConfig().width()
        val h = core.sceneConfig().height()
        val layer = Pane(canvas, pillCtrl.first).apply {
            prefWidth = w; prefHeight = h; minWidth = w; minHeight = h; maxWidth = w; maxHeight = h
        }
        return Pair(layer, Pair(target, TargetInteractionCoordinator(target, pillCtrl.second)))
    }
}

object SceneWiring : ISceneWiring {
    override fun buildScene(canvas: Canvas, core: IWorkspaceCore, target: ITargetRenderer, onRepaint: () -> Unit): ISceneRenderer {
        val input = SceneRenderInput(canvas.graphicsContext2D, core.sceneConfig(), core.coordTransformer(), core.stateModel(), core.pathRenderer(), target)
        val scene = SceneRendererWiring.assemble(input)
        scene.assignScrubbingController(ScrubbingController(core.stateModel(), core.coordTransformer(), core.sceneConfig(), null, onRepaint))
        return scene
    }
}

object WorkspaceVisualsWiring : IWorkspaceVisualsWiring {
    override fun buildVisuals(canvas: Canvas, core: IWorkspaceCore): IWorkspaceVisuals {
        lateinit var scene: ISceneRenderer
        val tp = DefaultTargetSubsystemBuilder.buildTargetSubsystem(canvas, core) { scene.render() }
        scene = SceneWiring.buildScene(canvas, core, tp.second.first) { scene.render() }
        return WorkspaceVisuals(tp.first, tp.second.second, scene)
    }
}

object DefaultSimulationWorkspaceFactory : ISimulationWorkspaceFactory {
    override fun create(canvas: Canvas, cfg: ISceneConfig): ISimulationWorkspace {
        val core = DefaultWorkspaceCoreBuilder.buildCore(canvas, cfg)
        return SimulationWorkspace(core, WorkspaceVisualsWiring.buildVisuals(canvas, core))
    }
}

object FlightHooksWiring : IFlightHooksWiring {
    override fun createHooks(ws: ISimulationWorkspace, controlsProvider: () -> IControlPanel): IFlightCompletionNotifier {
        return FlightEventHooks(
            onUpdate = { ws.renderScene() },
            onFinished = { controlsProvider().setAnimating(false) },
            onTick = { x, y -> ws.recordTrailPoint(x, y) },
            collisionListener = CollisionCallbackAdapter { controlsProvider().showCollisionResult(it) },
            onOutOfBounds = { handleOutOfBounds(ws, controlsProvider()) },
            onFlightCompleted = { ws.onFlightFinishedWithImpact(it) }
        )
    }

    private fun handleOutOfBounds(ws: ISimulationWorkspace, controls: IControlPanel) {
        ws.resetInteractionAndTrail()
        controls.setAnimating(false)
        controls.showOutOfBoundsMessage()
    }
}

object FlightEngineWiring : IFlightEngineWiring {
    override fun createEngine(ws: ISimulationWorkspace, controlsProvider: () -> IControlPanel): IAnimationEngine {
        val kin = DefaultFlightPortFactory.createKinematics(ws.stateModel(), ws.sceneConfig(), ws.coordTransformer())
        val eval = DefaultFlightPortFactory.createEvaluator(ws.coordTransformer())
        val hooks = FlightHooksWiring.createHooks(ws, controlsProvider)
        return DefaultAnimationEngineFactory.create(kin, eval, hooks, 1.85)
    }
}

class SimControlsWiring : ISimControlsWiring {
    override fun buildInteractiveControls(ws: ISimulationWorkspace): IControlPanel {
        lateinit var panel: IControlPanel
        val engine = FlightEngineWiring.createEngine(ws) { panel }
        val spec = ControlInitSpec(
            state = ws.stateModel(),
            onFire = { handleFire(ws, engine, panel) },
            onAngleChange = { ws.renderScene() },
            onReset = { handleReset(ws, engine, panel) },
            onRandomizeTarget = { ws.randomizeTarget() }
        )
        panel = ControlPanelWiring.assemble(spec)
        return panel
    }

    private fun handleFire(ws: ISimulationWorkspace, engine: IAnimationEngine, controls: IControlPanel) {
        ws.prepareForLaunch()
        controls.clearCollisionResult()
        controls.setAnimating(true)
        engine.start()
    }

    private fun handleReset(ws: ISimulationWorkspace, engine: IAnimationEngine, controls: IControlPanel) {
        ws.resetInteractionAndTrail()
        engine.reset()
        controls.clearCollisionResult()
        ws.renderScene()
    }
}

object DefaultSimulationAppAssembler : ISimulationAppAssembler {
    override fun defaultConfig(): ISceneConfig {
        return SceneConfig()
    }

    override fun createWorkspace(canvas: Canvas, cfg: ISceneConfig): ISimulationWorkspace {
        return DefaultSimulationWorkspaceFactory.create(canvas, cfg)
    }

    override fun buildControls(ws: ISimulationWorkspace): IControlPanel {
        return SimControlsWiring().buildInteractiveControls(ws)
    }
}

/**
 * Arrancador cohesivo de la aplicación JavaFX desacoplado por interfaces (SRP + DIP).
 */
class DefaultSimulationBootstrapper(
    assembler: ISimulationAppAssembler = DefaultSimulationAppAssembler,
    stageBuilderProvider: () -> ISimulationStageBuilder = ::SimulationStageBuilder
) : ISimulationBootstrapper {
    private val deps: Pair<ISimulationAppAssembler, () -> ISimulationStageBuilder> = Pair(assembler, stageBuilderProvider)

    override fun launchStage(primaryStage: Stage) {
        val asm = deps.first
        val cfg = asm.defaultConfig()
        val canvas = Canvas(cfg.width(), cfg.height())
        val workspace = asm.createWorkspace(canvas, cfg)
        val controls = asm.buildControls(workspace)
        deps.second().mount(primaryStage, controls.build(), workspace.canvasLayer(), cfg)
        workspace.installOn(canvas)
        workspace.renderScene()
    }
}

/**
 * Punto de entrada de la aplicación JavaFX de Simulación de Cañón desacoplado por DI (SRP + DIP).
 */
class CannonSimulation(
    private val bootstrapper: ISimulationBootstrapper
) : Application() {

    constructor(provider: () -> ISimulationBootstrapper = ::DefaultSimulationBootstrapper) : this(provider())

    override fun start(primaryStage: Stage) {
        bootstrapper.launchStage(primaryStage)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(CannonSimulation::class.java, *args)
        }
    }
}
