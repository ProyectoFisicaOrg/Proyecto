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
import org.example.collision.CollisionStatus
import org.example.model.SimulationState

/**
 * Contrato abstracto para el renderizador y manipulador del objetivo (DIP).
 */
interface ITargetRenderer {
    val targetRadius: Double
    fun drawTarget(x: Double)
    fun startPulse()
    fun stopPulse()
    fun canMoveTarget(): Boolean
    fun cancelDrag()
    fun getValidXRange(): ClosedFloatingPointRange<Double>
    fun randomizePosition(targetRandomizer: ITargetRandomizer = UniformTargetRandomizer()): Boolean
    fun installDrag()
}

/**
 * Estado visual instantáneo para el dibujado de la diana (SRP).
 */
private class TargetVisualFlags(
    private val animating: Boolean,
    private val hit: Boolean,
    private val dragging: Boolean
) {
    fun isAnimating(): Boolean = animating
    fun isHit(): Boolean = hit
    fun isDragging(): Boolean = dragging

    fun outerRingColor(): Color = when {
        hit -> Color.rgb(46, 204, 113)
        dragging -> Color.rgb(231, 76, 60)
        else -> Color.rgb(217, 30, 24)
    }
}

/**
 * Pintor dedicado exclusivamente a la representación gráfica de la diana (SRP).
 */
private class TargetCanvasPainter(
    private val gc: GraphicsContext,
    private val cfg: SceneConfig,
    private val baseRadius: Double
) {
    fun draw(x: Double, flags: TargetVisualFlags) {
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

    private fun drawConcentricRings(cx: Double, cy: Double, r: Double, flags: TargetVisualFlags) {
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
 * Controlador de animación de pulso en reposo del objetivo (SRP).
 */
private class TargetPulseController(
    private val state: SimulationState,
    private val onRepaint: () -> Unit
) {
    private var pulseTimer: AnimationTimer? = null

    fun start() {
        if (pulseTimer == null) {
            pulseTimer = object : AnimationTimer() {
                override fun handle(now: Long) {
                    if (!state.isAnimating) onRepaint()
                }
            }
        }
        pulseTimer?.start()
    }

    fun stop() {
        pulseTimer?.stop()
        pulseTimer = null
    }
}

/**
 * Gestor de eventos de arrastre horizontal y actualización de posición de la diana (SRP).
 */
private class TargetDragInteraction(
    private val canvas: Canvas,
    private val cfg: SceneConfig,
    private val state: SimulationState,
    private val onPositionChanged: (Double) -> Unit,
    private val onRepaint: () -> Unit
) {
    var isDragging: Boolean = false
        private set
    private var dragOffsetX: Double = 0.0

    fun validXRange(targetRadius: Double): ClosedFloatingPointRange<Double> =
        (cfg.cannonBaseX + 45.0)..(cfg.canvasWidth - targetRadius - 10.0)

    fun applyTargetX(newX: Double) {
        state.targetX = newX
        onPositionChanged(newX)
        onRepaint()
    }

    fun cancel() {
        if (isDragging) {
            isDragging = false
            canvas.cursor = Cursor.DEFAULT
        }
    }

    fun install(targetRadius: Double) {
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
            isDragging = true
            dragOffsetX = local.x - state.targetX
            canvas.cursor = Cursor.CLOSED_HAND
            onRepaint()
            event.consume()
        }
    }

    private fun onDragged(event: MouseEvent, validRange: ClosedFloatingPointRange<Double>) {
        if (event.isConsumed || !isDragging) return
        if (state.isAnimating) {
            cancel()
            return
        }
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        applyTargetX((local.x - dragOffsetX).coerceIn(validRange))
        event.consume()
    }

    private fun onReleased(event: MouseEvent) {
        if (!isDragging) return
        isDragging = false
        canvas.cursor = if (!state.isAnimating) Cursor.HAND else Cursor.DEFAULT
        onRepaint()
        event.consume()
    }

    private fun onMoved(event: MouseEvent) {
        if (event.isConsumed || isDragging || state.isAnimating) return
        val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
        if (isSelectableAt(local)) {
            canvas.cursor = Cursor.HAND
        }
    }
}

/**
 * Orquestador de renderizado y manipulación horizontal del objetivo (SRP + DIP).
 */
class TargetRenderer(
    gc: GraphicsContext,
    cfg: SceneConfig,
    canvas: Canvas,
    private val state: SimulationState,
    onTargetPositionChanged: (Double) -> Unit,
    onRepaint: () -> Unit,
    randomizer: ITargetRandomizer = UniformTargetRandomizer()
) : ITargetRenderer {
    override val targetRadius: Double = 20.0
    private val defaultRandomizer: ITargetRandomizer = randomizer

    private val painter = TargetCanvasPainter(gc, cfg, targetRadius)
    private val dragInteraction = TargetDragInteraction(canvas, cfg, state, onTargetPositionChanged, onRepaint)
    private val pulseController = TargetPulseController(state, onRepaint)

    init {
        state.targetRadius = targetRadius
    }

    override fun drawTarget(x: Double) {
        val isHit = state.lastCollisionResult?.status == CollisionStatus.HIT
        painter.draw(x, TargetVisualFlags(state.isAnimating, isHit, dragInteraction.isDragging))
    }

    override fun startPulse() = pulseController.start()

    override fun stopPulse() = pulseController.stop()

    override fun canMoveTarget(): Boolean = !state.isAnimating

    override fun cancelDrag() = dragInteraction.cancel()

    override fun getValidXRange(): ClosedFloatingPointRange<Double> =
        dragInteraction.validXRange(targetRadius)

    override fun randomizePosition(targetRandomizer: ITargetRandomizer): Boolean {
        if (!canMoveTarget()) return false
        val range = getValidXRange()
        val chosen = if (targetRandomizer === defaultRandomizer) defaultRandomizer else targetRandomizer
        dragInteraction.applyTargetX(chosen.calculateRandomX(range.start, range.endInclusive))
        return true
    }

    override fun installDrag() = dragInteraction.install(targetRadius)
}
