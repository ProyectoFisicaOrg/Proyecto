package org.example

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.serialization.Serializable

/**
 * Parámetros de física inmutables para el cálculo de la trayectoria.
 * Sigue el principio de responsabilidad única: solo almacena datos de configuración.
 *
 * @param angleDeg Ángulo de disparo en GRADOS (0-90° típico)
 * @param initialVelocity Velocidad inicial en m/s
 * @param gravity Gravedad en m/s²
 * @param mass Masa del proyectil en kg
 */
@Serializable
data class PhysicsParams(
    val angleDeg: Double,
    val initialVelocity: Double,
    val gravity: Double,
    val mass: Double
) {
    init {
        require(initialVelocity >= 0) { "La velocidad inicial no puede ser negativa" }
        require(gravity > 0) { "La gravedad debe ser positiva" }
        require(mass > 0) { "La masa debe ser positiva" }
        require(angleDeg >= -90 && angleDeg <= 90) { "El ángulo debe estar entre -90° y 90°" }
    }

    /** Ángulo interno en radianes para cálculos */
    val angleRad: Double
        get() = angleDeg * PI / 180.0
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

        val cosTheta = cos(params.angleRad)
        val sinTheta = sin(params.angleRad)

        val vx = params.initialVelocity * cosTheta
        val vy = params.initialVelocity * sinTheta - params.gravity * t

        val x = vx * t
        val y = params.initialVelocity * sinTheta * t - 0.5 * params.gravity * t * t

        return TrajectoryPoint(x, y, vx, vy, t)
    }

    override fun calculateTimeOfFlight(params: PhysicsParams): Double? {
        val sinTheta = sin(params.angleRad)
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
        val sinTheta = sin(params.angleRad)
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

/**
 * Frame de animación con toda la información del proyectil en un instante.
 * Listo para serializar a JSON y consumir en frontend gráfico.
 */
@Serializable
data class TrajectoryFrame(
    val t: Double,
    val x: Double,
    val y: Double,
    val vx: Double,
    val vy: Double,
    val speed: Double,
    val angleDeg: Double
)

/**
 * Datos completos de la simulación: parámetros, resultados y frames.
 */
@Serializable
data class TrajectoryData(
    val params: PhysicsParams,
    val timeOfFlight: Double,
    val range: Double,
    val maxHeight: Double,
    val frames: List<TrajectoryFrame>
)

/**
 * Ejecutor de simulación: genera frames a 60 FPS hasta impacto en suelo.
 * Toda la lógica de generación de frames está aquí (SRP).
 */
class SimulationRunner(
    private val calculator: TrajectoryCalculator = TrajectoryCalculatorFactory.createParabolic(),
    private val fps: Int = 60,
    private val groundThreshold: Double = 0.009
) {

    /** Genera la simulación completa y devuelve datos listos para JSON */
    fun run(params: PhysicsParams): TrajectoryData {
        val timeOfFlight = calculator.calculateTimeOfFlight(params) ?: 0.0
        val range = calculator.calculateRange(params) ?: 0.0
        val maxHeight = calculator.calculateMaxHeight(params)

        val frames = generateFrames(params, timeOfFlight)

        return TrajectoryData(params, timeOfFlight, range, maxHeight, frames)
    }

    private fun generateFrames(params: PhysicsParams, timeOfFlight: Double): List<TrajectoryFrame> {
        val dt = 1.0 / fps
        val frames = mutableListOf<TrajectoryFrame>()
        var t = 0.0

        while (true) {
            val point = calculator.calculateState(params, t)

            // Parar cuando toca suelo (y <= threshold)
            if (point.y <= groundThreshold && t > 0) {
                // Frame final interpolado exactamente en y=0
                if (point.y < 0) {
                    addInterpolatedGroundFrame(params, t - dt, dt, frames)
                }
                break
            }

            frames.add(createFrame(point))
            t += dt

            // Seguridad
            if (t > timeOfFlight + dt) break
        }

        return frames
    }

    private fun createFrame(point: TrajectoryPoint): TrajectoryFrame {
        val speed = hypot(point.vx, point.vy)
        val angleDeg = atan2(point.vy, point.vx) * 180.0 / PI
        return TrajectoryFrame(point.t, point.x, point.y, point.vx, point.vy, speed, angleDeg)
    }

    private fun addInterpolatedGroundFrame(
        params: PhysicsParams,
        prevT: Double,
        dt: Double,
        frames: MutableList<TrajectoryFrame>
    ) {
        val prevPoint = calculator.calculateState(params, prevT)
        val currPoint = calculator.calculateState(params, prevT + dt)

        if (prevPoint.y > 0 && currPoint.y < 0) {
            val ratio = prevPoint.y / (prevPoint.y - currPoint.y)
            val exactT = prevT + ratio * dt
            val exactPoint = calculator.calculateState(params, exactT)
            val speed = hypot(exactPoint.vx, exactPoint.vy)
            val angleDeg = atan2(exactPoint.vy, exactPoint.vx) * 180.0 / PI
            frames.add(TrajectoryFrame(exactT, exactPoint.x, 0.0, exactPoint.vx, exactPoint.vy, speed, angleDeg))
        }
    }
}