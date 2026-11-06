package org.example.telemetry

import org.example.IPosition
import org.example.ITrajectoryPoint

/**
 * Contrato de aliases cinemáticos estándar (t, y, v, distance, position) para telemetría (ISP + DIP).
 */
interface ITelemetryAliases {
    fun sourcePoint(): ITrajectoryPoint
    fun scalarSpeed(): Double
    fun currentPosition(): IPosition

    val t: Double get() = sourcePoint().t
    val y: Double get() = sourcePoint().y
    val v: Double get() = scalarSpeed()
    val distance: Double get() = sourcePoint().x
    val position: IPosition get() = currentPosition()
}
