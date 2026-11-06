package org.example

/**
 * Contrato de lectura de parámetros físicos del disparo (ISP + DIP).
 */
interface IPhysicsParams {
    fun angleDegrees(): Double
    fun launchVelocity(): Double
    fun gravityAcceleration(): Double
    fun projectileMass(): Double

    val angleDeg: Double get() = angleDegrees()
    val initialVelocity: Double get() = launchVelocity()
    val gravity: Double get() = gravityAcceleration()
    val mass: Double get() = projectileMass()
    val angleRad: Double get() = Math.toRadians(angleDegrees())
}
