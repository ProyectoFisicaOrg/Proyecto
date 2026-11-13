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
}
