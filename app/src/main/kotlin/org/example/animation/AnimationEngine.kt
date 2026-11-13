package org.example.animation

import org.example.SimulationFactoryWiring
import org.example.collision.CollisionDetector
import org.example.collision.ICollisionResult
import org.example.model.ISimulationState
import org.example.model.ITrajectoryMatrixSummary
import org.example.model.Target
import org.example.rendering.CoordinateTransformer
import org.example.rendering.ISceneConfig
import org.example.trajectory.TrajectoryMatrixGenerator

/**
 * Evaluador cinemático de impacto contra el suelo desacoplado por interfaces (SRP + DIP).
 */
class FlightCollisionEvaluator(
    transformer: CoordinateTransformer,
    collisionDetector: CollisionDetector
) : IFlightCollisionEvaluator {
    private val deps: Pair<CoordinateTransformer, CollisionDetector> = Pair(transformer, collisionDetector)

    override fun evaluateGroundCollision(matrix: ITrajectoryMatrixSummary, state: ISimulationState): ICollisionResult {
        val targetPhysicsX = deps.first.toPhysicsX(state.currentTargetX())
        val targetPhysicsRadius = deps.first.toPhysicsDistance(state.currentTargetRadius())
        return deps.second.checkCollision(matrix, Target(targetPhysicsX, targetPhysicsRadius))
    }
}

/**
 * Empaquetado cohesivo e inmutable de callbacks de ciclo de vida del vuelo (SRP).
 */
class FlightEventHooks(
    onUpdate: () -> Unit,
    onFinished: () -> Unit,
    onTick: ((Double, Double) -> Unit)? = null,
    collisionListener: CollisionListener? = null,
    onOutOfBounds: (() -> Unit)? = null,
    onFlightCompleted: ((ITrajectoryMatrixSummary) -> Unit)? = null
) : IFlightCompletionNotifier {
    private val hooks: Pair<Triple<() -> Unit, () -> Unit, (() -> Unit)?>, Triple<((Double, Double) -> Unit)?, CollisionListener?, ((ITrajectoryMatrixSummary) -> Unit)?>> =
        Pair(Triple(onUpdate, onFinished, onOutOfBounds), Triple(onTick, collisionListener, onFlightCompleted))

    override fun notifyUpdate() {
        hooks.first.first.invoke()
    }

    override fun notifyFinished() {
        hooks.first.second.invoke()
    }

    override fun notifyOutOfBounds() {
        hooks.first.third?.invoke()
    }

    override fun notifyTick(x: Double, y: Double) {
        hooks.second.first?.invoke(x, y)
    }

    override fun notifyCollision(result: ICollisionResult) {
        val listener = hooks.second.second
        if (listener != null) {
            listener.onCollision(result)
            hooks.first.first.invoke()
        }
    }

    override fun notifyFlightCompleted(matrix: ITrajectoryMatrixSummary) {
        hooks.second.third?.invoke(matrix)
    }
}

/**
 * Adaptador geométrico y cinemático del estado de vuelo sobre el escenario (SRP + DIP).
 */
class FlightKinematicsPort(
    state: ISimulationState,
    cfg: ISceneConfig,
    transformer: CoordinateTransformer,
    generator: TrajectoryMatrixGenerator = SimulationFactoryWiring.buildGenerator()
) : IFlightKinematicsPort {
    private val ctx: Pair<Pair<ISimulationState, ISceneConfig>, Pair<CoordinateTransformer, TrajectoryMatrixGenerator>> =
        Pair(Pair(state, cfg), Pair(transformer, generator))

    override fun stateModel(): ISimulationState {
        return ctx.first.first
    }

    override fun baseOrigin(): Pair<Double, Double> {
        return Pair(ctx.first.second.basePivotX(), ctx.first.second.basePivotY())
    }

    override fun isWithinCanvasBounds(canvasX: Double, canvasY: Double): Boolean {
        return ctx.first.second.isWithinCanvas(canvasX, canvasY, 15.0)
    }

    override fun toCanvasPoint(physicsX: Double, physicsY: Double): Pair<Double, Double> {
        return Pair(ctx.second.first.toCanvasX(physicsX), ctx.second.first.toCanvasY(physicsY))
    }

    override fun generateMatrix(samplesPerSecond: Int): ITrajectoryMatrixSummary {
        return ctx.second.second.generate(ctx.first.first.toPhysicsParams(), samplesPerSecond)
    }
}

