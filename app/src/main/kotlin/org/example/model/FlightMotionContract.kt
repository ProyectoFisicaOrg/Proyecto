package org.example.model

/**
 * Contrato de movimiento y animación del proyectil en el canvas (ISP + DIP).
 */
interface IFlightMotionState {
    fun checkAnimating(): Boolean
    fun changeAnimating(active: Boolean)
    fun flightStartTimeNano(): Long
    fun changeFlightStartTimeNano(nano: Long)
    fun currentProjectileCoords(): Pair<Double, Double>
    fun updateProjectilePosition(x: Double, y: Double)
}
