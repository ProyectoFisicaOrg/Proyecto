package org.example.collision

import kotlin.math.abs
import org.example.TrajectoryPoint
import org.example.model.Target
import org.example.model.TrajectoryMatrix
import org.example.trajectory.TrajectoryInterpolator

/** Estado de la colisión */
enum class CollisionStatus {
    HIT,
    MISS,
    UNKNOWN
}

/**
 * Punto de impacto resultante de una colisión con un objetivo (SRP + Encapsulación).
 */
data class ImpactResult(
    val point: TrajectoryPoint,
    val target: Target,
    private val distanceToCenter: Double,
    private val impactTime: Double
) {
    fun isExactHit(): Boolean = distanceToCenter < 1e-10

    fun centerDistance(): Double = distanceToCenter

    fun timeOfImpact(): Double = impactTime

    fun isWithinTarget(): Boolean = distanceToCenter <= target.radius

    fun deviationX(): Double = point.x - target.centerX
}

/**
 * Resultado completo de la verificación de colisión (SRP + Encapsulación).
 */
data class CollisionResult(
    val status: CollisionStatus,
    private val impact: ImpactResult? = null,
    val message: String = ""
) {
    fun impactResult(): ImpactResult? = impact

    fun isHit(): Boolean = status == CollisionStatus.HIT

    fun isMiss(): Boolean = status == CollisionStatus.MISS

    fun hasImpact(): Boolean = impact != null
}

/**
 * Abstracción de detección de colisión entre la trayectoria y un objetivo (SRP + ISP + DIP).
 */
interface CollisionDetector {
    fun checkCollision(matrix: TrajectoryMatrix, target: Target): CollisionResult
    fun isInsideTarget(impactX: Double, target: Target): Boolean
}

/**
 * Detector de colisión en el eje X (SRP).
 */
class XAxisCollisionDetector(
    private val interpolator: TrajectoryInterpolator,
    private val tolerance: Double = DEFAULT_TOLERANCE
) : CollisionDetector {

    override fun checkCollision(matrix: TrajectoryMatrix, target: Target): CollisionResult {
        val points = matrix.points
        if (points.isEmpty()) {
            return CollisionResult(CollisionStatus.UNKNOWN, message = "No hay puntos en la matriz de trayectoria")
        }

        val nearGround = findNearGroundPoints(points)
        if (nearGround.isEmpty()) {
            return CollisionResult(CollisionStatus.UNKNOWN, message = "No se encontraron puntos cerca del suelo")
        }

        return evaluateHitOrMiss(points, nearGround, target)
    }

    private fun findNearGroundPoints(points: List<TrajectoryPoint>): List<TrajectoryPoint> =
        points.filter { it.y <= tolerance && it.y >= -tolerance * 2.0 }

    private fun evaluateHitOrMiss(allPoints: List<TrajectoryPoint>, nearGround: List<TrajectoryPoint>, target: Target): CollisionResult {
        val hitCandidate = nearGround.firstOrNull { target.contains(it.x) }
        if (hitCandidate != null) {
            return buildHitResult(allPoints, hitCandidate, target)
        }
        return buildMissResult(nearGround, target)
    }

    private fun buildHitResult(allPoints: List<TrajectoryPoint>, point: TrajectoryPoint, target: Target): CollisionResult {
        val exactPoint = interpolator.interpolateToGround(allPoints, point)
        val dist = abs(exactPoint.x - target.centerX)
        val impact = ImpactResult(exactPoint, target, dist, exactPoint.t)
        return CollisionResult(
            status = CollisionStatus.HIT,
            impact = impact,
            message = "Impacto en x=%.2f, t=%.4f s".format(exactPoint.x, exactPoint.t)
        )
    }

    private fun buildMissResult(nearGround: List<TrajectoryPoint>, target: Target): CollisionResult {
        val closest = nearGround.minByOrNull { abs(it.x - target.centerX) }!!
        return CollisionResult(
            status = CollisionStatus.MISS,
            message = "El proyectil impactó en x=%.2f, fuera del objetivo [%.2f, %.2f]"
                .format(closest.x, target.left(), target.right())
        )
    }

    override fun isInsideTarget(impactX: Double, target: Target): Boolean =
        abs(impactX - target.centerX) <= target.radius

    companion object {
        const val DEFAULT_TOLERANCE = 0.5
    }
}
