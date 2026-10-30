package org.example.animation

import org.example.TrajectoryPoint
import org.example.TrajectorySimulationFactory
import org.example.collision.CollisionDetector
import org.example.collision.CollisionResult
import org.example.model.SimulationState
import org.example.model.Target
import org.example.model.TrajectoryMatrix
import org.example.rendering.CoordinateTransformer
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.SceneConfig
import org.example.trajectory.TrajectoryMatrixGenerator

/**
 * Contrato abstracto para el motor de animación de vuelo (DIP).
 */
interface IAnimationEngine {
    var timeScale: Double
    val currentMatrix: TrajectoryMatrix?
    fun getSamplesPerSecond(): Int
    fun start(matrix: TrajectoryMatrix? = null)
    fun stop()
    fun reset()
    fun isOutOfBounds(canvasX: Double, canvasY: Double): Boolean
    fun tick(now: Long)
    fun interpolatePosition(matrix: TrajectoryMatrix, elapsed: Double): Pair<Double, Double>
}

/**
 * Despachador cohesivo de notificaciones del ciclo de vida del vuelo (SRP).
 */
private class FlightEventNotifier(
    private val onUpdate: () -> Unit,
    private val onFinished: () -> Unit,
    private val onTick: ((Double, Double) -> Unit)?,
    private val collisionListener: CollisionListener?,
    private val onOutOfBounds: (() -> Unit)?,
    private val onFlightCompleted: ((TrajectoryMatrix) -> Unit)?
) {
    fun notifyUpdate() = onUpdate()
    fun notifyFinished() = onFinished()
    fun notifyTick(x: Double, y: Double) { onTick?.invoke(x, y) }
    fun notifyCollision(result: CollisionResult) { collisionListener?.onCollision(result) }
    fun notifyOutOfBounds() { onOutOfBounds?.invoke() }
    fun notifyFlightCompleted(matrix: TrajectoryMatrix) { onFlightCompleted?.invoke(matrix) }
}

/**
 * Evaluador cinemático de interpolación temporal e impacto contra el suelo (SRP + DIP).
 */
private class FlightTrajectoryHelper(
    private val cfg: SceneConfig,
    val transformer: CoordinateTransformer,
    private val trajectoryGenerator: TrajectoryMatrixGenerator,
    private val collisionDetector: CollisionDetector
) {
    fun cannonBaseX(): Double = cfg.cannonBaseX
    fun cannonBaseY(): Double = cfg.cannonBaseY

    fun isOutOfBounds(canvasX: Double, canvasY: Double): Boolean {
        val margin = 15.0
        val outX = canvasX < -margin || canvasX > cfg.canvasWidth + margin
        val outY = canvasY < -margin || canvasY > cfg.canvasHeight + margin
        return outX || outY
    }

    fun generateMatrix(state: SimulationState, samplesPerSecond: Int): TrajectoryMatrix =
        trajectoryGenerator.generate(state.toPhysicsParams(), samplesPerSecond = samplesPerSecond)

    fun evaluateGroundCollision(matrix: TrajectoryMatrix, state: SimulationState): CollisionResult {
        val targetPhysicsX = transformer.toPhysicsX(state.targetX)
        val targetPhysicsRadius = transformer.toPhysicsDistance(state.targetRadius)
        val target = Target(centerX = targetPhysicsX, radius = targetPhysicsRadius)
        return collisionDetector.checkCollision(matrix, target)
    }

    fun interpolate(matrix: TrajectoryMatrix, elapsed: Double): Pair<Double, Double> {
        val points = matrix.points
        if (points.size <= 1) return Pair(points[0].x, points[0].y)

        val idx = findSegmentIndex(points, elapsed)
        val p1 = points[idx]
        val p2 = points[idx + 1]
        val ratio = if (p2.t > p1.t) ((elapsed - p1.t) / (p2.t - p1.t)).coerceIn(0.0, 1.0) else 0.0
        return Pair(p1.x + ratio * (p2.x - p1.x), p1.y + ratio * (p2.y - p1.y))
    }

    private fun findSegmentIndex(points: List<TrajectoryPoint>, elapsed: Double): Int {
        var low = 0
        var high = points.size - 1
        while (low <= high) {
            val mid = (low + high) / 2
            if (points[mid].t <= elapsed) low = mid + 1 else high = mid - 1
        }
        return (low - 1).coerceIn(0, points.size - 2)
    }
}

