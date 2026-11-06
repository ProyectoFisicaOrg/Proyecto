package org.example.model

import org.example.collision.ICollisionResult

/**
 * Contrato de estado del objetivo y resultado de impacto (ISP + DIP).
 */
interface ITargetAndCollisionState {
    fun currentTargetX(): Double
    fun changeTargetX(value: Double)
    fun currentTargetRadius(): Double
    fun changeTargetRadius(value: Double)
    fun currentCollisionResult(): ICollisionResult?
    fun changeCollisionResult(result: ICollisionResult?)

    var targetX: Double
        get() = currentTargetX()
        set(value) = changeTargetX(value)

    var targetRadius: Double
        get() = currentTargetRadius()
        set(value) = changeTargetRadius(value)

    var lastCollisionResult: ICollisionResult?
        get() = currentCollisionResult()
        set(value) = changeCollisionResult(value)
}
