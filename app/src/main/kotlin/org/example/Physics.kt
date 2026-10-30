package org.example

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Contrato de lectura de parámetros físicos del disparo (ISP + DIP).
 */
interface IPhysicsParams {
    fun angleDegrees(): Double
    fun launchVelocity(): Double
    fun gravityAcceleration(): Double
    fun projectileMass(): Double

    val angleDeg: Double get() = angleDegrees()
    val initialVelocity: Double get() = launchVelocity()
    val gravity: Double get() = gravityAcceleration()
    val mass: Double get() = projectileMass()
    val angleRad: Double get() = angleDegrees() * PI / 180.0
}

/**
 * Parámetros de física inmutables y encapsulados para el cálculo de la trayectoria (SRP + Encapsulación).
 */
class PhysicsParams(
    angleDeg: Double,
    initialVelocity: Double,
    gravity: Double,
    mass: Double
) : IPhysicsParams {
    private val angleVal: Double = angleDeg
    private val velocityVal: Double = initialVelocity
    private val gravityVal: Double = gravity
    private val massVal: Double = mass

    init {
        require(initialVelocity >= 0) { "La velocidad inicial no puede ser negativa" }
        require(gravity > 0) { "La gravedad debe ser positiva" }
        require(mass > 0) { "La masa debe ser positiva" }
        require(angleDeg in -90.0..90.0) { "El ángulo debe estar entre -90° y 90°" }
    }

    override fun angleDegrees(): Double = angleVal

    override fun launchVelocity(): Double = velocityVal

    override fun gravityAcceleration(): Double = gravityVal

    override fun projectileMass(): Double = massVal

    fun initialVelocityX(): Double = velocityVal * cos(angleRad)

    fun initialVelocityY(): Double = velocityVal * sin(angleRad)

    fun initialKineticEnergy(): Double = 0.5 * massVal * velocityVal * velocityVal

    fun copy(angleDeg: Double = angleVal, initialVelocity: Double = velocityVal, gravity: Double = gravityVal, mass: Double = massVal): PhysicsParams =
        PhysicsParams(angleDeg, initialVelocity, gravity, mass)

    fun format(): String = "angle=${angleVal}°, v0=${velocityVal}m/s, g=${gravityVal}m/s², m=${massVal}kg"

    private fun components(): List<Double> = listOf(angleVal, velocityVal, gravityVal, massVal)

    override fun equals(other: Any?): Boolean = other is PhysicsParams && components() == other.components()

    override fun hashCode(): Int = components().hashCode()
}

/**
 * Contrato de estado cinemático puntual en la trayectoria (ISP + DIP).
 */
interface ITrajectoryPoint {
    fun posX(): Double
    fun posY(): Double
    fun velX(): Double
    fun velY(): Double
    fun timeSeconds(): Double

    val x: Double get() = posX()
    val y: Double get() = posY()
    val vx: Double get() = velX()
    val vy: Double get() = velY()
    val t: Double get() = timeSeconds()
}

/**
 * Representa un punto encapsulado en la trayectoria con posición, velocidad y tiempo (SRP + Encapsulación).
 */
class TrajectoryPoint(
    x: Double,
    y: Double,
    vx: Double,
    vy: Double,
    t: Double
) : ITrajectoryPoint {
    private val posXVal: Double = x
    private val posYVal: Double = y
    private val velXVal: Double = vx
    private val velYVal: Double = vy
    private val timeVal: Double = t

    override fun posX(): Double = posXVal

    override fun posY(): Double = posYVal

    override fun velX(): Double = velXVal

    override fun velY(): Double = velYVal

    override fun timeSeconds(): Double = timeVal

    fun speed(): Double = hypot(velXVal, velYVal)

    fun height(): Double = posYVal

    fun distance(): Double = posXVal

    private fun components(): List<Double> = listOf(posXVal, posYVal, velXVal, velYVal, timeVal)

    override fun equals(other: Any?): Boolean = other is TrajectoryPoint && components() == other.components()

    override fun hashCode(): Int = components().hashCode()

    override fun toString(): String = "TrajectoryPoint(x=$posXVal, y=$posYVal, vx=$velXVal, vy=$velYVal, t=$timeVal)"
}

/**
 * Interfaz para calculadores de trayectoria (DIP + ISP).
 */
interface TrajectoryCalculator {
    fun calculateState(params: PhysicsParams, t: Double): TrajectoryPoint
    fun calculateTimeOfFlight(params: PhysicsParams): Double?
    fun calculateRange(params: PhysicsParams): Double?
    fun calculateMaxHeight(params: PhysicsParams): Double
}

/**
 * Implementación estándar de calculadora de trayectoria parabólica (SRP + OCP).
 */
open class ParabolicTrajectoryCalculator : TrajectoryCalculator {

    override fun calculateState(params: PhysicsParams, t: Double): TrajectoryPoint {
        require(t >= 0) { "El tiempo no puede ser negativo" }
        val vx = params.initialVelocity * cos(params.angleRad)
        val v0y = params.initialVelocity * sin(params.angleRad)
        val vy = v0y - params.gravity * t
        val x = vx * t
        val y = v0y * t - 0.5 * params.gravity * t * t
        return TrajectoryPoint(x, y, vx, vy, t)
    }

