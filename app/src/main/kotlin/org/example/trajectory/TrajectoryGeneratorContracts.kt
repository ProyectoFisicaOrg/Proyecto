package org.example.trajectory

import org.example.IPhysicsParams
import org.example.ITrajectoryPoint
import org.example.model.TrajectoryMatrix

/**
 * Abstracción de generación de la matriz de trayectoria (SRP + DIP).
 */
interface TrajectoryMatrixGenerator {
    fun generate(params: IPhysicsParams, samplesPerSecond: Int): TrajectoryMatrix
}

/**
 * Abstracción de interpolación sobre la trayectoria (SRP + DIP).
 */
interface TrajectoryInterpolator {
    fun interpolateToGround(points: List<ITrajectoryPoint>, pointNearGround: ITrajectoryPoint): ITrajectoryPoint
}
