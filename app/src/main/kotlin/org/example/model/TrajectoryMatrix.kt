package org.example.model

import org.example.Position
import org.example.TrajectoryPoint
import org.example.telemetry.InstantaneousState
import org.example.telemetry.InstantaneousStateApi
import org.example.telemetry.TelemetryFactory

/**
 * Contrato de métricas resumidas y puntos discretizados de una trayectoria (ISP + DIP).
 */
interface ITrajectoryMatrixSummary {
    fun trajectoryPoints(): List<TrajectoryPoint>
    fun totalTimeOfFlight(): Double
    fun horizontalRange(): Double
    fun peakHeight(): Double

    val points: List<TrajectoryPoint> get() = trajectoryPoints()
    val timeOfFlight: Double get() = totalTimeOfFlight()
    val range: Double get() = horizontalRange()
    val maxHeight: Double get() = peakHeight()
    val size: Int get() = trajectoryPoints().size
    val launchPoint: TrajectoryPoint get() = trajectoryPoints().first()
    val impactPoint: TrajectoryPoint get() = trajectoryPoints().last()
}

/**
 * Matriz de trayectoria: contenedor inmutable y encapsulado de los puntos del recorrido del proyectil (SRP + Encapsulación).
 */
class TrajectoryMatrix(
    points: List<TrajectoryPoint>,
    timeOfFlight: Double,
    range: Double,
    maxHeight: Double
) : ITrajectoryMatrixSummary {
    private val sampledPoints: List<TrajectoryPoint> = points
    private val flightDuration: Double = timeOfFlight
    private val maxHorizontalRange: Double = range
    private val peakAltitude: Double = maxHeight

    init {
        require(sampledPoints.isNotEmpty()) { "La matriz no puede estar vacía" }
        require(flightDuration >= 0) { "El tiempo de vuelo no puede ser negativo" }
        require(maxHorizontalRange >= 0) { "El alcance no puede ser negativo" }
        require(peakAltitude >= 0) { "La altura máxima no puede ser negativa" }
    }

    override fun trajectoryPoints(): List<TrajectoryPoint> = sampledPoints

    override fun totalTimeOfFlight(): Double = flightDuration

    override fun horizontalRange(): Double = maxHorizontalRange

    override fun peakHeight(): Double = peakAltitude

    fun subMatrix(fromIndex: Int, toIndex: Int): List<TrajectoryPoint> {
        val safeFrom = fromIndex.coerceIn(0, sampledPoints.size)
        val safeTo = toIndex.coerceIn(0, sampledPoints.size)
        return sampledPoints.subList(safeFrom, safeTo)
    }

    fun pointsAbove(minHeight: Double): List<TrajectoryPoint> = sampledPoints.filter { it.y >= minHeight }

    fun getInstantaneousState(x: Double, y: Double, api: InstantaneousStateApi = TelemetryFactory.createApi()): InstantaneousState =
        api.getInstantaneousState(this, x, y)

    fun getInstantaneousState(position: Position, api: InstantaneousStateApi = TelemetryFactory.createApi()): InstantaneousState =
        api.getInstantaneousState(this, position)

    fun getInstantaneousStateByX(x: Double, api: InstantaneousStateApi = TelemetryFactory.createApi()): InstantaneousState =
        api.getInstantaneousStateByX(this, x)

    override fun toString(): String = buildString {
        appendLine("=== Matriz de Trayectoria ===")
        appendLine("Puntos: ${sampledPoints.size}")
        appendLine("Tiempo de vuelo: %.4f s".format(flightDuration))
        appendLine("Alcance: %.2f m".format(maxHorizontalRange))
        appendLine("Altura máxima: %.2f m".format(peakAltitude))
        appendLine("Punto de lanzamiento: (%.2f, %.2f)".format(launchPoint.x, launchPoint.y))
        appendLine("Punto de impacto: (%.2f, %.2f)".format(impactPoint.x, impactPoint.y))
    }
}
