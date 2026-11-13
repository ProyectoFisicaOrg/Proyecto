package org.example.trajectory

import org.example.ITrajectoryPoint
import org.example.TrajectoryPoint

/**
 * Interpolación lineal entre dos puntos consecutivos desacoplada por fábrica (SRP + DIP).
 */
class LinearTrajectoryInterpolator(
    private val pointFactory: (Double, Double, Double, Double, Double) -> ITrajectoryPoint = ::TrajectoryPoint
) : TrajectoryInterpolator {

    override fun interpolateToGround(points: List<ITrajectoryPoint>, pointNearGround: ITrajectoryPoint): ITrajectoryPoint {
        val idx = points.indexOf(pointNearGround)
        if (idx <= 0) return pointNearGround
        val prev = points[idx - 1]
        if (prev.posY() > 0.0 && pointNearGround.posY() <= 0.0) {
            return calculateGroundCrossing(prev, pointNearGround)
        }
        return pointNearGround
    }

    private fun calculateGroundCrossing(prev: ITrajectoryPoint, curr: ITrajectoryPoint): ITrajectoryPoint {
        val ratio = prev.posY() / (prev.posY() - curr.posY())
        val exactT = prev.timeSeconds() + ratio * (curr.timeSeconds() - prev.timeSeconds())
        val exactX = prev.posX() + ratio * (curr.posX() - prev.posX())
        val exactVy = prev.velY() + ratio * (curr.velY() - prev.velY())
        return pointFactory(exactX, 0.0, prev.velX(), exactVy, exactT)
    }
}
