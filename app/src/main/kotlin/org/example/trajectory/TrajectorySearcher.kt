package org.example.trajectory

import kotlin.math.abs
import org.example.ITrajectoryPoint

/**
 * Búsqueda sobre trayectoria mediante búsqueda binaria O(log n) (SRP).
 */
class BinaryTrajectorySearcher : TrajectorySearcher {

    override fun findClosestIndexByX(points: List<ITrajectoryPoint>, targetX: Double): Int {
        if (points.isEmpty()) return 0
        val boundaryIndex = searchInsertionIndex(points, targetX)
        return chooseCloserIndex(points, boundaryIndex, targetX)
    }

    override fun findClosestByX(points: List<ITrajectoryPoint>, targetX: Double): ITrajectoryPoint {
        return points[findClosestIndexByX(points, targetX)]
    }

    private fun searchInsertionIndex(points: List<ITrajectoryPoint>, targetX: Double): Int {
        var low = 0
        var high = points.size - 1
        while (low < high) {
            val mid = (low + high) ushr 1
            if (points[mid].x < targetX) low = mid + 1 else high = mid
        }
        return low
    }

    private fun chooseCloserIndex(points: List<ITrajectoryPoint>, index: Int, targetX: Double): Int {
        if (index <= 0) return index
        val diffCurr = abs(points[index].x - targetX)
        val diffPrev = abs(points[index - 1].x - targetX)
        return if (diffPrev < diffCurr) index - 1 else index
    }
}
