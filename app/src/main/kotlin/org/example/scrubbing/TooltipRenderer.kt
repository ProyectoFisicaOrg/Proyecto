package org.example.scrubbing

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import java.util.Locale

/**
 * Contrato para renderizadores de etiquetas flotantes (ISP + OCP).
 */
interface ITooltipRenderer {
    /** Dibuja la etiqueta flotante en la posición del proyectil */
    fun render(
        gc: GraphicsContext,
        point: TrajectoryPoint,
        targetCanvasX: Double,
        targetCanvasY: Double,
        canvasWidth: Double = 900.0,
        canvasHeight: Double = 550.0
    )
}

/**
 * Rectángulo delimitador del tooltip para cálculos geométricos y pruebas unitarias.
 */
data class TooltipBounds(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)

/**
 * Componente flotante que sigue visualmente la posición actual del proyectil (SRP + OCP).
 *
 * Responsabilidad Única:
 *  - Presentar en tiempo real la **Velocidad instantánea** y la **Altura (Y)** del proyectil
 *    en una etiqueta interactiva que acompaña visualmente al cursor mientras se realiza el arrastre.
 *
 * Principio Abierto/Cerrado (OCP):
 *  - Ancho, alto, compensación vertical (offset), colores y formatos son configurables.
 *
 * @param width Ancho base del tooltip en píxeles.
 * @param height Alto base del tooltip en píxeles.
 * @param verticalOffset Distancia vertical entre el proyectil y el tooltip en píxeles.
 */
class TooltipRenderer(
    val width: Double = 155.0,
    val height: Double = 56.0,
    val verticalOffset: Double = 18.0
) : ITooltipRenderer {

    /**
     * Calcula las coordenadas de la caja del tooltip asegurando que permanezca
     * dentro de los márgenes visibles del canvas.
     */
    fun calculateTooltipBounds(
        targetCanvasX: Double,
        targetCanvasY: Double,
        canvasWidth: Double,
        canvasHeight: Double
    ): TooltipBounds {
        // Por defecto, centrado horizontalmente sobre el proyectil y arriba de él
        var x = targetCanvasX - width / 2.0
        var y = targetCanvasY - height - verticalOffset

        // Si se sale por arriba, se posiciona debajo del proyectil
        if (y < 10.0) {
            y = targetCanvasY + verticalOffset + 12.0
        }

        // Clamp horizontal dentro de los márgenes del canvas
        val margin = 10.0
        x = x.coerceIn(margin, canvasWidth - width - margin)
        y = y.coerceIn(margin, canvasHeight - height - margin)

        return TooltipBounds(x, y, width, height)
    }

    /**
     * Retorna las cadenas de telemetría formateadas (útil para pruebas unitarias sin JavaFX).
     */
    fun formatLines(point: TrajectoryPoint): List<String> = listOf(
        String.format(Locale.US, "Velocidad: %.2f m/s", point.speed),
        String.format(Locale.US, "Altura (Y): %.2f m", point.height)
    )

    /**
     * Dibuja la etiqueta flotante sobre el contexto gráfico.
     */
    override fun render(
        gc: GraphicsContext,
        point: TrajectoryPoint,
        targetCanvasX: Double,
        targetCanvasY: Double,
        canvasWidth: Double,
        canvasHeight: Double
    ) {
        val bounds = calculateTooltipBounds(targetCanvasX, targetCanvasY, canvasWidth, canvasHeight)

        // 1. Sombra suave de la burbuja
        gc.fill = Color.rgb(0, 0, 0, 0.40)
        gc.fillRoundRect(bounds.x + 2.0, bounds.y + 3.0, bounds.width, bounds.height, 10.0, 10.0)

        // 2. Fondo oscuro semitransparente
        gc.fill = Color.rgb(15, 23, 42, 0.94)
        gc.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10.0, 10.0)

        // 3. Contorno brillante en cian/índigo
        gc.stroke = Color.rgb(99, 102, 241, 0.85) // Índigo vibrante
        gc.lineWidth = 1.4
        gc.strokeRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10.0, 10.0)

        // 4. Pequeña flecha / indicador que apunta al proyectil
        val arrowX = targetCanvasX.coerceIn(bounds.x + 12.0, bounds.x + bounds.width - 12.0)
        val isAbove = bounds.y < targetCanvasY
        val arrowY = if (isAbove) bounds.y + bounds.height else bounds.y

        gc.fill = Color.rgb(99, 102, 241, 0.9)
        gc.beginPath()
        if (isAbove) {
            gc.moveTo(arrowX - 5.0, arrowY)
            gc.lineTo(arrowX + 5.0, arrowY)
            gc.lineTo(arrowX, arrowY + 5.0)
        } else {
            gc.moveTo(arrowX - 5.0, arrowY)
            gc.lineTo(arrowX + 5.0, arrowY)
            gc.lineTo(arrowX, arrowY - 5.0)
        }
        gc.closePath()
        gc.fill()

        // 5. Línea 1: Velocidad instantánea (en amarillo ámbar)
        gc.font = Font.font("System", FontWeight.NORMAL, 10.5)
        gc.fill = Color.rgb(148, 163, 184)
        gc.fillText("v:", bounds.x + 10.0, bounds.y + 20.0)

        gc.font = Font.font("System", FontWeight.BOLD, 12.0)
        gc.fill = Color.rgb(250, 204, 21)
        gc.fillText(String.format(Locale.US, "%.2f m/s", point.speed), bounds.x + 26.0, bounds.y + 20.0)

        // 6. Línea 2: Altura instantánea (en verde lima)
        gc.font = Font.font("System", FontWeight.NORMAL, 10.5)
        gc.fill = Color.rgb(148, 163, 184)
        gc.fillText("h:", bounds.x + 10.0, bounds.y + 38.0)

        gc.font = Font.font("System", FontWeight.BOLD, 12.0)
        gc.fill = Color.rgb(52, 211, 153)
        gc.fillText(String.format(Locale.US, "%.2f m", point.height), bounds.x + 26.0, bounds.y + 38.0)

        // 7. Marca temporal discreta a la derecha
        gc.font = Font.font("System", FontWeight.NORMAL, 9.5)
        gc.fill = Color.rgb(148, 163, 184, 0.8)
        gc.fillText(String.format(Locale.US, "t = %.2fs", point.time), bounds.x + bounds.width - 48.0, bounds.y + 49.0)
    }
}
