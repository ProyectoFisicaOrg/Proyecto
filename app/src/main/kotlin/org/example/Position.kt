package org.example

import kotlin.math.hypot

/**
 * Objeto de valor inmutable que representa una posición en el espacio 2D.
 *
 * @property x Coordenada horizontal.
 * @property y Coordenada vertical.
 */
data class Position(
    val x: Double,
    val y: Double,
) {
    /** Calcula la distancia euclidiana hacia otra posición 2D. */
    fun distanceTo(other: Position): Double = hypot(x - other.x, y - other.y)

    /** Retorna una nueva posición trasladada según el desplazamiento indicado. */
    fun translate(dx: Double, dy: Double): Position = Position(x + dx, y + dy)
}
