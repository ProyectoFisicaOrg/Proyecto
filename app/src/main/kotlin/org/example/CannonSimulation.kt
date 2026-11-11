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
import org.example.animation.IAnimationEngineFactory
import org.example.animation.IFlightPortFactory
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
import org.example.rendering.SceneRendererAssembler
import org.example.rendering.TargetRenderer
import org.example.scrubbing.ITrajectoryInspector
import org.example.scrubbing.PhysicsTrajectoryInspectorAdapter
import org.example.scrubbing.ScrubbingController
import org.example.ui.ControlPanel
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
        stage.scene = Scene(root, cfg.canvasWidth + 400.0, cfg.canvasHeight + 60.0)
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
 * Contexto encapsulado y cohesivo de los componentes visuales e interactivos de la escena (SRP + DIP).
 */
class SimulationWorkspace(
    state: ISimulationState,
    cfg: ISceneConfig,
    transformer: CoordinateTransformer,
    sceneLayer: Pane,
    pathRenderer: IPathRenderer,
    targetCoordinator: ITargetInteractionCoordinator,
    sceneRenderer: ISceneRenderer,
    private val inspectorFactory: (ITrajectoryMatrixSummary, CoordinateTransformer) -> ITrajectoryInspector = ::PhysicsTrajectoryInspectorAdapter
) : ISimulationWorkspace {
    private val modelContext: Triple<ISimulationState, ISceneConfig, CoordinateTransformer> = Triple(state, cfg, transformer)
    private val visualContext: Pair<Pane, ISceneRenderer> = Pair(sceneLayer, sceneRenderer)
    private val interactionContext: Pair<IPathRenderer, ITargetInteractionCoordinator> = Pair(pathRenderer, targetCoordinator)

    override fun stateModel(): ISimulationState {
        return modelContext.first
    }

    override fun sceneConfig(): ISceneConfig {
        return modelContext.second
    }

    override fun coordTransformer(): CoordinateTransformer {
        return modelContext.third
    }

    override fun canvasLayer(): Pane {
        return visualContext.first
    }

    override fun renderScene() {
        visualContext.second.render()
    }

    override fun recordTrailPoint(x: Double, y: Double) {
        interactionContext.first.addPoint(x, y)
    }

    override fun randomizeTarget() {
        interactionContext.second.randomize()
    }

    override fun installOn(canvas: Canvas) {
        visualContext.second.scrubbingController?.install(canvas)
        interactionContext.second.install()
    }

    override fun prepareForLaunch() {
        interactionContext.second.prepareForLaunch()
        visualContext.second.scrubbingController?.disable()
        interactionContext.first.clear()
        interactionContext.first.recordLaunch()
    }

    override fun resetInteractionAndTrail() {
        visualContext.second.scrubbingController?.disable()
        interactionContext.first.clear()
        interactionContext.second.unlockEditing()
    }

    override fun onFlightFinishedWithImpact(matrix: ITrajectoryMatrixSummary) {
        interactionContext.first.freeze()
        visualContext.second.scrubbingController?.enable(inspectorFactory(matrix, modelContext.third))
        visualContext.second.render()
    }
}

/**
 * Ensamblador dedicado al subsistema interactivo del blanco y su píldora editable (SRP + DIP).
 */
class DefaultTargetSubsystemBuilder(
    private val onRepaint: () -> Unit
) : ITargetSubsystemBuilder {

    override fun buildTargetSubsystem(canvas: Canvas, cfg: ISceneConfig, state: ISimulationState, transformer: CoordinateTransformer): Triple<Pane, ITargetRenderer, ITargetInteractionCoordinator> {
        val pill = JavaFxTargetOverlayPill(cfg)
        val distCtrl = createDistanceController(state, transformer, pill)
        val target: ITargetRenderer = TargetRenderer(
            canvas.graphicsContext2D, cfg, canvas, state,
            onTargetPositionChanged = { x -> state.targetX = x; distCtrl.syncFromCanvasX(x); onRepaint() },
            onRepaint = onRepaint
        )
        return Triple(createOverlayLayer(canvas, pill, cfg), target, TargetInteractionCoordinator(target, distCtrl))
    }

    private fun createDistanceController(state: ISimulationState, tr: CoordinateTransformer, pill: JavaFxTargetOverlayPill): ITargetDistanceController {
        val spec = TargetDistanceSpec(18.0, 312.0, tr.toPhysicsX(state.targetX))
        val controller = TargetDistanceController(spec, tr, pill, { newX -> state.targetX = newX; onRepaint() })
        controller.bind()
        controller.syncFromCanvasX(state.targetX)
        return controller
    }

    private fun createOverlayLayer(canvas: Canvas, pill: JavaFxTargetOverlayPill, cfg: ISceneConfig): Pane {
        return Pane(canvas, pill.node()).apply {
            prefWidth = cfg.canvasWidth
            prefHeight = cfg.canvasHeight
            minWidth = cfg.canvasWidth
            minHeight = cfg.canvasHeight
            maxWidth = cfg.canvasWidth
            maxHeight = cfg.canvasHeight
        }
    }
}

/**
 * Constructor cohesivo de estado, transformador y estela base del espacio de trabajo (SRP + DIP).
 */
class DefaultWorkspaceCoreBuilder : IWorkspaceCoreBuilder {

