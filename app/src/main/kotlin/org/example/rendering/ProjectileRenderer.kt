package org.example.rendering

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment

/**
 * Dibuja el proyectil y el indicador de ángulo (SRP + DIP).
 */
class ProjectileRenderer(
    private val gc: GraphicsContext,
    private val cfg: ISceneConfig
) : IProjectileRenderer {
    private val labelFont: Font = Font.font("Monospace", FontWeight.BOLD, 14.0)

    override fun drawProjectile(x: Double, y: Double) {
        gc.save()
        try {
            gc.fill = Color.rgb(66, 153, 225)
            gc.fillOval(x - 10.0, y - 10.0, 20.0, 20.0)
            gc.fill = Color.rgb(99, 179, 237)
            gc.fillOval(x - 12.0, y - 12.0, 8.0, 8.0)
        } finally {
            gc.restore()
        }
    }

    override fun drawAngleIndicator(angle: Double) {
        val radius = 60.0
        val bx = cfg.basePivotX()
        val by = cfg.basePivotY()
        gc.save()
        try {
            drawAngleArc(bx, by, radius, angle)
            drawAngleLabel(bx, by, radius, angle)
        } finally {
            gc.restore()
        }
    }

    private fun drawAngleArc(bx: Double, by: Double, radius: Double, angle: Double) {
        gc.stroke = Color.rgb(233, 69, 96, 0.5)
        gc.lineWidth = 2.0
        gc.setLineDashes(6.0, 6.0)
        gc.strokeArc(bx - radius, by - radius, radius * 2.0, radius * 2.0, 90.0, -angle, ArcType.OPEN)
        gc.setLineDashes()
    }

    private fun drawAngleLabel(bx: Double, by: Double, radius: Double, angle: Double) {
        val labelAngle = Math.toRadians(angle) / 2.0 - Math.PI / 2.0
        val labelX = bx + Math.cos(labelAngle) * (radius + 25.0)
        val labelY = by + Math.sin(labelAngle) * (radius + 25.0)
        gc.fill = Color.rgb(233, 69, 96)
        gc.font = labelFont
        gc.textAlign = TextAlignment.CENTER
        gc.fillText("${angle.toInt()}°", labelX - 15.0, labelY + 5.0)
    }
}
