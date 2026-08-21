package org.example

import kotlin.math.cos
import kotlin.math.sin

/**
 * Parámetros de física inmutables para el cálculo de la trayectoria.
 * Sigue el principio de responsabilidad única: solo almacena datos de configuración.
 */
data class PhysicsParams(
    val angle: Double,
    val initialVelocity: Double,
    val gravity: Double,
    val mass: Double
) {
    init {
        require(initialVelocity >= 0) { "La velocidad inicial no puede ser negativa" }
        require(gravity > 0) { "La gravedad debe ser positiva" }
        require(mass > 0) { "La masa debe ser positiva" }
    }
}

/**
 * Representa un punto en la trayectoria con posición, velocidad y tiempo.
 * Inmutable y thread-safe.
 */
data class TrajectoryPoint(
    val x: Double,
    val y: Double,
    val vx: Double,
    val vy: Double,
    val t: Double
) {
    /**
     * Calcula la velocidad magnitud en este punto.
     */
    fun speed(): Double = Math.hypot(vx, vy)

    /**
     * Calcula la altura (coordenada y).
     */
    fun height(): Double = y

    /**
     * Calcula la distancia horizontal recorrida.
     */
    fun distance(): Double = x
}

/**
 * Interfaz para calculadores de trayectoria.
 * Principio de Inversión de Dependencias: el código cliente depende de esta abstracción.
 * Principio de Segregación de Interfaces: interfaz específica y cohesionada.
 */
interface TrajectoryCalculator {
    /**
     * Calcula el estado del proyectil en un tiempo dado.
     * @param params Parámetros de física
     * @param t Tiempo en segundos
     * @return Punto de la trayectoria en el tiempo t
     */
    fun calculateState(params: PhysicsParams, t: Double): TrajectoryPoint

    /**
     * Calcula el tiempo de vuelo total hasta que el proyectil impacta en el suelo (y=0).
     * @param params Parámetros de física
     * @return Tiempo de vuelo en segundos, o null si nunca impacta
     */
    fun calculateTimeOfFlight(params: PhysicsParams): Double?

    /**
     * Calcula el alcance máximo (distancia horizontal cuando y=0).
     * @param params Parámetros de física
     * @return Alcance en metros, o null si nunca impacta
     */
    fun calculateRange(params: PhysicsParams): Double?

    /**
     * Calcula la altura máxima alcanzada.
     * @param params Parámetros de física
     * @return Altura máxima en metros
     */
    fun calculateMaxHeight(params: PhysicsParams): Double
}

/**
 * Implementación estándar de calculadora de trayectoria parabólica (sin resistencia del aire).
 * Principio de Responsabilidad Única: solo calcula la cinemática del proyectil.
 * Principio Abierto/Cerrado: extensible mediante herencia sin modificar esta clase.
 */
open class ParabolicTrajectoryCalculator : TrajectoryCalculator {

    override fun calculateState(params: PhysicsParams, t: Double): TrajectoryPoint {
        require(t >= 0) { "El tiempo no puede ser negativo" }

        val cosTheta = cos(params.angle)
        val sinTheta = sin(params.angle)

        val vx = params.initialVelocity * cosTheta
        val vy = params.initialVelocity * sinTheta - params.gravity * t

        val x = vx * t
        val y = params.initialVelocity * sinTheta * t - 0.5 * params.gravity * t * t

        return TrajectoryPoint(x, y, vx, vy, t)
    }

    override fun calculateTimeOfFlight(params: PhysicsParams): Double? {
        val sinTheta = sin(params.angle)
        val v0y = params.initialVelocity * sinTheta

        // t = 0 es el lanzamiento, buscamos la otra raíz: t = 2 * v0y / g
        if (v0y <= 0) return null // Disparo horizontal o hacia abajo desde y=0

        return 2.0 * v0y / params.gravity
    }

    override fun calculateRange(params: PhysicsParams): Double? {
        val timeOfFlight = calculateTimeOfFlight(params)
        return timeOfFlight?.let { calculateState(params, it).x }
    }

    override fun calculateMaxHeight(params: PhysicsParams): Double {
        val sinTheta = sin(params.angle)
        val v0y = params.initialVelocity * sinTheta

        if (v0y <= 0) return 0.0

        // t_max = v0y / g, h_max = v0y^2 / (2g)
        return (v0y * v0y) / (2.0 * params.gravity)
    }
}

/**
 * Factoría para crear instancias de TrajectoryCalculator.
 * Principio de Inversión de Dependencias: desacopla la creación del uso.
 * Permite cambiar la implementación sin afectar al código cliente.
 */
object TrajectoryCalculatorFactory {
    /**
     * Crea una calculadora de trayectoria parabólica estándar.
     */
    fun createParabolic(): TrajectoryCalculator = ParabolicTrajectoryCalculator()

    /**
     * Crea una calculadora personalizada (para testing o extensiones futuras).
     */
    fun createCustom(calculator: TrajectoryCalculator): TrajectoryCalculator = calculator
}