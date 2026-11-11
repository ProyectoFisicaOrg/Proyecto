package org.example.collision

import org.example.ITrajectoryPoint
import org.example.model.ITrajectoryMatrixSummary

/**
 * Fábrica abstracta de resultados de colisión (DIP).
 */
interface ICollisionResultFactory {
    fun unknown(message: String): ICollisionResult
    fun hit(point: ITrajectoryPoint, target: ICollisionTarget, dist: Double): ICollisionResult
    fun miss(closestX: Double, target: ICollisionTarget): ICollisionResult
}

/**
 * Abstracción de detección de colisión entre la trayectoria y un objetivo (SRP + ISP + DIP).
 */
interface CollisionDetector {
    fun checkCollision(matrix: ITrajectoryMatrixSummary, target: ICollisionTarget): ICollisionResult
    fun isInsideTarget(impactX: Double, target: ICollisionTarget): Boolean
}