/**
 * Implementación estándar del contexto de ejecución de vuelo (SRP + DIP).
 */
class DefaultAnimationRuntimeContext(
    kinematics: IFlightKinematicsPort,
    evaluator: IFlightCollisionEvaluator,
    notifier: IFlightCompletionNotifier,
    ticker: FrameTicker
) : IAnimationRuntimeContext, IFlightKinematicsPort by kinematics {
    private val deps: Triple<IFlightCollisionEvaluator, IFlightCompletionNotifier, FrameTicker> =
        Triple(evaluator, notifier, ticker)

    constructor(
        kinematics: IFlightKinematicsPort,
        evaluator: IFlightCollisionEvaluator,
        notifier: IFlightCompletionNotifier,
        tickerProvider: () -> FrameTicker = ::JavaFxFrameTicker
    ) : this(kinematics, evaluator, notifier, tickerProvider())

    override fun evaluator(): IFlightCollisionEvaluator {
        return deps.first
    }

    override fun hooks(): IFlightCompletionNotifier {
        return deps.second
    }

    override fun startTicker(step: (Long) -> Unit) {
        deps.third.start(step)
    }

    override fun stopTicker() {
        deps.third.stop()
    }
}

/**
 * Fábricas cohesivas de puertos cinemáticos y evaluadores de impacto (SRP + DIP).
 */
object FlightKinematicsWiring : IFlightKinematicsFactory {
    override fun createKinematics(state: ISimulationState, cfg: ISceneConfig, transformer: CoordinateTransformer): IFlightKinematicsPort {
        return FlightKinematicsPort(state, cfg, transformer)
    }
}

object FlightEvaluatorWiring : IFlightEvaluatorFactory {
    override fun createEvaluator(transformer: CoordinateTransformer): IFlightCollisionEvaluator {
        return FlightCollisionEvaluator(transformer, SimulationFactoryWiring.buildCollisionDetector())
    }
}

object DefaultFlightPortFactory : IFlightPortFactory,
    IFlightKinematicsFactory by FlightKinematicsWiring,
    IFlightEvaluatorFactory by FlightEvaluatorWiring {
    override fun portFactoryId(): String {
        return "DefaultFlightPortFactory"
    }
}

object RuntimeContextWiring : IRuntimeContextWiring {
    override fun createDefault(kinematics: IFlightKinematicsPort, evaluator: IFlightCollisionEvaluator, hooks: IFlightCompletionNotifier): IAnimationRuntimeContext {
        return DefaultAnimationRuntimeContext(kinematics, evaluator, hooks)
    }

    override fun createWithTicker(kinematics: IFlightKinematicsPort, evaluator: IFlightCollisionEvaluator, hooks: IFlightCompletionNotifier, ticker: FrameTicker): IAnimationRuntimeContext {
        return DefaultAnimationRuntimeContext(kinematics, evaluator, hooks, ticker)
    }
}

object DefaultAnimationEngineFactory : IAnimationEngineFactory {
    override fun create(kinematics: IFlightKinematicsPort, evaluator: IFlightCollisionEvaluator, hooks: IFlightCompletionNotifier, timeScale: Double): IAnimationEngine {
        return AnimationEngine(RuntimeContextWiring.createDefault(kinematics, evaluator, hooks), timeScale)
    }
}

/**
 * Motor de animación de vuelo desacoplado mediante [IAnimationRuntimeContext] (SRP + DIP + OCP).
 */
