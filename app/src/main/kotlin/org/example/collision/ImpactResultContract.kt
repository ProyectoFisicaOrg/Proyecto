package org.example.collision

import org.example.ITrajectoryPoint

/**
 * Contrato de resultado detallado de punto de impacto (ISP + DIP).
 */
interface IImpactResult {
    fun impactPoint(): ITrajectoryPoint
    fun hitTarget(): ICollisionTarget
    fun centerDistance(): Double
    fun timeOfImpact(): Double

    val point: ITrajectoryPoint get() = impactPoint()
    val target: ICollisionTarget get() = hitTarget()
}
