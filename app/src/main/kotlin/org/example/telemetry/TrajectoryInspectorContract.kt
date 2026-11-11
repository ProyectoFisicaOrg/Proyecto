package org.example.telemetry

import org.example.IPosition
import org.example.model.ITrajectoryMatrixSummary

/**
 * Inspector interactivo acoplado a una trayectoria concreta (SRP + ISP).
 */
interface TrajectoryInspector {
    fun boundMatrix(): ITrajectoryMatrixSummary
    fun inspect(x: Double, y: Double): IInstantaneousState
    fun inspect(position: IPosition): IInstantaneousState = inspect(position.x, position.y)
    fun inspectByX(x: Double): IInstantaneousState

    val matrix: ITrajectoryMatrixSummary get() = boundMatrix()
}

/**
 * Contrato de fábrica de telemetría (DIP).
 */
interface ITelemetryFactory {
    fun buildApi(): InstantaneousStateApi
}
