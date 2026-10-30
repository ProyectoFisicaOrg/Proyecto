package org.example

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.example.animation.AnimationEngine
import org.example.animation.CollisionListener
import org.example.animation.IAnimationEngine
import org.example.model.SimulationState
import org.example.model.TrajectoryMatrix
import org.example.rendering.CoordinateTransformer
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.IPathRenderer
import org.example.rendering.ISceneConfig
import org.example.rendering.ISceneRenderer
import org.example.rendering.ITargetRenderer
import org.example.rendering.PathRenderer
import org.example.rendering.SceneConfig
import org.example.rendering.SceneRenderer
import org.example.rendering.TargetRenderer
import org.example.scrubbing.IScrubbingController
import org.example.scrubbing.PhysicsTrajectoryInspectorAdapter
import org.example.scrubbing.ScrubbingController
import org.example.ui.ControlPanel
import org.example.ui.IControlPanel
import org.example.ui.ITargetDistanceController
import org.example.ui.JavaFxTargetOverlayPill
import org.example.ui.TargetDistanceController
import org.example.ui.TargetDistanceSpec

/**
 * Abstracción para el montaje de la ventana principal JavaFX (DIP).
 */
interface ISimulationStageBuilder {
    fun mount(stage: Stage, controlsBox: VBox, canvasLayer: Pane, cfg: ISceneConfig)
}

/**
 * Constructor cohesivo de la ventana principal y layout JavaFX (SRP).
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

    private fun createCanvasContainer(canvasLayer: Pane): VBox =
        VBox(canvasLayer).apply {
            style = "-fx-alignment: center; -fx-background-color: #0f0f23; -fx-background-radius: 12; -fx-border-color: #0f3460; -fx-border-width: 1; -fx-border-radius: 12;"
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
    private val targetRenderer: ITargetRenderer,
    private val distanceController: ITargetDistanceController
) {
    fun install() {
        targetRenderer.installDrag()
        targetRenderer.startPulse()
    }

    fun randomize() {
        targetRenderer.randomizePosition()
    }

    fun prepareForLaunch() {
        targetRenderer.cancelDrag()
        distanceController.prepareForLaunch()
    }

    fun unlockEditing() {
        distanceController.unlockForEditing()
    }
}

/**
 * Contrato abstracto del espacio de trabajo visual e interactivo de la escena (ISP + DIP).
 */
interface ISimulationWorkspace {
    fun stateModel(): SimulationState
    fun sceneConfig(): SceneConfig
    fun coordTransformer(): CoordinateTransformer
    fun canvasLayer(): Pane
    fun renderScene()
    fun recordTrailPoint(x: Double, y: Double)
    fun randomizeTarget()
    fun installOn(canvas: Canvas)
    fun prepareForLaunch()
    fun resetInteractionAndTrail()
    fun onFlightFinishedWithImpact(matrix: TrajectoryMatrix)
}

/**
 * Contexto encapsulado de los componentes visuales e interactivos de la escena (SRP + DIP).
 */
class SimulationWorkspace(
    private val state: SimulationState,
    private val cfg: SceneConfig,
    private val transformer: CoordinateTransformer,
    private val sceneLayer: Pane,
    private val pathRenderer: IPathRenderer,
    private val targetCoordinator: TargetInteractionCoordinator,
    private val sceneRenderer: ISceneRenderer
) : ISimulationWorkspace {
    override fun stateModel(): SimulationState = state

    override fun sceneConfig(): SceneConfig = cfg

    override fun coordTransformer(): CoordinateTransformer = transformer

    override fun canvasLayer(): Pane = sceneLayer

    override fun renderScene() = sceneRenderer.render()

    override fun recordTrailPoint(x: Double, y: Double) = pathRenderer.addPoint(x, y)

    override fun randomizeTarget() = targetCoordinator.randomize()

    override fun installOn(canvas: Canvas) {
        sceneRenderer.scrubbingController?.install(canvas)
        targetCoordinator.install()
    }

    override fun prepareForLaunch() {
        targetCoordinator.prepareForLaunch()
        sceneRenderer.scrubbingController?.disable()
        pathRenderer.clear()
        pathRenderer.recordLaunch()
    }

    override fun resetInteractionAndTrail() {
        sceneRenderer.scrubbingController?.disable()
        pathRenderer.clear()
        targetCoordinator.unlockEditing()
    }

    override fun onFlightFinishedWithImpact(matrix: TrajectoryMatrix) {
        pathRenderer.freeze()
        sceneRenderer.scrubbingController?.enable(PhysicsTrajectoryInspectorAdapter(matrix, transformer))
        sceneRenderer.render()
    }
}

/**
 * Fábrica dedicada a construir el espacio de trabajo visual e interactivo (SRP + DIP).
 */
interface ISimulationWorkspaceFactory {
    fun create(canvas: Canvas, cfg: SceneConfig): ISimulationWorkspace
}

/**
 * Implementación por defecto de [ISimulationWorkspaceFactory] (SRP + DIP).
 */
