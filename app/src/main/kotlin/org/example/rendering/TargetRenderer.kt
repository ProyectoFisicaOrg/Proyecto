package org.example.rendering

import javafx.animation.AnimationTimer
import javafx.geometry.Point2D
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import kotlin.math.hypot
import kotlin.math.sin
import org.example.model.ISimulationState

/**
 * Estado visual instantáneo encapsulado para el dibujado de la diana (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class TargetVisualFlags internal constructor(
    private val flags: List<Boolean>
) : ITargetVisualFlags {

    constructor(animating: Boolean, hit: Boolean, dragging: Boolean) : this(listOf(animating, hit, dragging))

    override fun isAnimating(): Boolean {
        return flags[0]
    }

    override fun isHit(): Boolean {
        return flags[1]
    }

    override fun isDragging(): Boolean {
        return flags[2]
    }

    override fun outerRingColor(): Color {
        if (flags[1]) return Color.rgb(46, 204, 113)
        if (flags[2]) return Color.rgb(231, 76, 60)
        return Color.rgb(217, 30, 24)
    }
}

/**
 * Pintor dedicado exclusivamente a la representación gráfica de la diana (SRP + DIP).
 */
class TargetCanvasPainter(
    gc: GraphicsContext,
    cfg: ISceneConfig,
    baseRadius: Double
) : ITargetCanvasPainter {
    private val ctx: Triple<GraphicsContext, ISceneConfig, Double> = Triple(gc, cfg, baseRadius)

    override fun draw(x: Double, flags: ITargetVisualFlags) {
        val gc = ctx.first
        val pulse = if (flags.isAnimating()) 1.0 else 1.0 + 0.06 * sin((System.nanoTime() / 1_000_000_000.0) * 3.0)
        val r = ctx.third * pulse
        val cy = ctx.second.groundLevelY()
        gc.save()
        try {
            gc.fill = Color.rgb(0, 0, 0, 0.22)
            gc.fillOval(x - r * 1.1, cy - r * 0.4 + 4.0, r * 2.2, r * 0.8)
            drawConcentricRings(x, cy, r, flags)
            drawCrosshair(x, cy, r * 0.35, flags.isHit())
            if (flags.isDragging()) drawDragHalo(x, cy, r)
        } finally {
            gc.restore()
        }
    }

    private fun drawConcentricRings(cx: Double, cy: Double, r: Double, flags: ITargetVisualFlags) {
        val gc = ctx.first
        gc.fill = flags.outerRingColor()
        gc.fillOval(cx - r, cy - r, r * 2.0, r * 2.0)
        val midR = r * 0.65
        gc.fill = Color.rgb(255, 255, 255)
        gc.fillOval(cx - midR, cy - midR, midR * 2.0, midR * 2.0)
        val centerR = r * 0.35
        gc.fill = if (flags.isHit()) Color.rgb(39, 174, 96) else Color.rgb(192, 57, 43)
        gc.fillOval(cx - centerR, cy - centerR, centerR * 2.0, centerR * 2.0)
    }

    private fun drawCrosshair(cx: Double, cy: Double, centerR: Double, isHit: Boolean) {
        val gc = ctx.first
        gc.stroke = if (isHit) Color.WHITE else Color.rgb(240, 240, 240, 0.9)
        gc.lineWidth = 1.5
        gc.strokeLine(cx - centerR * 1.2, cy, cx + centerR * 1.2, cy)
        gc.strokeLine(cx, cy - centerR * 1.2, cx, cy + centerR * 1.2)
    }

    private fun drawDragHalo(cx: Double, cy: Double, r: Double) {
        val gc = ctx.first
        gc.stroke = Color.rgb(255, 230, 0, 0.8)
        gc.lineWidth = 2.0
        gc.strokeOval(cx - r - 4.0, cy - r - 4.0, (r + 4.0) * 2.0, (r + 4.0) * 2.0)
    }
}

/**
 * Controlador de animación de pulso en reposo del objetivo (SRP + DIP).
 */
class TargetPulseController(
    state: ISimulationState,
    onRepaint: () -> Unit
) : IPulseController {
    private val slot: MutableList<Pair<Pair<ISimulationState, () -> Unit>, AnimationTimer?>> =
        mutableListOf(Pair(Pair(state, onRepaint), null))

    override fun start() {
        val cur = slot[0]
        val timer = cur.second ?: object : AnimationTimer() {
            override fun handle(now: Long) {
                if (!cur.first.first.checkAnimating()) cur.first.second.invoke()
            }
        }
        slot[0] = Pair(cur.first, timer)
        timer.start()
    }

    override fun stop() {
        val cur = slot[0]
        cur.second?.stop()
        slot[0] = Pair(cur.first, null)
    }
}

/**
 * Gestor encapsulado de eventos de arrastre horizontal de la diana (SRP + DIP).
 */
