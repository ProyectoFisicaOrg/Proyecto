package org.example.rendering

/**
 * Contratos de los sub-pintores y renderizador principal del cañón (ISP + DIP).
 */
interface ICannonDecalPainter {
    fun drawSkull(cx: Double, cy: Double, scale: Double)
    fun drawFlames(baseX: Double, baseY: Double)
}

interface ICannonBarrelPainter {
    fun drawRotatedBarrel(angle: Double)
}
