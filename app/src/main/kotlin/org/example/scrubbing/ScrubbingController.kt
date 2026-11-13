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
 * Sesión cohesiva y encapsulada del estado interactivo de scrubbing (SRP + DIP).
 */
class ScrubbingSession : IScrubbingSession {
    private var slot: Pair<List<Boolean>, Pair<ITrajectoryInspector?, ISpanishKinematicView?>> =
        Pair(listOf(false, false, false), Pair(null, null))

    override fun checkEnabled(): Boolean {
        return slot.first[0]
    }

    override fun checkDragging(): Boolean {
        return slot.first[1]
    }

    override fun checkHovered(): Boolean {
        return slot.first[2]
    }

    override fun currentInspector(): ITrajectoryInspector? {
        return slot.second.first
    }

    override fun selectedTrajectoryPoint(): ISpanishKinematicView? {
        return slot.second.second
    }

    override fun getState(): IScrubbingState {
        val fl = slot.first
        return ScrubbingState(fl[0], fl[1], fl[2], slot.second.second)
    }

    override fun activateSession(newInspector: ITrajectoryInspector) {
        slot = Pair(listOf(true, false, false), Pair(newInspector, null))
    }

    override fun clearSession() {
        slot = Pair(listOf(false, false, false), Pair(null, null))
    }

    override fun updateDraggingFlag(dragging: Boolean) {
        val fl = slot.first
        slot = Pair(listOf(fl[0], dragging, fl[2]), slot.second)
    }

    override fun updateHoveredFlag(hovered: Boolean) {
        val fl = slot.first
        slot = Pair(listOf(fl[0], fl[1], hovered), slot.second)
    }

    override fun updateSelectedPoint(point: ISpanishKinematicView?) {
        slot = Pair(slot.first, Pair(slot.second.first, point))
    }

    override fun activeInspector(): ITrajectoryInspector? {
        return if (slot.first[0]) slot.second.first else null
    }
}

/**
 * Evaluador geométrico de hit-testing para proyectil, diana y curva de trayectoria (SRP + DIP).
 */
class ScrubbingHitTester(
    state: ISimulationState,
    transformer: CoordinateTransformer,
    cfg: ISceneConfig,
    hitThresholdPixels: Double,
    projectileHitRadius: Double
) : IScrubbingHitTester {
    private val ctx: Pair<Triple<ISimulationState, CoordinateTransformer, ISceneConfig>, Pair<Double, Double>> =
        Pair(Triple(state, transformer, cfg), Pair(hitThresholdPixels, projectileHitRadius))

    override fun isOverProjectile(canvasX: Double, canvasY: Double): Boolean {
        val proj = ctx.first.first.currentProjectileCoords()
        return hypot(canvasX - proj.first, canvasY - proj.second) <= ctx.second.second
    }

    override fun isOverTarget(canvasX: Double, canvasY: Double): Boolean {
        val st = ctx.first.first
        val gy = ctx.first.third.groundLevelY()
        return hypot(canvasX - st.currentTargetX(), canvasY - gy) <= st.currentTargetRadius() + 6.0
    }

    override fun findCurvePointWithinThreshold(inspector: ITrajectoryInspector, canvasX: Double, canvasY: Double): ISpanishKinematicView? {
        val tr = ctx.first.second
        val nearest = inspector.findNearestPointFromCursor(canvasX, canvasY)
        val nx = tr.toCanvasX(nearest.coordX())
        val ny = tr.toCanvasY(nearest.coordY())
        return if (hypot(canvasX - nx, canvasY - ny) <= ctx.second.first) nearest else null
    }
}

/**
 * Puerto de sincronización de posición del proyectil y callbacks de repintado durante el scrubbing (SRP + DIP).
 */
class ScrubbingSyncPort(
    state: ISimulationState,
    transformer: CoordinateTransformer,
    onPointSelected: ((ISpanishKinematicView) -> Unit)?,
    onRepaint: (() -> Unit)?
) : IScrubbingSyncPort {
    private val ctx: Pair<Pair<ISimulationState, CoordinateTransformer>, Pair<((ISpanishKinematicView) -> Unit)?, (() -> Unit)?>> =
        Pair(Pair(state, transformer), Pair(onPointSelected, onRepaint))

    override fun projectileCoords(): Pair<Double, Double> {
        return ctx.first.first.currentProjectileCoords()
    }

    override fun commitSelectedPoint(point: ISpanishKinematicView) {
        val tr = ctx.first.second
        ctx.first.first.updateProjectilePosition(tr.toCanvasX(point.coordX()), tr.toCanvasY(point.coordY()))
        ctx.second.first?.invoke(point)
    }

    override fun requestRepaint() {
        ctx.second.second?.invoke()
    }
}