class TargetDragInteraction(
    canvas: Canvas,
    cfg: ISceneConfig,
    state: ISimulationState,
    callbacks: Pair<(Double) -> Unit, () -> Unit>
) : ITargetDragInteraction {
    private val slot: MutableList<Pair<Pair<Pair<Canvas, ISceneConfig>, Pair<ISimulationState, Pair<(Double) -> Unit, () -> Unit>>>, Pair<Boolean, Double>>> =
        mutableListOf(Pair(Pair(Pair(canvas, cfg), Pair(state, callbacks)), Pair(false, 0.0)))

    override fun checkDragging(): Boolean {
        return slot[0].second.first
    }

    override fun validXRange(targetRadius: Double): ClosedFloatingPointRange<Double> {
        val cfg = slot[0].first.first.second
        return (cfg.basePivotX() + 45.0)..(cfg.width() - targetRadius - 10.0)
    }

    override fun applyTargetX(newX: Double) {
        val stAndCb = slot[0].first.second
        stAndCb.first.changeTargetX(newX)
        stAndCb.second.first.invoke(newX)
        stAndCb.second.second.invoke()
    }

    override fun cancel() {
        val cur = slot[0]
        if (cur.second.first) {
            slot[0] = Pair(cur.first, Pair(false, 0.0))
            cur.first.first.first.cursor = Cursor.DEFAULT
        }
    }

    override fun install(targetRadius: Double) {
        val canvas = slot[0].first.first.first
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED) { onPressed(it) }
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED) { onDragged(it, validXRange(targetRadius)) }
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED) { onReleased(it) }
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED) { onMoved(it) }
    }

    private fun isSelectableAt(local: Point2D): Boolean {
        val cur = slot[0].first
        val st = cur.second.first
        val proj = st.currentProjectileCoords()
        val overProjectile = hypot(local.x - proj.first, local.y - proj.second) <= 22.0
        val overTarget = hypot(local.x - st.currentTargetX(), local.y - cur.first.second.groundLevelY()) <= 26.0
        return !overProjectile && overTarget
    }

    private fun onPressed(event: MouseEvent) {
        val cur = slot[0]
        val st = cur.first.second.first
        if (event.isConsumed || st.checkAnimating()) return
        val canvas = cur.first.first.first
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (isSelectableAt(local)) {
            slot[0] = Pair(cur.first, Pair(true, local.x - st.currentTargetX()))
            canvas.cursor = Cursor.CLOSED_HAND
            cur.first.second.second.second.invoke()
            event.consume()
        }
    }

    private fun onDragged(event: MouseEvent, validRange: ClosedFloatingPointRange<Double>) {
        val cur = slot[0]
        if (event.isConsumed || !cur.second.first) return
        if (cur.first.second.first.checkAnimating()) {
            cancel()
            return
        }
        val local = cur.first.first.first.sceneToLocal(event.sceneX, event.sceneY)
        applyTargetX((local.x - cur.second.second).coerceIn(validRange))
        event.consume()
    }

    private fun onReleased(event: MouseEvent) {
        val cur = slot[0]
        if (!cur.second.first) return
        slot[0] = Pair(cur.first, Pair(false, 0.0))
        cur.first.first.first.cursor = if (!cur.first.second.first.checkAnimating()) Cursor.HAND else Cursor.DEFAULT
        cur.first.second.second.second.invoke()
        event.consume()
    }

    private fun onMoved(event: MouseEvent) {
        val cur = slot[0]
        if (event.isConsumed || cur.second.first || cur.first.second.first.checkAnimating()) return
        val canvas = cur.first.first.first
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (isSelectableAt(local)) {
            canvas.cursor = Cursor.HAND
        }
    }
}

/**
 * Especificación inmutable de inicialización del subsistema gráfico de la diana (SRP).
 */
@ConsistentCopyVisibility
data class TargetInitSpec internal constructor(
    private val pack: Pair<Pair<GraphicsContext, Canvas>, Pair<Pair<ISceneConfig, ISimulationState>, Pair<Pair<(Double) -> Unit, () -> Unit>, ITargetRandomizer?>>>
) : ITargetInitSpec {

    constructor(
        gc: GraphicsContext,
        canvas: Canvas,
        cfg: ISceneConfig,
        state: ISimulationState,
        onMoved: (Double) -> Unit,
        onRepaint: () -> Unit,
        randomizer: ITargetRandomizer? = null
    ) : this(Pair(Pair(gc, canvas), Pair(Pair(cfg, state), Pair(Pair(onMoved, onRepaint), randomizer))))

    override fun graphics(): GraphicsContext {
        return pack.first.first
    }

    override fun canvasRef(): Canvas {
        return pack.first.second
    }

    override fun sceneConfig(): ISceneConfig {
        return pack.second.first.first
    }

    override fun stateModel(): ISimulationState {
        return pack.second.first.second
    }

    override fun notifyMoved(newX: Double) {
        pack.second.second.first.first.invoke(newX)
    }

    override fun notifyRepaint() {
        pack.second.second.first.second.invoke()
    }

    override fun customRandomizer(): ITargetRandomizer? {
        return pack.second.second.second
    }
}

