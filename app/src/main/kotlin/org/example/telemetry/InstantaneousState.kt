package org.example.telemetry

import java.util.Locale
import kotlin.math.hypot
import org.example.IPosition
import org.example.ITrajectoryPoint
import org.example.Position

/**
 * Estado instantáneo encapsulado (telemetría) del proyectil en un punto específico (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class InstantaneousState internal constructor(
    private val stateData: Pair<List<Double>, Pair<ITrajectoryPoint, IPosition>>
) : IInstantaneousState {

    constructor(
        time: Double,
        height: Double,
        speed: Double,
        x: Double,
        point: ITrajectoryPoint,
        distanceToQuery: Double = 0.0
    ) : this(Pair(listOf(time, height, speed, x, distanceToQuery), Pair(point, Position(x, height))))

    init {
        require(stateData.first[0] >= -1e-9) { "El tiempo transcurrido no puede ser negativo: ${stateData.first[0]}" }
        require(stateData.first[2] >= 0.0) { "La velocidad escalar no puede ser negativa: ${stateData.first[2]}" }
        require(stateData.first[4] >= 0.0) { "La distancia a la consulta no puede ser negativa: ${stateData.first[4]}" }
    }

    override fun sourcePoint(): ITrajectoryPoint {
        return stateData.second.first
    }

    override fun currentPosition(): IPosition {
        return stateData.second.second
    }

    override fun scalarSpeed(): Double {
        return stateData.first[2]
    }

    override fun elapsedTime(): Double {
        return stateData.first[0]
    }

    override fun altitude(): Double {
        return stateData.first[1]
    }

    override fun horizontalCoord(): Double {
        return stateData.first[3]
    }

    override fun queryDistance(): Double {
        return stateData.first[4]
    }

    override fun format(): String {
        val m = stateData.first
        return String.format(Locale.US, "t=%.2f s, h=%.2f m, v=%.2f m/s (x=%.2f m)", m[0], m[1], m[2], m[3])
    }

    companion object {
        fun fromTrajectoryPoint(point: ITrajectoryPoint, distanceToQuery: Double = 0.0): InstantaneousState {
            val speed = hypot(point.vx, point.vy)
            return InstantaneousState(point.t, point.y, speed, point.x, point, distanceToQuery)
        }
    }
}
