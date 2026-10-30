package org.example.rendering

import java.util.Random
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment

/**
 * Contrato abstracto para el renderizado del cañón (DIP).
 */
interface ICannonRenderer {
    fun drawCannon(angle: Double)
}

/**
 * Renderizador dedicado del tubo, boca y recámara del cañón (SRP).
 */
private class CannonBarrelPainter(
    private val gc: GraphicsContext,
    private val cfg: SceneConfig
) {
    private val decals = CannonDecalPainter(gc)

    fun drawRotatedBarrel(angle: Double) {
        gc.save()
        try {
            gc.translate(cfg.cannonBaseX, cfg.cannonBaseY)
            gc.rotate(-angle)
            drawTubeAndRings()
            drawMuzzle()
            drawBreech()
            decals.drawSkull(-11.0, -2.0, 0.7)
            decals.drawFlames(-27.0, 0.0)
        } finally {
            gc.restore()
        }
    }

    private fun drawTubeAndRings() {
        gc.fill = Color.rgb(40, 40, 40, 0.6)
        gc.fillRoundRect(3.0, -cfg.cannonWidth / 2.0 + 3.0, cfg.cannonLength, cfg.cannonWidth, 6.0, 6.0)

        gc.fill = LinearGradient(
            0.0, -cfg.cannonWidth / 2.0, 0.0, cfg.cannonWidth / 2.0, true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(180, 185, 190)), Stop(0.5, Color.rgb(120, 125, 130)), Stop(1.0, Color.rgb(60, 65, 70))
        )
        gc.fillRoundRect(0.0, -cfg.cannonWidth / 2.0, cfg.cannonLength, cfg.cannonWidth, 6.0, 6.0)

        gc.fill = Color.rgb(220, 225, 230, 0.4)
        gc.fillRoundRect(0.0, -cfg.cannonWidth / 2.0, cfg.cannonLength, cfg.cannonWidth * 0.3, 6.0, 6.0)

        drawRingStrokes(Color.rgb(80, 85, 90), 3.0)
        drawRingStrokes(Color.rgb(160, 165, 170), 1.5)
    }

    private fun drawRingStrokes(color: Color, width: Double) {
        gc.stroke = color
        gc.lineWidth = width
        for (i in 1..3) {
            val x = i * cfg.cannonLength / 4.0
            gc.strokeLine(x, -cfg.cannonWidth / 2.0 - 2.0, x, cfg.cannonWidth / 2.0 + 2.0)
        }
    }

    private fun drawMuzzle() {
        val mw = cfg.cannonWidth * 1.8
        val ml = 12.0
        gc.fill = Color.rgb(30, 30, 30, 0.5)
        gc.fillOval(cfg.cannonLength + 3.0, -mw / 2.0 + 3.0, ml, mw)

        gc.fill = LinearGradient(
            cfg.cannonLength, -mw / 2.0, cfg.cannonLength + ml, mw / 2.0, true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(140, 145, 150)), Stop(0.5, Color.rgb(90, 95, 100)), Stop(1.0, Color.rgb(40, 45, 50))
        )
        gc.fillOval(cfg.cannonLength, -mw / 2.0, ml, mw)

        gc.stroke = Color.rgb(200, 205, 210)
        gc.lineWidth = 2.5
        gc.strokeOval(cfg.cannonLength, -mw / 2.0, ml, mw)
    }

    private fun drawBreech() {
        val cw = cfg.cannonWidth * 2.2
        val cl = 22.0
        gc.fill = Color.rgb(70, 50, 35)
        gc.fillRoundRect(-cl, -cw / 2.0, cl, cw, 8.0, 8.0)

        gc.stroke = Color.rgb(50, 35, 25, 0.6)
        gc.lineWidth = 1.0
        for (i in 1..5) {
            val y = -cw / 2.0 + i * cw / 6.0
            gc.strokeLine(-cl + 2.0, y, 2.0, y + 3.0)
        }
        drawBreechRivets(cw, cl)
    }

    private fun drawBreechRivets(cw: Double, cl: Double) {
        for (row in 0..1) {
            for (col in 0..2) {
                val sx = -cl + 5.0 + col * 7.0
                val sy = -cw / 2.0 + 6.0 + row * (cw - 12.0)
                gc.fill = Color.rgb(180, 185, 190)
                gc.fillOval(sx - 2.5, sy - 2.5, 5.0, 5.0)
                gc.fill = Color.rgb(120, 125, 130)
                gc.fillOval(sx - 1.5, sy - 1.5, 3.0, 3.0)
            }
        }
    }
}

/**
 * Renderizador dedicado de insignias decorativas del cañón (calavera y llamas) (SRP).
 */
private class CannonDecalPainter(private val gc: GraphicsContext) {
    private val flameColors = listOf(Color.rgb(255, 80, 20), Color.rgb(255, 160, 20), Color.rgb(255, 230, 40))

    fun drawSkull(cx: Double, cy: Double, scale: Double) {
        val s = 8.0 * scale
        gc.fill = Color.rgb(240, 240, 230)
        gc.fillOval(cx - s, cy - s * 1.1, s * 2.0, s * 2.2)

        gc.fill = Color.BLACK
        gc.fillOval(cx - s * 0.5, cy - s * 0.3, s * 0.6, s * 0.7)
        gc.fillOval(cx + s * 0.2, cy - s * 0.3, s * 0.6, s * 0.7)
        drawSkullDetails(cx, cy, s)
    }

