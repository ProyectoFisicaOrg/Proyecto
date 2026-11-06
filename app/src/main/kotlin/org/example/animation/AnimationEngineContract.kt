package org.example.animation

import org.example.model.ITrajectoryMatrixSummary

/**
 * Contrato abstracto para el motor de animación de vuelo (DIP).
 */
interface IAnimationEngine {
    fun currentTimeScale(): Double
    fun updateTimeScale(value: Double)
    fun activeTrajectoryMatrix(): ITrajectoryMatrixSummary?
    fun getSamplesPerSecond(): Int
    fun start(matrix: ITrajectoryMatrixSummary? = null)
    fun stop()
    fun reset()
    fun isOutOfBounds(canvasX: Double, canvasY: Double): Boolean
    fun tick(now: Long)
    fun interpolatePosition(matrix: ITrajectoryMatrixSummary, elapsed: Double): Pair<Double, Double>

    var timeScale: Double
        get() = currentTimeScale()
        set(value) = updateTimeScale(value)

    val currentMatrix: ITrajectoryMatrixSummary? get() = activeTrajectoryMatrix()
}
