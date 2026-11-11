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
        if (prev.y > 0.0 && pointNearGround.y <= 0.0) {
            return calculateGroundCrossing(prev, pointNearGround)
        }
        return pointNearGround
    }

    private fun calculateGroundCrossing(prev: ITrajectoryPoint, curr: ITrajectoryPoint): ITrajectoryPoint {
        val ratio = prev.y / (prev.y - curr.y)
        val exactT = prev.t + ratio * (curr.t - prev.t)
        val exactX = prev.x + ratio * (curr.x - prev.x)
        val exactVy = prev.vy + ratio * (curr.vy - prev.vy)
        return pointFactory(exactX, 0.0, prev.vx, exactVy, exactT)
    }
}
