package org.example.rendering

import java.util.Random
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop

/** Datos y comportamiento de renderizado de un rayo de sol precalculado. */
private data class SunRay(
    private val coords: List<Double>
) : ICanvasDrawableElement {
    override fun drawOn(gc: GraphicsContext) {
        gc.strokeLine(coords[0], coords[1], coords[2], coords[3])
    }
}

/** Datos y comportamiento de renderizado de un óvalo de nube precalculado. */
private data class CloudCircle(
    private val bounds: List<Double>
) : ICanvasDrawableElement {
    override fun drawOn(gc: GraphicsContext) {
        gc.fillOval(bounds[0], bounds[1], bounds[2], bounds[3])
        gc.strokeOval(bounds[0], bounds[1], bounds[2], bounds[3])
    }

    companion object {
        fun fromCenter(cx: Double, cy: Double, w: Double, h: Double): ICanvasDrawableElement {
            return CloudCircle(listOf(cx - w / 2.0, cy - h / 2.0, w, h))
        }
    }
}

/** Datos y comportamiento de renderizado de un mechón de pasto precalculado. */
private data class GrassBlade(
    private val geometry: List<Double>
) : ICanvasDrawableElement {
    override fun drawOn(gc: GraphicsContext) {
        val xPos = geometry[0]
        val bend = geometry[1]
        val height = geometry[2]
        val groundY = geometry[3]
        gc.beginPath()
        gc.moveTo(xPos, groundY + 2.0)
        gc.quadraticCurveTo(xPos + bend, groundY - height, xPos + bend * 0.5, groundY - height - 4.0)
        gc.stroke()
    }
}

/** Datos y comportamiento de renderizado de una flor silvestre precalculada. */
private data class Wildflower(
    private val coords: Pair<Double, Double>,
    private val petalColor: Color
) : ICanvasDrawableElement {
    override fun drawOn(gc: GraphicsContext) {
        val x = coords.first
        val y = coords.second
        gc.stroke = Color.rgb(50, 130, 50, 0.7)
        gc.lineWidth = 1.0
        gc.strokeLine(x, y, x, y - 6.0)
        gc.fill = petalColor
        gc.fillOval(x + 1.0, y - 10.0, 4.0, 4.0)
        gc.fillOval(x - 2.0, y - 7.0, 4.0, 4.0)
        gc.fillOval(x - 5.0, y - 10.0, 4.0, 4.0)
        gc.fillOval(x - 2.0, y - 13.0, 4.0, 4.0)
        gc.fill = Color.rgb(255, 255, 100)
        gc.fillOval(x - 1.5, y - 9.5, 3.0, 3.0)
    }
}

/**
 * Renderizador dedicado de cielo, sol, nube y colinas lejanas (SRP + DIP).
 */
class SkyBackdropPainter(
    private val gc: GraphicsContext,
    private val cfg: ISceneConfig
) : ISkyBackdropPainter {
    private val skyGradient = LinearGradient(
        0.0, 0.0, 0.0, cfg.groundLevelY(), true, CycleMethod.NO_CYCLE,
        Stop(0.0, Color.rgb(30, 60, 120)), Stop(0.3, Color.rgb(60, 110, 180)),
        Stop(0.6, Color.rgb(100, 160, 220)), Stop(1.0, Color.rgb(170, 210, 240))
    )
    private val sunRays: List<ICanvasDrawableElement> = buildSunRays()
    private val cloudCircles: List<ICanvasDrawableElement> = buildCloudCircles()

    private fun buildSunRays(): List<ICanvasDrawableElement> {
        val sunX = cfg.width() - 130.0
        val sunY = 80.0
        val rand = Random(42)
        return (0..11).map { i ->
            val angle = i * Math.PI / 6.0
            val r2 = 60.0 + rand.nextDouble() * 10.0
            SunRay(listOf(sunX + Math.cos(angle) * 45.0, sunY + Math.sin(angle) * 45.0, sunX + Math.cos(angle) * r2, sunY + Math.sin(angle) * r2))
        }
    }

    private fun buildCloudCircles(): List<ICanvasDrawableElement> {
        val baseX = cfg.basePivotX() + 52.5
        return listOf(
            CloudCircle.fromCenter(baseX, 100.0, 50.0, 35.0),
            CloudCircle.fromCenter(baseX + 25.0, 82.5, 50.0, 35.0),
            CloudCircle.fromCenter(baseX + 50.0, 80.0, 50.0, 35.0),
            CloudCircle.fromCenter(baseX + 75.0, 92.5, 50.0, 35.0),
            CloudCircle.fromCenter(baseX + 40.0, 107.5, 50.0, 35.0)
        )
    }

    override fun renderSkyLayers() {
        gc.fill = skyGradient
        gc.fillRect(0.0, 0.0, cfg.width(), cfg.groundLevelY())
        drawSun()
        drawCloud()
        drawHills()
    }

    private fun drawSun() {
        val sunX = cfg.width() - 130.0
        gc.stroke = Color.rgb(255, 210, 60, 0.45)
        gc.lineWidth = 2.0
        for (ray in sunRays) ray.drawOn(gc)
        gc.fill = Color.rgb(255, 220, 40)
        gc.fillOval(sunX - 40.0, 40.0, 80.0, 80.0)
        gc.stroke = Color.rgb(255, 180, 0)
        gc.strokeOval(sunX - 40.0, 40.0, 80.0, 80.0)
    }

    private fun drawCloud() {
        gc.fill = Color.rgb(255, 255, 255, 0.95)
        gc.stroke = Color.rgb(220, 230, 240, 0.8)
        gc.lineWidth = 1.5
        for (c in cloudCircles) c.drawOn(gc)
    }

    private fun drawHills() {
        drawHillRidge(Color.rgb(100, 170, 100, 0.5), 0.3, 60.0, 0.6)
        drawHillRidge(Color.rgb(70, 140, 70, 0.6), 0.4, 35.0, 0.7)
    }

    private fun drawHillRidge(fill: Color, peakRatio: Double, peakH: Double, midRatio: Double) {
        val w = cfg.width()
        val gy = cfg.groundLevelY()
        val h = cfg.height()
        gc.fill = fill
        gc.beginPath()
        gc.moveTo(0.0, gy)
        gc.quadraticCurveTo(w * peakRatio, gy - peakH, w * midRatio, gy)
        gc.quadraticCurveTo(w * 0.85, gy - peakH * 0.65, w, gy)
        gc.lineTo(w, h)
        gc.lineTo(0.0, h)
        gc.closePath()
        gc.fill()
    }
}

