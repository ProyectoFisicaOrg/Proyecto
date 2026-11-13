package org.example.model

import org.example.ITrajectoryPoint

/**
 * Contrato de métricas resumidas y puntos discretizados de una trayectoria (ISP + DIP).
 */
interface ITrajectoryMatrixSummary {
    fun trajectoryPoints(): List<ITrajectoryPoint>
    fun totalTimeOfFlight(): Double
    fun horizontalRange(): Double
    fun peakHeight(): Double
    fun interpolateAt(elapsed: Double): Pair<Double, Double>
    fun subMatrix(fromIndex: Int, toIndex: Int): List<ITrajectoryPoint>
    fun pointsAbove(minHeight: Double): List<ITrajectoryPoint>
    fun pointCount(): Int = trajectoryPoints().size
    fun firstPoint(): ITrajectoryPoint = trajectoryPoints().first()
    fun finalPoint(): ITrajectoryPoint = trajectoryPoints().last()
}
