package org.example.rendering

import java.util.Random

/**
 * Implementación estándar de distribución uniforme para reposicionar el objetivo (SRP + DIP).
 */
class UniformTargetRandomizer(
    private val random: Random = Random()
) : ITargetRandomizer {

    override fun calculateRandomX(minX: Double, maxX: Double): Double {
        require(maxX >= minX) { "El límite superior ($maxX) no puede ser menor que el límite inferior ($minX)" }
        return minX + random.nextDouble() * (maxX - minX)
    }
}

/**
 * Instancia compartida por defecto de [ITargetRandomizer] (DIP).
 */
object DefaultTargetRandomizer : ITargetRandomizer {
    private val delegate: ITargetRandomizer = UniformTargetRandomizer()

    override fun calculateRandomX(minX: Double, maxX: Double): Double {
        return delegate.calculateRandomX(minX, maxX)
    }
}