/**
 * Renderizador dedicado del suelo, pradera, pasto y flores silvestres (SRP + DIP).
 */
class GroundFoliagePainter(
    private val gc: GraphicsContext,
    private val cfg: ISceneConfig
) : IGroundFoliagePainter {
    private val grassGradient = LinearGradient(
        0.0, cfg.groundLevelY(), 0.0, cfg.height(), true, CycleMethod.NO_CYCLE,
        Stop(0.0, Color.rgb(45, 150, 50)), Stop(0.4, Color.rgb(35, 120, 40)),
        Stop(0.7, Color.rgb(25, 90, 30)), Stop(1.0, Color.rgb(18, 70, 22))
    )
    private val foliageElements: List<ICanvasDrawableElement> = buildFoliage()

    private fun buildFoliage(): List<ICanvasDrawableElement> {
        return buildBlades() + buildFlowers()
    }

    private fun buildBlades(): List<ICanvasDrawableElement> {
        val rand = Random(12345)
        val gy = cfg.groundLevelY()
        return (0..(cfg.width() / 6.0).toInt()).map { x ->
            GrassBlade(listOf(x * 6.0 + (rand.nextDouble() * 3.0 - 1.5), (rand.nextDouble() - 0.5) * 6.0, 8.0 + rand.nextDouble() * 14.0, gy))
        }
    }

    private fun buildFlowers(): List<ICanvasDrawableElement> {
        val rand = Random(5555)
        val gy = cfg.groundLevelY()
        val w = cfg.width()
        val h = cfg.height()
        val palette = listOf(Color.rgb(255, 100, 100), Color.rgb(255, 200, 50), Color.rgb(180, 100, 255), Color.rgb(100, 200, 255), Color.rgb(255, 150, 200))
        return (0..30).map {
            val fx = 50.0 + rand.nextDouble() * (w - 100.0)
            val fy = gy + 5.0 + rand.nextDouble() * (h - gy - 15.0)
            Wildflower(Pair(fx, fy), palette[rand.nextInt(palette.size)])
        }
    }

    override fun renderFoliageLayers() {
        val gy = cfg.groundLevelY()
        gc.fill = grassGradient
        gc.fillRect(0.0, gy, cfg.width(), cfg.height() - gy)
        gc.stroke = Color.rgb(60, 180, 60, 0.4)
        gc.lineWidth = 1.2
        for (elem in foliageElements) elem.drawOn(gc)
    }

    override fun renderGroundLine() {
        val gy = cfg.groundLevelY()
        val w = cfg.width()
        gc.stroke = Color.rgb(60, 80, 40)
        gc.lineWidth = 4.0
        gc.strokeLine(0.0, gy, w, gy)
        gc.stroke = Color.rgb(40, 60, 30)
        gc.lineWidth = 2.0
        gc.strokeLine(0.0, gy + 2.0, w, gy + 2.0)
    }
}

/**
 * Orquestador de pintura del escenario de fondo y suelo desacoplado por interfaces (SRP + DIP).
 */
class Scenery(
    private val skyPainter: ISkyBackdropPainter,
    private val foliagePainter: IGroundFoliagePainter
) : IScenery {

    override fun drawBackground() {
        skyPainter.renderSkyLayers()
        foliagePainter.renderFoliageLayers()
    }

    override fun drawGround() {
        foliagePainter.renderGroundLine()
    }
}
