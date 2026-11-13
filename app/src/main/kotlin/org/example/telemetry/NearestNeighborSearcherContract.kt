package org.example.telemetry

import org.example.IPosition
import org.example.ITrajectoryPoint

/**
 * Abstracción para el algoritmo de búsqueda de vecino más cercano (SRP + ISP + OCP).
 */
interface TrajectoryNearestNeighborSearcher {
    fun findNearest(points: List<ITrajectoryPoint>, x: Double, y: Double): ITrajectoryPoint
    fun findNearest(points: List<ITrajectoryPoint>, position: IPosition): ITrajectoryPoint =
        findNearest(points, position.horizontal(), position.vertical())
    fun findNearestByX(points: List<ITrajectoryPoint>, targetX: Double): ITrajectoryPoint
    fun findNearestIndex(points: List<ITrajectoryPoint>, x: Double, y: Double): Int
    fun findNearestIndexByX(points: List<ITrajectoryPoint>, targetX: Double): Int
}
