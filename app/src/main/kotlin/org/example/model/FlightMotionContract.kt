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

    var isAnimating: Boolean
        get() = checkAnimating()
        set(value) = changeAnimating(value)

    var animationStartTime: Long
        get() = flightStartTimeNano()
        set(value) = changeFlightStartTimeNano(value)

    var projectileX: Double
        get() = currentProjectileCoords().first
        set(value) = updateProjectilePosition(value, currentProjectileCoords().second)

    var projectileY: Double
        get() = currentProjectileCoords().second
        set(value) = updateProjectilePosition(currentProjectileCoords().first, value)
}
