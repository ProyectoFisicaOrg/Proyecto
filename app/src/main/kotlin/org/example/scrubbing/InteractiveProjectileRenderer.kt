package org.example.scrubbing

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color

/**
 * Renderizador de efectos visuales interactivos para el proyectil durante el modo Scrubbing (SRP).
 *
 * Responsabilidad Única:
 *  - Proporcionar retroalimentación visual al usuario (halo de selección, anillo de enfoque,
 *    y efecto de agarre activo) cuando el proyectil se encuentra en modo interactivo sobre la trayectoria.
 */
class InteractiveProjectileRenderer {

    /**
     * Dibuja los efectos visuales del proyectil interactivo.
     *
     * @param gc Contexto gráfico del canvas.
     * @param x Coordenada X del proyectil en el canvas.
     * @param y Coordenada Y del proyectil en el canvas.
     * @param isDragging Indica si el usuario está actualmente arrastrando el proyectil.
     * @param isHovered Indica si el cursor está sobre la trayectoria o el proyectil.
     * @param radius Radio base del proyectil en píxeles.
     */
    fun drawInteractiveEffects(
        gc: GraphicsContext,
        x: Double,
        y: Double,
        isDragging: Boolean,
        isHovered: Boolean,
        radius: Double = 8.0
    ) {
        if (!isDragging && !isHovered) return

        if (isDragging) {
            // Halo brillante ámbar/dorado cuando está siendo arrastrado
            gc.fill = Color.rgb(250, 204, 21, 0.25)
            gc.fillOval(x - radius * 2.2, y - radius * 2.2, radius * 4.4, radius * 4.4)

            // Anillo exterior de enfoque
            gc.stroke = Color.rgb(250, 204, 21, 0.9)
            gc.lineWidth = 2.0
            gc.strokeOval(x - radius * 1.5, y - radius * 1.5, radius * 3.0, radius * 3.0)

            // Retícula o cruz sutil
            gc.stroke = Color.rgb(255, 255, 255, 0.7)
            gc.lineWidth = 1.0
            gc.strokeLine(x - radius * 2.0, y, x + radius * 2.0, y)
            gc.strokeLine(x, y - radius * 2.0, x, y + radius * 2.0)
        } else if (isHovered) {
            // Halo cian suave para indicar que es interactivo
            gc.fill = Color.rgb(56, 189, 248, 0.20)
            gc.fillOval(x - radius * 1.8, y - radius * 1.8, radius * 3.6, radius * 3.6)

            gc.stroke = Color.rgb(56, 189, 248, 0.8)
            gc.lineWidth = 1.5
            gc.strokeOval(x - radius * 1.3, y - radius * 1.3, radius * 2.6, radius * 2.6)
        }
    }
}
