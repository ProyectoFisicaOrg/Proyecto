package org.example.telemetry

import kotlin.math.abs
import kotlin.math.hypot
import org.example.ITrajectoryPoint
import org.example.model.ITrajectoryMatrixSummary

/**
 * Implementación estándar de [InstantaneousStateApi] desacoplada por constructor (SRP + DIP).
 */
class DefaultInstantaneousStateApi(
    searcher: TrajectoryNearestNeighborSearcher,
    inspectorFactory: (ITrajectoryMatrixSummary, InstantaneousStateApi) -> TrajectoryInspector = ::BoundTrajectoryInspector
) : InstantaneousStateApi {
    private val deps: Pair<TrajectoryNearestNeighborSearcher, (ITrajectoryMatrixSummary, InstantaneousStateApi) -> TrajectoryInspector> =
        Pair(searcher, inspectorFactory)

    override fun getInstantaneousState(matrix: ITrajectoryMatrixSummary, x: Double, y: Double): IInstantaneousState {
        return getInstantaneousState(matrix.points, x, y)
    }

    override fun getInstantaneousStateByX(matrix: ITrajectoryMatrixSummary, x: Double): IInstantaneousState {
        return getInstantaneousStateByX(matrix.points, x)
    }

    override fun getInstantaneousState(points: List<ITrajectoryPoint>, x: Double, y: Double): IInstantaneousState {
        require(points.isNotEmpty()) { "La lista de puntos de trayectoria no puede estar vacía" }
        val nearestPoint = deps.first.findNearest(points, x, y)
        val distance = hypot(nearestPoint.x - x, nearestPoint.y - y)
        return InstantaneousState.fromTrajectoryPoint(nearestPoint, distance)
    }

    override fun getInstantaneousStateByX(points: List<ITrajectoryPoint>, x: Double): IInstantaneousState {
        require(points.isNotEmpty()) { "La lista de puntos de trayectoria no puede estar vacía" }
        val nearestPoint = deps.first.findNearestByX(points, x)
        val distance = abs(nearestPoint.x - x)
        return InstantaneousState.fromTrajectoryPoint(nearestPoint, distance)
    }

    override fun createInspector(matrix: ITrajectoryMatrixSummary): TrajectoryInspector {
        return deps.second(matrix, this)
    }
}

/**
 * Implementación encapsulada de [TrajectoryInspector] vinculada a una matriz (SRP + Encapsulación).
 */
class BoundTrajectoryInspector(
    private val trajectoryMatrix: ITrajectoryMatrixSummary,
    private val api: InstantaneousStateApi
) : TrajectoryInspector {

    override fun boundMatrix(): ITrajectoryMatrixSummary {
        return trajectoryMatrix
    }

    override fun inspect(x: Double, y: Double): IInstantaneousState {
        return api.getInstantaneousState(trajectoryMatrix, x, y)
    }

    override fun inspectByX(x: Double): IInstantaneousState {
        return api.getInstantaneousStateByX(trajectoryMatrix, x)
    }
}
