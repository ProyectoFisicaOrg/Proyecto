package org.example

/**
 * Contrato inmutable para posiciones en el plano 2D (ISP + DIP).
 */
interface IPosition {
    fun horizontal(): Double
    fun vertical(): Double
    fun distanceTo(other: IPosition): Double
    fun translate(dx: Double, dy: Double): IPosition

    val x: Double get() = horizontal()
    val y: Double get() = vertical()
}
