package org.example.collision

import kotlin.math.abs
import org.example.ITrajectoryPoint
import org.example.model.ITrajectoryMatrixSummary
import org.example.trajectory.TrajectoryInterpolator

/**
 * Estado polimórfico de la colisión con formato y color propios (OCP + Encapsulación).
 */
enum class CollisionStatus(
    private val bannerHeader: String,
    private val hexColor: String
) {
    HIT("🎯 ¡IMPACTO EN EL BLANCO!\n", "#4ade80"),
    MISS("❌ FUERA DEL OBJETIVO\n", "#f87171"),
    UNKNOWN("⚠️ ", "#fb923c");

    fun formatBanner(detail: String): String {
        return "$bannerHeader$detail"
    }

    fun colorHex(): String {
        return hexColor
    }
}

/**
 * Punto de impacto resultante de una colisión con un objetivo (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class ImpactResult internal constructor(
    private val content: Triple<ITrajectoryPoint, ICollisionTarget, Pair<Double, Double>>
) : IImpactResult {

    constructor(
        point: ITrajectoryPoint,
        target: ICollisionTarget,
        distanceToCenter: Double,
        impactTime: Double
    ) : this(Triple(point, target, Pair(distanceToCenter, impactTime)))

    override fun impactPoint(): ITrajectoryPoint {
        return content.first
    }

    override fun hitTarget(): ICollisionTarget {
        return content.second
    }

    override fun centerDistance(): Double {
        return content.third.first
    }

    override fun timeOfImpact(): Double {
        return content.third.second
    }

    fun isExactHit(): Boolean {
        return content.third.first < 1e-10
    }

    fun isWithinTarget(): Boolean {
        val rad = content.second.radiusMeters()
        return content.third.first <= rad && abs(content.first.posX() - content.second.centerPosition()) <= rad + 1.0
    }

    fun deviationX(): Double {
        return content.first.posX() - content.second.centerPosition()
    }
}

/**
 * Resultado completo y encapsulado de la verificación de colisión (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class CollisionResult internal constructor(
    private val content: Triple<CollisionStatus, IImpactResult?, String>
) : ICollisionResult {

    constructor(
        status: CollisionStatus,
        impact: IImpactResult? = null,
        message: String = ""
    ) : this(Triple(status, impact, message))

    override fun collisionStatus(): CollisionStatus {
        return content.first
    }

    override fun impactResult(): IImpactResult? {
        return content.second
    }

    override fun statusMessage(): String {
        return content.third
    }

    override fun isHit(): Boolean {
        return content.first == CollisionStatus.HIT
    }

    fun isMiss(): Boolean {
        return content.first == CollisionStatus.MISS
    }

    override fun formatBannerText(): String {
        return content.first.formatBanner(content.third)
    }

    override fun bannerColorHex(): String {
        return content.first.colorHex()
    }
}

/**
 * Fábrica estándar de resultados de colisión para inyección de dependencias (SRP + DIP).
 */
class DefaultCollisionResultFactory : ICollisionResultFactory {
    override fun unknown(message: String): ICollisionResult {
        return CollisionResult(CollisionStatus.UNKNOWN, null, message)
    }

    override fun hit(point: ITrajectoryPoint, target: ICollisionTarget, dist: Double): ICollisionResult {
        val impact = ImpactResult(point, target, dist, point.timeSeconds())
        val msg = "Impacto en x=%.2f, t=%.4f s".format(point.posX(), point.timeSeconds())
        return CollisionResult(CollisionStatus.HIT, impact, msg)
    }

    override fun miss(closestX: Double, target: ICollisionTarget): ICollisionResult {
        val msg = "El proyectil impactó en x=%.2f, fuera del objetivo [%.2f, %.2f]"
            .format(closestX, target.left(), target.right())
        return CollisionResult(CollisionStatus.MISS, null, msg)
    }
}

/**
 * Detector de colisión en el eje X desacoplado por constructor e interfaces (SRP + DIP).
 */
class XAxisCollisionDetector(
    interpolator: TrajectoryInterpolator,
    tolerance: Double,
    resultFactory: ICollisionResultFactory
) : CollisionDetector {
    private val cfg: Triple<TrajectoryInterpolator, Double, ICollisionResultFactory> = Triple(interpolator, tolerance, resultFactory)

    override fun checkCollision(matrix: ITrajectoryMatrixSummary, target: ICollisionTarget): ICollisionResult {
        val points = matrix.trajectoryPoints()
        if (points.isEmpty()) {
            return cfg.third.unknown("No hay puntos en la matriz de trayectoria")
        }
        val nearGround = findNearGroundPoints(points)
        if (nearGround.isEmpty()) {
            return cfg.third.unknown("No se encontraron puntos cerca del suelo")
        }
        return evaluateHitOrMiss(points, nearGround, target)
    }

    private fun findNearGroundPoints(points: List<ITrajectoryPoint>): List<ITrajectoryPoint> {
        val tol = cfg.second
        return points.filter { it.posY() <= tol && it.posY() >= -tol * 2.0 }
    }

    private fun evaluateHitOrMiss(allPoints: List<ITrajectoryPoint>, nearGround: List<ITrajectoryPoint>, target: ICollisionTarget): ICollisionResult {
        val center = target.centerPosition()
        val hitCandidate = nearGround.firstOrNull { target.contains(it.posX()) }
        if (hitCandidate != null) {
            val exactPoint = cfg.first.interpolateToGround(allPoints, hitCandidate)
            val dist = abs(exactPoint.posX() - center)
            return cfg.third.hit(exactPoint, target, dist)
        }
        val closest = nearGround.minByOrNull { abs(it.posX() - center) }!!
        return cfg.third.miss(closest.posX(), target)
    }

    override fun isInsideTarget(impactX: Double, target: ICollisionTarget): Boolean {
        return target.isHit(impactX, 0.0) && cfg.second >= 0.0
    }
}
