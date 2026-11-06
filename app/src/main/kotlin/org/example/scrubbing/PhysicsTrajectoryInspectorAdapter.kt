package org.example.scrubbing

import org.example.model.ITrajectoryMatrixSummary
import org.example.rendering.CoordinateTransformer
import org.example.telemetry.DefaultTelemetryProvider
import org.example.telemetry.InstantaneousStateApi

/**
 * Adaptador que implementa [ITrajectoryInspector] desacoplado mediante interfaces (Adapter + DIP + SRP).
 */
class PhysicsTrajectoryInspectorAdapter(
    private val matrixRef: ITrajectoryMatrixSummary,
    private val transformer: CoordinateTransformer,
    private val telemetryApi: InstantaneousStateApi = DefaultTelemetryProvider().buildApi(),
    private val pointFactory: (Double, Double, Double, Double, Double) -> ISpanishKinematicView = ::TrajectoryPoint,
    private val summaryFactory: (Double, Double) -> ISpanishFlightSummaryView = ::FlightSummary
) : ITrajectoryInspector {
    private val sessionContext: Pair<ITrajectoryMatrixSummary, InstantaneousStateApi> = Pair(matrixRef, telemetryApi)

    fun boundMatrix(): ITrajectoryMatrixSummary {
        return sessionContext.first
    }

    override fun findNearestPoint(physicsX: Double, physicsY: Double): ISpanishKinematicView {
        val state = sessionContext.second.getInstantaneousState(sessionContext.first, physicsX, physicsY)
        return pointFactory(state.x, state.height, state.time, state.height, state.speed)
    }

    override fun findNearestPointFromCursor(cursorCanvasX: Double, cursorCanvasY: Double): ISpanishKinematicView {
        val physicsX = transformer.toPhysicsX(cursorCanvasX)
        val physicsY = transformer.toPhysicsY(cursorCanvasY)
        return findNearestPoint(physicsX, physicsY)
    }

    override fun findNearestPointByX(physicsX: Double): ISpanishKinematicView {
        val state = sessionContext.second.getInstantaneousStateByX(sessionContext.first, physicsX)
        return pointFactory(state.x, state.height, state.time, state.height, state.speed)
    }

    override fun getFlightSummary(): ISpanishFlightSummaryView {
        return summaryFactory(sessionContext.first.timeOfFlight, sessionContext.first.range)
    }
}
