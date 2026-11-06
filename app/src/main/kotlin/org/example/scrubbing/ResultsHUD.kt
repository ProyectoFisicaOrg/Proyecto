package org.example.scrubbing

import javafx.geometry.VPos
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment

/**
 * Tarjeta de resultados finales estática y cohesiva en pantalla (SRP + OCP + Encapsulación).
 *
 * Presenta con alineación centrada y márgenes holgados las métricas finales del vuelo:
 * título "RESULTADOS DEL VUELO", "Tiempo de caída:" y "Distancia:".
 */
class ResultsHUD(
    x: Double = 24.0,
    y: Double = 24.0,
    width: Double = 264.0,
    height: Double = 98.0,
    isVisible: Boolean = true,
    padding: Double = 18.0
) : IHudRenderer {
    private val cfg: Pair<List<Double>, Boolean> = Pair(listOf(x, y, width, height, padding), isVisible)

    override fun checkVisible(): Boolean {
        return cfg.second
    }

    override fun formatLines(summary: ISpanishFlightSummaryView): List<String> {
        val hdr = if (cfg.first[2] > 0.0) "RESULTADOS DEL VUELO" else ""
        return listOf(
            hdr,
            "Tiempo total:  ${summary.formattedTime()}",
            "Distancia:     ${summary.formattedDistance()}"
        )
    }

    override fun render(gc: GraphicsContext, summary: ISpanishFlightSummaryView) {
        if (!cfg.second) return
        gc.save()
        try {
            drawCardFrame(gc)
            drawCardHeader(gc)
            drawMetricRow(gc, "Tiempo de caída:", summary.formattedTime(), true)
            drawMetricRow(gc, "Distancia:", summary.formattedDistance(), false)
        } finally {
            gc.restore()
        }
    }

    private fun drawCardFrame(gc: GraphicsContext) {
        val layout = cfg.first
        val x = layout[0]
        val y = layout[1]
        val w = layout[2]
        val h = layout[3]
        gc.fill = Color.rgb(0, 0, 0, 0.35)
        gc.fillRoundRect(x + 3.0, y + 4.0, w, h, 16.0, 16.0)
        gc.fill = Color.rgb(15, 23, 42, 0.92)
        gc.fillRoundRect(x, y, w, h, 16.0, 16.0)
        gc.stroke = Color.rgb(56, 189, 248, 0.82)
        gc.lineWidth = 1.6
        gc.strokeRoundRect(x, y, w, h, 16.0, 16.0)
    }

    private fun drawCardHeader(gc: GraphicsContext) {
        val layout = cfg.first
        val x = layout[0]
        val y = layout[1]
        val w = layout[2]
        val pad = layout[4]
        val accentWidth = 38.0
        gc.fill = Color.rgb(56, 189, 248, 0.90)
        gc.fillRoundRect(x + (w - accentWidth) / 2.0, y + 8.0, accentWidth, 3.0, 3.0, 3.0)
        gc.fill = Color.rgb(241, 245, 249)
        gc.font = Font.font("System", FontWeight.BOLD, 12.5)
        gc.textAlign = TextAlignment.CENTER
        gc.textBaseline = VPos.CENTER
        gc.fillText("RESULTADOS DEL VUELO", x + w / 2.0, y + 24.0)
        gc.stroke = Color.rgb(255, 255, 255, 0.16)
        gc.lineWidth = 1.0
        gc.strokeLine(x + pad, y + 38.0, x + w - pad, y + 38.0)
    }

    private fun drawMetricRow(gc: GraphicsContext, label: String, value: String, isTimeRow: Boolean) {
        val layout = cfg.first
        val x = layout[0]
        val rowY = layout[1] + (if (isTimeRow) 56.0 else 78.0)
        val w = layout[2]
        val pad = layout[4]
        gc.font = Font.font("System", FontWeight.NORMAL, 12.0)
        gc.fill = Color.rgb(148, 163, 184)
        gc.textAlign = TextAlignment.LEFT
        gc.textBaseline = VPos.CENTER
        gc.fillText(label, x + pad, rowY)
        gc.font = Font.font("System", FontWeight.BOLD, 13.0)
        gc.fill = if (isTimeRow) Color.rgb(250, 204, 21) else Color.rgb(52, 211, 153)
        gc.textAlign = TextAlignment.RIGHT
        gc.textBaseline = VPos.CENTER
        gc.fillText(value, x + w - pad, rowY)
    }
}
