package org.example

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Parámetros de física inmutables y encapsulados para el cálculo de la trayectoria (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class PhysicsParams internal constructor(
    private val values: List<Double>
) : IPhysicsParams {

    constructor(
        angleDeg: Double,
        initialVelocity: Double,
        gravity: Double,
        mass: Double
    ) : this(listOf(angleDeg, initialVelocity, gravity, mass))

    init {
        require(values[1] >= 0.0) { "La velocidad inicial no puede ser negativa" }
        require(values[2] > 0.0) { "La gravedad debe ser positiva" }
        require(values[3] > 0.0) { "La masa debe ser positiva" }
        require(values[0] in -90.0..90.0) { "El ángulo debe estar entre -90° y 90°" }
    }

    override fun angleDegrees(): Double {
        return values[0]
    }

    override fun launchVelocity(): Double {
        return values[1]
    }

    override fun gravityAcceleration(): Double {
        return values[2]
    }

    override fun projectileMass(): Double {
        return values[3]
    }

    fun initialVelocityX(): Double {
        return values[1] * cos(angleRad)
    }

    fun initialVelocityY(): Double {
        return values[1] * sin(angleRad)
    }

    fun initialKineticEnergy(): Double {
        return 0.5 * values[3] * values[1] * values[1]
    }

    fun copy(angleDeg: Double = values[0], initialVelocity: Double = values[1], gravity: Double = values[2], mass: Double = values[3]): PhysicsParams {
        return PhysicsParams(angleDeg, initialVelocity, gravity, mass)
    }

    fun format(): String {
        return "angle=${values[0]}°, v0=${values[1]}m/s, g=${values[2]}m/s², m=${values[3]}kg"
    }
}

/**
 * Punto cinemático encapsulado en la trayectoria con posición, velocidad y tiempo (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class TrajectoryPoint internal constructor(
    private val values: List<Double>
) : ITrajectoryPoint {

    constructor(
        x: Double,
        y: Double,
        vx: Double,
        vy: Double,
        t: Double
    ) : this(listOf(x, y, vx, vy, t))

    override fun posX(): Double {
        return values[0]
    }

    override fun posY(): Double {
        return values[1]
    }

    override fun velX(): Double {
        return values[2]
    }

    override fun velY(): Double {
        return values[3]
    }

    override fun timeSeconds(): Double {
        return values[4]
    }

    fun speed(): Double {
        return hypot(values[2], values[3])
    }

    fun height(): Double {
        return values[1]
    }

    fun distance(): Double {
        return values[0]
    }

    fun toFrame(overrideY: Double = values[1]): TrajectoryFrame {
        val sp = hypot(values[2], values[3])
        val ang = Math.toDegrees(atan2(values[3], values[2]))
        return TrajectoryFrame(values[4], values[0], overrideY, values[2], values[3], sp, ang)
    }
}

/**
 * Implementación estándar de calculadora de trayectoria parabólica (SRP + OCP).
 */
open class ParabolicTrajectoryCalculator(
    private val pointFactory: (Double, Double, Double, Double, Double) -> TrajectoryPoint = ::TrajectoryPoint
) : TrajectoryCalculator {

    override fun calculateState(params: IPhysicsParams, t: Double): TrajectoryPoint {
        require(t >= 0.0) { "El tiempo no puede ser negativo" }
        val vx = params.initialVelocity * cos(params.angleRad)
        val v0y = params.initialVelocity * sin(params.angleRad)
        val vy = v0y - params.gravity * t
        val x = vx * t
        val y = v0y * t - 0.5 * params.gravity * t * t
        return pointFactory(x, y, vx, vy, t)
    }

    override fun calculateTimeOfFlight(params: IPhysicsParams): Double? {
        val v0y = params.initialVelocity * sin(params.angleRad)
        if (v0y <= 0.0) return null
        return 2.0 * v0y / params.gravity
    }

    override fun calculateRange(params: IPhysicsParams): Double? {
        val timeOfFlight = calculateTimeOfFlight(params) ?: return null
        return calculateState(params, timeOfFlight).x
    }

    override fun calculateMaxHeight(params: IPhysicsParams): Double {
        val v0y = params.initialVelocity * sin(params.angleRad)
        if (v0y <= 0.0) return 0.0
        return (v0y * v0y) / (2.0 * params.gravity)
    }
}

/**
 * Factoría para crear instancias de [TrajectoryCalculator] conforme a DIP.
 */
object TrajectoryCalculatorFactory : ITrajectoryCalculatorFactory {
    override fun createParabolic(): TrajectoryCalculator {
        return ParabolicTrajectoryCalculator()
    }

    override fun createCustom(calculator: TrajectoryCalculator): TrajectoryCalculator {
        return calculator
    }
}

/**
 * Frame de animación con la información cinemática del proyectil en un instante (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class TrajectoryFrame internal constructor(
    private val values: List<Double>
) : ITrajectoryPoint {

    constructor(
        t: Double,
        x: Double,
        y: Double,
        vx: Double,
        vy: Double,
        speed: Double,
        angleDeg: Double
    ) : this(listOf(x, y, vx, vy, t, speed, angleDeg))

    override fun posX(): Double {
        return values[0]
    }

    override fun posY(): Double {
        return values[1]
    }

    override fun velX(): Double {
        return values[2]
    }

    override fun velY(): Double {
        return values[3]
    }

    override fun timeSeconds(): Double {
        return values[4]
    }

    fun kineticEnergy(mass: Double): Double {
        return 0.5 * mass * values[5] * values[5]
    }

    fun potentialEnergy(mass: Double, gravity: Double): Double {
        return (mass * gravity * values[1]).coerceAtLeast(0.0)
    }

    fun totalEnergy(mass: Double, gravity: Double): Double {
        return kineticEnergy(mass) + potentialEnergy(mass, gravity)
    }
}

/**
 * Datos completos encapsulados de la simulación: parámetros, resultados y frames (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class TrajectoryData internal constructor(
    private val content: Triple<IPhysicsParams, List<Double>, List<TrajectoryFrame>>
) : ITrajectoryData {

    constructor(
        params: IPhysicsParams,
        timeOfFlight: Double,
        range: Double,
        maxHeight: Double,
        frames: List<TrajectoryFrame>
    ) : this(Triple(params, listOf(timeOfFlight, range, maxHeight), frames))

    override fun parameters(): IPhysicsParams {
        return content.first
    }

    override fun totalFlightTime(): Double {
        return content.second[0]
    }

    override fun horizontalRange(): Double {
        return content.second[1]
    }

    override fun peakHeight(): Double {
        return content.second[2]
    }

    override fun frameCount(): Int {
        return content.third.size
    }

    fun describeSummary(): String {
        return "${content.first.angleDeg}° -> T=${content.second[0]}s, R=${content.second[1]}m, frames=${content.third.size}"
    }

    fun frameAt(index: Int): TrajectoryFrame {
        val framesList = content.third
        val safeIdx = index.coerceIn(0, (framesList.size - 1).coerceAtLeast(0))
        return framesList[safeIdx]
    }
}

/**
 * Ejecutor de simulación: genera frames discretos hasta el impacto en el suelo (SRP + DIP).
 */
class SimulationRunner(
    private val calculator: TrajectoryCalculator = TrajectoryCalculatorFactory.createParabolic(),
    fps: Int = 60,
    groundThreshold: Double = 0.009
) : ISimulationRunner {
    private val runnerCfg: Pair<Double, Double> = Pair(1.0 / fps, groundThreshold)

    override fun run(params: IPhysicsParams): ITrajectoryData {
        val timeOfFlight = calculator.calculateTimeOfFlight(params) ?: 0.0
        val range = calculator.calculateRange(params) ?: 0.0
        val maxHeight = calculator.calculateMaxHeight(params)
        val frames = generateFrames(params, timeOfFlight)
        return TrajectoryData(params, timeOfFlight, range, maxHeight, frames)
    }

    private fun generateFrames(params: IPhysicsParams, timeOfFlight: Double): List<TrajectoryFrame> {
        val dt = runnerCfg.first
        val frames = mutableListOf<TrajectoryFrame>()
        var t = 0.0
        while (t <= timeOfFlight + dt) {
            val point = calculator.calculateState(params, t)
            if (point.y <= runnerCfg.second && t > 0.0) {
                appendGroundCrossingIfNeeded(params, point.y, t - dt, frames)
                break
            }
            frames.add(point.toFrame())
            t += dt
        }
        return frames
    }

    private fun appendGroundCrossingIfNeeded(params: IPhysicsParams, pointY: Double, prevT: Double, frames: MutableList<TrajectoryFrame>) {
        if (pointY >= 0.0) return
        val dt = runnerCfg.first
        val prevPoint = calculator.calculateState(params, prevT)
        if (prevPoint.y <= 0.0) return
        val ratio = prevPoint.y / (prevPoint.y - pointY)
        frames.add(calculator.calculateState(params, prevT + ratio * dt).toFrame(0.0))
    }
}
