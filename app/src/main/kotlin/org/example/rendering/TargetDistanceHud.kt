package org.example.rendering

import java.util.Locale
import javafx.geometry.VPos
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment

/**
 * Renderizador cohesivo del HUD superior "DISTANCIA AL OBJETIVO" y su conector al campo editable bajo el blanco (SRP + DIP).
 */
class TargetDistanceHud(
    cfg: ISceneConfig,
    transformer: CoordinateTransformer,
    cardWidth: Double = 248.0,
    cardHeight: Double = 62.0
) : ITargetDistanceHud {
    private val hudSpec: Pair<Pair<ISceneConfig, CoordinateTransformer>, List<Double>> =
        Pair(Pair(cfg, transformer), listOf(cardWidth, cardHeight, 16.0))

    override fun distanceMeters(targetCanvasX: Double): Double {
        return hudSpec.first.second.toPhysicsX(targetCanvasX).coerceAtLeast(0.0)
    }

    override fun formatDistance(targetCanvasX: Double): String {
        return String.format(Locale.US, "%.2f m", distanceMeters(targetCanvasX))
    }

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
        val dims = hudSpec.second
        val cardW = dims[0]
        val cardH = dims[1]
        val cardX = (hudSpec.first.first.canvasWidth - cardW) / 2.0
        val cardY = 24.0
        gc.fill = Color.rgb(0, 0, 0, 0.32)
        gc.fillRoundRect(cardX + 3.0, cardY + 4.0, cardW, cardH, 14.0, 14.0)
        gc.fill = Color.rgb(15, 23, 42, 0.90)
        gc.fillRoundRect(cardX, cardY, cardW, cardH, 14.0, 14.0)
        gc.stroke = Color.rgb(250, 204, 21, 0.80)
        gc.lineWidth = 1.5
        gc.strokeRoundRect(cardX, cardY, cardW, cardH, 14.0, 14.0)
        drawTopCardTexts(gc, cardX, cardY, distanceText)
    }

    private fun drawTopCardTexts(gc: GraphicsContext, cardX: Double, cardY: Double, distanceText: String) {
        val dims = hudSpec.second
        val cardW = dims[0]
        val pad = dims[2]
        gc.fill = Color.rgb(241, 245, 249)
        gc.font = Font.font("System", FontWeight.BOLD, 11.5)
        gc.textAlign = TextAlignment.CENTER
        gc.textBaseline = VPos.CENTER
        gc.fillText("DISTANCIA AL OBJETIVO", cardX + cardW / 2.0, cardY + 18.0)
        gc.stroke = Color.rgb(255, 255, 255, 0.15)
        gc.lineWidth = 1.0
        gc.strokeLine(cardX + pad, cardY + 31.0, cardX + cardW - pad, cardY + 31.0)
        drawDistanceValueRow(gc, cardX, cardY, distanceText)
    }

    private fun drawDistanceValueRow(gc: GraphicsContext, cardX: Double, cardY: Double, distanceText: String) {
        val dims = hudSpec.second
        val cardW = dims[0]
        val pad = dims[2]
        gc.font = Font.font("System", FontWeight.NORMAL, 12.0)
        gc.fill = Color.rgb(148, 163, 184)
        gc.textAlign = TextAlignment.LEFT
        gc.fillText("Desde el cañón:", cardX + pad, cardY + 46.0)
        gc.font = Font.font("System", FontWeight.BOLD, 13.5)
        gc.fill = Color.rgb(250, 204, 21)
        gc.textAlign = TextAlignment.RIGHT
        gc.fillText(distanceText, cardX + cardW - pad, cardY + 46.0)
    }

    private fun drawGroundConnectorTick(gc: GraphicsContext, targetCanvasX: Double) {
        val groundY = hudSpec.first.first.groundY
        gc.stroke = Color.rgb(250, 204, 21, 0.75)
        gc.lineWidth = 1.4
        gc.strokeLine(targetCanvasX, groundY + 20.0, targetCanvasX, groundY + 26.0)
    }
}
