package org.example.collision

import org.example.TrajectoryPoint
import org.example.model.Target
import org.example.model.TrajectoryMatrix
import org.example.trajectory.TrajectoryInterpolator

/** Estado de la colisión */
enum class CollisionStatus {
    /** El proyectil impactó dentro del objetivo */
    HIT,
    /** El proyectil no impactó dentro del objetivo */
    MISS,
    /** No se pudo determinar (matriz vacía, sin puntos cerca del suelo, etc.) */
    UNKNOWN
}

/**
 * Punto de impacto resultante de una colisión con un objetivo.
 */
data class ImpactResult(
    val point: TrajectoryPoint,
    val target: Target,
    val distanceToCenter: Double,
    val impactTime: Double
) {
    /** true si impactó exactamente en el centro */
    val isExactHit: Boolean get() = distanceToCenter < 1e-10
}

/**
 * Resultado completo de la verificación de colisión.
 */
data class CollisionResult(
    val status: CollisionStatus,
    val impact: ImpactResult? = null,
    val message: String = ""
)

/**
 * Abstracción de detección de colisión entre la trayectoria y un objetivo.
 *
 * Responsabilidad Única (SRP): validar si el impacto cae dentro del objetivo.
 * Principio de Segregación de Interfaces (ISP): interfaz específica de colisión.
 * Principio de Inversión de Dependencias (DIP): depende de [TrajectoryInterpolator].
 */
interface CollisionDetector {
    /**
     * Verifica si el proyectil impacta dentro del rango horizontal de un objetivo.
     *
     * @param matrix Matriz de trayectoria
     * @param target Objetivo circular sobre el suelo
     * @return CollisionResult con el estado y detalles del impacto
     */
    fun checkCollision(matrix: TrajectoryMatrix, target: Target): CollisionResult

    /**
     * Verifica si un x de impacto cae dentro del radio del objetivo.
     *
     * @param impactX Coordenada x donde impacta el proyectil
     * @param target Objetivo circular
     * @return true si el impacto cae dentro del objetivo
     */
    fun isInsideTarget(impactX: Double, target: Target): Boolean
}

/**
 * Detector de colisión en el eje X: comprueba si el punto de impacto (y≈0)
 * cae dentro del intervalo [left, right] del objetivo.
 *
 * Responsabilidad Única (SRP): implementar la lógica de colisión en el eje X.
 */
class XAxisCollisionDetector(
    private val interpolator: TrajectoryInterpolator,
    private val tolerance: Double = DEFAULT_TOLERANCE
) : CollisionDetector {

    override fun checkCollision(matrix: TrajectoryMatrix, target: Target): CollisionResult {
        val points = matrix.points

        if (points.isEmpty()) {
            return CollisionResult(
                status = CollisionStatus.UNKNOWN,
                message = "No hay puntos en la matriz de trayectoria"
            )
        }

        // Puntos cercanos al suelo (y dentro de la tolerancia)
        val nearGroundPoints = points.filter { it.y <= tolerance && it.y >= -tolerance * 2 }

        if (nearGroundPoints.isEmpty()) {
            return CollisionResult(
                status = CollisionStatus.UNKNOWN,
                message = "No se encontraron puntos cerca del suelo"
            )
        }

        // Verificar si algún punto cae en el rango horizontal del objetivo
        for (point in nearGroundPoints) {
            if (point.x >= target.left && point.x <= target.right) {
                val exactPoint = interpolator.interpolateToGround(points, point)
                val distanceToCenter = kotlin.math.abs(exactPoint.x - target.centerX)

                val impact = ImpactResult(
                    point = exactPoint,
                    target = target,
                    distanceToCenter = distanceToCenter,
                    impactTime = exactPoint.t
                )

                return CollisionResult(
                    status = CollisionStatus.HIT,
                    impact = impact,
                    message = "Impacto en x=%.2f, t=%.4f s".format(exactPoint.x, exactPoint.t)
                )
            }
        }

        // No impactó: reportar el punto de impacto real más cercano
        val closestToTarget = nearGroundPoints.minByOrNull {
            kotlin.math.abs(it.x - target.centerX)
        }!!

        return CollisionResult(
            status = CollisionStatus.MISS,
            message = "El proyectil impactó en x=%.2f, fuera del objetivo [%.2f, %.2f]"
                .format(closestToTarget.x, target.left, target.right)
        )
    }

    override fun isInsideTarget(impactX: Double, target: Target): Boolean {
        return kotlin.math.abs(impactX - target.centerX) <= target.radius
    }

    companion object {
        /** Tolerancia por defecto para considerar un punto "cerca del suelo" (metros) */
        const val DEFAULT_TOLERANCE = 0.5
    }
}
