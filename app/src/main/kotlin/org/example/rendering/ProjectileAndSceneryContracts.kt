package org.example.rendering

/**
 * Contratos de renderizado de proyectil y capas de escenario (ISP + DIP).
 */
interface IProjectileRenderer {
    fun drawProjectile(x: Double, y: Double)
    fun drawAngleIndicator(angle: Double)
}

interface IScenery {
    fun drawBackground()
    fun drawGround()
}
