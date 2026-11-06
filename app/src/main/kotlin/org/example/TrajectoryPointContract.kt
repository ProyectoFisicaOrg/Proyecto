package org.example

/**
 * Contrato de estado cinemático puntual en la trayectoria (ISP + DIP).
 */
interface ITrajectoryPoint {
    fun posX(): Double
    fun posY(): Double
    fun velX(): Double
    fun velY(): Double
    fun timeSeconds(): Double

    val x: Double get() = posX()
    val y: Double get() = posY()
    val vx: Double get() = velX()
    val vy: Double get() = velY()
    val t: Double get() = timeSeconds()
}