/**
 * Motor de animación de vuelo (SRP, DIP, OCP).
 */
class AnimationEngine(
    private val state: SimulationState,
    cfg: SceneConfig,
    transformer: CoordinateTransformer = DefaultCoordinateTransformer(cfg),
    trajectoryGenerator: TrajectoryMatrixGenerator = TrajectorySimulationFactory.createGenerator(),
    collisionDetector: CollisionDetector = TrajectorySimulationFactory.createCollisionDetector(),
    onUpdate: () -> Unit,
    onFinished: () -> Unit,
    onTick: ((Double, Double) -> Unit)? = null,
    collisionListener: CollisionListener? = null,
    onOutOfBounds: (() -> Unit)? = null,
    onFlightCompleted: ((TrajectoryMatrix) -> Unit)? = null,
    override var timeScale: Double = 1.75,
    private val frameTicker: FrameTicker = JavaFxFrameTicker(),
    private val samplesPerSecond: Int = 240
) : IAnimationEngine {

    private val notifier = FlightEventNotifier(onUpdate, onFinished, onTick, collisionListener, onOutOfBounds, onFlightCompleted)
    private val helper = FlightTrajectoryHelper(cfg, transformer, trajectoryGenerator, collisionDetector)

    override var currentMatrix: TrajectoryMatrix? = null
        private set

    override fun getSamplesPerSecond(): Int = samplesPerSecond

    override fun start(matrix: TrajectoryMatrix?) {
        val flightMatrix = matrix ?: helper.generateMatrix(state, samplesPerSecond)
        currentMatrix = flightMatrix
        state.startFlight(0L)

        val launch = flightMatrix.launchPoint
        state.updateProjectilePosition(helper.transformer.toCanvasX(launch.x), helper.transformer.toCanvasY(launch.y))
        notifier.notifyTick(state.projectileX, state.projectileY)

        frameTicker.start { now -> tick(now) }
        notifier.notifyUpdate()
    }

    override fun stop() {
        if (!state.isAnimating) return
        state.stopFlight()
        frameTicker.stop()
        notifier.notifyFinished()
    }

    override fun reset() {
        stop()
        state.updateProjectilePosition(helper.cannonBaseX(), helper.cannonBaseY())
        notifier.notifyOutOfBounds()
        notifier.notifyUpdate()
    }

    override fun isOutOfBounds(canvasX: Double, canvasY: Double): Boolean =
        helper.isOutOfBounds(canvasX, canvasY)

    override fun tick(now: Long) {
        if (!state.isAnimating) return
        val matrix = currentMatrix ?: return
        if (state.animationStartTime == 0L) state.animationStartTime = now

        val elapsed = ((now - state.animationStartTime) / 1_000_000_000.0) * timeScale
        if (elapsed >= matrix.timeOfFlight) {
            handleGroundImpact(matrix)
            return
        }
        processInFlight(matrix, elapsed)
    }

    private fun processInFlight(matrix: TrajectoryMatrix, elapsed: Double) {
        val (physX, physY) = interpolatePosition(matrix, elapsed)
        val canvasX = helper.transformer.toCanvasX(physX)
        val canvasY = helper.transformer.toCanvasY(physY)

        if (isOutOfBounds(canvasX, canvasY)) {
            reset()
            return
        }
        state.updateProjectilePosition(canvasX, canvasY)
        notifier.notifyTick(canvasX, canvasY)
        notifier.notifyUpdate()
    }

    private fun handleGroundImpact(matrix: TrajectoryMatrix) {
        val impact = matrix.impactPoint
        val canvasX = helper.transformer.toCanvasX(impact.x)
        val canvasY = helper.transformer.toCanvasY(impact.y)
        state.updateProjectilePosition(canvasX, canvasY)
        notifier.notifyTick(canvasX, canvasY)

        val result = helper.evaluateGroundCollision(matrix, state)
        state.lastCollisionResult = result
        notifier.notifyCollision(result)
        notifier.notifyFlightCompleted(matrix)

        stop()
        notifier.notifyUpdate()
    }

    override fun interpolatePosition(matrix: TrajectoryMatrix, elapsed: Double): Pair<Double, Double> =
        helper.interpolate(matrix, elapsed)
}