/**
 * Adaptador de eventos JavaFX de ratón sobre el Canvas hacia el controlador de scrubbing (SRP + DIP).
 */
class ScrubbingCanvasInstaller(
    controller: IScrubbingController,
    hitTester: IScrubbingHitTester
) : ICanvasMouseInstaller {
    private val deps: Pair<IScrubbingController, IScrubbingHitTester> = Pair(controller, hitTester)

    override fun bind(canvas: Canvas) {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED) { onPressed(canvas, it) }
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED) { onDragged(canvas, it) }
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED) { onReleased(canvas, it) }
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED) { onMoved(canvas, it) }
    }

    private fun onPressed(canvas: Canvas, event: MouseEvent) {
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (deps.first.handleMousePressed(local.x, local.y)) {
            canvas.cursor = Cursor.CLOSED_HAND
            event.consume()
        }
    }

    private fun onDragged(canvas: Canvas, event: MouseEvent) {
        val ctrl = deps.first
        if (!ctrl.checkDragging()) return
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (ctrl.handleMouseDragged(local.x, local.y)) {
            canvas.cursor = Cursor.CLOSED_HAND
            event.consume()
        }
    }

    private fun onReleased(canvas: Canvas, event: MouseEvent) {
        val ctrl = deps.first
        if (ctrl.handleMouseReleased()) {
            canvas.cursor = if (ctrl.checkHovered()) Cursor.HAND else Cursor.DEFAULT
            event.consume()
        }
    }

    private fun onMoved(canvas: Canvas, event: MouseEvent) {
        val ctrl = deps.first
        if (!ctrl.checkEnabled() || ctrl.checkDragging()) return
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (ctrl.handleMouseMoved(local.x, local.y)) {
            canvas.cursor = Cursor.HAND
        } else if (!deps.second.isOverTarget(local.x, local.y)) {
            canvas.cursor = Cursor.DEFAULT
        }
    }
}

/**
 * Especificación inmutable de inicialización para ensamblar [ScrubbingController] (SRP).
 */
@ConsistentCopyVisibility
data class ScrubbingInitSpec internal constructor(
    private val pack: Pair<Triple<ISimulationState, CoordinateTransformer, ISceneConfig>, Pair<Pair<Double, Double>, Pair<((ISpanishKinematicView) -> Unit)?, (() -> Unit)?>>>
) : IScrubbingInitSpec {

    constructor(
        state: ISimulationState,
        transformer: CoordinateTransformer,
        cfg: ISceneConfig,
        onPointSelected: ((ISpanishKinematicView) -> Unit)?,
        onRepaint: (() -> Unit)?,
        hitThresholdPixels: Double,
        projectileHitRadius: Double
    ) : this(Pair(Triple(state, transformer, cfg), Pair(Pair(hitThresholdPixels, projectileHitRadius), Pair(onPointSelected, onRepaint))))

    override fun stateModel(): ISimulationState {
        return pack.first.first
    }

    override fun coordTransformer(): CoordinateTransformer {
        return pack.first.second
    }

    override fun sceneConfig(): ISceneConfig {
        return pack.first.third
    }

    override fun hitThreshold(): Double {
        return pack.second.first.first
    }

    override fun projectileRadius(): Double {
        return pack.second.first.second
    }

    override fun notifyPointSelected(point: ISpanishKinematicView) {
        pack.second.second.first?.invoke(point)
    }

    override fun notifyRepaint() {
        pack.second.second.second?.invoke()
    }
}

/**
 * Implementación modular por composición del controlador de scrubbing (SRP + DIP + OCP).
 */
