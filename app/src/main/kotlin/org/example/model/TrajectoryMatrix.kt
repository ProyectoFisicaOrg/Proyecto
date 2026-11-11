package org.example.model

import org.example.IPosition
import org.example.ITrajectoryPoint
import org.example.telemetry.DefaultTelemetryProvider
import org.example.telemetry.IInstantaneousState
import org.example.telemetry.InstantaneousStateApi

/**
 * Matriz de trayectoria: contenedor inmutable, cohesivo y encapsulado del recorrido del proyectil (SRP + Encapsulación).
 */
class TrajectoryMatrix(
    points: List<ITrajectoryPoint>,
    timeOfFlight: Double,
    range: Double,
    maxHeight: Double
) : ITrajectoryMatrixSummary {
    private val data: Pair<List<ITrajectoryPoint>, List<Double>> = Pair(points, listOf(timeOfFlight, range, maxHeight))

    init {
        require(data.first.isNotEmpty()) { "La matriz no puede estar vacía" }
        require(data.second[0] >= 0.0) { "El tiempo de vuelo no puede ser negativo" }
        require(data.second[1] >= 0.0) { "El alcance no puede ser negativo" }
        require(data.second[2] >= 0.0) { "La altura máxima no puede ser negativa" }
    }

    override fun trajectoryPoints(): List<ITrajectoryPoint> {
        return data.first
    }

    override fun totalTimeOfFlight(): Double {
        return data.second[0]
    }

    override fun horizontalRange(): Double {
        return data.second[1]
    }

    override fun peakHeight(): Double {
        return data.second[2]
    }

    override fun subMatrix(fromIndex: Int, toIndex: Int): List<ITrajectoryPoint> {
        val pts = data.first
        return pts.subList(fromIndex.coerceIn(0, pts.size), toIndex.coerceIn(0, pts.size))
    }

    override fun pointsAbove(minHeight: Double): List<ITrajectoryPoint> {
        return data.first.filter { it.y >= minHeight }
    }

    override fun interpolateAt(elapsed: Double): Pair<Double, Double> {
        val pts = data.first
        if (pts.size <= 1) return Pair(pts[0].x, pts[0].y)
        val idx = findSegmentIndex(elapsed)
        val p1 = pts[idx]
        val p2 = pts[idx + 1]
        val dt = p2.t - p1.t
        val ratio = if (dt > 0.0) ((elapsed - p1.t) / dt).coerceIn(0.0, 1.0) else 0.0
        return Pair(p1.x + ratio * (p2.x - p1.x), p1.y + ratio * (p2.y - p1.y))
    }

    private fun findSegmentIndex(elapsed: Double): Int {
        val pts = data.first
        var low = 0
        var high = pts.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (pts[mid].t <= elapsed) low = mid + 1 else high = mid - 1
        }
        return (low - 1).coerceIn(0, pts.size - 2)
    }

    fun getInstantaneousState(x: Double, y: Double, api: InstantaneousStateApi = DefaultTelemetryProvider().buildApi()): IInstantaneousState {
        return if (data.first.isNotEmpty()) api.getInstantaneousState(this, x, y) else api.getInstantaneousState(this, x, y)
    }

    fun getInstantaneousState(position: IPosition, api: InstantaneousStateApi = DefaultTelemetryProvider().buildApi()): IInstantaneousState {
        return getInstantaneousState(position.x, position.y, api)
    }

    fun getInstantaneousStateByX(x: Double, api: InstantaneousStateApi = DefaultTelemetryProvider().buildApi()): IInstantaneousState {
        return if (data.first.isNotEmpty()) api.getInstantaneousStateByX(this, x) else api.getInstantaneousStateByX(this, x)
    }

    override fun toString(): String {
        return buildString {
            appendLine("=== Matriz de Trayectoria ===")
            appendLine("Puntos: ${data.first.size}")
            appendLine("Tiempo de vuelo: %.4f s".format(data.second[0]))
            appendLine("Alcance: %.2f m".format(data.second[1]))
            appendLine("Altura máxima: %.2f m".format(data.second[2]))
            appendLine("Punto de lanzamiento: (%.2f, %.2f)".format(launchPoint.x, launchPoint.y))
            appendLine("Punto de impacto: (%.2f, %.2f)".format(impactPoint.x, impactPoint.y))
        }
    }
}
