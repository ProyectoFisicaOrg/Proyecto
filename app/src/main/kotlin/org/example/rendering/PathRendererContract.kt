package org.example.rendering

/**
 * Contrato abstracto para el renderizador de trayectorias (DIP).
 */
interface IPathFadeMetrics {
    fun getFadeProgress(nowNano: Long = System.nanoTime()): Double
    fun getOpacity(nowNano: Long = System.nanoTime()): Double
    fun getPointAlpha(point: ITrailPoint, nowNano: Long = System.nanoTime()): Double
}

interface IPathRenderer : IPathFadeMetrics {
    fun checkFrozen(): Boolean
    fun updateFrozen(frozen: Boolean)
    fun recordLaunch(nowNano: Long = System.nanoTime())
    fun addPoint(x: Double, y: Double, nowNano: Long = System.nanoTime())
    fun clear()
    fun getTrailPoints(): List<ITrailPoint>
    fun drawPath(nowNano: Long = System.nanoTime())
    fun freeze() = updateFrozen(true)
    fun currentPointCount(): Int = getTrailPoints().size
}
