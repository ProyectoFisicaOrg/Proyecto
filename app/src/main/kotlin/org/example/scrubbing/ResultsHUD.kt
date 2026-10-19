package org.example.scrubbing

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

/**
 * Contrato para renderizadores de tarjetas o paneles del HUD (ISP + OCP).
 *
 * Permite extender nuevas formas de visualización en el HUD sin modificar la lógica
 * del motor de scrubbing ni la escena gráfica.
 */
interface IHudRenderer {
    /** Dibuja la tarjeta de resultados con el resumen provisto */
    fun render(gc: GraphicsContext, summary: FlightSummary)
}

/**
 * Tarjeta de resultados finales estática en pantalla (SRP + OCP).
 *
 * Responsabilidad Única:
 *  - Presentar de forma clara, estética y organizada en el canvas las métricas finales
 *    del disparo tras el impacto: **Tiempo total de caída** y **Distancia recorrida**.
 *
 * Principio Abierto/Cerrado (OCP):
 *  - Coordenadas de ubicación (x, y), dimensiones (width, height), fuentes y colores
 *    son totalmente configurables mediante el constructor.
 *
 * @param x Posición horizontal de la esquina superior izquierda de la tarjeta en píxeles.
 * @param y Posición vertical de la esquina superior izquierda de la tarjeta en píxeles.
 * @param width Ancho de la tarjeta en píxeles.
 * @param height Alto de la tarjeta en píxeles.
 */
class ResultsHUD(
    var x: Double = 25.0,
    var y: Double = 25.0,
    val width: Double = 230.0,
    val height: Double = 90.0,
    val isVisible: Boolean = true
) : IHudRenderer {

    /**
     * Genera las líneas de texto formateadas para visualización o pruebas unitarias sin UI.
     */
    fun formatLines(summary: FlightSummary): List<String> = listOf(
        "RESULTADOS DEL VUELO",
        "Tiempo total:  ${summary.formattedTime()}",
        "Distancia:     ${summary.formattedDistance()}"
    )

    /**
     * Renderiza la tarjeta de resultados sobre el contexto gráfico del canvas.
     *
     * @param gc Contexto gráfico de JavaFX.
     * @param summary Resumen del vuelo con tiempo total de caída y distancia total.
     */
    override fun render(gc: GraphicsContext, summary: FlightSummary) {
        if (!isVisible) return

        // 1. Sombra suave para dar profundidad a la tarjeta
        gc.fill = Color.rgb(0, 0, 0, 0.35)
        gc.fillRoundRect(x + 3.0, y + 4.0, width, height, 14.0, 14.0)

        // 2. Fondo semitransparente oscuro de estilo "glassmorphism"
        gc.fill = Color.rgb(15, 23, 42, 0.88)
        gc.fillRoundRect(x, y, width, height, 14.0, 14.0)

        // 3. Borde elegante con acento cian/azul
        gc.stroke = Color.rgb(56, 189, 248, 0.75)
        gc.lineWidth = 1.6
        gc.strokeRoundRect(x, y, width, height, 14.0, 14.0)

        // 4. Barra de acento decorativa superior
        gc.fill = Color.rgb(56, 189, 248, 0.9)
        gc.fillRoundRect(x + 12.0, y + 10.0, 24.0, 3.0, 3.0, 3.0)

        // 5. Título de la tarjeta
        gc.fill = Color.rgb(241, 245, 249)
        gc.font = Font.font("System", FontWeight.BOLD, 12.5)
        gc.fillText("RESULTADOS DEL VUELO", x + 42.0, y + 21.0)

        // 6. Línea divisoria sutil
        gc.stroke = Color.rgb(255, 255, 255, 0.12)
        gc.lineWidth = 1.0
        gc.strokeLine(x + 12.0, y + 28.0, x + width - 12.0, y + 28.0)

        // 7. Métrica 1: Tiempo total de caída
        gc.font = Font.font("System", FontWeight.NORMAL, 12.0)
        gc.fill = Color.rgb(148, 163, 184)
        gc.fillText("Tiempo de caída:", x + 14.0, y + 50.0)

        gc.font = Font.font("System", FontWeight.BOLD, 13.0)
        gc.fill = Color.rgb(250, 204, 21) // Amarillo ámbar brillante
        gc.fillText(summary.formattedTime(), x + width - 70.0, y + 50.0)

        // 8. Métrica 2: Distancia recorrida
        gc.font = Font.font("System", FontWeight.NORMAL, 12.0)
        gc.fill = Color.rgb(148, 163, 184)
        gc.fillText("Distancia:", x + 14.0, y + 74.0)

        gc.font = Font.font("System", FontWeight.BOLD, 13.0)
        gc.fill = Color.rgb(52, 211, 153) // Verde esmeralda brillante
        gc.fillText(summary.formattedDistance(), x + width - 70.0, y + 74.0)
    }
}
