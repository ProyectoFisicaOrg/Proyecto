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
}
