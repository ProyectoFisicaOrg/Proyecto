package org.example.rendering

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Punto individual encapsulado de la estela con coordenadas y marca temporal (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class TrailPoint internal constructor(
    private val coords: List<Double>,
    private val timestampNano: Long
) : ITrailPoint {

    constructor(x: Double, y: Double, timestampNano: Long) : this(listOf(x, y), timestampNano)

    override fun coordX(): Double {
        return coords[0]
    }

    override fun coordY(): Double {
        return coords[1]
    }

    override fun createdAtNano(): Long {
        return timestampNano
    }

    fun distanceTo(otherX: Double, otherY: Double): Double {
        return hypot(coords[0] - otherX, coords[1] - otherY)
    }

    override fun isVeryCloseTo(otherX: Double, otherY: Double): Boolean {
        return abs(coords[0] - otherX) < 0.5 && abs(coords[1] - otherY) < 0.5
    }
}

/**
 * Reloj temporal encapsulado de desvanecimiento progresivo de la estela (SRP + DIP).
 */
class TrailFadeClock(
    fadeDelaySeconds: Double,
    fadeDurationSeconds: Double
) : ITrailFadeClock {
    private val timings: Pair<Double, Double> = Pair(fadeDelaySeconds, fadeDurationSeconds)
    private var clockState: Pair<Long, Boolean> = Pair(0L, false)

    override fun checkFrozen(): Boolean {
        return clockState.second
    }

    override fun updateFrozen(frozen: Boolean) {
        clockState = Pair(clockState.first, frozen)
    }

    override fun recordLaunch(nowNano: Long) {
        clockState = Pair(nowNano, false)
    }

    override fun reset() {
        clockState = Pair(0L, false)
    }

    override fun computeProgress(nowNano: Long, fallbackFirstPointNano: Long): Double {
        if (clockState.second) return 0.0
        val launch = if (clockState.first != 0L) clockState.first else fallbackFirstPointNano
        if (launch == 0L) return 0.0
        val age = (nowNano - launch) / 1_000_000_000.0
        if (age <= timings.first) return 0.0
        return ((age - timings.first) / timings.second).coerceIn(0.0, 1.0)
    }
}

/**
 * Pintor dedicado al trazado gráfico de la estela en el contexto de JavaFX (SRP + DIP).
 */
class TrailStrokePainter(
    gc: GraphicsContext?,
    baseAlpha: Double
) : ITrailStrokePainter {
    private val config: Pair<GraphicsContext?, Double> = Pair(gc, baseAlpha)

    override fun paint(points: List<ITrailPoint>, fadeProgress: Double) {
        val context = config.first ?: return
        context.save()
        try {
            context.lineWidth = 3.5
            context.lineCap = StrokeLineCap.ROUND
            context.lineJoin = StrokeLineJoin.ROUND
            context.setLineDashes()
            if (fadeProgress <= 0.0) drawSolid(context, points) else drawFading(context, points, fadeProgress)
        } finally {
            context.restore()
        }
    }

    private fun drawSolid(context: GraphicsContext, points: List<ITrailPoint>) {
        context.stroke = Color.rgb(66, 153, 225, config.second)
        context.beginPath()
        context.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            context.lineTo(points[i].x, points[i].y)
        }
        context.stroke()
    }

    private fun drawFading(context: GraphicsContext, points: List<ITrailPoint>, fadeProgress: Double) {
        val totalSegments = points.size - 1
        for (i in 0 until totalSegments) {
            val u2 = (i + 1).toDouble() / totalSegments
            if (u2 > fadeProgress) {
                val uMid = (i + 0.5) / totalSegments
                val factor = ((uMid - fadeProgress) / (1.0 - fadeProgress)).coerceIn(0.0, 1.0)
                val alpha = (config.second * factor).coerceIn(0.0, config.second)
                drawSegmentWithAlpha(context, points[i], points[i + 1], alpha)
            }
        }
    }

    private fun drawSegmentWithAlpha(context: GraphicsContext, p1: ITrailPoint, p2: ITrailPoint, alpha: Double) {
        if (alpha > 0.01 && alpha <= config.second) {
            context.stroke = Color.rgb(66, 153, 225, alpha)
            context.strokeLine(p1.x, p1.y, p2.x, p2.y)
        }
    }
}

/**
 * Renderizador y gestor temporal cohesivo de la estela continua del proyectil (SRP + DIP + OCP).
 */
class PathRenderer(
    painter: ITrailStrokePainter,
    fadeClock: ITrailFadeClock,
    baseAlpha: Double = 0.55,
    pointFactory: (Double, Double, Long) -> ITrailPoint = ::TrailPoint
) : IPathRenderer {

    constructor(
        gc: GraphicsContext? = null,
        fadeDelaySeconds: Double = 2.0,
        fadeDurationSeconds: Double = 1.2,
        baseAlpha: Double = 0.55
    ) : this(TrailStrokePainter(gc, baseAlpha), TrailFadeClock(fadeDelaySeconds, fadeDurationSeconds), baseAlpha)

    private val ctx: Pair<Triple<ITrailStrokePainter, Double, (Double, Double, Long) -> ITrailPoint>, Pair<ITrailFadeClock, MutableList<ITrailPoint>>> =
        Pair(Triple(painter, baseAlpha, pointFactory), Pair(fadeClock, mutableListOf()))

    override fun checkFrozen(): Boolean {
        return ctx.second.first.checkFrozen()
    }

    override fun updateFrozen(frozen: Boolean) {
        ctx.second.first.updateFrozen(frozen)
    }

    override fun recordLaunch(nowNano: Long) {
        ctx.second.first.recordLaunch(nowNano)
    }

    override fun addPoint(x: Double, y: Double, nowNano: Long) {
        val pts = ctx.second.second
        if (pts.isNotEmpty() && pts.last().isVeryCloseTo(x, y)) return
        pts.add(ctx.first.third(x, y, nowNano))
    }

    override fun clear() {
        ctx.second.second.clear()
        ctx.second.first.reset()
    }

    override fun getTrailPoints(): List<ITrailPoint> {
        return ctx.second.second.toList()
    }

    override fun getFadeProgress(nowNano: Long): Double {
        val firstNano = ctx.second.second.firstOrNull()?.createdAtNano() ?: 0L
        return ctx.second.first.computeProgress(nowNano, firstNano)
    }

    override fun getOpacity(nowNano: Long): Double {
        return (ctx.first.second * (1.0 - getFadeProgress(nowNano))).coerceAtLeast(0.0)
    }

    override fun getPointAlpha(point: ITrailPoint, nowNano: Long): Double {
        val progress = getFadeProgress(nowNano)
        if (progress <= 0.0) return ctx.first.second
        if (progress >= 1.0) return 0.0
        return interpolatePointAlpha(point, progress, nowNano)
    }

    private fun interpolatePointAlpha(point: ITrailPoint, progress: Double, nowNano: Long): Double {
        val pts = ctx.second.second
        val alphaMax = ctx.first.second
        val idx = pts.indexOf(point)
        if (idx < 0 || pts.size <= 1) return getOpacity(nowNano)
        val u = idx.toDouble() / (pts.size - 1.0)
        if (u <= progress) return 0.0
        return (alphaMax * ((u - progress) / (1.0 - progress))).coerceIn(0.0, alphaMax)
    }

    override fun drawPath(nowNano: Long) {
        val fadeProgress = getFadeProgress(nowNano)
        if (fadeProgress >= 1.0) {
            ctx.second.second.clear()
            return
        }
        if (ctx.second.second.size >= 2) {
            ctx.first.first.paint(ctx.second.second, fadeProgress)
        }
    }
}
