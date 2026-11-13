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
}
