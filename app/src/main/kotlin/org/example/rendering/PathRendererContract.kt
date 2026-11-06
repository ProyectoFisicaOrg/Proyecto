package org.example.rendering

/**
 * Contrato abstracto para el renderizador de trayectorias (DIP).
 */
interface IPathRenderer {
    fun checkFrozen(): Boolean
    fun updateFrozen(frozen: Boolean)
    fun recordLaunch(nowNano: Long = System.nanoTime())
    fun addPoint(x: Double, y: Double, nowNano: Long = System.nanoTime())
    fun clear()
    fun getTrailPoints(): List<ITrailPoint>
    fun getFadeProgress(nowNano: Long = System.nanoTime()): Double
    fun getOpacity(nowNano: Long = System.nanoTime()): Double
    fun getPointAlpha(point: ITrailPoint, nowNano: Long = System.nanoTime()): Double
    fun drawPath(nowNano: Long = System.nanoTime())

    fun freeze() = updateFrozen(true)
    fun unfreeze() = updateFrozen(false)
    fun currentPointCount(): Int = getTrailPoints().size
    fun getPoints(): List<Pair<Double, Double>> = getTrailPoints().map { Pair(it.x, it.y) }

    var isFrozen: Boolean
        get() = checkFrozen()
        set(value) = updateFrozen(value)

    val pointCount: Int get() = currentPointCount()
}
