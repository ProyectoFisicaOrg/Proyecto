package org.example.scrubbing

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color

/**
 * Estado visual encapsulado de interacción del proyectil sobre el canvas (SRP).
 */
class InteractiveEffectState(
    private val x: Double,
    private val y: Double,
    private val isDragging: Boolean,
    private val isHovered: Boolean,
    private val radius: Double = 8.0
) {
    fun isActive(): Boolean = isDragging || isHovered

    fun isDraggingNow(): Boolean = isDragging

    fun centerX(): Double = x

    fun centerY(): Double = y

    fun projectileRadius(): Double = radius
}

/**
 * Contrato para efectos visuales interactivos del proyectil durante scrubbing (DIP).
 */
interface IInteractiveProjectileRenderer {
    fun drawInteractiveEffects(gc: GraphicsContext, effectState: InteractiveEffectState)
}

/**
 * Renderizador de efectos visuales interactivos para el proyectil durante el modo Scrubbing (SRP + DIP).
 */
class InteractiveProjectileRenderer : IInteractiveProjectileRenderer {

    override fun drawInteractiveEffects(gc: GraphicsContext, effectState: InteractiveEffectState) {
        if (!effectState.isActive()) return

        gc.save()
        try {
            if (effectState.isDraggingNow()) {
                drawDraggingFeedback(gc, effectState.centerX(), effectState.centerY(), effectState.projectileRadius())
            } else {
                drawHoveredFeedback(gc, effectState.centerX(), effectState.centerY(), effectState.projectileRadius())
            }
        } finally {
            gc.restore()
        }
    }

    private fun drawDraggingFeedback(gc: GraphicsContext, x: Double, y: Double, radius: Double) {
        gc.fill = Color.rgb(250, 204, 21, 0.25)
        gc.fillOval(x - radius * 2.2, y - radius * 2.2, radius * 4.4, radius * 4.4)

        gc.stroke = Color.rgb(250, 204, 21, 0.9)
        gc.lineWidth = 2.0
        gc.strokeOval(x - radius * 1.5, y - radius * 1.5, radius * 3.0, radius * 3.0)

        gc.stroke = Color.rgb(255, 255, 255, 0.7)
        gc.lineWidth = 1.0
        gc.strokeLine(x - radius * 2.0, y, x + radius * 2.0, y)
        gc.strokeLine(x, y - radius * 2.0, x, y + radius * 2.0)
    }

    private fun drawHoveredFeedback(gc: GraphicsContext, x: Double, y: Double, radius: Double) {
        gc.fill = Color.rgb(56, 189, 248, 0.20)
        gc.fillOval(x - radius * 1.8, y - radius * 1.8, radius * 3.6, radius * 3.6)

        gc.stroke = Color.rgb(56, 189, 248, 0.8)
        gc.lineWidth = 1.5
        gc.strokeOval(x - radius * 1.3, y - radius * 1.3, radius * 2.6, radius * 2.6)
    }
}
