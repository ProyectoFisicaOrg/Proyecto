package org.example.scrubbing

import javafx.geometry.VPos
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment

/**
 * Contrato para renderizadores de tarjetas o paneles del HUD (ISP + OCP).
 */
interface IHudRenderer {
    fun render(gc: GraphicsContext, summary: FlightSummary)
}

/**
 * Especificación visual de una fila de métrica en la tarjeta HUD (SRP).
 */
private class HudMetricRow(
    private val label: String,
    private val value: String,
    private val valueColor: Color,
    private val rowY: Double
) {
    fun draw(gc: GraphicsContext, cardX: Double, cardWidth: Double, padding: Double) {
        gc.font = Font.font("System", FontWeight.NORMAL, 12.0)
        gc.fill = Color.rgb(148, 163, 184)
        gc.textAlign = TextAlignment.LEFT
        gc.textBaseline = VPos.CENTER
        gc.fillText(label, cardX + padding, rowY)

        gc.font = Font.font("System", FontWeight.BOLD, 13.0)
        gc.fill = valueColor
        gc.textAlign = TextAlignment.RIGHT
        gc.textBaseline = VPos.CENTER
        gc.fillText(value, cardX + cardWidth - padding, rowY)
    }
}

/**
 * Tarjeta de resultados finales estática en pantalla (SRP + OCP).
 *
 * Presenta con alineación centrada y márgenes holgados las métricas finales del vuelo:
 * título "RESULTADOS DEL VUELO", "Tiempo de caída:" y "Distancia:".
 */
class ResultsHUD(
    private var x: Double = 24.0,
    private var y: Double = 24.0,
    private val width: Double = 264.0,
    private val height: Double = 98.0,
    val isVisible: Boolean = true,
    private val padding: Double = 18.0
) : IHudRenderer {

    fun formatLines(summary: FlightSummary): List<String> = listOf(
        "RESULTADOS DEL VUELO",
        "Tiempo total:  ${summary.formattedTime()}",
        "Distancia:     ${summary.formattedDistance()}"
    )

    override fun render(gc: GraphicsContext, summary: FlightSummary) {
        if (!isVisible) return

        gc.save()
        try {
            drawCardFrame(gc)
            drawCardHeader(gc)
            HudMetricRow("Tiempo de caída:", summary.formattedTime(), Color.rgb(250, 204, 21), y + 56.0).draw(gc, x, width, padding)
            HudMetricRow("Distancia:", summary.formattedDistance(), Color.rgb(52, 211, 153), y + 78.0).draw(gc, x, width, padding)
        } finally {
            gc.restore()
        }
    }

    private fun drawCardFrame(gc: GraphicsContext) {
        gc.fill = Color.rgb(0, 0, 0, 0.35)
        gc.fillRoundRect(x + 3.0, y + 4.0, width, height, 16.0, 16.0)

        gc.fill = Color.rgb(15, 23, 42, 0.92)
        gc.fillRoundRect(x, y, width, height, 16.0, 16.0)

        gc.stroke = Color.rgb(56, 189, 248, 0.82)
        gc.lineWidth = 1.6
        gc.strokeRoundRect(x, y, width, height, 16.0, 16.0)
    }

    private fun drawCardHeader(gc: GraphicsContext) {
        val accentWidth = 38.0
        gc.fill = Color.rgb(56, 189, 248, 0.90)
        gc.fillRoundRect(x + (width - accentWidth) / 2.0, y + 8.0, accentWidth, 3.0, 3.0, 3.0)

        gc.fill = Color.rgb(241, 245, 249)
        gc.font = Font.font("System", FontWeight.BOLD, 12.5)
        gc.textAlign = TextAlignment.CENTER
        gc.textBaseline = VPos.CENTER
        gc.fillText("RESULTADOS DEL VUELO", x + width / 2.0, y + 24.0)

        gc.stroke = Color.rgb(255, 255, 255, 0.16)
        gc.lineWidth = 1.0
        gc.strokeLine(x + padding, y + 38.0, x + width - padding, y + 38.0)
    }
}
