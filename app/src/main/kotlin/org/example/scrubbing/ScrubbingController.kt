package org.example.scrubbing

import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import kotlin.math.hypot
import org.example.model.SimulationState
import org.example.rendering.CoordinateTransformer
import org.example.rendering.SceneConfig

/**
 * Estado inmutable de la interacción de scrubbing con consultas de dominio (SRP).
 */
data class ScrubbingState(
    private val enabledFlag: Boolean = false,
    private val draggingFlag: Boolean = false,
    private val hoveredFlag: Boolean = false,
    val currentPoint: TrajectoryPoint? = null
) {
    fun isInteracting(): Boolean = enabledFlag && (draggingFlag || hoveredFlag)

    fun hasSelection(): Boolean = currentPoint != null
}

/**
 * Contrato abstracto para el controlador de scrubbing sobre la trayectoria (DIP).
 */
interface IScrubbingController {
    val isEnabled: Boolean
    val isDragging: Boolean
    val isHovered: Boolean
    val currentPoint: TrajectoryPoint?
    val flightSummary: FlightSummary?
    fun getState(): ScrubbingState
    fun enable(inspector: ITrajectoryInspector)
    fun disable()
    fun install(canvas: Canvas)
}

/**
 * Estado base encapsulado del ciclo de sesión de scrubbing (SRP).
 */
abstract class ScrubbingSessionBase : IScrubbingController {
    var inspector: ITrajectoryInspector? = null
        protected set

    override var isEnabled: Boolean = false
        protected set

    override var isDragging: Boolean = false
        protected set

    override var isHovered: Boolean = false
        protected set

    override var currentPoint: TrajectoryPoint? = null
        protected set

    override val flightSummary: FlightSummary? get() = inspector?.getFlightSummary()

    override fun getState(): ScrubbingState =
        ScrubbingState(enabledFlag = isEnabled, draggingFlag = isDragging, hoveredFlag = isHovered, currentPoint = currentPoint)

    protected fun activateSession(newInspector: ITrajectoryInspector) {
        inspector = newInspector
        isEnabled = true
        isDragging = false
        isHovered = false
    }

    protected fun clearSession() {
        isEnabled = false
        isDragging = false
        isHovered = false
        inspector = null
        currentPoint = null
    }

    protected fun activeInspector(): ITrajectoryInspector? = if (isEnabled) inspector else null
}

/**
 * Evaluador geométrico de hit-testing para proyectil, diana y curva de trayectoria (SRP).
 */
private class ScrubbingHitTester(
    val state: SimulationState,
    val transformer: CoordinateTransformer,
    private val cfg: SceneConfig,
    private val hitThresholdPixels: Double,
    private val projectileHitRadius: Double
) {
    fun isOverProjectile(canvasX: Double, canvasY: Double): Boolean =
        hypot(canvasX - state.projectileX, canvasY - state.projectileY) <= projectileHitRadius

    fun isOverTarget(canvasX: Double, canvasY: Double): Boolean =
        hypot(canvasX - state.targetX, canvasY - cfg.groundY) <= state.targetRadius + 6.0

    fun findCurvePointWithinThreshold(inspector: ITrajectoryInspector, canvasX: Double, canvasY: Double): TrajectoryPoint? {
        val nearest = inspector.findNearestPointFromCursor(canvasX, canvasY)
        val nx = transformer.toCanvasX(nearest.x)
        val ny = transformer.toCanvasY(nearest.y)
        return if (hypot(canvasX - nx, canvasY - ny) <= hitThresholdPixels) nearest else null
    }
}

/**
 * Adaptador de eventos JavaFX de ratón sobre el Canvas hacia el controlador de scrubbing (SRP).
 */
private class ScrubbingCanvasInstaller(
    private val controller: ScrubbingController,
    private val hitTester: ScrubbingHitTester
) {
    fun bind(canvas: Canvas) {
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
    state: SimulationState,
    transformer: CoordinateTransformer,
    cfg: SceneConfig,
    var onPointSelected: ((TrajectoryPoint) -> Unit)? = null,
    var onRepaint: (() -> Unit)? = null,
    hitThresholdPixels: Double = 40.0,
    projectileHitRadius: Double = 22.0
) : ScrubbingSessionBase() {

    private val hitTester = ScrubbingHitTester(state, transformer, cfg, hitThresholdPixels, projectileHitRadius)
    private val canvasInstaller = ScrubbingCanvasInstaller(this, hitTester)

    override fun enable(inspector: ITrajectoryInspector) {
        activateSession(inspector)
        snapToPoint(inspector.findNearestPointFromCursor(hitTester.state.projectileX, hitTester.state.projectileY))
        onRepaint?.invoke()
    }

    override fun disable() {
        clearSession()
        onRepaint?.invoke()
    }

    fun handleMousePressed(canvasX: Double, canvasY: Double): Boolean {
        val active = activeInspector() ?: return false
        if (hitTester.isOverProjectile(canvasX, canvasY)) {
            return startDraggingAt(active.findNearestPointFromCursor(canvasX, canvasY))
        }
        if (hitTester.isOverTarget(canvasX, canvasY)) return false
        val curvePoint = hitTester.findCurvePointWithinThreshold(active, canvasX, canvasY) ?: return false
        return startDraggingAt(curvePoint)
    }

    private fun startDraggingAt(point: TrajectoryPoint): Boolean {
        isDragging = true
        snapToPoint(point)
        onRepaint?.invoke()
        return true
    }

    fun handleMouseDragged(canvasX: Double, canvasY: Double): Boolean {
        if (!isDragging) return false
        val active = activeInspector() ?: return false
        snapToPoint(active.findNearestPointFromCursor(canvasX, canvasY))
        onRepaint?.invoke()
        return true
    }

    fun handleMouseReleased(): Boolean {
        if (!isDragging) return false
        isDragging = false
        onRepaint?.invoke()
        return true
    }

    fun handleMouseMoved(canvasX: Double, canvasY: Double): Boolean {
        if (isDragging) return false
        val active = activeInspector() ?: return false
        val hovered = computeHoverAt(active, canvasX, canvasY)
        if (hovered != isHovered) {
            isHovered = hovered
            onRepaint?.invoke()
        }
        return isHovered
    }

    private fun computeHoverAt(active: ITrajectoryInspector, canvasX: Double, canvasY: Double): Boolean {
        if (hitTester.isOverProjectile(canvasX, canvasY)) return true
        if (hitTester.isOverTarget(canvasX, canvasY)) return false
        return hitTester.findCurvePointWithinThreshold(active, canvasX, canvasY) != null
    }

    private fun snapToPoint(point: TrajectoryPoint) {
        currentPoint = point
        hitTester.state.updateProjectilePosition(hitTester.transformer.toCanvasX(point.x), hitTester.transformer.toCanvasY(point.y))
        onPointSelected?.invoke(point)
    }

    override fun install(canvas: Canvas) = canvasInstaller.bind(canvas)
}
