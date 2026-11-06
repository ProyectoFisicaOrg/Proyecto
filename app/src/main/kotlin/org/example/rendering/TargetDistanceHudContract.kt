package org.example.rendering

import javafx.scene.canvas.GraphicsContext

/**
 * Contrato para el indicador visual dinámico de distancia hacia el objetivo (ISP + DIP).
 */
interface ITargetDistanceHud {
    fun distanceMeters(targetCanvasX: Double): Double
    fun formatDistance(targetCanvasX: Double): String
    fun render(gc: GraphicsContext, targetCanvasX: Double)
}