    override fun calculateTimeOfFlight(params: PhysicsParams): Double? {
        val v0y = params.initialVelocity * sin(params.angleRad)
        if (v0y <= 0) return null
        return 2.0 * v0y / params.gravity
    }

    override fun calculateRange(params: PhysicsParams): Double? {
        val timeOfFlight = calculateTimeOfFlight(params)
        return timeOfFlight?.let { calculateState(params, it).x }
    }

    override fun calculateMaxHeight(params: PhysicsParams): Double {
        val v0y = params.initialVelocity * sin(params.angleRad)
        if (v0y <= 0) return 0.0
        return (v0y * v0y) / (2.0 * params.gravity)
    }
}

/**
 * Factoría para crear instancias de TrajectoryCalculator (DIP).
 */
object TrajectoryCalculatorFactory {
    fun createParabolic(): TrajectoryCalculator = ParabolicTrajectoryCalculator()

    fun createCustom(calculator: TrajectoryCalculator): TrajectoryCalculator = calculator
}

/**
 * Frame de animación con la información cinemática del proyectil en un instante (SRP + Encapsulación).
 */
data class TrajectoryFrame(
    private val t: Double,
    private val x: Double,
    private val y: Double,
    private val vx: Double,
    private val vy: Double,
    private val speed: Double,
    private val angleDeg: Double
) {
    fun time(): Double = t

    fun posX(): Double = x

    fun posY(): Double = y

    fun kineticEnergy(mass: Double): Double = 0.5 * mass * speed * speed

    fun potentialEnergy(mass: Double, gravity: Double): Double = (mass * gravity * y).coerceAtLeast(0.0)

    fun totalEnergy(mass: Double, gravity: Double): Double = kineticEnergy(mass) + potentialEnergy(mass, gravity)
}

/**
 * Datos completos encapsulados de la simulación: parámetros, resultados y frames (SRP + Encapsulación).
 */
data class TrajectoryData(
    private val params: PhysicsParams,
    private val timeOfFlight: Double,
    private val range: Double,
    private val maxHeight: Double,
    private val frames: List<TrajectoryFrame>
) {
    fun parameters(): PhysicsParams = params

    fun totalFlightTime(): Double = timeOfFlight

    fun horizontalRange(): Double = range

    fun peakHeight(): Double = maxHeight

    fun frameCount(): Int = frames.size

    fun initialFrame(): TrajectoryFrame? = frames.firstOrNull()

    fun finalFrame(): TrajectoryFrame? = frames.lastOrNull()

    fun frameAt(index: Int): TrajectoryFrame = frames[index.coerceIn(0, (frames.size - 1).coerceAtLeast(0))]
}

/**
 * Ejecutor de simulación: genera frames a 60 FPS hasta impacto en suelo (SRP + DIP).
 */
class SimulationRunner(
    private val calculator: TrajectoryCalculator = TrajectoryCalculatorFactory.createParabolic(),
    private val fps: Int = 60,
    private val groundThreshold: Double = 0.009
) {

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

        while (t <= timeOfFlight + dt) {
            val point = calculator.calculateState(params, t)
            if (hasReachedGround(point, t)) {
                if (point.y < 0.0) appendFinalGroundFrame(params, t - dt, dt, frames)
                break
            }
            frames.add(createFrame(point))
            t += dt
        }
        return frames
    }

    private fun hasReachedGround(point: TrajectoryPoint, t: Double): Boolean =
        point.y <= groundThreshold && t > 0.0

    private fun appendFinalGroundFrame(params: PhysicsParams, prevT: Double, dt: Double, frames: MutableList<TrajectoryFrame>) {
        computeGroundFrame(params, prevT, dt)?.let { frames.add(it) }
    }

    private fun createFrame(point: TrajectoryPoint): TrajectoryFrame {
        val speed = hypot(point.vx, point.vy)
        val angleDeg = atan2(point.vy, point.vx) * 180.0 / PI
        return TrajectoryFrame(point.t, point.x, point.y, point.vx, point.vy, speed, angleDeg)
    }

    private fun computeGroundFrame(params: PhysicsParams, prevT: Double, dt: Double): TrajectoryFrame? {
        val prevPoint = calculator.calculateState(params, prevT)
        val currPoint = calculator.calculateState(params, prevT + dt)
        if (prevPoint.y <= 0.0 || currPoint.y >= 0.0) return null

        val ratio = prevPoint.y / (prevPoint.y - currPoint.y)
        val exactT = prevT + ratio * dt
        val exactPoint = calculator.calculateState(params, exactT)
        val speed = hypot(exactPoint.vx, exactPoint.vy)
        val angleDeg = atan2(exactPoint.vy, exactPoint.vx) * 180.0 / PI
        return TrajectoryFrame(exactT, exactPoint.x, 0.0, exactPoint.vx, exactPoint.vy, speed, angleDeg)
    }
}
