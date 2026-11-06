package org.example.scrubbing

import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import kotlin.math.hypot
import org.example.model.ISimulationState
import org.example.rendering.CoordinateTransformer
import org.example.rendering.ISceneConfig

/**
 * Estado inmutable encapsulado de la interacción de scrubbing (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class ScrubbingState internal constructor(
    private val statePair: Pair<List<Boolean>, ISpanishKinematicView?>
) : IScrubbingState {

    constructor(
        enabledFlag: Boolean = false,
        draggingFlag: Boolean = false,
        hoveredFlag: Boolean = false,
        currentPoint: ISpanishKinematicView? = null
    ) : this(Pair(listOf(enabledFlag, draggingFlag, hoveredFlag), currentPoint))

    override fun selectedPoint(): ISpanishKinematicView? {
        return statePair.second
    }

    override fun isInteracting(): Boolean {
        val flags = statePair.first
        return flags[0] && (flags[1] || flags[2])
    }

    override fun hasSelection(): Boolean {
        return statePair.second != null
    }
}

/**
 * Estado base encapsulado y cohesivo del ciclo de sesión de scrubbing (SRP + Encapsulación).
 */
abstract class ScrubbingSessionBase : IScrubbingController {
    private var flags: List<Boolean> = listOf(false, false, false)
    private var refs: Pair<ITrajectoryInspector?, ISpanishKinematicView?> = Pair(null, null)

    override fun checkEnabled(): Boolean {
        return flags[0]
    }

    override fun checkDragging(): Boolean {
        return flags[1]
    }

    override fun checkHovered(): Boolean {
        return flags[2]
    }

    override fun currentInspector(): ITrajectoryInspector? {
        return refs.first
    }

    override fun selectedTrajectoryPoint(): ISpanishKinematicView? {
        return refs.second
    }

    override fun getState(): IScrubbingState {
        return ScrubbingState(flags[0], flags[1], flags[2], refs.second)
    }

    protected fun activateSession(newInspector: ITrajectoryInspector) {
        flags = listOf(true, false, false)
        refs = Pair(newInspector, null)
    }

    protected fun clearSession() {
        flags = listOf(false, false, false)
        refs = Pair(null, null)
    }

    protected fun updateDraggingFlag(dragging: Boolean) {
        flags = listOf(flags[0], dragging, flags[2])
    }

    protected fun updateHoveredFlag(hovered: Boolean) {
        flags = listOf(flags[0], flags[1], hovered)
    }

    protected fun updateSelectedPoint(point: ISpanishKinematicView?) {
        refs = Pair(refs.first, point)
    }

    protected fun activeInspector(): ITrajectoryInspector? {
        return if (flags[0]) refs.first else null
    }
}

/**
 * Evaluador geométrico de hit-testing para proyectil, diana y curva de trayectoria (SRP + DIP).
 */
class ScrubbingHitTester(
    private val state: ISimulationState,
    private val transformer: CoordinateTransformer,
    private val cfg: ISceneConfig,
    hitThresholdPixels: Double,
    projectileHitRadius: Double
) : IScrubbingHitTester {
    private val thresholds: Pair<Double, Double> = Pair(hitThresholdPixels, projectileHitRadius)

    override fun isOverProjectile(canvasX: Double, canvasY: Double): Boolean {
        return hypot(canvasX - state.projectileX, canvasY - state.projectileY) <= thresholds.second
    }

    override fun isOverTarget(canvasX: Double, canvasY: Double): Boolean {
        return hypot(canvasX - state.targetX, canvasY - cfg.groundY) <= state.targetRadius + 6.0
    }

    override fun findCurvePointWithinThreshold(inspector: ITrajectoryInspector, canvasX: Double, canvasY: Double): ISpanishKinematicView? {
        val nearest = inspector.findNearestPointFromCursor(canvasX, canvasY)
        val nx = transformer.toCanvasX(nearest.x)
        val ny = transformer.toCanvasY(nearest.y)
        return if (hypot(canvasX - nx, canvasY - ny) <= thresholds.first) nearest else null
    }
}

/**
 * Adaptador de eventos JavaFX de ratón sobre el Canvas hacia el controlador de scrubbing (SRP + DIP).
 */
