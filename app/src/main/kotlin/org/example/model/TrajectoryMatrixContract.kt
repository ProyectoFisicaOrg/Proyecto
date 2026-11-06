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

    val points: List<ITrajectoryPoint> get() = trajectoryPoints()
    val timeOfFlight: Double get() = totalTimeOfFlight()
    val range: Double get() = horizontalRange()
    val maxHeight: Double get() = peakHeight()
    val size: Int get() = trajectoryPoints().size
    val launchPoint: ITrajectoryPoint get() = trajectoryPoints().first()
    val impactPoint: ITrajectoryPoint get() = trajectoryPoints().last()
}
