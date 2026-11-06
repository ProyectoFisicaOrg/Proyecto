package org.example.model

import kotlin.math.abs
import org.example.collision.ICollisionTarget

/**
 * Objetivo circular desplazable sobre el suelo (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class Target internal constructor(
    private val specs: List<Double>
) : ICollisionTarget {

    constructor(centerX: Double, radius: Double) : this(listOf(centerX, radius))

    init {
        require(specs[1] >= 0.0) { "El radio no puede ser negativo" }
    }

    override fun centerPosition(): Double {
        return specs[0]
    }

    override fun radiusMeters(): Double {
        return specs[1]
    }

    override fun left(): Double {
        return specs[0] - specs[1]
    }

    override fun right(): Double {
        return specs[0] + specs[1]
    }

    fun diameter(): Double {
        return 2.0 * specs[1]
    }

    override fun contains(x: Double): Boolean {
        return x in left()..right()
    }

    fun distanceToCenter(x: Double): Double {
        return abs(x - specs[0])
    }

    override fun isHit(impactX: Double, tolerance: Double): Boolean {
        return distanceToCenter(impactX) <= (specs[1] + tolerance)
    }
}
