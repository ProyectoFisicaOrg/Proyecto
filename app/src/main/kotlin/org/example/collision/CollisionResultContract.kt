package org.example.collision

import org.example.ITrajectoryPoint
import org.example.model.ITrajectoryMatrixSummary

/**
 * Contrato de resultado de evaluación de colisión (ISP + DIP).
 */
interface ICollisionResult {
    fun collisionStatus(): CollisionStatus
    fun impactResult(): IImpactResult?
    fun statusMessage(): String
    fun isHit(): Boolean
    fun formatBannerText(): String
    fun bannerColorHex(): String

    val status: CollisionStatus get() = collisionStatus()
    val message: String get() = statusMessage()
}
