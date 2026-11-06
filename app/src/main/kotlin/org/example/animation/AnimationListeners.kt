package org.example.animation

import javafx.animation.AnimationTimer
import org.example.collision.ICollisionResult

/**
 * Adaptador concreto de callback funcional hacia [CollisionListener] (Adapter + DIP).
 */
class CollisionCallbackAdapter(
    private val callback: (ICollisionResult) -> Unit
) : CollisionListener {
    override fun onCollision(result: ICollisionResult) {
        callback(result)
    }
}

/**
 * Implementación de producción de [FrameTicker] usando [AnimationTimer] (SRP + DIP).
 */
class JavaFxFrameTicker : FrameTicker {
    private var timer: AnimationTimer? = null

    override fun start(onTick: (now: Long) -> Unit) {
        val created = object : AnimationTimer() {
            override fun handle(now: Long) {
                onTick(now)
            }
        }
        timer = created
        created.start()
    }

    override fun stop() {
        timer?.stop()
        timer = null
    }
}

/**
 * Implementación encapsulada de [IInspectableTicker] para pruebas deterministas sin entorno gráfico (SRP + Encapsulación).
 */
class ManualFrameTicker : IInspectableTicker {
    private var tickerState: Pair<Boolean, ((Long) -> Unit)?> = Pair(false, null)

    override fun checkRunning(): Boolean {
        return tickerState.first
    }

    override fun start(onTick: (now: Long) -> Unit) {
        tickerState = Pair(true, onTick)
    }

    override fun stop() {
        tickerState = Pair(false, null)
    }

    override fun advance(nowNano: Long) {
        if (tickerState.first) {
            tickerState.second?.invoke(nowNano)
        }
    }
}
