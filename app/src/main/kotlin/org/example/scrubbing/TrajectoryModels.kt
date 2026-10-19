package org.example.scrubbing

import java.util.Locale
import kotlin.math.hypot

/**
 * Representa un punto discreto sobre la curva de la trayectoria con sus propiedades cinemáticas instantáneas (SRP).
 *
 * Estructura de datos inmutable utilizada por los componentes de UI e interacción (Dev 2)
 * para realizar scrubbing e inspección en el HUD sin acoplarse a los detalles internos
 * del motor de física (Dev 1).
 *
 * @property x Coordenada horizontal (en metros).
 * @property y Coordenada vertical / altura (en metros).
 * @property time Tiempo transcurrido desde el lanzamiento en segundos (t).
 * @property height Altura del proyectil en metros (alias cinemático de y).
 * @property speed Velocidad escalar instantánea v = sqrt(vx^2 + vy^2) en m/s.
 */
data class TrajectoryPoint(
    val x: Double,
    val y: Double,
    val time: Double,
    val height: Double = y,
    val speed: Double
) {
    init {
        require(time >= -1e-9) { "El tiempo transcurrido no puede ser negativo: $time" }
        require(speed >= 0.0) { "La velocidad escalar no puede ser negativa: $speed" }
    }

    // --- Aliases en español para cumplir explícitamente con la nomenclatura del requerimiento ---
    val tiempo: Double get() = time
    val altura: Double get() = height
    val velocidad: Double get() = speed

    /** Formato textual de telemetría instantánea para etiquetas y depuración */
    fun formatTelemetry(): String =
        String.format(Locale.US, "v = %.2f m/s, h = %.2f m (t = %.2f s)", speed, height, time)

    override fun toString(): String =
        String.format(Locale.US, "TrajectoryPoint(x=%.2f, y=%.2f, t=%.2f, v=%.2f)", x, y, time, speed)

    companion object {
        /**
         * Crea un [TrajectoryPoint] a partir de componentes individuales con nombres en español.
         */
        fun of(
            x: Double,
            y: Double,
            tiempo: Double,
            altura: Double = y,
            velocidad: Double
        ): TrajectoryPoint = TrajectoryPoint(
            x = x,
            y = y,
            time = tiempo,
            height = altura,
            speed = velocidad
        )
    }
}

/**
 * Resumen consolidado del vuelo del proyectil tras impactar en el suelo (SRP).
 *
 * Estructura inmutable que almacena las métricas finales requeridas por la
 * tarjeta de resultados del HUD (Issue 3.2).
 *
 * @property totalFlightTime Tiempo total de vuelo hasta el impacto contra el suelo (en segundos).
 * @property totalDistance Distancia horizontal total alcanzada en metros (alcance máximo).
 */
data class FlightSummary(
    val totalFlightTime: Double,
    val totalDistance: Double
) {
    init {
        require(totalFlightTime >= 0.0) { "El tiempo total de vuelo no puede ser negativo: $totalFlightTime" }
        require(totalDistance >= 0.0) { "La distancia total recorrida no puede ser negativa: $totalDistance" }
    }

    // --- Aliases en español solicitados explícitamente por los requerimientos ---
    val tiempoTotalCaida: Double get() = totalFlightTime
    val distanciaRecorrida: Double get() = totalDistance

    /** Formatea el tiempo total en segundos con 2 decimales */
    fun formattedTime(): String = String.format(Locale.US, "%.2f s", totalFlightTime)

    /** Formatea la distancia total en metros con 2 decimales */
    fun formattedDistance(): String = String.format(Locale.US, "%.2f m", totalDistance)

    override fun toString(): String =
        String.format(Locale.US, "FlightSummary(tiempo=%.2f s, distancia=%.2f m)", totalFlightTime, totalDistance)

    companion object {
        /**
         * Constructor de fábrica con nomenclatura en español.
         */
        fun of(tiempoTotalCaida: Double, distanciaRecorrida: Double): FlightSummary =
            FlightSummary(totalFlightTime = tiempoTotalCaida, totalDistance = distanciaRecorrida)
    }
}
