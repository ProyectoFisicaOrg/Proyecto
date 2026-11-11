package org.example.rendering

/**
 * Contratos de estrategia aleatoria y sub-componentes de interacción del objetivo (ISP + DIP).
 */
interface ITargetRandomizer {
    fun calculateRandomX(minX: Double, maxX: Double): Double
}

interface IPulseController {
    fun start()
    fun stop()
}
