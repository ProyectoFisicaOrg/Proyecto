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
}
