package org.example.telemetry

import org.example.ITrajectoryPoint

/**
 * Contrato completo de lectura del estado instantáneo de telemetría (ISP + DIP).
 */
interface IInstantaneousState : ITelemetryAliases {
    fun elapsedTime(): Double
    fun altitude(): Double
    fun horizontalCoord(): Double
    fun queryDistance(): Double
    fun format(): String
}
