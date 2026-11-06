package org.example

import org.example.collision.CollisionDetector
import org.example.trajectory.TrajectoryInterpolator
import org.example.trajectory.TrajectoryMatrixGenerator
import org.example.trajectory.TrajectorySearcher

/**
 * Contratos segregados para ensambladores cinemáticos, de búsqueda y colisión (ISP + DIP).
 */
interface IKinematicsBuilder {
    fun buildCalculator(): TrajectoryCalculator
    fun buildGenerator(calculator: TrajectoryCalculator = buildCalculator()): TrajectoryMatrixGenerator
}

interface ISearcherBuilder {
    fun buildSearcher(): TrajectorySearcher
}

interface ICollisionBuilder {
    fun buildInterpolator(): TrajectoryInterpolator
    fun buildCollisionDetector(interpolator: TrajectoryInterpolator = buildInterpolator()): CollisionDetector
}

interface ITrajectorySimulationFactory : IKinematicsBuilder, ISearcherBuilder, ICollisionBuilder {
    fun factoryName(): String
}

/**
 * Contrato para demos de consola y fachadas de simulación (DIP).
 */
interface ISimulationCliDemo {
    fun executeDemo()
}
