package org.example.trajectory

import org.example.ITrajectoryPoint

/**
 * Abstracción de búsqueda de puntos dentro de una trayectoria (SRP + ISP).
 */
interface TrajectorySearcher {
    fun findClosestIndexByX(points: List<ITrajectoryPoint>, targetX: Double): Int
    fun findClosestByX(points: List<ITrajectoryPoint>, targetX: Double): ITrajectoryPoint
}
