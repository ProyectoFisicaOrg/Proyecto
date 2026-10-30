package org.example.trajectory

import org.example.TrajectoryPoint

/**
 * Abstracción de interpolación sobre la trayectoria (SRP + DIP).
 */
interface TrajectoryInterpolator {
    /** Interpola linealmente entre el punto y su anterior para hallar y=0 exacto. */
    fun interpolateToGround(points: List<TrajectoryPoint>, pointNearGround: TrajectoryPoint): TrajectoryPoint
}

/**
 * Interpolación lineal entre dos puntos consecutivos (SRP).
 */
class LinearTrajectoryInterpolator : TrajectoryInterpolator {

    override fun interpolateToGround(points: List<TrajectoryPoint>, pointNearGround: TrajectoryPoint): TrajectoryPoint {
        val idx = points.indexOf(pointNearGround)
        if (idx <= 0) return pointNearGround

        val prev = points[idx - 1]
        if (prev.y > 0.0 && pointNearGround.y <= 0.0) {
            return calculateGroundCrossing(prev, pointNearGround)
        }
        return pointNearGround
    }

    private fun calculateGroundCrossing(prev: TrajectoryPoint, curr: TrajectoryPoint): TrajectoryPoint {
        val ratio = prev.y / (prev.y - curr.y)
        val exactT = prev.t + ratio * (curr.t - prev.t)
        val exactX = prev.x + ratio * (curr.x - prev.x)
        val exactVy = prev.vy + ratio * (curr.vy - prev.vy)
        return TrajectoryPoint(exactX, 0.0, prev.vx, exactVy, exactT)
    }
}
