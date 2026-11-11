package org.example.rendering

/**
 * Contratos de punto de estela y sub-componentes temporales/gráficos (ISP + DIP).
 */
interface ITrailPoint {
    fun coordX(): Double
    fun coordY(): Double
    fun createdAtNano(): Long
    fun isVeryCloseTo(otherX: Double, otherY: Double): Boolean

    val x: Double get() = coordX()
    val y: Double get() = coordY()
}
