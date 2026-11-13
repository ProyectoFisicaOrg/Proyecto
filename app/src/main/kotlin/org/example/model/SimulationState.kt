package org.example.model

import org.example.IPhysicsParams
import org.example.PhysicsParams
import org.example.collision.ICollisionResult

/**
 * Instantánea inmutable de estado de diana e impacto para cohesión interna (SRP).
 */
private data class TargetCollisionSnapshot(
    private val posX: Double,
    private val rad: Double,
    private val collision: ICollisionResult?
) : IStateSnapshot {
    override fun isSnapshotReady(): Boolean {
        return rad > 0.0 && posX >= 0.0
    }
    fun x(): Double {
        return posX
    }

    fun radius(): Double {
        return rad
    }

    fun result(): ICollisionResult? {
        return collision
    }

    fun withX(newX: Double): TargetCollisionSnapshot {
        return TargetCollisionSnapshot(newX, rad, collision)
    }

    fun withRadius(newRad: Double): TargetCollisionSnapshot {
        return TargetCollisionSnapshot(posX, newRad, collision)
    }

    fun withCollision(newRes: ICollisionResult?): TargetCollisionSnapshot {
        return TargetCollisionSnapshot(posX, rad, newRes)
    }
}

/**
 * Instantánea inmutable de movimiento del proyectil en vuelo (SRP).
 */
private data class FlightMotionSnapshot(
    private val active: Boolean,
    private val startNano: Long,
    private val coords: Pair<Double, Double>
) : IStateSnapshot {
    override fun isSnapshotReady(): Boolean {
        return startNano >= 0L && (active || coords.first >= -1e9)
    }
    fun isAnimating(): Boolean {
        return active
    }

    fun startTime(): Long {
        return startNano
    }

    fun position(): Pair<Double, Double> {
        return coords
    }

    fun withAnimating(flag: Boolean): FlightMotionSnapshot {
        return FlightMotionSnapshot(flag, startNano, coords)
    }

    fun withStartTime(nano: Long): FlightMotionSnapshot {
        return FlightMotionSnapshot(active, nano, coords)
    }

    fun withPosition(x: Double, y: Double): FlightMotionSnapshot {
        return FlightMotionSnapshot(active, startNano, Pair(x, y))
    }
}

/**
 * Estado base encapsulado y cohesivo de los parámetros configurables del lanzamiento (SRP + Encapsulación).
 */
class LaunchParametersState(
    initialParams: PhysicsParams
) : ILaunchParametersState {

    constructor() : this(PhysicsParams(45.0, 50.0, 9.8, 10.0))

    private var params: PhysicsParams = initialParams

    override fun hasValidLaunchConfig(): Boolean {
        return params.launchVelocity() >= 0.0
    }

    override fun currentAngle(): Double {
        return params.angleDegrees()
    }

    override fun updateAngle(value: Double) {
        require(value in 0.0..90.0) { "El ángulo debe estar entre 0° y 90°" }
        params = params.copy(angleDeg = value)
    }

    override fun currentVelocity(): Double {
        return params.launchVelocity()
    }

    override fun updateVelocity(value: Double) {
        require(value >= 0.0) { "La velocidad no puede ser negativa" }
        params = params.copy(initialVelocity = value)
    }

    override fun currentGravity(): Double {
        return params.gravityAcceleration()
    }

    override fun updateGravity(value: Double) {
        require(value > 0.0) { "La gravedad debe ser mayor a cero" }
        params = params.copy(gravity = value)
    }

    override fun currentMass(): Double {
        return params.projectileMass()
    }

    override fun updateMass(value: Double) {
        require(value > 0.0) { "La masa debe ser mayor a cero" }
        params = params.copy(mass = value)
    }

    override fun toPhysicsParams(): IPhysicsParams {
        return params
    }
}

/**
 * Implementación encapsulada y cohesiva del estado de la diana y colisiones (SRP + Encapsulación).
 */
class TargetAndCollisionState : ITargetAndCollisionState {
    private var snapshot: TargetCollisionSnapshot = TargetCollisionSnapshot(750.0, 20.0, null)

    override fun currentTargetX(): Double {
        return snapshot.x()
    }

    override fun changeTargetX(value: Double) {
        snapshot = snapshot.withX(value)
    }

    override fun currentTargetRadius(): Double {
        return snapshot.radius()
    }

    override fun changeTargetRadius(value: Double) {
        require(value > 0.0) { "El radio de la diana debe ser positivo" }
        snapshot = snapshot.withRadius(value)
    }

    override fun currentCollisionResult(): ICollisionResult? {
        return snapshot.result()
    }

    override fun changeCollisionResult(result: ICollisionResult?) {
        snapshot = snapshot.withCollision(result)
    }
}

/**
 * Contenedor encapsulado del estado dinámico de la simulación (SRP + DIP + Encapsulación).
 */
class SimulationState(
    launchProvider: () -> ILaunchParametersState = ::LaunchParametersState,
    targetProvider: () -> ITargetAndCollisionState = ::TargetAndCollisionState
) : ISimulationState, ILaunchParametersState by launchProvider(), ITargetAndCollisionState by targetProvider() {

    constructor() : this(::LaunchParametersState, ::TargetAndCollisionState)

    private var flight: FlightMotionSnapshot = FlightMotionSnapshot(false, 0L, Pair(90.0, 470.0))

    override fun checkAnimating(): Boolean {
        return flight.isAnimating()
    }

    override fun changeAnimating(active: Boolean) {
        flight = flight.withAnimating(active)
    }

    override fun flightStartTimeNano(): Long {
        return flight.startTime()
    }

    override fun changeFlightStartTimeNano(nano: Long) {
        flight = flight.withStartTime(nano)
    }

    override fun currentProjectileCoords(): Pair<Double, Double> {
        return flight.position()
    }

    override fun updateProjectilePosition(x: Double, y: Double) {
        flight = flight.withPosition(x, y)
    }

    override fun resetProjectile(baseX: Double, baseY: Double) {
        flight = FlightMotionSnapshot(false, 0L, Pair(baseX, baseY))
    }

    override fun startFlight(nowNano: Long) {
        flight = flight.withAnimating(true).withStartTime(nowNano)
    }

    override fun stopFlight() {
        flight = flight.withAnimating(false)
    }

    override fun clearCollisionResult() {
        changeCollisionResult(null)
    }
}
