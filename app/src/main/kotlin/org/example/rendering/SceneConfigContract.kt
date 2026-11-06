package org.example.rendering

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
