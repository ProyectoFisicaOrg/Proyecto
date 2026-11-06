package org.example.rendering

/**
 * Contratos del reloj de desvanecimiento y trazador de estela (ISP + DIP).
 */
interface ITrailFadeClock {
    fun checkFrozen(): Boolean
    fun updateFrozen(frozen: Boolean)
    fun recordLaunch(nowNano: Long)
    fun reset()
    fun computeProgress(nowNano: Long, fallbackFirstPointNano: Long): Double
}

interface ITrailStrokePainter {
    fun paint(points: List<ITrailPoint>, fadeProgress: Double)
}
