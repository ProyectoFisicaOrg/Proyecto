package org.example.rendering

/**
 * Contratos de base de montaje, cañón completo y proyectil (ISP + DIP).
 */
interface ICannonMountPainter {
    fun drawCannonBase()
}

interface ICannonRenderer {
    fun drawCannon(angle: Double)
}
