package org.example

import kotlin.math.hypot

/**
 * Objeto de valor inmutable y encapsulado que representa una posición en el espacio 2D (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class Position internal constructor(
    private val coords: List<Double>
) : IPosition {

    constructor(x: Double, y: Double) : this(listOf(x, y))

    override fun horizontal(): Double {
        return coords[0]
    }

    override fun vertical(): Double {
        return coords[1]
    }

    override fun distanceTo(other: IPosition): Double {
        return hypot(coords[0] - other.x, coords[1] - other.y)
    }

    override fun translate(dx: Double, dy: Double): Position {
        return Position(coords[0] + dx, coords[1] + dy)
    }
}