    override fun buildCore(canvas: Canvas, cfg: ISceneConfig): Triple<ISimulationState, CoordinateTransformer, IPathRenderer> {
        val state: ISimulationState = SimulationState()
        val tr: CoordinateTransformer = DefaultCoordinateTransformer(cfg, 2.5)
        val path: IPathRenderer = PathRenderer(canvas.graphicsContext2D)
        return Triple(state, tr, path)
    }
}

/**
 * Implementación modular y concisa de [ISimulationWorkspaceFactory] (SRP + DIP).
 */
class DefaultSimulationWorkspaceFactory(
    coreBuilder: IWorkspaceCoreBuilder = DefaultWorkspaceCoreBuilder(),
    builderFactory: (() -> Unit) -> ITargetSubsystemBuilder = ::DefaultTargetSubsystemBuilder
) : ISimulationWorkspaceFactory {
    private val builders: Pair<IWorkspaceCoreBuilder, (() -> Unit) -> ITargetSubsystemBuilder> = Pair(coreBuilder, builderFactory)

    override fun create(canvas: Canvas, cfg: ISceneConfig): ISimulationWorkspace {
        val core = builders.first.buildCore(canvas, cfg)
        lateinit var scene: ISceneRenderer
        val targetPack = builders.second { scene.render() }.buildTargetSubsystem(canvas, cfg, core.first, core.second)
        scene = SceneRendererAssembler(cfg, core.second).assemble(canvas.graphicsContext2D, core.first, core.third, targetPack.second)
        scene.scrubbingController = ScrubbingController(core.first, core.second, cfg, null, { scene.render() })
        return SimulationWorkspace(core.first, cfg, core.second, targetPack.first, core.third, targetPack.third, scene)
    }
}

/**
 * Ensamblador concreto de motor y controles sobre [ISimulationWorkspace] (SRP + DIP).
 */
class DefaultSimulationAppAssembler(
    workspaceFactory: ISimulationWorkspaceFactory = DefaultSimulationWorkspaceFactory(),
    portFactory: IFlightPortFactory = DefaultFlightPortFactory(),
    engineFactory: IAnimationEngineFactory = DefaultAnimationEngineFactory()
) : ISimulationAppAssembler {
    private val factories: Triple<ISimulationWorkspaceFactory, IFlightPortFactory, IAnimationEngineFactory> =
        Triple(workspaceFactory, portFactory, engineFactory)

    override fun createWorkspace(canvas: Canvas, cfg: ISceneConfig): ISimulationWorkspace {
        return factories.first.create(canvas, cfg)
    }

    override fun createEngine(ws: ISimulationWorkspace, controlsProvider: () -> IControlPanel): IAnimationEngine {
        val kin = factories.second.createKinematics(ws.stateModel(), ws.sceneConfig(), ws.coordTransformer())
        val eval = factories.second.createEvaluator(ws.coordTransformer())
        val hooks = FlightEventHooks(
            onUpdate = { ws.renderScene() },
            onFinished = { controlsProvider().setAnimating(false) },
            onTick = { x, y -> ws.recordTrailPoint(x, y) },
            collisionListener = CollisionCallbackAdapter { controlsProvider().showCollisionResult(it) },
            onOutOfBounds = { handleOutOfBounds(ws, controlsProvider()) },
            onFlightCompleted = { ws.onFlightFinishedWithImpact(it) }
        )
        return factories.third.create(kin, eval, hooks, 1.85)
    }

    private fun handleOutOfBounds(ws: ISimulationWorkspace, controls: IControlPanel) {
        ws.resetInteractionAndTrail()
        controls.setAnimating(false)
        controls.showOutOfBoundsMessage()
    }

    override fun createControls(ws: ISimulationWorkspace, engine: IAnimationEngine): IControlPanel {
        lateinit var panel: IControlPanel
        panel = ControlPanel(
            state = ws.stateModel(),
            onFire = { handleFire(ws, engine, panel) },
            onAngleChange = { ws.renderScene() },
            onReset = { handleReset(ws, engine, panel) },
            onRandomizeTarget = { ws.randomizeTarget() }
        )
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

/**
 * Arrancador cohesivo de la aplicación JavaFX desacoplado por interfaces (SRP + DIP).
 */
class DefaultSimulationBootstrapper(
    private val assembler: ISimulationAppAssembler = DefaultSimulationAppAssembler(),
    private val stageBuilder: ISimulationStageBuilder = SimulationStageBuilder(),
    private val configProvider: () -> ISceneConfig = ::SceneConfig
) : ISimulationBootstrapper {

    override fun launchStage(primaryStage: Stage) {
        val cfg = configProvider()
        val canvas = Canvas(cfg.canvasWidth, cfg.canvasHeight)
        val workspace = assembler.createWorkspace(canvas, cfg)
        lateinit var controls: IControlPanel
        val engine = assembler.createEngine(workspace) { controls }
        controls = assembler.createControls(workspace, engine)
        stageBuilder.mount(primaryStage, controls.build(), workspace.canvasLayer(), cfg)
        workspace.installOn(canvas)
        workspace.renderScene()
    }
}

/**
 * Punto de entrada de la aplicación JavaFX de Simulación de Cañón desacoplado por DI (SRP + DIP).
 */
class CannonSimulation(
    private val bootstrapper: ISimulationBootstrapper = DefaultSimulationBootstrapper()
) : Application() {

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
