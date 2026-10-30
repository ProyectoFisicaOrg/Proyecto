package org.example.telemetry

import java.util.Locale
import kotlin.math.hypot
import org.example.Position
import org.example.TrajectoryPoint

/**
 * Contrato de aliases cinemáticos estándar (t, y, v, distance, position) para telemetría (ISP + DIP).
 */
interface ITelemetryAliases {
    fun sourcePoint(): TrajectoryPoint
    fun scalarSpeed(): Double

    val t: Double get() = sourcePoint().t
    val y: Double get() = sourcePoint().y
    val v: Double get() = scalarSpeed()
    val distance: Double get() = sourcePoint().x
    val position: Position get() = Position(sourcePoint().x, sourcePoint().y)
}

/**
 * Contrato completo de lectura del estado instantáneo de telemetría (ISP + DIP).
 */
interface IInstantaneousState : ITelemetryAliases {
    fun elapsedTime(): Double
    fun altitude(): Double
    fun horizontalCoord(): Double
    fun queryDistance(): Double

    val time: Double get() = elapsedTime()
    val height: Double get() = altitude()
    val speed: Double get() = scalarSpeed()
    val x: Double get() = horizontalCoord()
    val point: TrajectoryPoint get() = sourcePoint()
    val distanceToQuery: Double get() = queryDistance()
}

/**
 * Representa el estado instantáneo encapsulado (telemetría) del proyectil en un punto específico (SRP + Encapsulación).
 */
class InstantaneousState(
    time: Double,
    height: Double,
    speed: Double,
    x: Double,
    point: TrajectoryPoint,
    distanceToQuery: Double = 0.0
) : IInstantaneousState {
    private val timeSec: Double = time
    private val heightM: Double = height
    private val speedMps: Double = speed
    private val posX: Double = x
    private val trajPoint: TrajectoryPoint = point
    private val queryDist: Double = distanceToQuery

    init {
        require(time >= -1e-9) { "El tiempo transcurrido no puede ser negativo: $time" }
        require(speed >= 0.0) { "La velocidad escalar no puede ser negativa: $speed" }
        require(distanceToQuery >= 0.0) { "La distancia a la consulta no puede ser negativa: $distanceToQuery" }
    }

    override fun sourcePoint(): TrajectoryPoint = trajPoint

    override fun scalarSpeed(): Double = speedMps

    override fun elapsedTime(): Double = timeSec

    override fun altitude(): Double = heightM

    override fun horizontalCoord(): Double = posX

    override fun queryDistance(): Double = queryDist

    fun format(): String =
        String.format(Locale.US, "t=%.2f s, h=%.2f m, v=%.2f m/s (x=%.2f m)", timeSec, heightM, speedMps, posX)

    private fun components(): List<Any> = listOf(timeSec, heightM, speedMps, posX, trajPoint, queryDist)

    override fun equals(other: Any?): Boolean = other is InstantaneousState && components() == other.components()

    override fun hashCode(): Int = components().hashCode()

    override fun toString(): String =
        String.format(Locale.US, "InstantaneousState(t=%.4f s, y=%.4f m, v=%.4f m/s, x=%.4f m)", timeSec, heightM, speedMps, posX)

    companion object {
        fun fromTrajectoryPoint(point: TrajectoryPoint, distanceToQuery: Double = 0.0): InstantaneousState {
            val speed = hypot(point.vx, point.vy)
            return InstantaneousState(point.t, point.y, speed, point.x, point, distanceToQuery)
        }
    }
}