/**
 * Paquete cohesivo de sub-componentes visuales, de pulso y de arrastre de la diana (SRP + DIP).
 */
class TargetComponentsBundle(
    painter: ITargetCanvasPainter,
    drag: ITargetDragInteraction,
    pulse: IPulseController,
    randomizer: ITargetRandomizer
) : ITargetComponentsBundle {
    private val parts: Pair<Pair<ITargetCanvasPainter, ITargetDragInteraction>, Pair<IPulseController, ITargetRandomizer>> =
        Pair(Pair(painter, drag), Pair(pulse, randomizer))

    override fun drawAt(x: Double, animating: Boolean, isHit: Boolean) {
        val flags = TargetVisualFlags(animating, isHit, parts.first.second.checkDragging())
        parts.first.first.draw(x, flags)
    }

    override fun startPulse() {
        parts.second.first.start()
    }

    override fun stopPulse() {
        parts.second.first.stop()
    }

    override fun cancelDrag() {
        parts.first.second.cancel()
    }

    override fun validRange(radiusPx: Double): ClosedFloatingPointRange<Double> {
        return parts.first.second.validXRange(radiusPx)
    }

    override fun applyRandomX(radiusPx: Double, overrideRandomizer: ITargetRandomizer?) {
        val range = parts.first.second.validXRange(radiusPx)
        val chosen = overrideRandomizer ?: parts.second.second
        parts.first.second.applyTargetX(chosen.calculateRandomX(range.start, range.endInclusive))
    }

    override fun installDrag(radiusPx: Double) {
        parts.first.second.install(radiusPx)
    }
}

/**
 * Ensambladores modulares de sub-componentes de [TargetRenderer] conforme a DIP.
 */
object TargetVisualWiring : ITargetVisualWiring {
    override fun createPainter(spec: ITargetInitSpec): ITargetCanvasPainter {
        return TargetCanvasPainter(spec.graphics(), spec.sceneConfig(), 20.0)
    }
}

object TargetInteractionWiring : ITargetInteractionWiring {
    override fun createDragAndPulse(spec: ITargetInitSpec): Pair<ITargetDragInteraction, IPulseController> {
        val callbacks: Pair<(Double) -> Unit, () -> Unit> = Pair({ x -> spec.notifyMoved(x) }, { spec.notifyRepaint() })
        val drag = TargetDragInteraction(spec.canvasRef(), spec.sceneConfig(), spec.stateModel(), callbacks)
        val pulse = TargetPulseController(spec.stateModel()) { spec.notifyRepaint() }
        return Pair(drag, pulse)
    }
}

object TargetBundleWiring : ITargetBundleWiring {
    override fun build(spec: ITargetInitSpec): ITargetComponentsBundle {
        val painter = TargetVisualWiring.createPainter(spec)
        val dragAndPulse = TargetInteractionWiring.createDragAndPulse(spec)
        val randomizer = spec.customRandomizer() ?: DefaultTargetRandomizer
        return TargetComponentsBundle(painter, dragAndPulse.first, dragAndPulse.second, randomizer)
    }
}

/**
 * Orquestador cohesivo de renderizado y manipulación horizontal del objetivo (SRP + DIP).
 */
class TargetRenderer(
    state: ISimulationState,
    bundle: ITargetComponentsBundle,
    radiusPx: Double = 20.0
) : ITargetRenderer {

    constructor(
        gc: GraphicsContext,
        cfg: ISceneConfig,
        canvas: Canvas,
        state: ISimulationState,
        onTargetPositionChanged: (Double) -> Unit,
        onRepaint: () -> Unit,
        randomizer: ITargetRandomizer? = null
    ) : this(
        state = state,
        bundle = TargetBundleWiring.build(TargetInitSpec(gc, canvas, cfg, state, onTargetPositionChanged, onRepaint, randomizer)),
        radiusPx = 20.0
    )

    private val ctx: Triple<ISimulationState, ITargetComponentsBundle, Double> = Triple(state, bundle, radiusPx)

    init {
        ctx.first.changeTargetRadius(ctx.third)
    }

    override fun radiusPixels(): Double {
        return ctx.third
    }

    override fun drawTarget(x: Double) {
        val st = ctx.first
        val isHit = st.currentCollisionResult()?.isHit() == true
        ctx.second.drawAt(x, st.checkAnimating(), isHit)
    }

    override fun startPulse() {
        ctx.second.startPulse()
    }

    override fun stopPulse() {
        ctx.second.stopPulse()
    }

    override fun canMoveTarget(): Boolean {
        return !ctx.first.checkAnimating()
    }

    override fun cancelDrag() {
        ctx.second.cancelDrag()
    }

    override fun getValidXRange(): ClosedFloatingPointRange<Double> {
        return ctx.second.validRange(ctx.third)
    }

    override fun randomizePosition(targetRandomizer: ITargetRandomizer?): Boolean {
        if (!canMoveTarget()) return false
        ctx.second.applyRandomX(ctx.third, targetRandomizer)
        return true
    }

    override fun installDrag() {
        ctx.second.installDrag(ctx.third)
    }
}
