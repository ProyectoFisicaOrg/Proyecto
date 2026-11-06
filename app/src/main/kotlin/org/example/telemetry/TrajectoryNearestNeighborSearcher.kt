package org.example.telemetry

import kotlin.math.abs
import org.example.ITrajectoryPoint

/**
 * Implementación de Nearest Neighbor basada en distancia euclidiana 2D y búsqueda binaria en 1D (SRP + LSP).
 */
class EuclideanNearestNeighborSearcher : TrajectoryNearestNeighborSearcher {

    override fun findNearest(points: List<ITrajectoryPoint>, x: Double, y: Double): ITrajectoryPoint {
        return points[findNearestIndex(points, x, y)]
    }

    override fun findNearestIndex(points: List<ITrajectoryPoint>, x: Double, y: Double): Int {
        require(points.isNotEmpty()) { "La lista de puntos no puede estar vacía" }
        if (points.size == 1) return 0
        return findLinearNearest2D(points, x, y)
    }

    override fun findNearestByX(points: List<ITrajectoryPoint>, targetX: Double): ITrajectoryPoint {
        return points[findNearestIndexByX(points, targetX)]
    }

    override fun findNearestIndexByX(points: List<ITrajectoryPoint>, targetX: Double): Int {
        require(points.isNotEmpty()) { "La lista de puntos no puede estar vacía" }
        if (targetX <= points.first().x) return 0
        if (targetX >= points.last().x) return points.size - 1
        val lowerIdx = lowerBoundByX(points, targetX)
        return pickCloserNeighbor(points, lowerIdx, targetX)
    }

    private fun findLinearNearest2D(points: List<ITrajectoryPoint>, x: Double, y: Double): Int {
        var bestIndex = 0
        var minDistanceSq = Double.MAX_VALUE
        for (i in points.indices) {
            val distSq = distanceSquared(points[i], x, y)
            if (distSq < minDistanceSq) {
                minDistanceSq = distSq
                bestIndex = i
            }
        }
        return bestIndex
    }

    private fun lowerBoundByX(points: List<ITrajectoryPoint>, targetX: Double): Int {
        var low = 0
        var high = points.size - 1
        while (low < high) {
            val mid = (low + high) ushr 1
            if (points[mid].x < targetX) low = mid + 1 else high = mid
        }
        return low
    }

    private fun pickCloserNeighbor(points: List<ITrajectoryPoint>, idx: Int, targetX: Double): Int {
        if (idx <= 0) return 0
        val prevDiff = abs(points[idx - 1].x - targetX)
        val currDiff = abs(points[idx].x - targetX)
        return if (prevDiff <= currDiff) idx - 1 else idx
    }

    private fun distanceSquared(p: ITrajectoryPoint, x: Double, y: Double): Double {
        val dx = p.x - x
        val dy = p.y - y
        return dx * dx + dy * dy
    }
}