    private fun drawSkullDetails(cx: Double, cy: Double, s: Double) {
        gc.beginPath()
        gc.moveTo(cx, cy + s * 0.1)
        gc.lineTo(cx - s * 0.25, cy + s * 0.4)
        gc.lineTo(cx + s * 0.25, cy + s * 0.4)
        gc.closePath()
        gc.fill()

        gc.stroke = Color.rgb(255, 50, 50)
        gc.lineWidth = 1.5
        gc.strokeLine(cx, cy - s * 1.2, cx, cy - s * 0.8)
        gc.strokeLine(cx - s * 0.2, cy - s * 1.0, cx + s * 0.2, cy - s * 1.0)
    }

    fun drawFlames(baseX: Double, baseY: Double) {
        val random = Random(999)
        for (i in 0..4) {
            val fx = baseX - 3.0 - random.nextDouble() * 6.0
            val fy = baseY - 6.0 + (random.nextDouble() - 0.5) * 12.0
            val h = 10.0 + random.nextDouble() * 8.0
            val w = 6.0 + random.nextDouble() * 4.0
            drawFlameTongue(fx, fy, w, h)
        }
    }

    private fun drawFlameTongue(fx: Double, fy: Double, w: Double, h: Double) {
        for (layer in 0..2) {
            gc.fill = flameColors[layer]
            val lw = w * (1.0 - layer * 0.25)
            val lh = h * (1.0 - layer * 0.2)
            gc.beginPath()
            gc.moveTo(fx, fy)
            gc.quadraticCurveTo(fx - lw / 2.0, fy - lh / 3.0, fx, fy - lh)
            gc.quadraticCurveTo(fx + lw / 2.0, fy - lh / 3.0, fx, fy)
            gc.closePath()
            gc.fill()
        }
    }
}

/**
 * Renderizador dedicado de la plataforma, pivote y escala graduada de la base (SRP).
 */
private class CannonMountPainter(
    private val gc: GraphicsContext,
    private val cfg: SceneConfig
) {
    fun drawCannonBase() {
        gc.save()
        try {
            val bx = cfg.cannonBaseX
            val by = cfg.cannonBaseY
            drawBasePlatform(bx, by, 32.0)
            drawBasePivot(bx, by, 12.0)
            drawDegreeMarks(bx, by, 32.0)
        } finally {
            gc.restore()
        }
    }

    private fun drawBasePlatform(bx: Double, by: Double, r: Double) {
        gc.fill = Color.rgb(0, 0, 0, 0.3)
        gc.fillOval(bx - r + 3.0, by - r + 8.0, r * 2.0, r * 2.0)

        gc.fill = LinearGradient(
            bx - r, by - r, bx + r, by + r, true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(130, 90, 50)), Stop(0.5, Color.rgb(100, 70, 40)), Stop(1.0, Color.rgb(70, 50, 30))
        )
        gc.fillOval(bx - r, by - r, r * 2.0, r * 2.0)

        gc.stroke = Color.rgb(100, 105, 110)
        gc.lineWidth = 4.0
        gc.strokeOval(bx - r, by - r, r * 2.0, r * 2.0)
    }

    private fun drawBasePivot(bx: Double, by: Double, pr: Double) {
        gc.fill = Color.rgb(60, 65, 70)
        gc.fillOval(bx - pr, by - pr, pr * 2.0, pr * 2.0)

        for (i in 0..5) {
            val angle = i * Math.PI / 3.0
            val sx = bx + Math.cos(angle) * (pr * 0.6)
            val sy = by + Math.sin(angle) * (pr * 0.6)
            gc.fill = Color.rgb(180, 185, 190)
            gc.fillOval(sx - 3.0, sy - 3.0, 6.0, 6.0)
        }
        gc.fill = Color.rgb(20, 20, 20)
        gc.fillOval(bx - 5.0, by - 5.0, 10.0, 10.0)
    }

    private fun drawDegreeMarks(bx: Double, by: Double, r: Double) {
        gc.stroke = Color.rgb(200, 205, 210, 0.6)
        gc.lineWidth = 1.0
        for (deg in 0..8) {
            drawSingleDegreeTick(bx, by, r, deg)
        }
    }

    private fun drawSingleDegreeTick(bx: Double, by: Double, r: Double, deg: Int) {
        val rad = Math.toRadians(deg * 10.0) - Math.PI / 2.0
        gc.strokeLine(bx + Math.cos(rad) * r * 0.7, by + Math.sin(rad) * r * 0.7, bx + Math.cos(rad) * r * 0.9, by + Math.sin(rad) * r * 0.9)
        if (deg > 0 && deg % 2 == 0) {
            gc.fill = Color.rgb(200, 205, 210, 0.7)
            gc.font = Font.font("Monospace", FontWeight.BOLD, 9.0)
            gc.textAlign = TextAlignment.CENTER
            gc.fillText("${deg * 10}°", bx + Math.cos(rad) * (r * 1.15), by + Math.sin(rad) * (r * 1.15) + 3.0)
        }
    }
}

/**
 * Orquestador del renderizado del cañón (SRP + DIP).
 */
class CannonRenderer(
    gc: GraphicsContext,
    cfg: SceneConfig
) : ICannonRenderer {
    private val barrelPainter = CannonBarrelPainter(gc, cfg)
    private val mountPainter = CannonMountPainter(gc, cfg)

    override fun drawCannon(angle: Double) {
        barrelPainter.drawRotatedBarrel(angle)
        mountPainter.drawCannonBase()
    }
}
