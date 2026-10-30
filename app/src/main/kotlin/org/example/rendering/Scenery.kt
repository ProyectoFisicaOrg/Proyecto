package org.example.rendering

import java.util.Random
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop

/** Datos y comportamiento de renderizado de un rayo de sol precalculado. */
private data class SunRay(
    private val startX: Double,
    private val startY: Double,
    private val endX: Double,
    private val endY: Double
) {
    fun render(gc: GraphicsContext) = gc.strokeLine(startX, startY, endX, endY)
}

/** Datos y comportamiento de renderizado de un óvalo de nube precalculado. */
private data class CloudCircle(
    private val x: Double,
    private val y: Double,
    private val width: Double,
    private val height: Double
) {
    fun render(gc: GraphicsContext) {
        gc.fillOval(x, y, width, height)
        gc.strokeOval(x, y, width, height)
    }

    companion object {
        fun fromCenter(cx: Double, cy: Double, w: Double, h: Double): CloudCircle =
            CloudCircle(cx - w / 2.0, cy - h / 2.0, w, h)
    }
}

/** Datos y comportamiento de renderizado de un mechón de pasto precalculado. */
private data class GrassBlade(
    private val xPos: Double,
    private val bend: Double,
    private val height: Double
) {
    fun render(gc: GraphicsContext, groundY: Double) {
        gc.beginPath()
        gc.moveTo(xPos, groundY + 2.0)
        gc.quadraticCurveTo(xPos + bend, groundY - height, xPos + bend * 0.5, groundY - height - 4.0)
        gc.stroke()
    }
}

/** Datos y comportamiento de renderizado de una flor silvestre precalculada. */
private data class Wildflower(
    private val x: Double,
    private val y: Double,
    private val color: Color
) {
    fun render(gc: GraphicsContext, stemColor: Color, centerColor: Color) {
        gc.stroke = stemColor
        gc.lineWidth = 1.0
        gc.strokeLine(x, y, x, y - 6.0)

        gc.fill = color
        gc.fillOval(x + 1.0, y - 10.0, 4.0, 4.0)
        gc.fillOval(x - 2.0, y - 7.0, 4.0, 4.0)
        gc.fillOval(x - 5.0, y - 10.0, 4.0, 4.0)
        gc.fillOval(x - 2.0, y - 13.0, 4.0, 4.0)

        gc.fill = centerColor
        gc.fillOval(x - 1.5, y - 9.5, 3.0, 3.0)
    }
}

/**
 * Renderizador dedicado de cielo, sol, nube y colinas lejanas (SRP).
 */
private class SkyBackdropPainter(
    private val gc: GraphicsContext,
    private val cfg: SceneConfig
) {
    private val skyGradient = LinearGradient(
        0.0, 0.0, 0.0, cfg.groundY, true, CycleMethod.NO_CYCLE,
        Stop(0.0, Color.rgb(30, 60, 120)), Stop(0.3, Color.rgb(60, 110, 180)),
        Stop(0.6, Color.rgb(100, 160, 220)), Stop(1.0, Color.rgb(170, 210, 240))
    )
    private val sunRays: List<SunRay> = buildSunRays()
    private val cloudCircles: List<CloudCircle> = buildCloudCircles()

    private fun buildSunRays(): List<SunRay> {
        val sunX = cfg.canvasWidth - 130.0
        val sunY = 80.0
        val rand = Random(42)
        return (0..11).map { i ->
            val angle = i * Math.PI / 6.0
            val r2 = 45.0 + 15.0 + rand.nextDouble() * 10.0
            SunRay(sunX + Math.cos(angle) * 45.0, sunY + Math.sin(angle) * 45.0, sunX + Math.cos(angle) * r2, sunY + Math.sin(angle) * r2)
        }
    }

    private fun buildCloudCircles(): List<CloudCircle> = listOf(
        CloudCircle.fromCenter(142.5, 100.0, 50.0, 35.0),
        CloudCircle.fromCenter(167.5, 82.5, 50.0, 35.0),
        CloudCircle.fromCenter(192.5, 80.0, 50.0, 35.0),
        CloudCircle.fromCenter(217.5, 92.5, 50.0, 35.0),
        CloudCircle.fromCenter(182.5, 107.5, 50.0, 35.0)
    )

    fun renderSkyLayers() {
        gc.fill = skyGradient
        gc.fillRect(0.0, 0.0, cfg.canvasWidth, cfg.groundY)
        drawSun()
        drawCloud()
        drawHills()
    }

    private fun drawSun() {
        val sunX = cfg.canvasWidth - 130.0
        gc.stroke = Color.rgb(255, 210, 60, 0.45)
        gc.lineWidth = 2.0
        for (ray in sunRays) ray.render(gc)

        gc.fill = Color.rgb(255, 220, 40)
        gc.fillOval(sunX - 40.0, 40.0, 80.0, 80.0)
        gc.stroke = Color.rgb(255, 180, 0)
        gc.strokeOval(sunX - 40.0, 40.0, 80.0, 80.0)
    }

    private fun drawCloud() {
        gc.fill = Color.rgb(255, 255, 255, 0.95)
        gc.stroke = Color.rgb(220, 230, 240, 0.8)
        gc.lineWidth = 1.5
        for (c in cloudCircles) c.render(gc)
    }

    private fun drawHills() {
        drawHillRidge(Color.rgb(100, 170, 100, 0.5), 0.3, 60.0, 0.6)
        drawHillRidge(Color.rgb(70, 140, 70, 0.6), 0.4, 35.0, 0.7)
    }

    private fun drawHillRidge(fill: Color, peakRatio: Double, peakH: Double, midRatio: Double) {
        gc.fill = fill
        gc.beginPath()
        gc.moveTo(0.0, cfg.groundY)
        gc.quadraticCurveTo(cfg.canvasWidth * peakRatio, cfg.groundY - peakH, cfg.canvasWidth * midRatio, cfg.groundY)
        gc.quadraticCurveTo(cfg.canvasWidth * 0.85, cfg.groundY - peakH * 0.65, cfg.canvasWidth, cfg.groundY)
        gc.lineTo(cfg.canvasWidth, cfg.canvasHeight)
        gc.lineTo(0.0, cfg.canvasHeight)
        gc.closePath()
        gc.fill()
    }
}

