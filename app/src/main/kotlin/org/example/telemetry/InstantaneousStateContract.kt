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

    val time: Double get() = elapsedTime()
    val height: Double get() = altitude()
    val speed: Double get() = scalarSpeed()
    val x: Double get() = horizontalCoord()
    val point: ITrajectoryPoint get() = sourcePoint()
    val distanceToQuery: Double get() = queryDistance()
}
