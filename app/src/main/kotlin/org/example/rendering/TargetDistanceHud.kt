package org.example.rendering

import java.util.Locale
import javafx.geometry.VPos
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment

/**
 * Contrato para el indicador visual dinámico de distancia hacia el objetivo (ISP + DIP).
 */
interface ITargetDistanceHud {
    fun distanceMeters(targetCanvasX: Double): Double
    fun formatDistance(targetCanvasX: Double): String
    fun render(gc: GraphicsContext, targetCanvasX: Double)
}

/**
 * Renderizador dedicado del HUD superior "DISTANCIA AL OBJETIVO" y el conector hacia el campo editable bajo el blanco (SRP + DIP).
 */
class TargetDistanceHud(
    private val cfg: ISceneConfig,
    private val transformer: CoordinateTransformer,
    private val cardWidth: Double = 248.0,
    private val cardHeight: Double = 62.0
) : ITargetDistanceHud {
    private val padding: Double = 16.0

    override fun distanceMeters(targetCanvasX: Double): Double =
        transformer.toPhysicsX(targetCanvasX).coerceAtLeast(0.0)

    override fun formatDistance(targetCanvasX: Double): String =
        String.format(Locale.US, "%.2f m", distanceMeters(targetCanvasX))

    override fun render(gc: GraphicsContext, targetCanvasX: Double) {
        val distanceText = formatDistance(targetCanvasX)
        gc.save()
        try {
            drawTopCard(gc, distanceText)
            drawGroundConnectorTick(gc, targetCanvasX)
        } finally {
            gc.restore()
        }
    }

    private fun drawTopCard(gc: GraphicsContext, distanceText: String) {
        val cardX = (cfg.canvasWidth - cardWidth) / 2.0
        val cardY = 24.0

        gc.fill = Color.rgb(0, 0, 0, 0.32)
        gc.fillRoundRect(cardX + 3.0, cardY + 4.0, cardWidth, cardHeight, 14.0, 14.0)

        gc.fill = Color.rgb(15, 23, 42, 0.90)
        gc.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 14.0, 14.0)

        gc.stroke = Color.rgb(250, 204, 21, 0.80)
        gc.lineWidth = 1.5
        gc.strokeRoundRect(cardX, cardY, cardWidth, cardHeight, 14.0, 14.0)

        drawTopCardTexts(gc, cardX, cardY, distanceText)
    }

    private fun drawTopCardTexts(gc: GraphicsContext, cardX: Double, cardY: Double, distanceText: String) {
        gc.fill = Color.rgb(241, 245, 249)
        gc.font = Font.font("System", FontWeight.BOLD, 11.5)
        gc.textAlign = TextAlignment.CENTER
        gc.textBaseline = VPos.CENTER
        gc.fillText("DISTANCIA AL OBJETIVO", cardX + cardWidth / 2.0, cardY + 18.0)

        gc.stroke = Color.rgb(255, 255, 255, 0.15)
        gc.lineWidth = 1.0
        gc.strokeLine(cardX + padding, cardY + 31.0, cardX + cardWidth - padding, cardY + 31.0)

        gc.font = Font.font("System", FontWeight.NORMAL, 12.0)
        gc.fill = Color.rgb(148, 163, 184)
        gc.textAlign = TextAlignment.LEFT
        gc.fillText("Desde el cañón:", cardX + padding, cardY + 46.0)

        gc.font = Font.font("System", FontWeight.BOLD, 13.5)
        gc.fill = Color.rgb(250, 204, 21)
        gc.textAlign = TextAlignment.RIGHT
        gc.fillText(distanceText, cardX + cardWidth - padding, cardY + 46.0)
    }

    private fun drawGroundConnectorTick(gc: GraphicsContext, targetCanvasX: Double) {
        gc.stroke = Color.rgb(250, 204, 21, 0.75)
        gc.lineWidth = 1.4
        gc.strokeLine(targetCanvasX, cfg.groundY + 20.0, targetCanvasX, cfg.groundY + 26.0)
    }
}
