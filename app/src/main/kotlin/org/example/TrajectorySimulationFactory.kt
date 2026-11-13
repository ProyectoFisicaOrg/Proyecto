package org.example

import org.example.collision.CollisionDetector
import org.example.collision.DefaultCollisionResultFactory
import org.example.collision.ICollisionResultFactory
import org.example.collision.XAxisCollisionDetector
import org.example.model.ITrajectoryMatrixSummary
import org.example.telemetry.InstantaneousStateApi
import org.example.telemetry.TelemetryFactory
import org.example.telemetry.TrajectoryInspector
import org.example.trajectory.BinaryTrajectorySearcher
import org.example.trajectory.LinearTrajectoryInterpolator
import org.example.trajectory.SamplingTrajectoryMatrixGenerator
import org.example.trajectory.TrajectoryInterpolator
import org.example.trajectory.TrajectoryMatrixGenerator
import org.example.trajectory.TrajectorySearcher

/**
 * Ensamblador dedicado a calculadoras y generadores cinemáticos (SRP + DIP).
 */
class DefaultKinematicsBuilder(
    calculatorProvider: () -> TrajectoryCalculator = TrajectoryCalculatorFactory::createParabolic,
    generatorFactory: (TrajectoryCalculator) -> TrajectoryMatrixGenerator = ::SamplingTrajectoryMatrixGenerator
) : IKinematicsBuilder {
    private val deps: Pair<() -> TrajectoryCalculator, (TrajectoryCalculator) -> TrajectoryMatrixGenerator> =
        Pair(calculatorProvider, generatorFactory)

    override fun buildCalculator(): TrajectoryCalculator {
        return deps.first()
    }

    override fun buildGenerator(calculator: TrajectoryCalculator): TrajectoryMatrixGenerator {
        return deps.second(calculator)
    }
}

/**
 * Ensamblador dedicado a buscadores binarios de trayectoria (SRP + DIP).
 */
class DefaultSearcherBuilder(
    private val searcherProvider: () -> TrajectorySearcher = ::BinaryTrajectorySearcher
) : ISearcherBuilder {

    override fun buildSearcher(): TrajectorySearcher {
        return searcherProvider()
    }
}

/**
 * Ensamblador dedicado a interpolación y detección de colisión en suelo (SRP + DIP).
 */
class DefaultCollisionBuilder(
    interpolatorProvider: () -> TrajectoryInterpolator = ::LinearTrajectoryInterpolator,
    detectorFactory: (TrajectoryInterpolator, Double, ICollisionResultFactory) -> CollisionDetector = ::XAxisCollisionDetector,
    resultFactoryProvider: () -> ICollisionResultFactory = ::DefaultCollisionResultFactory
) : ICollisionBuilder {
    private val deps: Triple<() -> TrajectoryInterpolator, (TrajectoryInterpolator, Double, ICollisionResultFactory) -> CollisionDetector, () -> ICollisionResultFactory> =
        Triple(interpolatorProvider, detectorFactory, resultFactoryProvider)

    override fun buildInterpolator(): TrajectoryInterpolator {
        return deps.first()
    }

    override fun buildCollisionDetector(interpolator: TrajectoryInterpolator): CollisionDetector {
        return deps.second(interpolator, 0.5, deps.third())
    }
}

/**
 * Implementación modular por composición de los ensambladores de trayectoria (SRP + DIP).
 */
class DefaultTrajectorySimulationFactory(
    kinematics: IKinematicsBuilder,
    searcher: ISearcherBuilder,
    collision: ICollisionBuilder
) : ITrajectorySimulationFactory, IKinematicsBuilder by kinematics, ISearcherBuilder by searcher, ICollisionBuilder by collision {

    constructor() : this(SimulationFactoryWiring, SimulationFactoryWiring, SimulationFactoryWiring)

    override fun factoryName(): String {
        return "DefaultTrajectorySimulationFactory"
    }
}

/**
 * Raíz de composición predeterminada para los ensambladores de trayectoria (DIP).
 */
object SimulationFactoryWiring : ITrajectorySimulationFactory by DefaultTrajectorySimulationFactory(
    DefaultKinematicsBuilder(),
    DefaultSearcherBuilder(),
    DefaultCollisionBuilder()
) {
    override fun factoryName(): String {
        return "DefaultTrajectorySimulationFactory"
    }
}

/**
 * Fachada ligera para ensamblar componentes de telemetría y simulación (SRP + DIP).
 */
class TrajectorySimulationFactory(
    private val delegate: ITrajectorySimulationFactory = SimulationFactoryWiring
) : ITrajectorySimulationFactory by delegate {

    fun activeFactory(): ITrajectorySimulationFactory {
        return delegate
    }

    companion object {
        fun createInstantaneousStateApi(): InstantaneousStateApi {
            return TelemetryFactory.createApi()
        }

        fun createTrajectoryInspector(matrix: ITrajectoryMatrixSummary): TrajectoryInspector {
            return TelemetryFactory.createInspector(matrix)
        }
    }
}
