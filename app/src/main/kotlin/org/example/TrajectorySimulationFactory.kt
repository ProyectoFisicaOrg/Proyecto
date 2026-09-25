package org.example

import org.example.collision.CollisionDetector
import org.example.collision.XAxisCollisionDetector
import org.example.model.TrajectoryMatrix
import org.example.trajectory.BinaryTrajectorySearcher
import org.example.trajectory.LinearTrajectoryInterpolator
import org.example.trajectory.SamplingTrajectoryMatrixGenerator
import org.example.trajectory.TrajectoryInterpolator
import org.example.trajectory.TrajectoryMatrixGenerator
import org.example.trajectory.TrajectorySearcher

// PhysicsParams y TrajectoryCalculator se toman de org.example (Physics.kt)

/**
 * Fábrica que ensambla las dependencias de la simulación de trayectoria.
 *
 * Principio de Inversión de Dependencias (DIP): centraliza la construcción de
 * objetos, desacoplándola de su uso. El cliente recibe abstracciones sin conocer
 * las implementaciones concretas.
 * Principio de Responsabilidad Única (SRP): único punto de ensamblaje del módulo.
 */
class TrajectorySimulationFactory {

    companion object {
        /** Crea la calculadora de cinemática (reutiliza la fábrica de Physics.kt). */
        fun createCalculator(): TrajectoryCalculator = TrajectoryCalculatorFactory.createParabolic()

        /** Crea el generador de matriz de trayectoria. */
        fun createGenerator(
            calculator: TrajectoryCalculator = createCalculator()
        ): TrajectoryMatrixGenerator = SamplingTrajectoryMatrixGenerator(calculator)

        /** Crea el buscador de puntos sobre la trayectoria. */
        fun createSearcher(): TrajectorySearcher = BinaryTrajectorySearcher()

        /** Crea el interpolador de trayectoria. */
        fun createInterpolator(): TrajectoryInterpolator = LinearTrajectoryInterpolator()

        /** Crea el detector de colisiones. */
        fun createCollisionDetector(
            interpolator: TrajectoryInterpolator = createInterpolator(),
            tolerance: Double = XAxisCollisionDetector.DEFAULT_TOLERANCE
        ): CollisionDetector = XAxisCollisionDetector(interpolator, tolerance)

        /**
         * Flujo típico de la Issue 2.1: genera la matriz a partir de parámetros
         * y la devuelve lista para ser consumida por el detector y el renderizado.
         */
        fun generateMatrix(
            params: PhysicsParams,
            samplesPerSecond: Int = SamplingTrajectoryMatrixGenerator.DEFAULT_SAMPLES_PER_SECOND
        ): TrajectoryMatrix = createGenerator().generate(params, samplesPerSecond)
    }
}
