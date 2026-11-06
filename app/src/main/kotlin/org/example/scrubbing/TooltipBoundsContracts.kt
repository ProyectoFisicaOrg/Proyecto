package org.example.scrubbing

/**
 * Contratos de geometría de anclaje, límites del tooltip y efecto interactivo (ISP + DIP).
 */
interface ITooltipAnchor {
    fun x(): Double
    fun y(): Double
    fun canvasW(): Double
    fun canvasH(): Double
}

interface ITooltipBounds {
    fun leftX(): Double
    fun topY(): Double
    fun boxWidth(): Double
    fun boxHeight(): Double
    fun right(): Double
    fun bottom(): Double
    fun contains(px: Double, py: Double): Boolean

    val x: Double get() = leftX()
    val y: Double get() = topY()
}