class DefaultScrubbingController(
    session: IScrubbingSession,
    hitTester: IScrubbingHitTester,
    syncPort: IScrubbingSyncPort,
    installerFactory: (IScrubbingController, IScrubbingHitTester) -> ICanvasMouseInstaller
) : IScrubbingController, IScrubbingSession by session {
    private val ctx: Pair<Pair<IScrubbingSession, IScrubbingHitTester>, Pair<IScrubbingSyncPort, ICanvasMouseInstaller>> =
        Pair(Pair(session, hitTester), Pair(syncPort, installerFactory(this, hitTester)))

    override fun enable(inspector: ITrajectoryInspector) {
        ctx.first.first.activateSession(inspector)
        val proj = ctx.second.first.projectileCoords()
        snapToPoint(inspector.findNearestPointFromCursor(proj.first, proj.second))
        ctx.second.first.requestRepaint()
    }

    override fun disable() {
        ctx.first.first.clearSession()
        ctx.second.first.requestRepaint()
    }

    override fun handleMousePressed(canvasX: Double, canvasY: Double): Boolean {
        val active = ctx.first.first.activeInspector() ?: return false
        val tester = ctx.first.second
        if (tester.isOverProjectile(canvasX, canvasY)) {
            return startDraggingAt(active.findNearestPointFromCursor(canvasX, canvasY))
        }
        if (tester.isOverTarget(canvasX, canvasY)) return false
        val curvePoint = tester.findCurvePointWithinThreshold(active, canvasX, canvasY) ?: return false
        return startDraggingAt(curvePoint)
    }

    private fun startDraggingAt(point: ISpanishKinematicView): Boolean {
        ctx.first.first.updateDraggingFlag(true)
        snapToPoint(point)
        ctx.second.first.requestRepaint()
        return true
    }

    override fun handleMouseDragged(canvasX: Double, canvasY: Double): Boolean {
        val sess = ctx.first.first
        if (!sess.checkDragging()) return false
        val active = sess.activeInspector() ?: return false
        snapToPoint(active.findNearestPointFromCursor(canvasX, canvasY))
        ctx.second.first.requestRepaint()
        return true
    }

    override fun handleMouseReleased(): Boolean {
        val sess = ctx.first.first
        if (!sess.checkDragging()) return false
        sess.updateDraggingFlag(false)
        ctx.second.first.requestRepaint()
        return true
    }

    override fun handleMouseMoved(canvasX: Double, canvasY: Double): Boolean {
        val sess = ctx.first.first
        if (sess.checkDragging()) return false
        val active = sess.activeInspector() ?: return false
        val hovered = computeHoverAt(active, canvasX, canvasY)
        if (hovered != sess.checkHovered()) {
            sess.updateHoveredFlag(hovered)
            ctx.second.first.requestRepaint()
        }
        return sess.checkHovered()
    }

    private fun computeHoverAt(active: ITrajectoryInspector, canvasX: Double, canvasY: Double): Boolean {
        val tester = ctx.first.second
        if (tester.isOverProjectile(canvasX, canvasY)) return true
        if (tester.isOverTarget(canvasX, canvasY)) return false
        return tester.findCurvePointWithinThreshold(active, canvasX, canvasY) != null
    }

    private fun snapToPoint(point: ISpanishKinematicView) {
        ctx.first.first.updateSelectedPoint(point)
        ctx.second.first.commitSelectedPoint(point)
    }

    override fun install(canvas: Canvas) {
        ctx.second.second.bind(canvas)
    }
}

/**
 * Ensambladores predeterminados de puertos y sesión de scrubbing (DIP).
 */
object ScrubbingPortsWiring : IScrubbingPortsWiring {
    override fun createPorts(spec: IScrubbingInitSpec): Pair<IScrubbingHitTester, IScrubbingSyncPort> {
        val tester = ScrubbingHitTester(spec.stateModel(), spec.coordTransformer(), spec.sceneConfig(), spec.hitThreshold(), spec.projectileRadius())
        val sync = ScrubbingSyncPort(spec.stateModel(), spec.coordTransformer(), { pt -> spec.notifyPointSelected(pt) }, { spec.notifyRepaint() })
        return Pair(tester, sync)
    }
}

object ScrubbingWiring : IScrubbingWiring {
    override fun createController(spec: IScrubbingInitSpec): IScrubbingController {
        val ports = ScrubbingPortsWiring.createPorts(spec)
        return DefaultScrubbingController(ScrubbingSession(), ports.first, ports.second, ::ScrubbingCanvasInstaller)
    }
}

/**
 * Fachada de controlador de scrubbing compatible con construcción estándar e inyección (SRP + DIP).
 */
class ScrubbingController(
    private val delegate: IScrubbingController
) : IScrubbingController by delegate {

    constructor(
        state: ISimulationState,
        transformer: CoordinateTransformer,
        cfg: ISceneConfig,
        onPointSelected: ((ISpanishKinematicView) -> Unit)? = null,
        onRepaint: (() -> Unit)? = null,
        hitThresholdPixels: Double = 40.0,
        projectileHitRadius: Double = 22.0
    ) : this(
        ScrubbingWiring.createController(
            ScrubbingInitSpec(state, transformer, cfg, onPointSelected, onRepaint, hitThresholdPixels, projectileHitRadius)
        )
    )

    override fun disable() {
        delegate.disable()
    }
}
