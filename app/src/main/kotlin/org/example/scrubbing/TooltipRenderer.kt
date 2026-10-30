package org.example.scrubbing

import java.util.Locale
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

/**
 * Coordenadas de anclaje y dimensiones del lienzo para posicionar el tooltip (SRP).
 */
class TooltipAnchor(
    private val targetX: Double,
    private val targetY: Double,
    private val canvasWidth: Double = 900.0,
    private val canvasHeight: Double = 550.0
) {
    fun x(): Double = targetX
    fun y(): Double = targetY
    fun canvasW(): Double = canvasWidth
    fun canvasH(): Double = canvasHeight
}

/**
 * Contrato para renderizadores de etiquetas flotantes (ISP + OCP).
 */
interface ITooltipRenderer {
    fun render(gc: GraphicsContext, point: TrajectoryPoint, anchor: TooltipAnchor)
}

/**
 * Rectángulo delimitador del tooltip con operaciones geométricas de dominio (SRP).
 */
data class TooltipBounds(
    val x: Double,
    val y: Double,
    private val width: Double,
    private val height: Double
) {
    fun boxWidth(): Double = width

    fun boxHeight(): Double = height

    fun right(): Double = x + width

    fun bottom(): Double = y + height

    fun contains(px: Double, py: Double): Boolean = px in x..right() && py in y..bottom()
}

/**
 * Componente flotante que sigue visualmente la posición actual del proyectil (SRP + OCP).
 */
class TooltipRenderer(
    private val width: Double = 155.0,
    private val height: Double = 56.0,
    private val verticalOffset: Double = 18.0
) : ITooltipRenderer {

    fun calculateTooltipBounds(targetCanvasX: Double, targetCanvasY: Double, canvasWidth: Double, canvasHeight: Double): TooltipBounds {
        var x = targetCanvasX - width / 2.0
        var y = targetCanvasY - height - verticalOffset
        if (y < 10.0) {
            y = targetCanvasY + verticalOffset + 12.0
        }
        val margin = 10.0
        x = x.coerceIn(margin, canvasWidth - width - margin)
        y = y.coerceIn(margin, canvasHeight - height - margin)
        return TooltipBounds(x, y, width, height)
    }

    fun formatLines(point: TrajectoryPoint): List<String> = listOf(
        String.format(Locale.US, "Velocidad: %.2f m/s", point.speed),
        String.format(Locale.US, "Altura (Y): %.2f m", point.height)
    )

    override fun render(gc: GraphicsContext, point: TrajectoryPoint, anchor: TooltipAnchor) {
        val bounds = calculateTooltipBounds(anchor.x(), anchor.y(), anchor.canvasW(), anchor.canvasH())
        gc.save()
        try {
            drawBubbleBox(gc, bounds)
            drawPointerArrow(gc, bounds, anchor.x(), anchor.y())
            drawTelemetryValues(gc, bounds, point)
        } finally {
            gc.restore()
        }
    }

    private fun drawBubbleBox(gc: GraphicsContext, bounds: TooltipBounds) {
        gc.fill = Color.rgb(0, 0, 0, 0.40)
        gc.fillRoundRect(bounds.x + 2.0, bounds.y + 3.0, bounds.boxWidth(), bounds.boxHeight(), 10.0, 10.0)

        gc.fill = Color.rgb(15, 23, 42, 0.94)
        gc.fillRoundRect(bounds.x, bounds.y, bounds.boxWidth(), bounds.boxHeight(), 10.0, 10.0)

        gc.stroke = Color.rgb(99, 102, 241, 0.85)
        gc.lineWidth = 1.4
        gc.strokeRoundRect(bounds.x, bounds.y, bounds.boxWidth(), bounds.boxHeight(), 10.0, 10.0)
    }

    private fun drawPointerArrow(gc: GraphicsContext, bounds: TooltipBounds, targetX: Double, targetY: Double) {
        val arrowX = targetX.coerceIn(bounds.x + 12.0, bounds.right() - 12.0)
        val isAbove = bounds.y < targetY
        val arrowY = if (isAbove) bounds.bottom() else bounds.y
        val tipDelta = if (isAbove) 5.0 else -5.0

        gc.fill = Color.rgb(99, 102, 241, 0.9)
        gc.beginPath()
        gc.moveTo(arrowX - 5.0, arrowY)
        gc.lineTo(arrowX + 5.0, arrowY)
        gc.lineTo(arrowX, arrowY + tipDelta)
        gc.closePath()
        gc.fill()
    }

    private fun drawTelemetryValues(gc: GraphicsContext, bounds: TooltipBounds, point: TrajectoryPoint) {
        gc.font = Font.font("System", FontWeight.NORMAL, 10.5)
        gc.fill = Color.rgb(148, 163, 184)
        gc.fillText("v:", bounds.x + 10.0, bounds.y + 20.0)
        gc.fillText("h:", bounds.x + 10.0, bounds.y + 38.0)

        gc.font = Font.font("System", FontWeight.BOLD, 12.0)
        gc.fill = Color.rgb(250, 204, 21)
        gc.fillText(String.format(Locale.US, "%.2f m/s", point.speed), bounds.x + 26.0, bounds.y + 20.0)

        gc.fill = Color.rgb(52, 211, 153)
        gc.fillText(String.format(Locale.US, "%.2f m", point.height), bounds.x + 26.0, bounds.y + 38.0)

        gc.font = Font.font("System", FontWeight.NORMAL, 9.5)
        gc.fill = Color.rgb(148, 163, 184, 0.8)
        gc.fillText(String.format(Locale.US, "t = %.2fs", point.time), bounds.right() - 48.0, bounds.y + 49.0)
    }
}
