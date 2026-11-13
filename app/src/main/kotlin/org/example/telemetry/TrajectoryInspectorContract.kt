package org.example.telemetry

import org.example.IPosition
import org.example.model.ITrajectoryMatrixSummary

/**
 * Inspector interactivo acoplado a una trayectoria concreta (SRP + ISP).
 */
interface TrajectoryInspector {
    fun boundMatrix(): ITrajectoryMatrixSummary
    fun inspect(x: Double, y: Double): IInstantaneousState
    fun inspect(position: IPosition): IInstantaneousState = inspect(position.horizontal(), position.vertical())
    fun inspectByX(x: Double): IInstantaneousState
}

/**
 * Contrato de fábrica de telemetría (DIP).
 */
interface ITelemetryFactory {
    fun buildApi(): InstantaneousStateApi
}
