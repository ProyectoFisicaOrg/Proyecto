package org.example.rendering

/**
 * Contrato de dimensiones base del lienzo de simulación (ISP + DIP).
 */
interface ICanvasBounds {
    fun width(): Double
    fun height(): Double

    val canvasWidth: Double get() = width()
    val canvasHeight: Double get() = height()
}

/**
 * Contrato completo de configuración geométrica del escenario (ISP + DIP).
 */
interface ISceneConfig : ICanvasBounds {
    fun groundLevelY(): Double
    fun basePivotX(): Double
    fun basePivotY(): Double
    fun barrelLength(): Double
    fun barrelWidth(): Double

    val groundY: Double get() = groundLevelY()
    val cannonBaseX: Double get() = basePivotX()
    val cannonBaseY: Double get() = basePivotY()
    val cannonLength: Double get() = barrelLength()
    val cannonWidth: Double get() = barrelWidth()
}

/**
 * Configuración geométrica encapsulada del escenario (SRP + Encapsulación).
 */
class SceneConfig(
    canvasWidth: Double = 900.0,
    canvasHeight: Double = 550.0,
) : ISceneConfig {
    private val widthPx: Double = canvasWidth
    private val heightPx: Double = canvasHeight
    private val groundLineY: Double = canvasHeight - 80.0
    private val pivotX: Double = 90.0
    private val barrelLen: Double = 46.0
    private val barrelWid: Double = 12.0

    override fun width(): Double = widthPx

    override fun height(): Double = heightPx

    override fun groundLevelY(): Double = groundLineY

    override fun basePivotX(): Double = pivotX

    override fun basePivotY(): Double = groundLineY

    override fun barrelLength(): Double = barrelLen

    override fun barrelWidth(): Double = barrelWid

    fun isWithinCanvas(x: Double, y: Double, margin: Double = 0.0): Boolean =
        x in -margin..(widthPx + margin) && y in -margin..(heightPx + margin)

    fun clampToCanvasX(x: Double, margin: Double = 0.0): Double =
        x.coerceIn(margin, widthPx - margin)

    fun clampToGround(y: Double): Double =
        y.coerceAtMost(groundLineY)

    fun cannonMuzzlePosition(angleDeg: Double): Pair<Double, Double> {
        val angleRad = Math.toRadians(angleDeg)
        val mx = pivotX + Math.cos(angleRad) * barrelLen
        val my = groundLineY - Math.sin(angleRad) * barrelLen
        return Pair(mx, my)
    }
}