/**
 * Renderizador dedicado del suelo, pradera, pasto y flores silvestres (SRP).
 */
private class GroundFoliagePainter(
    private val gc: GraphicsContext,
    private val cfg: SceneConfig
) {
    private val grassGradient = LinearGradient(
        0.0, cfg.groundY, 0.0, cfg.canvasHeight, true, CycleMethod.NO_CYCLE,
        Stop(0.0, Color.rgb(45, 150, 50)), Stop(0.4, Color.rgb(35, 120, 40)),
        Stop(0.7, Color.rgb(25, 90, 30)), Stop(1.0, Color.rgb(18, 70, 22))
    )
    private val grassBlades: List<GrassBlade> = buildBlades()
    private val wildflowers: List<Wildflower> = buildFlowers()

    private fun buildBlades(): List<GrassBlade> {
        val rand = Random(12345)
        return (0..(cfg.canvasWidth / 6.0).toInt()).map { x ->
            GrassBlade(x * 6.0 + (rand.nextDouble() * 3.0 - 1.5), (rand.nextDouble() - 0.5) * 6.0, 8.0 + rand.nextDouble() * 14.0)
        }
    }

    private fun buildFlowers(): List<Wildflower> {
        val rand = Random(5555)
        val palette = listOf(Color.rgb(255, 100, 100), Color.rgb(255, 200, 50), Color.rgb(180, 100, 255), Color.rgb(100, 200, 255), Color.rgb(255, 150, 200))
        return (0..30).map {
            val fx = 50.0 + rand.nextDouble() * (cfg.canvasWidth - 100.0)
            val fy = cfg.groundY + 5.0 + rand.nextDouble() * (cfg.canvasHeight - cfg.groundY - 15.0)
            Wildflower(fx, fy, palette[rand.nextInt(palette.size)])
        }
    }

    fun renderFoliageLayers() {
        gc.fill = grassGradient
        gc.fillRect(0.0, cfg.groundY, cfg.canvasWidth, cfg.canvasHeight - cfg.groundY)

        gc.stroke = Color.rgb(60, 180, 60, 0.4)
        gc.lineWidth = 1.2
        for (blade in grassBlades) blade.render(gc, cfg.groundY)

        val stem = Color.rgb(50, 130, 50, 0.7)
        val center = Color.rgb(255, 255, 100)
        for (flower in wildflowers) flower.render(gc, stem, center)
    }

    fun renderGroundLine() {
        gc.stroke = Color.rgb(60, 80, 40)
        gc.lineWidth = 4.0
        gc.strokeLine(0.0, cfg.groundY, cfg.canvasWidth, cfg.groundY)

        gc.stroke = Color.rgb(40, 60, 30)
        gc.lineWidth = 2.0
        gc.strokeLine(0.0, cfg.groundY + 2.0, cfg.canvasWidth, cfg.groundY + 2.0)
    }
}

/** Contrato abstracto para el renderizado del escenario (DIP). */
interface IScenery {
    fun drawBackground()
    fun drawGround()
}

/**
 * Orquestador de pintura del escenario de fondo y suelo (SRP + DIP).
 */
class Scenery(
    gc: GraphicsContext,
    cfg: SceneConfig
) : IScenery {
    private val skyPainter = SkyBackdropPainter(gc, cfg)
    private val foliagePainter = GroundFoliagePainter(gc, cfg)

    override fun drawBackground() {
        skyPainter.renderSkyLayers()
        foliagePainter.renderFoliageLayers()
    }

    override fun drawGround() {
        foliagePainter.renderGroundLine()
    }
}
