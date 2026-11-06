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
class TargetVisualFlags(
    animating: Boolean,
    hit: Boolean,
    dragging: Boolean
) : ITargetVisualFlags {
    private val flags: List<Boolean> = listOf(animating, hit, dragging)

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
    private val gc: GraphicsContext,
    private val cfg: ISceneConfig,
    private val baseRadius: Double
) : ITargetCanvasPainter {

    override fun draw(x: Double, flags: ITargetVisualFlags) {
        val pulse = if (flags.isAnimating()) 1.0 else 1.0 + 0.06 * sin((System.nanoTime() / 1_000_000_000.0) * 3.0)
        val r = baseRadius * pulse
        val cy = cfg.groundY
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
        gc.stroke = if (isHit) Color.WHITE else Color.rgb(240, 240, 240, 0.9)
        gc.lineWidth = 1.5
        gc.strokeLine(cx - centerR * 1.2, cy, cx + centerR * 1.2, cy)
        gc.strokeLine(cx, cy - centerR * 1.2, cx, cy + centerR * 1.2)
    }

    private fun drawDragHalo(cx: Double, cy: Double, r: Double) {
        gc.stroke = Color.rgb(255, 230, 0, 0.8)
        gc.lineWidth = 2.0
        gc.strokeOval(cx - r - 4.0, cy - r - 4.0, (r + 4.0) * 2.0, (r + 4.0) * 2.0)
    }
}

/**
 * Controlador de animación de pulso en reposo del objetivo (SRP + DIP).
 */
class TargetPulseController(
    private val state: ISimulationState,
    private val onRepaint: () -> Unit
) : IPulseController {
    private var pulseTimer: AnimationTimer? = null

    override fun start() {
        if (pulseTimer == null) {
            pulseTimer = object : AnimationTimer() {
                override fun handle(now: Long) {
                    if (!state.isAnimating) onRepaint()
                }
            }
        }
        pulseTimer?.start()
    }

    override fun stop() {
        pulseTimer?.stop()
        pulseTimer = null
    }
}

/**
 * Gestor encapsulado de eventos de arrastre horizontal de la diana (SRP + DIP).
 */
class TargetDragInteraction(
    private val canvas: Canvas,
    private val cfg: ISceneConfig,
    private val state: ISimulationState,
    private val callbacks: Pair<(Double) -> Unit, () -> Unit>
) : ITargetDragInteraction {
    private var dragState: Pair<Boolean, Double> = Pair(false, 0.0)

    override fun checkDragging(): Boolean {
        return dragState.first
    }

    override fun validXRange(targetRadius: Double): ClosedFloatingPointRange<Double> {
        return (cfg.cannonBaseX + 45.0)..(cfg.canvasWidth - targetRadius - 10.0)
    }

    override fun applyTargetX(newX: Double) {
        state.targetX = newX
        callbacks.first.invoke(newX)
        callbacks.second.invoke()
    }

    override fun cancel() {
        if (dragState.first) {
            dragState = Pair(false, 0.0)
            canvas.cursor = Cursor.DEFAULT
        }
    }

    override fun install(targetRadius: Double) {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED) { onPressed(it) }
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED) { onDragged(it, validXRange(targetRadius)) }
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED) { onReleased(it) }
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED) { onMoved(it) }
    }

    private fun isSelectableAt(local: Point2D): Boolean {
        val overProjectile = hypot(local.x - state.projectileX, local.y - state.projectileY) <= 22.0
        val overTarget = hypot(local.x - state.targetX, local.y - cfg.groundY) <= 26.0
        return !overProjectile && overTarget
    }

    private fun onPressed(event: MouseEvent) {
        if (event.isConsumed || state.isAnimating) return
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (isSelectableAt(local)) {
            dragState = Pair(true, local.x - state.targetX)
            canvas.cursor = Cursor.CLOSED_HAND
            callbacks.second.invoke()
            event.consume()
        }
    }

    private fun onDragged(event: MouseEvent, validRange: ClosedFloatingPointRange<Double>) {
        if (event.isConsumed || !dragState.first) return
        if (state.isAnimating) {
            cancel()
            return
        }
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        applyTargetX((local.x - dragState.second).coerceIn(validRange))
        event.consume()
    }

    private fun onReleased(event: MouseEvent) {
        if (!dragState.first) return
        dragState = Pair(false, 0.0)
        canvas.cursor = if (!state.isAnimating) Cursor.HAND else Cursor.DEFAULT
        callbacks.second.invoke()
        event.consume()
    }

    private fun onMoved(event: MouseEvent) {
        if (event.isConsumed || dragState.first || state.isAnimating) return
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (isSelectableAt(local)) {
            canvas.cursor = Cursor.HAND
        }
    }
}

/**
 * Orquestador cohesivo de renderizado y manipulación horizontal del objetivo (SRP + DIP).
 */
class TargetRenderer(
    private val state: ISimulationState,
    painter: ITargetCanvasPainter,
    dragInteraction: ITargetDragInteraction,
    pulseController: IPulseController,
    private val defaultRandomizer: ITargetRandomizer = UniformTargetRandomizer(),
    private val radiusPx: Double = 20.0
) : ITargetRenderer {

    constructor(
        gc: GraphicsContext,
        cfg: ISceneConfig,
        canvas: Canvas,
        state: ISimulationState,
        onTargetPositionChanged: (Double) -> Unit,
        onRepaint: () -> Unit,
        randomizer: ITargetRandomizer = UniformTargetRandomizer()
    ) : this(
        state = state,
        painter = TargetCanvasPainter(gc, cfg, 20.0),
        dragInteraction = TargetDragInteraction(canvas, cfg, state, Pair(onTargetPositionChanged, onRepaint)),
        pulseController = TargetPulseController(state, onRepaint),
        defaultRandomizer = randomizer,
        radiusPx = 20.0
    )

    private val components: Triple<ITargetCanvasPainter, ITargetDragInteraction, IPulseController> =
        Triple(painter, dragInteraction, pulseController)

    init {
        state.targetRadius = radiusPx
    }

    override fun radiusPixels(): Double {
        return radiusPx
    }

    override fun drawTarget(x: Double) {
        val isHit = state.lastCollisionResult?.isHit() == true
        val flags = TargetVisualFlags(state.isAnimating, isHit, components.second.checkDragging())
        components.first.draw(x, flags)
    }

    override fun startPulse() {
        components.third.start()
    }

    override fun stopPulse() {
        components.third.stop()
    }

    override fun canMoveTarget(): Boolean {
        return !state.isAnimating
    }

    override fun cancelDrag() {
        components.second.cancel()
    }

    override fun getValidXRange(): ClosedFloatingPointRange<Double> {
        return components.second.validXRange(radiusPx)
    }

    override fun randomizePosition(targetRandomizer: ITargetRandomizer?): Boolean {
        if (!canMoveTarget()) return false
        val range = getValidXRange()
        val chosen = targetRandomizer ?: defaultRandomizer
        components.second.applyTargetX(chosen.calculateRandomX(range.start, range.endInclusive))
        return true
    }

    override fun installDrag() {
        components.second.install(radiusPx)
    }
}
