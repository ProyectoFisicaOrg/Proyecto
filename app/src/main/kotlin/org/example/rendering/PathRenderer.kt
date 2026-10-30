package org.example.rendering

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Punto individual de la estela con coordenadas y marca temporal de creación.
 */
data class TrailPoint(
    val x: Double,
    val y: Double,
    private val timestampNano: Long
) {
    fun createdAtNano(): Long = timestampNano

    fun distanceTo(otherX: Double, otherY: Double): Double = hypot(x - otherX, y - otherY)

    fun isVeryCloseTo(otherX: Double, otherY: Double): Boolean =
        abs(x - otherX) < 0.5 && abs(y - otherY) < 0.5
}

/**
 * Contrato abstracto para el renderizador de trayectorias (DIP).
 */
interface IPathRenderer {
    var isFrozen: Boolean
    val pointCount: Int
    fun freeze()
    fun unfreeze()
    fun recordLaunch(nowNano: Long = System.nanoTime())
    fun addPoint(x: Double, y: Double, nowNano: Long = System.nanoTime())
    fun clear()
    fun getPoints(): List<Pair<Double, Double>>
    fun getTrailPoints(): List<TrailPoint>
    fun getFadeProgress(nowNano: Long = System.nanoTime()): Double
    fun getOpacity(nowNano: Long = System.nanoTime()): Double
    fun getPointAlpha(point: TrailPoint, nowNano: Long = System.nanoTime()): Double
    fun drawPath(nowNano: Long = System.nanoTime())
}

/**
 * Reloj temporal de desvanecimiento progresivo de la estela (SRP).
 */
private class TrailFadeClock(
    private val fadeDelaySeconds: Double,
    private val fadeDurationSeconds: Double
) {
    private var launchTimestampNano: Long = 0L
    var isFrozen: Boolean = false

    fun recordLaunch(nowNano: Long) {
        launchTimestampNano = nowNano
        isFrozen = false
    }

    fun reset() {
        launchTimestampNano = 0L
        isFrozen = false
    }

    fun computeProgress(nowNano: Long, fallbackFirstPointNano: Long): Double {
        if (isFrozen) return 0.0
        val launch = if (launchTimestampNano != 0L) launchTimestampNano else fallbackFirstPointNano
        if (launch == 0L) return 0.0
        val age = (nowNano - launch) / 1_000_000_000.0
        return if (age <= fadeDelaySeconds) 0.0 else ((age - fadeDelaySeconds) / fadeDurationSeconds).coerceIn(0.0, 1.0)
    }
}

/**
 * Pintor dedicado al trazado gráfico de la estela en el contexto de JavaFX (SRP).
 */
private class TrailStrokePainter(
    private val gc: GraphicsContext?,
    private val baseAlpha: Double
) {
    fun paint(points: List<TrailPoint>, fadeProgress: Double) {
        val context = gc ?: return
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

    private fun drawSolid(context: GraphicsContext, points: List<TrailPoint>) {
        context.stroke = Color.rgb(66, 153, 225, baseAlpha)
        context.beginPath()
        context.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            context.lineTo(points[i].x, points[i].y)
        }
        context.stroke()
    }

    private fun drawFading(context: GraphicsContext, points: List<TrailPoint>, fadeProgress: Double) {
        val totalSegments = points.size - 1
        for (i in 0 until totalSegments) {
            val u2 = (i + 1).toDouble() / totalSegments
            if (u2 > fadeProgress) {
                val uMid = (i + 0.5) / totalSegments
                val factor = ((uMid - fadeProgress) / (1.0 - fadeProgress)).coerceIn(0.0, 1.0)
                val alpha = (baseAlpha * factor).coerceIn(0.0, baseAlpha)
                drawSegmentWithAlpha(context, points[i], points[i + 1], alpha)
            }
        }
    }

    private fun drawSegmentWithAlpha(context: GraphicsContext, p1: TrailPoint, p2: TrailPoint, alpha: Double) {
        if (alpha > 0.01) {
            context.stroke = Color.rgb(66, 153, 225, alpha)
            context.strokeLine(p1.x, p1.y, p2.x, p2.y)
        }
    }
}

/**
 * Renderizador y gestor temporal de la estela continua del proyectil (SRP + DIP + OCP).
 */
class PathRenderer(
    gc: GraphicsContext? = null,
    fadeDelaySeconds: Double = 2.0,
    fadeDurationSeconds: Double = 1.2,
    private val baseAlpha: Double = 0.55
) : IPathRenderer {
    private val painter = TrailStrokePainter(gc, baseAlpha)
    private val fadeClock = TrailFadeClock(fadeDelaySeconds, fadeDurationSeconds)
    private val points = mutableListOf<TrailPoint>()

    override var isFrozen: Boolean
        get() = fadeClock.isFrozen
        set(value) { fadeClock.isFrozen = value }

    override val pointCount: Int get() = points.size

    override fun freeze() { fadeClock.isFrozen = true }

    override fun unfreeze() { fadeClock.isFrozen = false }

    override fun recordLaunch(nowNano: Long) = fadeClock.recordLaunch(nowNano)

    override fun addPoint(x: Double, y: Double, nowNano: Long) {
        if (points.lastOrNull()?.isVeryCloseTo(x, y) == true) return
        points.add(TrailPoint(x, y, nowNano))
    }

    override fun clear() {
        points.clear()
        fadeClock.reset()
    }

    override fun getPoints(): List<Pair<Double, Double>> = points.map { Pair(it.x, it.y) }

    override fun getTrailPoints(): List<TrailPoint> = points.toList()

    override fun getFadeProgress(nowNano: Long): Double =
        fadeClock.computeProgress(nowNano, points.firstOrNull()?.createdAtNano() ?: 0L)

    override fun getOpacity(nowNano: Long): Double =
        (baseAlpha * (1.0 - getFadeProgress(nowNano))).coerceAtLeast(0.0)

    override fun getPointAlpha(point: TrailPoint, nowNano: Long): Double {
        val progress = getFadeProgress(nowNano)
        if (progress <= 0.0 || progress >= 1.0) return if (progress >= 1.0) 0.0 else baseAlpha
        val idx = points.indexOf(point)
        if (idx < 0 || points.size <= 1) return getOpacity(nowNano)
        val u = idx.toDouble() / (points.size - 1.0)
        return if (u <= progress) 0.0 else (baseAlpha * ((u - progress) / (1.0 - progress))).coerceIn(0.0, baseAlpha)
    }

    override fun drawPath(nowNano: Long) {
        val fadeProgress = getFadeProgress(nowNano)
        if (fadeProgress >= 1.0) {
            points.clear()
            return
        }
        if (points.size >= 2) {
            painter.paint(points, fadeProgress)
        }
    }
}
