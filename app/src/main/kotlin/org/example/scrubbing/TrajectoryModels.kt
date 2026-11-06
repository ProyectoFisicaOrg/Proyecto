package org.example.scrubbing

import java.util.Locale

/**
 * Punto discreto encapsulado sobre la curva de la trayectoria con vistas bilingües (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class TrajectoryPoint internal constructor(
    private val values: List<Double>
) : ISpanishKinematicView {

    constructor(
        x: Double,
        y: Double,
        time: Double,
        height: Double = y,
        speed: Double
    ) : this(listOf(x, y, time, height, speed))

    init {
        require(values[2] >= -1e-9) { "El tiempo transcurrido no puede ser negativo: ${values[2]}" }
        require(values[4] >= 0.0) { "La velocidad escalar no puede ser negativa: ${values[4]}" }
    }

    override fun coordX(): Double {
        return values[0]
    }

    override fun coordY(): Double {
        return values[1]
    }

    override fun timeValue(): Double {
        return values[2]
    }

    override fun heightValue(): Double {
        return values[3]
    }

    override fun speedValue(): Double {
        return values[4]
    }

    override fun formatTelemetry(): String {
        return String.format(Locale.US, "v = %.2f m/s, h = %.2f m (t = %.2f s)", values[4], values[3], values[2])
    }

    companion object {
        fun of(x: Double, y: Double, tiempo: Double, velocidad: Double): TrajectoryPoint {
            return TrajectoryPoint(x, y, tiempo, y, velocidad)
        }
    }
}

/**
 * Resumen consolidado y encapsulado del vuelo tras impactar en el suelo (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class FlightSummary internal constructor(
    private val values: List<Double>
) : ISpanishFlightSummaryView {

    constructor(totalFlightTime: Double, totalDistance: Double) : this(listOf(totalFlightTime, totalDistance))

    init {
        require(values[0] >= 0.0) { "El tiempo total de vuelo no puede ser negativo: ${values[0]}" }
        require(values[1] >= 0.0) { "La distancia total recorrida no puede ser negativa: ${values[1]}" }
    }

    override fun flightTimeSeconds(): Double {
        return values[0]
    }

    override fun rangeMeters(): Double {
        return values[1]
    }

    override fun formattedTime(): String {
        return String.format(Locale.US, "%.2f s", values[0])
    }

    override fun formattedDistance(): String {
        return String.format(Locale.US, "%.2f m", values[1])
    }

    companion object {
        fun of(tiempoTotalCaida: Double, distanciaRecorrida: Double): FlightSummary {
            return FlightSummary(tiempoTotalCaida, distanciaRecorrida)
        }
    }
}
