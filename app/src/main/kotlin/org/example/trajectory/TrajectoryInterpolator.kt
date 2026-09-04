package org.example.trajectory

import org.example.TrajectoryPoint

/**
 * Abstracción de interpolación sobre la trayectoria.
 *
 * Responsabilidad Única (SRP): estimar valores exactos entre puntos muestreados.
 * Principio de Inversión de Dependencias (DIP): el detector de colisiones depende
 * de esta abstracción, no de una implementación concreta.
 */
interface TrajectoryInterpolator {
    /**
     * Interpola linealmente entre el punto y su anterior para hallar y=0 exacto.
     *
     * @param points Puntos de la trayectoria
     * @param pointNearGround Punto de la trayectoria cercano al suelo (y <= 0)
     * @return Punto interpolado en y=0, o el mismo punto si no hay cruce
     */
    fun interpolateToGround(points: List<TrajectoryPoint>, pointNearGround: TrajectoryPoint): TrajectoryPoint
}

/**
 * Interpolación lineal entre dos puntos consecutivos.
 *
 * Responsabilidad Única (SRP): implementar la estrategia de interpolación lineal.
 */
class LinearTrajectoryInterpolator : TrajectoryInterpolator {

    override fun interpolateToGround(
        points: List<TrajectoryPoint>,
        pointNearGround: TrajectoryPoint
    ): TrajectoryPoint {
        val idx = points.indexOf(pointNearGround)

        if (idx <= 0) return pointNearGround

        val prev = points[idx - 1]
        val curr = pointNearGround

        if (prev.y > 0 && curr.y <= 0) {
            val ratio = prev.y / (prev.y - curr.y)
            val exactT = prev.t + ratio * (curr.t - prev.t)
            val exactX = prev.x + ratio * (curr.x - prev.x)
            val exactVx = prev.vx // vx constante en parábola sin resistencia
            val exactVy = prev.vy + ratio * (curr.vy - prev.vy)

            return TrajectoryPoint(exactX, 0.0, exactVx, exactVy, exactT)
        }

        return pointNearGround
    }
}
