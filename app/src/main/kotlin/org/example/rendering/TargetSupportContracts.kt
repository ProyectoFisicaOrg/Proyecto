package org.example.rendering

import javafx.scene.paint.Color

/**
 * Contratos visuales y de arrastre del objetivo en el suelo (ISP + DIP).
 */
interface ITargetVisualFlags {
    fun isAnimating(): Boolean
    fun isHit(): Boolean
    fun isDragging(): Boolean
    fun outerRingColor(): Color
}

interface ITargetCanvasPainter {
    fun draw(x: Double, flags: ITargetVisualFlags)
}

interface ITargetDragInteraction {
    fun checkDragging(): Boolean
    fun validXRange(targetRadius: Double): ClosedFloatingPointRange<Double>
    fun applyTargetX(newX: Double)
    fun cancel()
    fun install(targetRadius: Double)
}
