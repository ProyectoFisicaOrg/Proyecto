package org.example.animation

import org.example.collision.ICollisionResult

/**
 * Listener para eventos de colisión (ISP + DIP).
 */
interface CollisionListener {
    fun onCollision(result: ICollisionResult)
}

/**
 * Abstracción del reloj de fotogramas para animación (DIP + SRP).
 */
interface FrameTicker {
    fun start(onTick: (now: Long) -> Unit)
    fun stop()
}