class ScrubbingCanvasInstaller(
    private val controller: IScrubbingController,
    private val hitTester: IScrubbingHitTester
) : ICanvasMouseInstaller {

    override fun bind(canvas: Canvas) {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED) { onPressed(canvas, it) }
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED) { onDragged(canvas, it) }
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED) { onReleased(canvas, it) }
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED) { onMoved(canvas, it) }
    }

    private fun onPressed(canvas: Canvas, event: MouseEvent) {
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (controller.handleMousePressed(local.x, local.y)) {
            canvas.cursor = Cursor.CLOSED_HAND
            event.consume()
        }
    }

    private fun onDragged(canvas: Canvas, event: MouseEvent) {
        if (!controller.isDragging) return
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (controller.handleMouseDragged(local.x, local.y)) {
            canvas.cursor = Cursor.CLOSED_HAND
            event.consume()
        }
    }

    private fun onReleased(canvas: Canvas, event: MouseEvent) {
        if (controller.handleMouseReleased()) {
            canvas.cursor = if (controller.isHovered) Cursor.HAND else Cursor.DEFAULT
            event.consume()
        }
    }

    private fun onMoved(canvas: Canvas, event: MouseEvent) {
        if (!controller.isEnabled || controller.isDragging) return
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (controller.handleMouseMoved(local.x, local.y)) {
            canvas.cursor = Cursor.HAND
        } else if (!hitTester.isOverTarget(local.x, local.y)) {
            canvas.cursor = Cursor.DEFAULT
        }
    }
}

/**
 * Controlador de eventos de mouse y arrastre (Scrubbing) sobre la trayectoria (SRP + DIP + OCP).
 */
class ScrubbingController(
    state: ISimulationState,
    transformer: CoordinateTransformer,
    cfg: ISceneConfig,
    onPointSelected: ((ISpanishKinematicView) -> Unit)? = null,
    onRepaint: (() -> Unit)? = null,
    hitThresholdPixels: Double = 40.0,
    projectileHitRadius: Double = 22.0
) : ScrubbingSessionBase() {
    private val context: Triple<ISimulationState, CoordinateTransformer, IScrubbingHitTester> = Triple(
        state,
        transformer,
        ScrubbingHitTester(state, transformer, cfg, hitThresholdPixels, projectileHitRadius)
    )
    private val callbacks: Pair<((ISpanishKinematicView) -> Unit)?, (() -> Unit)?> = Pair(onPointSelected, onRepaint)
    private val installer: ICanvasMouseInstaller = ScrubbingCanvasInstaller(this, context.third)

    override fun enable(inspector: ITrajectoryInspector) {
        activateSession(inspector)
        val st = context.first
        snapToPoint(inspector.findNearestPointFromCursor(st.projectileX, st.projectileY))
        callbacks.second?.invoke()
    }

    override fun disable() {
        clearSession()
        callbacks.second?.invoke()
    }

    override fun handleMousePressed(canvasX: Double, canvasY: Double): Boolean {
        val active = activeInspector() ?: return false
        val tester = context.third
        if (tester.isOverProjectile(canvasX, canvasY)) {
            return startDraggingAt(active.findNearestPointFromCursor(canvasX, canvasY))
        }
        if (tester.isOverTarget(canvasX, canvasY)) return false
        val curvePoint = tester.findCurvePointWithinThreshold(active, canvasX, canvasY) ?: return false
        return startDraggingAt(curvePoint)
    }

    private fun startDraggingAt(point: ISpanishKinematicView): Boolean {
        updateDraggingFlag(true)
        snapToPoint(point)
        callbacks.second?.invoke()
        return true
    }

    override fun handleMouseDragged(canvasX: Double, canvasY: Double): Boolean {
        if (!checkDragging()) return false
        val active = activeInspector() ?: return false
        snapToPoint(active.findNearestPointFromCursor(canvasX, canvasY))
        callbacks.second?.invoke()
        return true
    }

    override fun handleMouseReleased(): Boolean {
        if (!checkDragging()) return false
        updateDraggingFlag(false)
        callbacks.second?.invoke()
        return true
    }

    override fun handleMouseMoved(canvasX: Double, canvasY: Double): Boolean {
        if (checkDragging()) return false
        val active = activeInspector() ?: return false
        val hovered = computeHoverAt(active, canvasX, canvasY)
        if (hovered != checkHovered()) {
            updateHoveredFlag(hovered)
            callbacks.second?.invoke()
        }
        return checkHovered()
    }

    private fun computeHoverAt(active: ITrajectoryInspector, canvasX: Double, canvasY: Double): Boolean {
        val tester = context.third
        if (tester.isOverProjectile(canvasX, canvasY)) return true
        if (tester.isOverTarget(canvasX, canvasY)) return false
        return tester.findCurvePointWithinThreshold(active, canvasX, canvasY) != null
    }

    private fun snapToPoint(point: ISpanishKinematicView) {
        updateSelectedPoint(point)
        context.first.updateProjectilePosition(context.second.toCanvasX(point.x), context.second.toCanvasY(point.y))
        callbacks.first?.invoke(point)
    }

    override fun install(canvas: Canvas) {
        installer.bind(canvas)
    }
}
