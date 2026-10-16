package org.example.trajectory

import org.example.PhysicsParams
import org.example.TrajectoryCalculator
import org.example.TrajectoryPoint
import org.example.model.TrajectoryMatrix

/**
 * Abstracción de generación de la matriz de trayectoria.
 *
 * Responsabilidad Única (SRP): construir una [TrajectoryMatrix] a partir de
 * parámetros físicos.
 * Principio de Inversión de Dependencias (DIP): depende de [TrajectoryCalculator],
 * no de una implementación concreta.
 */
interface TrajectoryMatrixGenerator {
    /**
     * Genera una matriz muestreando la curva a intervalos regulares.
     *
     * @param params Parámetros de la simulación
     * @param samplesPerSecond Puntos por segundo de vuelo (mayor = más preciso)
     * @return TrajectoryMatrix con todos los puntos mapeados
     * @throws IllegalArgumentException Si los parámetros no producen vuelo válido
     *         o samplesPerSecond no es positivo
     */
    fun generate(params: PhysicsParams, samplesPerSecond: Int): TrajectoryMatrix
}

/**
 * Generador de matriz por muestreo temporal de la curva parabólica.
 *
 * Responsabilidad Única (SRP): orquesta el muestreo usando un [TrajectoryCalculator].
 */
class SamplingTrajectoryMatrixGenerator(
    private val calculator: TrajectoryCalculator
) : TrajectoryMatrixGenerator {

    companion object {
        /** Muestreo por defecto: 120 puntos por segundo de vuelo */
        const val DEFAULT_SAMPLES_PER_SECOND = 120
    }

    override fun generate(params: PhysicsParams, samplesPerSecond: Int): TrajectoryMatrix {
        require(samplesPerSecond > 0) { "Las muestras por segundo deben ser positivas" }

        val timeOfFlight = calculator.calculateTimeOfFlight(params)
            ?: throw IllegalArgumentException(
                "Los parámetros no producen vuelo válido (ángulo=${params.angleDeg}°, v0=${params.initialVelocity} m/s)"
            )

        val range = calculator.calculateRange(params) ?: 0.0
        val maxHeight = calculator.calculateMaxHeight(params)

        val dt = 1.0 / samplesPerSecond
        val totalSamples = (timeOfFlight * samplesPerSecond).toInt() + 1

        val points = ArrayList<TrajectoryPoint>(totalSamples)
        var t = 0.0

        while (t <= timeOfFlight) {
            points.add(calculator.calculateState(params, t))
            t += dt
        }

        // Asegurar que el último punto sea exactamente en el impacto
        val lastPoint = calculator.calculateState(params, timeOfFlight)
        if (points.isEmpty() || points.last().t < timeOfFlight) {
            points.add(lastPoint)
        }

        return TrajectoryMatrix(points, timeOfFlight, range, maxHeight)
    }
}
