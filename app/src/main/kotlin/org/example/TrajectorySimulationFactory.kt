package org.example

import org.example.collision.CollisionDetector
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
    private val calculatorProvider: () -> TrajectoryCalculator = TrajectoryCalculatorFactory::createParabolic
) : IKinematicsBuilder {

    override fun buildCalculator(): TrajectoryCalculator {
        return calculatorProvider()
    }

    override fun buildGenerator(calculator: TrajectoryCalculator): TrajectoryMatrixGenerator {
        return SamplingTrajectoryMatrixGenerator(calculator)
    }
}

/**
 * Ensamblador dedicado a buscadores binarios de trayectoria (SRP + DIP).
 */
class DefaultSearcherBuilder : ISearcherBuilder {

    override fun buildSearcher(): TrajectorySearcher {
        return BinaryTrajectorySearcher()
    }
}

/**
 * Ensamblador dedicado a interpolación y detección de colisión en suelo (SRP + DIP).
 */
class DefaultCollisionBuilder : ICollisionBuilder {

    override fun buildInterpolator(): TrajectoryInterpolator {
        return LinearTrajectoryInterpolator()
    }

    override fun buildCollisionDetector(interpolator: TrajectoryInterpolator): CollisionDetector {
        return XAxisCollisionDetector(interpolator, 0.5)
    }
}

/**
 * Implementación modular por composición de los ensambladores de trayectoria (SRP + DIP).
 */
class DefaultTrajectorySimulationFactory(
    kinematics: IKinematicsBuilder = DefaultKinematicsBuilder(),
    searcher: ISearcherBuilder = DefaultSearcherBuilder(),
    collision: ICollisionBuilder = DefaultCollisionBuilder()
) : ITrajectorySimulationFactory, IKinematicsBuilder by kinematics, ISearcherBuilder by searcher, ICollisionBuilder by collision {

    override fun factoryName(): String {
        return "DefaultTrajectorySimulationFactory"
    }
}

/**
 * Fachada ligera para ensamblar componentes de telemetría y simulación (SRP + DIP).
 */
class TrajectorySimulationFactory(
    private val delegate: ITrajectorySimulationFactory = DefaultTrajectorySimulationFactory()
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
