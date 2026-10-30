package org.example.model

import org.example.PhysicsParams
import org.example.collision.CollisionResult

/**
 * Contrato de parámetros cinemáticos iniciales del lanzamiento (ISP + DIP).
 */
interface ILaunchKinematics {
    fun currentAngle(): Double
    fun updateAngle(value: Double)
    fun currentVelocity(): Double
    fun updateVelocity(value: Double)

    var angle: Double
        get() = currentAngle()
        set(value) = updateAngle(value)

    var velocity: Double
        get() = currentVelocity()
        set(value) = updateVelocity(value)
}

/**
 * Contrato de parámetros físicos ambientales y de masa (ISP + DIP).
 */
interface IEnvironmentParameters {
    fun currentGravity(): Double
    fun updateGravity(value: Double)
    fun currentMass(): Double
    fun updateMass(value: Double)

    var gravity: Double
        get() = currentGravity()
        set(value) = updateGravity(value)

    var mass: Double
        get() = currentMass()
        set(value) = updateMass(value)
}

/**
 * Estado base encapsulado de los parámetros configurables del lanzamiento (SRP + Encapsulación).
 */
open class LaunchParametersState : ILaunchKinematics, IEnvironmentParameters {
    private var angleDeg: Double = 45.0
    private var initialVel: Double = 50.0
    private var gravityAcc: Double = 9.8
    private var projectileMass: Double = 10.0

    override fun currentAngle(): Double = angleDeg

    override fun updateAngle(value: Double) {
        require(value in 0.0..90.0) { "El ángulo debe estar entre 0° y 90°" }
        angleDeg = value
    }

    override fun currentVelocity(): Double = initialVel

    override fun updateVelocity(value: Double) {
        require(value >= 0.0) { "La velocidad no puede ser negativa" }
        initialVel = value
    }

    override fun currentGravity(): Double = gravityAcc

    override fun updateGravity(value: Double) {
        require(value > 0.0) { "La gravedad debe ser mayor a cero" }
        gravityAcc = value
    }

    override fun currentMass(): Double = projectileMass

    override fun updateMass(value: Double) {
        require(value > 0.0) { "La masa debe ser mayor a cero" }
        projectileMass = value
    }

    fun toPhysicsParams(): PhysicsParams =
        PhysicsParams(angleDeg = angleDeg, initialVelocity = initialVel, gravity = gravityAcc, mass = projectileMass)
}

/**
 * Contrato de estado del objetivo y resultado de impacto (ISP + DIP).
 */
interface ITargetAndCollisionState {
    fun currentTargetX(): Double
    fun changeTargetX(value: Double)
    fun currentTargetRadius(): Double
    fun changeTargetRadius(value: Double)
    fun currentCollisionResult(): CollisionResult?
    fun changeCollisionResult(result: CollisionResult?)

    var targetX: Double
        get() = currentTargetX()
        set(value) = changeTargetX(value)

    var targetRadius: Double
        get() = currentTargetRadius()
        set(value) = changeTargetRadius(value)

    var lastCollisionResult: CollisionResult?
        get() = currentCollisionResult()
        set(value) = changeCollisionResult(value)
}

/**
 * Implementación encapsulada del estado de la diana y colisiones (SRP + Encapsulación).
 */
class TargetAndCollisionState : ITargetAndCollisionState {
    private var targetPosX: Double = 750.0
    private var targetRad: Double = 20.0
    private var collisionRes: CollisionResult? = null

    override fun currentTargetX(): Double = targetPosX

    override fun changeTargetX(value: Double) {
        targetPosX = value
    }

    override fun currentTargetRadius(): Double = targetRad

    override fun changeTargetRadius(value: Double) {
        require(value > 0.0) { "El radio de la diana debe ser positivo" }
        targetRad = value
    }

    override fun currentCollisionResult(): CollisionResult? = collisionRes

    override fun changeCollisionResult(result: CollisionResult?) {
        collisionRes = result
    }
}

/**
 * Contrato de movimiento y animación del proyectil en el canvas (ISP + DIP).
 */
interface IFlightMotionState {
    fun checkAnimating(): Boolean
    fun changeAnimating(active: Boolean)
    fun flightStartTimeNano(): Long
    fun changeFlightStartTimeNano(nano: Long)
    fun currentProjectileCoords(): Pair<Double, Double>
    fun updateProjectilePosition(x: Double, y: Double)

    var isAnimating: Boolean
        get() = checkAnimating()
        set(value) = changeAnimating(value)

    var animationStartTime: Long
        get() = flightStartTimeNano()
        set(value) = changeFlightStartTimeNano(value)

    var projectileX: Double
        get() = currentProjectileCoords().first
        set(value) = updateProjectilePosition(value, currentProjectileCoords().second)

    var projectileY: Double
        get() = currentProjectileCoords().second
        set(value) = updateProjectilePosition(currentProjectileCoords().first, value)
}

/**
 * Contenedor encapsulado del estado dinámico de la simulación (SRP + DIP + Encapsulación).
 */
class SimulationState(
    private val targetState: ITargetAndCollisionState = TargetAndCollisionState()
) : LaunchParametersState(), IFlightMotionState, ITargetAndCollisionState by targetState {
    private var animating: Boolean = false
    private var startTimeNano: Long = 0L
    private var projX: Double = 90.0
    private var projY: Double = 470.0

    override fun checkAnimating(): Boolean = animating

    override fun changeAnimating(active: Boolean) {
        animating = active
    }

    override fun flightStartTimeNano(): Long = startTimeNano

    override fun changeFlightStartTimeNano(nano: Long) {
        startTimeNano = nano
    }

    override fun currentProjectileCoords(): Pair<Double, Double> = Pair(projX, projY)

    override fun updateProjectilePosition(x: Double, y: Double) {
        projX = x
        projY = y
    }

    fun resetProjectile(baseX: Double, baseY: Double) {
        projX = baseX
        projY = baseY
        animating = false
        startTimeNano = 0L
    }

    fun startFlight(nowNano: Long = System.nanoTime()) {
        animating = true
        startTimeNano = nowNano
    }

    fun stopFlight() {
        animating = false
    }

    fun clearCollisionResult() {
        changeCollisionResult(null)
    }
}
