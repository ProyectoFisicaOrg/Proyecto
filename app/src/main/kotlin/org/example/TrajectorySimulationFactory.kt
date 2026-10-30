package org.example

import org.example.collision.CollisionDetector
import org.example.collision.XAxisCollisionDetector
import org.example.model.TrajectoryMatrix
import org.example.telemetry.InstantaneousStateApi
import org.example.telemetry.TelemetryFactory
import org.example.telemetry.TrajectoryInspector
import org.example.telemetry.TrajectoryNearestNeighborSearcher
import org.example.trajectory.BinaryTrajectorySearcher
import org.example.trajectory.LinearTrajectoryInterpolator
import org.example.trajectory.SamplingTrajectoryMatrixGenerator
import org.example.trajectory.TrajectoryInterpolator
import org.example.trajectory.TrajectoryMatrixGenerator
import org.example.trajectory.TrajectorySearcher

/**
 * Contrato abstracto para la fábrica ensambladora de subsistemas cinemáticos (DIP).
 */
interface ITrajectorySimulationFactory {
    fun buildCalculator(): TrajectoryCalculator
    fun buildGenerator(calculator: TrajectoryCalculator = buildCalculator()): TrajectoryMatrixGenerator
    fun buildSearcher(): TrajectorySearcher
    fun buildInterpolator(): TrajectoryInterpolator
    fun buildCollisionDetector(interpolator: TrajectoryInterpolator = buildInterpolator()): CollisionDetector
}

/**
 * Implementación inyectable por constructor de los ensambladores de trayectoria (SRP + DIP).
 */
class DefaultTrajectorySimulationFactory(
    private val calculatorProvider: () -> TrajectoryCalculator = TrajectoryCalculatorFactory::createParabolic,
    private val searcherProvider: () -> TrajectorySearcher = ::BinaryTrajectorySearcher,
    private val interpolatorProvider: () -> TrajectoryInterpolator = ::LinearTrajectoryInterpolator,
    private val defaultTolerance: Double = XAxisCollisionDetector.DEFAULT_TOLERANCE
) : ITrajectorySimulationFactory {

    override fun buildCalculator(): TrajectoryCalculator = calculatorProvider()

    override fun buildGenerator(calculator: TrajectoryCalculator): TrajectoryMatrixGenerator =
        SamplingTrajectoryMatrixGenerator(calculator)

    override fun buildSearcher(): TrajectorySearcher = searcherProvider()

    override fun buildInterpolator(): TrajectoryInterpolator = interpolatorProvider()

    override fun buildCollisionDetector(interpolator: TrajectoryInterpolator): CollisionDetector =
        XAxisCollisionDetector(interpolator, defaultTolerance)
}

/**
 * Fachada de fábrica de compatibilidad para ensamblar subsistemas de simulación (SRP + DIP).
 */
class TrajectorySimulationFactory(
    private val delegate: ITrajectorySimulationFactory = DefaultTrajectorySimulationFactory()
) {
    fun buildCollisionDetector(interpolator: TrajectoryInterpolator = delegate.buildInterpolator()): CollisionDetector =
        delegate.buildCollisionDetector(interpolator)

    companion object {
        private val defaultFactory: ITrajectorySimulationFactory = DefaultTrajectorySimulationFactory()

        fun createCalculator(): TrajectoryCalculator = defaultFactory.buildCalculator()

        fun createGenerator(calculator: TrajectoryCalculator = createCalculator()): TrajectoryMatrixGenerator =
            defaultFactory.buildGenerator(calculator)

        fun createSearcher(): TrajectorySearcher = defaultFactory.buildSearcher()

        fun createInterpolator(): TrajectoryInterpolator = defaultFactory.buildInterpolator()

        fun createCollisionDetector(interpolator: TrajectoryInterpolator = createInterpolator(), tolerance: Double = XAxisCollisionDetector.DEFAULT_TOLERANCE): CollisionDetector =
            XAxisCollisionDetector(interpolator, tolerance)

        fun generateMatrix(params: PhysicsParams, samplesPerSecond: Int = SamplingTrajectoryMatrixGenerator.DEFAULT_SAMPLES_PER_SECOND): TrajectoryMatrix =
            createGenerator().generate(params, samplesPerSecond)

        fun createInstantaneousStateApi(searcher: TrajectoryNearestNeighborSearcher = TelemetryFactory.createSearcher()): InstantaneousStateApi =
            TelemetryFactory.createApi(searcher)

        fun createTrajectoryInspector(matrix: TrajectoryMatrix): TrajectoryInspector =
            TelemetryFactory.createInspector(matrix)
    }
}
