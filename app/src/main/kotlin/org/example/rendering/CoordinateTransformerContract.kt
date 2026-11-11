package org.example.rendering

/**
 * Abstracción para transformar coordenadas entre metros físicos y píxeles del canvas (SRP + DIP).
 */
interface CoordinateTransformer {
    fun toCanvasX(physicsX: Double): Double
    fun toCanvasY(physicsY: Double): Double
    fun toPhysicsX(canvasX: Double): Double
    fun toPhysicsY(canvasY: Double): Double
    fun toCanvasDistance(physicsMeters: Double): Double
    fun toPhysicsDistance(canvasPixels: Double): Double
}
