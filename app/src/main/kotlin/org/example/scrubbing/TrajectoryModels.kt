package org.example.scrubbing

import java.util.Locale

/**
 * Contrato de vista cinemática bilingüe para un punto discreto de la trayectoria (ISP + DIP).
 */
interface ISpanishKinematicView {
    fun coordX(): Double
    fun coordY(): Double
    fun timeValue(): Double
    fun heightValue(): Double
    fun speedValue(): Double

    val x: Double get() = coordX()
    val y: Double get() = coordY()
    val time: Double get() = timeValue()
    val height: Double get() = heightValue()
    val speed: Double get() = speedValue()
    val tiempo: Double get() = timeValue()
    val altura: Double get() = heightValue()
    val velocidad: Double get() = speedValue()
}

/**
 * Representa un punto discreto encapsulado sobre la curva de la trayectoria (SRP + Encapsulación).
 */
class TrajectoryPoint(
    x: Double,
    y: Double,
    time: Double,
    height: Double = y,
    speed: Double
) : ISpanishKinematicView {
    private val posX: Double = x
    private val posY: Double = y
    private val elapsed: Double = time
    private val alt: Double = height
    private val scalarSpeed: Double = speed

    init {
        require(time >= -1e-9) { "El tiempo transcurrido no puede ser negativo: $time" }
        require(speed >= 0.0) { "La velocidad escalar no puede ser negativa: $speed" }
    }

    override fun coordX(): Double = posX

    override fun coordY(): Double = posY

    override fun timeValue(): Double = elapsed

    override fun heightValue(): Double = alt

    override fun speedValue(): Double = scalarSpeed

    fun formatTelemetry(): String =
        String.format(Locale.US, "v = %.2f m/s, h = %.2f m (t = %.2f s)", scalarSpeed, alt, elapsed)

    private fun components(): List<Double> = listOf(posX, posY, elapsed, alt, scalarSpeed)

    override fun equals(other: Any?): Boolean = other is TrajectoryPoint && components() == other.components()

    override fun hashCode(): Int = components().hashCode()

    override fun toString(): String =
        String.format(Locale.US, "TrajectoryPoint(x=%.2f, y=%.2f, t=%.2f, v=%.2f)", posX, posY, elapsed, scalarSpeed)

    companion object {
        fun of(x: Double, y: Double, tiempo: Double, velocidad: Double): TrajectoryPoint =
            TrajectoryPoint(x = x, y = y, time = tiempo, height = y, speed = velocidad)
    }
}

/**
 * Contrato bilingüe para métricas finales del vuelo (ISP + DIP).
 */
interface ISpanishFlightSummaryView {
    fun flightTimeSeconds(): Double
    fun rangeMeters(): Double

    val totalFlightTime: Double get() = flightTimeSeconds()
    val totalDistance: Double get() = rangeMeters()
    val tiempoTotalCaida: Double get() = flightTimeSeconds()
    val distanciaRecorrida: Double get() = rangeMeters()
}

/**
 * Resumen consolidado encapsulado del vuelo del proyectil tras impactar en el suelo (SRP + Encapsulación).
 */
class FlightSummary(
    totalFlightTime: Double,
    totalDistance: Double
) : ISpanishFlightSummaryView {
    private val flightTime: Double = totalFlightTime
    private val distance: Double = totalDistance

    init {
        require(totalFlightTime >= 0.0) { "El tiempo total de vuelo no puede ser negativo: $totalFlightTime" }
        require(totalDistance >= 0.0) { "La distancia total recorrida no puede ser negativa: $totalDistance" }
    }

    override fun flightTimeSeconds(): Double = flightTime

    override fun rangeMeters(): Double = distance

    fun formattedTime(): String = String.format(Locale.US, "%.2f s", flightTime)

    fun formattedDistance(): String = String.format(Locale.US, "%.2f m", distance)

    override fun equals(other: Any?): Boolean =
        other is FlightSummary && flightTime == other.flightTime && distance == other.distance

    override fun hashCode(): Int = listOf(flightTime, distance).hashCode()

    override fun toString(): String =
        String.format(Locale.US, "FlightSummary(tiempo=%.2f s, distancia=%.2f m)", flightTime, distance)

    companion object {
        fun of(tiempoTotalCaida: Double, distanciaRecorrida: Double): FlightSummary =
            FlightSummary(totalFlightTime = tiempoTotalCaida, totalDistance = distanciaRecorrida)
    }
}
