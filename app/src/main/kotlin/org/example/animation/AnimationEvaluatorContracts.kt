package org.example.animation

import org.example.collision.ICollisionResult
import org.example.model.ISimulationState
import org.example.model.ITrajectoryMatrixSummary
import org.example.rendering.CoordinateTransformer
import org.example.rendering.ISceneConfig

/**
 * Contrato de evaluación cinemática de impacto al finalizar el vuelo (DIP).
 */
interface IFlightCollisionEvaluator {
    fun evaluateGroundCollision(matrix: ITrajectoryMatrixSummary, state: ISimulationState): ICollisionResult
}

/**
 * Contrato completo de despacho de eventos de impacto y cierre de vuelo (ISP + DIP).
 */
interface IFlightCompletionNotifier : IFlightEventNotifier {
    fun notifyCollision(result: ICollisionResult)
    fun notifyFlightCompleted(matrix: ITrajectoryMatrixSummary)
}

/**
 * Puerto cinemático y geométrico de consulta durante la animación de vuelo (ISP + DIP).
 */
interface IFlightKinematicsPort {
    fun stateModel(): ISimulationState
    fun baseOrigin(): Pair<Double, Double>
    fun isWithinCanvasBounds(canvasX: Double, canvasY: Double): Boolean
    fun toCanvasPoint(physicsX: Double, physicsY: Double): Pair<Double, Double>
    fun generateMatrix(samplesPerSecond: Int): ITrajectoryMatrixSummary
}

/**
 * Contexto de ejecución inyectable para [AnimationEngine] (DIP + SRP).
 */
interface IAnimationRuntimeContext : IFlightKinematicsPort {
    fun evaluateGroundImpact(matrix: ITrajectoryMatrixSummary): ICollisionResult
    fun hooks(): IFlightCompletionNotifier
    fun startTicker(step: (Long) -> Unit)
    fun stopTicker()
}

/**
 * Fábricas modulares de puertos y motor de animación (ISP + DIP).
 */
interface IFlightPortFactory {
    fun createKinematics(state: ISimulationState, cfg: ISceneConfig, transformer: CoordinateTransformer): IFlightKinematicsPort
    fun createEvaluator(transformer: CoordinateTransformer): IFlightCollisionEvaluator
}

interface IAnimationEngineFactory {
    fun create(kinematics: IFlightKinematicsPort, evaluator: IFlightCollisionEvaluator, hooks: IFlightCompletionNotifier, timeScale: Double = 1.75): IAnimationEngine
    fun createWithTicker(kinematics: IFlightKinematicsPort, evaluator: IFlightCollisionEvaluator, hooks: IFlightCompletionNotifier, ticker: FrameTicker): IAnimationEngine
}
