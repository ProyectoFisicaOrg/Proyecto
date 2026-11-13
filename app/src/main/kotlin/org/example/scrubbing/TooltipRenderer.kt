package org.example.scrubbing

import java.util.Locale
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

/**
 * Coordenadas encapsuladas de anclaje y dimensiones del lienzo para posicionar el tooltip (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class TooltipAnchor internal constructor(
    private val values: List<Double>
) : ITooltipAnchor {

    constructor(
        targetX: Double,
        targetY: Double,
        canvasWidth: Double = 900.0,
        canvasHeight: Double = 550.0
    ) : this(listOf(targetX, targetY, canvasWidth, canvasHeight))

    override fun x(): Double {
        return values[0]
    }

    override fun y(): Double {
        return values[1]
    }

    override fun canvasW(): Double {
        return values[2]
    }

    override fun canvasH(): Double {
        return values[3]
    }
}

/**
 * Rectángulo delimitador encapsulado del tooltip con operaciones geométricas de dominio (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class TooltipBounds internal constructor(
    private val coords: List<Double>
) : ITooltipBounds {

    constructor(x: Double, y: Double, width: Double, height: Double) : this(listOf(x, y, width, height))

    override fun leftX(): Double {
        return coords[0]
    }

    override fun topY(): Double {
        return coords[1]
    }

    override fun boxWidth(): Double {
        return coords[2]
    }

    override fun boxHeight(): Double {
        return coords[3]
    }

    override fun right(): Double {
        return coords[0] + coords[2]
    }

    override fun bottom(): Double {
        return coords[1] + coords[3]
    }

    override fun contains(px: Double, py: Double): Boolean {
        return px in coords[0]..right() && py in coords[1]..bottom()
    }
}

/**
 * Componente flotante cohesivo que sigue visualmente la posición actual del proyectil (SRP + OCP).
 */
class TooltipRenderer(
    width: Double = 155.0,
    height: Double = 56.0,
    verticalOffset: Double = 18.0,
    boundsFactory: (Double, Double, Double, Double) -> ITooltipBounds = ::TooltipBounds
) : ITooltipRenderer {
    private val cfg: Pair<List<Double>, (Double, Double, Double, Double) -> ITooltipBounds> =
        Pair(listOf(width, height, verticalOffset), boundsFactory)

    override fun calculateTooltipBounds(targetCanvasX: Double, targetCanvasY: Double, canvasWidth: Double, canvasHeight: Double): ITooltipBounds {
        val dims = cfg.first
        val w = dims[0]
        val h = dims[1]
        val offset = dims[2]
        var x = targetCanvasX - w / 2.0
        var y = targetCanvasY - h - offset
        if (y < 10.0) {
            y = targetCanvasY + offset + 12.0
        }
        val margin = 10.0
        x = x.coerceIn(margin, canvasWidth - w - margin)
        y = y.coerceIn(margin, canvasHeight - h - margin)
        return cfg.second(x, y, w, h)
    }

    override fun formatLines(point: ISpanishKinematicView): List<String> {
        val active = cfg.first[0] > 0.0
        val spd = if (active) point.speedValue() else 0.0
        return listOf(
            String.format(Locale.US, "Velocidad: %.2f m/s", spd),
            String.format(Locale.US, "Altura (Y): %.2f m", point.heightValue())
        )
    }

    override fun render(gc: GraphicsContext, point: ISpanishKinematicView, anchor: ITooltipAnchor) {
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

    private fun drawBubbleBox(gc: GraphicsContext, bounds: ITooltipBounds) {
        val bx = bounds.leftX()
        val by = bounds.topY()
        gc.fill = Color.rgb(0, 0, 0, 0.40)
        gc.fillRoundRect(bx + 2.0, by + 3.0, bounds.boxWidth(), bounds.boxHeight(), 10.0, 10.0)
        gc.fill = Color.rgb(15, 23, 42, 0.94)
        gc.fillRoundRect(bx, by, bounds.boxWidth(), bounds.boxHeight(), 10.0, 10.0)
        gc.stroke = Color.rgb(99, 102, 241, 0.85)
        gc.lineWidth = 1.4
        gc.strokeRoundRect(bx, by, bounds.boxWidth(), bounds.boxHeight(), 10.0, 10.0)
    }

    private fun drawPointerArrow(gc: GraphicsContext, bounds: ITooltipBounds, targetX: Double, targetY: Double) {
        val by = bounds.topY()
        val arrowX = targetX.coerceIn(bounds.leftX() + 12.0, bounds.right() - 12.0)
        val isAbove = by < targetY
        val arrowY = if (isAbove) bounds.bottom() else by
        val tipDelta = if (isAbove) 5.0 else -5.0
        gc.fill = Color.rgb(99, 102, 241, 0.9)
        gc.beginPath()
        gc.moveTo(arrowX - 5.0, arrowY)
        gc.lineTo(arrowX + 5.0, arrowY)
        gc.lineTo(arrowX, arrowY + tipDelta)
        gc.closePath()
        gc.fill()
    }

    private fun drawTelemetryValues(gc: GraphicsContext, bounds: ITooltipBounds, point: ISpanishKinematicView) {
        val bx = bounds.leftX()
        val by = bounds.topY()
        gc.font = Font.font("System", FontWeight.NORMAL, 10.5)
        gc.fill = Color.rgb(148, 163, 184)
        gc.fillText("v:", bx + 10.0, by + 20.0)
        gc.fillText("h:", bx + 10.0, by + 38.0)
        gc.font = Font.font("System", FontWeight.BOLD, 12.0)
        gc.fill = Color.rgb(250, 204, 21)
        gc.fillText(String.format(Locale.US, "%.2f m/s", point.speedValue()), bx + 26.0, by + 20.0)
        gc.fill = Color.rgb(52, 211, 153)
        gc.fillText(String.format(Locale.US, "%.2f m", point.heightValue()), bx + 26.0, by + 38.0)
        gc.font = Font.font("System", FontWeight.NORMAL, 9.5)
        gc.fill = Color.rgb(148, 163, 184, 0.8)
        gc.fillText(String.format(Locale.US, "t = %.2fs", point.timeValue()), bounds.right() - 48.0, by + 49.0)
    }
}