class AnimationEngine(
    runtime: IAnimationRuntimeContext,
    timeScale: Double = 1.75,
    samplesPerSecond: Int = 240
) : IAnimationEngine {
    private val slot: MutableList<Pair<IAnimationRuntimeContext, Triple<Double, Int, ITrajectoryMatrixSummary?>>> =
        mutableListOf(Pair(runtime, Triple(timeScale, samplesPerSecond, null)))

    override fun currentTimeScale(): Double {
        return slot[0].second.first
    }

    override fun updateTimeScale(value: Double) {
        val cur = slot[0]
        slot[0] = Pair(cur.first, Triple(value, cur.second.second, cur.second.third))
    }

    override fun activeTrajectoryMatrix(): ITrajectoryMatrixSummary? {
        return slot[0].second.third
    }

    override fun getSamplesPerSecond(): Int {
        return slot[0].second.second
    }

    override fun start(matrix: ITrajectoryMatrixSummary?) {
        val rt = slot[0].first
        val cfg = slot[0].second
        val flightMatrix = matrix ?: rt.generateMatrix(cfg.second)
        slot[0] = Pair(rt, Triple(cfg.first, cfg.second, flightMatrix))
        val st = rt.stateModel()
        st.startFlight(0L)
        val firstPt = flightMatrix.firstPoint()
        val launchCanvas = rt.toCanvasPoint(firstPt.posX(), firstPt.posY())
        st.updateProjectilePosition(launchCanvas.first, launchCanvas.second)
        val curCoords = st.currentProjectileCoords()
        rt.hooks().notifyTick(curCoords.first, curCoords.second)
        rt.startTicker { now -> tick(now) }
        rt.hooks().notifyUpdate()
    }

    override fun stop() {
        val rt = slot[0].first
        val st = rt.stateModel()
        if (!st.checkAnimating()) return
        st.stopFlight()
        rt.stopTicker()
        rt.hooks().notifyFinished()
    }

    override fun reset() {
        stop()
        val rt = slot[0].first
        val origin = rt.baseOrigin()
        rt.stateModel().updateProjectilePosition(origin.first, origin.second)
        rt.hooks().notifyOutOfBounds()
        rt.hooks().notifyUpdate()
    }

    override fun isOutOfBounds(canvasX: Double, canvasY: Double): Boolean {
        return !slot[0].first.isWithinCanvasBounds(canvasX, canvasY)
    }

    override fun tick(now: Long) {
        val rt = slot[0].first
        val st = rt.stateModel()
        if (!st.checkAnimating()) return
        val cfg = slot[0].second
        val matrix = cfg.third ?: return
        if (st.flightStartTimeNano() == 0L) st.changeFlightStartTimeNano(now)
        val elapsed = ((now - st.flightStartTimeNano()) / 1_000_000_000.0) * cfg.first
        if (elapsed >= matrix.totalTimeOfFlight()) handleGroundImpact(matrix) else processInFlight(matrix, elapsed)
    }

    private fun processInFlight(matrix: ITrajectoryMatrixSummary, elapsed: Double) {
        val rt = slot[0].first
        val phys = interpolatePosition(matrix, elapsed)
        val canvasPt = rt.toCanvasPoint(phys.first, phys.second)
        if (isOutOfBounds(canvasPt.first, canvasPt.second)) {
            reset()
            return
        }
        rt.stateModel().updateProjectilePosition(canvasPt.first, canvasPt.second)
        rt.hooks().notifyTick(canvasPt.first, canvasPt.second)
        rt.hooks().notifyUpdate()
    }

    private fun handleGroundImpact(matrix: ITrajectoryMatrixSummary) {
        val rt = slot[0].first
        val st = rt.stateModel()
        val hooks = rt.hooks()
        val lastPt = matrix.finalPoint()
        val impactPt = rt.toCanvasPoint(lastPt.posX(), lastPt.posY())
        st.updateProjectilePosition(impactPt.first, impactPt.second)
        hooks.notifyTick(impactPt.first, impactPt.second)
        val result = rt.evaluator().evaluateGroundCollision(matrix, st)
        st.changeCollisionResult(result)
        hooks.notifyCollision(result)
        hooks.notifyFlightCompleted(matrix)
        stop()
        hooks.notifyUpdate()
    }

    override fun interpolatePosition(matrix: ITrajectoryMatrixSummary, elapsed: Double): Pair<Double, Double> {
        return if (slot[0].second.second > 0) matrix.interpolateAt(elapsed) else Pair(0.0, 0.0)
    }
}
