package org.example.telemetry

import org.example.IPosition
import org.example.ITrajectoryPoint
import org.example.model.ITrajectoryMatrixSummary

/**
 * Contrato de la API de Intersección y Estado Instantáneo (DIP + SRP + ISP).
 */
interface InstantaneousStateApi {
    fun getInstantaneousState(matrix: ITrajectoryMatrixSummary, x: Double, y: Double): IInstantaneousState
    fun getInstantaneousState(matrix: ITrajectoryMatrixSummary, position: IPosition): IInstantaneousState =
        getInstantaneousState(matrix, position.x, position.y)
    fun getInstantaneousStateByX(matrix: ITrajectoryMatrixSummary, x: Double): IInstantaneousState
    fun getInstantaneousState(points: List<ITrajectoryPoint>, x: Double, y: Double): IInstantaneousState
    fun getInstantaneousStateByX(points: List<ITrajectoryPoint>, x: Double): IInstantaneousState
    fun createInspector(matrix: ITrajectoryMatrixSummary): TrajectoryInspector
}
