package org.example.model

import kotlin.math.abs

/**
 * Objetivo circular desplazable sobre el suelo (SRP + Encapsulación).
 *
 * @param centerX Posición X del centro del objetivo en el suelo (metros)
 * @param radius Radio del objetivo (metros)
 */
data class Target(
    val centerX: Double,
    val radius: Double
) {
    init {
        require(radius >= 0) { "El radio no puede ser negativo" }
    }

    /** Borde izquierdo del objetivo */
    fun left(): Double = centerX - radius

    /** Borde derecho del objetivo */
    fun right(): Double = centerX + radius

    /** Diámetro del objetivo */
    fun diameter(): Double = 2.0 * radius

    /** Determina si una coordenada horizontal x cae dentro del objetivo. */
    fun contains(x: Double): Boolean = x in left()..right()

    /** Calcula la distancia horizontal absoluta desde una coordenada x al centro del objetivo. */
    fun distanceToCenter(x: Double): Double = abs(x - centerX)

    /** Determina si un punto de impacto impacta en el blanco dentro de una tolerancia. */
    fun isHit(impactX: Double, tolerance: Double = 0.0): Boolean =
        distanceToCenter(impactX) <= (radius + tolerance)
}
