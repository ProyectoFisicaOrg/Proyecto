package org.example.collision

/**
 * Contrato geométrico de un objetivo de impacto sobre el suelo (ISP + DIP).
 */
interface ICollisionTarget {
    fun centerPosition(): Double
    fun radiusMeters(): Double
    fun left(): Double
    fun right(): Double
    fun contains(x: Double): Boolean
    fun isHit(impactX: Double, tolerance: Double = 0.0): Boolean

    val centerX: Double get() = centerPosition()
    val radius: Double get() = radiusMeters()
}
