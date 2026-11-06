package org.example.rendering

/**
 * Contrato de dimensiones base del lienzo de simulación (ISP + DIP).
 */
interface ICanvasBounds {
    fun width(): Double
    fun height(): Double
    fun isWithinCanvas(x: Double, y: Double, margin: Double = 0.0): Boolean

    val canvasWidth: Double get() = width()
    val canvasHeight: Double get() = height()
}
