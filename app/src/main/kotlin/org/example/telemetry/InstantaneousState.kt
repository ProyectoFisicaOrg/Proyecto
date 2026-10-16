package org.example.telemetry

import org.example.Position
import org.example.TrajectoryPoint
import java.util.Locale
import kotlin.math.hypot

/**
 * Representa el estado instantáneo (telemetría) del proyectil en un punto específico de su trayectoria.
 *
 * Sigue el Principio de Responsabilidad Única (SRP): su único propósito es encapsular
 * de forma inmutable los datos cinemáticos instantáneos requeridos por la especificación:
 * tiempo transcurrido (t), altura (y) y velocidad escalar v = sqrt(vx^2 + vy^2).
 *
 * @property time Tiempo transcurrido desde el disparo en segundos (t).
 * @property height Altura del proyectil en metros (y).
 * @property speed Velocidad escalar (módulo de la velocidad) v = sqrt(vx^2 + vy^2) en m/s.
 * @property x Distancia horizontal recorrida en metros.
 * @property point Punto cinemático completo [TrajectoryPoint] de la trayectoria.
 * @property distanceToQuery Distancia euclidiana entre la coordenada de consulta y este punto.
 */
data class InstantaneousState(
    val time: Double,
    val height: Double,
    val speed: Double,
    val x: Double,
    val point: TrajectoryPoint,
    val distanceToQuery: Double = 0.0
) {
    init {
        require(time >= -1e-9) { "El tiempo transcurrido no puede ser negativo: $time" }
        require(speed >= 0.0) { "La velocidad escalar no puede ser negativa: $speed" }
        require(distanceToQuery >= 0.0) { "La distancia a la consulta no puede ser negativa: $distanceToQuery" }
    }

    /** Alias para tiempo transcurrido (t). */
    val t: Double get() = time

    /** Alias para altura (y). */
    val y: Double get() = height

    /** Alias para velocidad escalar (v). */
    val v: Double get() = speed

    /** Alias para distancia horizontal (x). */
    val distance: Double get() = x

    /** Representación de la posición 2D del proyectil en este estado. */
    val position: Position get() = Position(x, height)

    /**
     * Formatea el estado en una cadena legible para el HUD o la consola usando Locale.US.
     */
    fun format(): String =
        String.format(Locale.US, "t=%.2f s, h=%.2f m, v=%.2f m/s (x=%.2f m)", time, height, speed, x)

    override fun toString(): String =
        String.format(Locale.US, "InstantaneousState(t=%.4f s, y=%.4f m, v=%.4f m/s, x=%.4f m)", time, height, speed, x)

    companion object {
        /**
         * Crea un [InstantaneousState] a partir de un [TrajectoryPoint] y una distancia opcional.
         *
         * @param point Punto de la trayectoria.
         * @param distanceToQuery Distancia euclidiana a la coordenada consultada.
         */
        fun fromTrajectoryPoint(
            point: TrajectoryPoint,
            distanceToQuery: Double = 0.0
        ): InstantaneousState {
            val speed = hypot(point.vx, point.vy)
            return InstantaneousState(
                time = point.t,
                height = point.y,
                speed = speed,
                x = point.x,
                point = point,
                distanceToQuery = distanceToQuery
            )
        }
    }
}
