package org.example.scrubbing

import org.example.model.TrajectoryMatrix
import org.example.rendering.CoordinateTransformer
import org.example.telemetry.InstantaneousStateApi
import org.example.telemetry.TelemetryFactory

/**
 * Adaptador que implementa [ITrajectoryInspector] conectando el motor de física y telemetría de Dev 1
 * con el módulo de interfaz y scrubbing de Dev 2 (Adapter Pattern + DIP + SRP).
 *
 * Responsabilidad Única (SRP):
 *  - Su única función es traducir las consultas de [ITrajectoryInspector] hacia la API de telemetría
 *    de Dev 1 ([InstantaneousStateApi]) y aplicar la transformación de coordenadas mediante [CoordinateTransformer].
 *
 * Inversión de Dependencias (DIP):
 *  - Depende de abstracciones ([CoordinateTransformer], [InstantaneousStateApi]).
 *
 * @param matrix Matriz de trayectoria calculada tras el lanzamiento.
 * @param transformer Conversor de coordenadas entre píxeles del canvas y metros de física.
 * @param telemetryApi API de estado instantáneo de Dev 1 (por defecto [TelemetryFactory.createInstantaneousStateApi]).
 */
class PhysicsTrajectoryInspectorAdapter(
    val matrix: TrajectoryMatrix,
    private val transformer: CoordinateTransformer,
    private val telemetryApi: InstantaneousStateApi = TelemetryFactory.createApi()
) : ITrajectoryInspector {

    override fun findNearestPoint(physicsX: Double, physicsY: Double): TrajectoryPoint {
        val state = telemetryApi.getInstantaneousState(matrix, physicsX, physicsY)
        return TrajectoryPoint(
            x = state.x,
            y = state.height,
            time = state.time,
            height = state.height,
            speed = state.speed
        )
    }

    override fun findNearestPointFromCursor(cursorCanvasX: Double, cursorCanvasY: Double): TrajectoryPoint {
        val physicsX = transformer.toPhysicsX(cursorCanvasX)
        val physicsY = transformer.toPhysicsY(cursorCanvasY)
        return findNearestPoint(physicsX, physicsY)
    }

    override fun findNearestPointByX(physicsX: Double): TrajectoryPoint {
        val state = telemetryApi.getInstantaneousStateByX(matrix, physicsX)
        return TrajectoryPoint(
            x = state.x,
            y = state.height,
            time = state.time,
            height = state.height,
            speed = state.speed
        )
    }

    override fun getFlightSummary(): FlightSummary {
        return FlightSummary(
            totalFlightTime = matrix.timeOfFlight,
            totalDistance = matrix.range
        )
    }
}
