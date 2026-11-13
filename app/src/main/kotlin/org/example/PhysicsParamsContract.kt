package org.example

/**
 * Contrato de lectura de parámetros físicos del disparo (ISP + DIP).
 */
interface IPhysicsParams {
    fun angleDegrees(): Double
    fun launchVelocity(): Double
    fun gravityAcceleration(): Double
    fun projectileMass(): Double
    fun angleRadians(): Double = Math.toRadians(angleDegrees())
}