class DefaultSimulationWorkspaceFactory : ISimulationWorkspaceFactory {
    override fun create(canvas: Canvas, cfg: SceneConfig): ISimulationWorkspace {
        val state = SimulationState()
        val transformer = DefaultCoordinateTransformer(cfg, pixelsPerMeter = 2.5)
        val gc = canvas.graphicsContext2D
        val pathRenderer: IPathRenderer = PathRenderer(gc)

        lateinit var sceneRenderer: ISceneRenderer
        lateinit var distanceController: ITargetDistanceController
        val targetRenderer: ITargetRenderer = TargetRenderer(
            gc = gc, cfg = cfg, canvas = canvas, state = state,
            onTargetPositionChanged = { newX ->
                state.targetX = newX
                distanceController.syncFromCanvasX(newX)
                sceneRenderer.render()
            },
            onRepaint = { sceneRenderer.render() }
        )
        sceneRenderer = SceneRenderer(gc, state, cfg, pathRenderer, targetRenderer, transformer)
        sceneRenderer.scrubbingController = ScrubbingController(state = state, transformer = transformer, cfg = cfg, onRepaint = { sceneRenderer.render() })

        val pill = JavaFxTargetOverlayPill(cfg)
        distanceController = createTargetDistanceController(state, transformer, pill) { sceneRenderer.render() }
        val sceneLayer = createOverlayLayer(canvas, pill, cfg)
        val coordinator = TargetInteractionCoordinator(targetRenderer, distanceController)
        return SimulationWorkspace(state, cfg, transformer, sceneLayer, pathRenderer, coordinator, sceneRenderer)
    }

    private fun createTargetDistanceController(state: SimulationState, transformer: CoordinateTransformer, pill: JavaFxTargetOverlayPill, onRepaint: () -> Unit): ITargetDistanceController {
        val spec = TargetDistanceSpec(minVal = 18.0, maxVal = 312.0, defaultVal = transformer.toPhysicsX(state.targetX))
        val controller = TargetDistanceController(spec, transformer, pill, { newCanvasX ->
            state.targetX = newCanvasX
            onRepaint()
        })
        controller.bind()
        controller.syncFromCanvasX(state.targetX)
        return controller
    }

    private fun createOverlayLayer(canvas: Canvas, pill: JavaFxTargetOverlayPill, cfg: ISceneConfig): Pane =
        Pane(canvas, pill.node()).apply {
            prefWidth = cfg.canvasWidth
            prefHeight = cfg.canvasHeight
            minWidth = cfg.canvasWidth
            minHeight = cfg.canvasHeight
            maxWidth = cfg.canvasWidth
            maxHeight = cfg.canvasHeight
        }
}

/**
 * Abstracción de fábrica para el cableado de subsistemas de la aplicación (DIP).
 */
interface ISimulationAppAssembler {
    fun createWorkspace(canvas: Canvas, cfg: SceneConfig): ISimulationWorkspace
    fun createEngine(ws: ISimulationWorkspace, controlsProvider: () -> IControlPanel): IAnimationEngine
    fun createControls(ws: ISimulationWorkspace, engine: IAnimationEngine): IControlPanel
}

/**
 * Ensamblador concreto de motor y controles sobre [ISimulationWorkspace] (SRP + DIP).
 */
class DefaultSimulationAppAssembler(
    private val workspaceFactory: ISimulationWorkspaceFactory = DefaultSimulationWorkspaceFactory()
) : ISimulationAppAssembler {

    override fun createWorkspace(canvas: Canvas, cfg: SceneConfig): ISimulationWorkspace =
        workspaceFactory.create(canvas, cfg)

    override fun createEngine(ws: ISimulationWorkspace, controlsProvider: () -> IControlPanel): IAnimationEngine =
        AnimationEngine(
            state = ws.stateModel(),
            cfg = ws.sceneConfig(),
            transformer = ws.coordTransformer(),
            onUpdate = { ws.renderScene() },
            onFinished = { controlsProvider().setAnimating(false) },
            onTick = { x, y -> ws.recordTrailPoint(x, y) },
            collisionListener = CollisionListener { controlsProvider().showCollisionResult(it) },
            onOutOfBounds = { handleOutOfBounds(ws, controlsProvider()) },
            onFlightCompleted = { ws.onFlightFinishedWithImpact(it) },
            timeScale = 1.85
        )

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
 * Punto de entrada de la aplicación JavaFX de Simulación de Cañón desacoplado por DI (SRP + DIP).
 */
class CannonSimulation(
    private val assembler: ISimulationAppAssembler = DefaultSimulationAppAssembler(),
    private val stageBuilder: ISimulationStageBuilder = SimulationStageBuilder()
) : Application() {

    override fun start(primaryStage: Stage) {
        val cfg = SceneConfig()
        val canvas = Canvas(cfg.canvasWidth, cfg.canvasHeight)
        val workspace = assembler.createWorkspace(canvas, cfg)
        lateinit var controls: IControlPanel
        val engine = assembler.createEngine(workspace) { controls }
        controls = assembler.createControls(workspace, engine)

        stageBuilder.mount(primaryStage, controls.build(), workspace.canvasLayer(), cfg)
        workspace.installOn(canvas)
        workspace.renderScene()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(CannonSimulation::class.java, *args)
        }
    }
}
