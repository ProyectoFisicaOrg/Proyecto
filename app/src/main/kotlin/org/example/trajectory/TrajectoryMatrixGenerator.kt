package org.example.trajectory

import org.example.IPhysicsParams
import org.example.ITrajectoryPoint
import org.example.TrajectoryCalculator
import org.example.model.TrajectoryMatrix

/**
 * Generador de matriz por muestreo temporal de la curva parabólica (SRP + DIP).
 */
class SamplingTrajectoryMatrixGenerator(
    private val calculator: TrajectoryCalculator,
    private val matrixFactory: (List<ITrajectoryPoint>, Double, Double, Double) -> TrajectoryMatrix = ::TrajectoryMatrix
) : TrajectoryMatrixGenerator {

    constructor(calculator: TrajectoryCalculator) : this(calculator, ::TrajectoryMatrix)

    companion object {
        const val DEFAULT_SAMPLES_PER_SECOND = 120
    }

    override fun generate(params: IPhysicsParams, samplesPerSecond: Int): TrajectoryMatrix {
        require(samplesPerSecond > 0) { "Las muestras por segundo deben ser positivas" }
        val timeOfFlight = requireValidFlightTime(params)
        val range = calculator.calculateRange(params) ?: 0.0
        val maxHeight = calculator.calculateMaxHeight(params)
        val points = sampleTrajectoryPoints(params, timeOfFlight, samplesPerSecond)
        return matrixFactory(points, timeOfFlight, range, maxHeight)
    }

    private fun requireValidFlightTime(params: IPhysicsParams): Double {
        return calculator.calculateTimeOfFlight(params)
            ?: throw IllegalArgumentException("Los parámetros no producen vuelo válido")
    }

    private fun sampleTrajectoryPoints(params: IPhysicsParams, timeOfFlight: Double, samplesPerSecond: Int): List<ITrajectoryPoint> {
        val dt = 1.0 / samplesPerSecond
        val points = ArrayList<ITrajectoryPoint>((timeOfFlight * samplesPerSecond).toInt() + 2)
        var t = 0.0
        while (t <= timeOfFlight) {
            points.add(calculator.calculateState(params, t))
            t += dt
        }
        val impactPoint = calculator.calculateState(params, timeOfFlight)
        if (points.isEmpty() || points.last().timeSeconds() < timeOfFlight) {
            points.add(impactPoint)
        }
        return points
    }
}
