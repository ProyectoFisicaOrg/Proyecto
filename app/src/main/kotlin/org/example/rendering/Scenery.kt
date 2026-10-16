package org.example.rendering

// --- Imports de JavaFX usados para dibujar el escenario ---
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop

/** Datos inmutables de un rayo de sol precalculado para evitar allocations en cada frame */
private data class SunRay(val startX: Double, val startY: Double, val endX: Double, val endY: Double)

/** Datos inmutables de un círculo de nube precalculado */
private data class CloudCircle(val cx: Double, val cy: Double, val radiusX: Double, val radiusY: Double)

/** Datos inmutables de un mechón de pasto precalculado */
private data class GrassBlade(val xPos: Double, val bend: Double, val height: Double)

/** Datos inmutables de una flor silvestre precalculada */
private data class Wildflower(val x: Double, val y: Double, val color: Color)

/**
 * Dibuja el escenario de fondo del canvas (SRP + OCP).
 *
 * SRP: su única responsabilidad es pintar el fondo (cielo, sol, nube,
 * colinas, pasto y suelo). No conoce nada del cañón ni del proyectil.
 *
 * Rendimiento / Fluidez:
 *  - Precalcula y cachea gradientes, rayos del sol, mechones de pasto y flores
 *    para evitar miles de asignaciones de objetos (GC pressure) y cálculos trigonométricos
 *    en cada fotograma, garantizando 60+ FPS estables sin tirones ("trabado").
 *
 * Orientado a objetos: el contexto gráfico y la configuración se
 * inyectan por constructor (dependencias), y cada fotograma se pinta
 * llamando a [drawBackground] sobre esa instancia.
 */
class Scenery(
    // Contexto gráfico del canvas sobre el que se dibuja.
    private val gc: GraphicsContext,
    // Configuración geométrica (medidas del canvas y del suelo).
    private val cfg: SceneConfig,
) {
    // --- Recursos estáticos precalculados (Zero-allocation rendering loop) ---
    private val skyGradient = LinearGradient(
        0.0, 0.0, 0.0, cfg.groundY, true, CycleMethod.NO_CYCLE,
        Stop(0.0, Color.rgb(30, 60, 120)),       // Azul profundo en la parte alta.
        Stop(0.3, Color.rgb(60, 110, 180)),      // Azul medio.
        Stop(0.6, Color.rgb(100, 160, 220)),     // Azul claro.
        Stop(1.0, Color.rgb(170, 210, 240))      // Casi blanco en el horizonte.
    )

    private val grassGradient = LinearGradient(
        0.0, cfg.groundY, 0.0, cfg.canvasHeight, true, CycleMethod.NO_CYCLE,
        Stop(0.0, Color.rgb(45, 150, 50)),   // Verde brillante junto al suelo.
        Stop(0.4, Color.rgb(35, 120, 40)),   // Verde medio.
        Stop(0.7, Color.rgb(25, 90, 30)),    // Verde oscuro.
        Stop(1.0, Color.rgb(18, 70, 22))     // Muy oscuro abajo.
    )

    private val sunRays: List<SunRay>
    private val cloudCircles: List<CloudCircle>
    private val grassBlades: List<GrassBlade>
    private val wildflowers: List<Wildflower>

    init {
        // 1. Precalcular los rayos del sol
        val sunX = cfg.canvasWidth - 130.0
        val sunY = 80.0
        val sunRadius = 40.0
        val rays = mutableListOf<SunRay>()
        val sunRandom = java.util.Random(42)
        for (i in 0..11) {
            val angle = i * Math.PI / 6.0
            val rayLength = 15.0 + sunRandom.nextDouble() * 10.0
            val startX = sunX + Math.cos(angle) * (sunRadius + 5.0)
            val startY = sunY + Math.sin(angle) * (sunRadius + 5.0)
            val endX = sunX + Math.cos(angle) * (sunRadius + 5.0 + rayLength)
            val endY = sunY + Math.sin(angle) * (sunRadius + 5.0 + rayLength)
            rays.add(SunRay(startX, startY, endX, endY))
        }
        sunRays = rays

        // 2. Precalcular círculos de la nube (cx=180.0, cy=100.0, scale=1.0)
        val b = 25.0
        val cx = 180.0
        val cy = 100.0
        cloudCircles = listOf(
            CloudCircle(cx - 1.5 * b, cy, b * 2.0, b * 1.4),
            CloudCircle(cx - 0.5 * b, cy - 0.7 * b, b * 2.0, b * 1.4),
            CloudCircle(cx + 0.5 * b, cy - 0.8 * b, b * 2.0, b * 1.4),
            CloudCircle(cx + 1.5 * b, cy - 0.3 * b, b * 2.0, b * 1.4),
            CloudCircle(cx + 0.8 * b, cy + 0.4 * b, b * 2.0, b * 1.4)
        )

        // 3. Precalcular mechones de pasto con semilla determinista 12345
        val blades = mutableListOf<GrassBlade>()
        val grassRandom = java.util.Random(12345)
        for (x in 0..(cfg.canvasWidth / 6.0).toInt()) {
            val xPos = x * 6.0 + (grassRandom.nextDouble() * 3.0 - 1.5)
            val height = 8.0 + grassRandom.nextDouble() * 14.0
            val bend = (grassRandom.nextDouble() - 0.5) * 6.0
            blades.add(GrassBlade(xPos, bend, height))
        }
        grassBlades = blades

        // 4. Precalcular flores silvestres con semilla determinista 5555
        val flowers = mutableListOf<Wildflower>()
        val flowerRandom = java.util.Random(5555)
        val colors = listOf(
            Color.rgb(255, 100, 100),
            Color.rgb(255, 200, 50),
            Color.rgb(180, 100, 255),
            Color.rgb(100, 200, 255),
            Color.rgb(255, 150, 200)
        )
        for (i in 0..30) {
            val x = 50.0 + flowerRandom.nextDouble() * (cfg.canvasWidth - 100.0)
            val y = cfg.groundY + 5.0 + flowerRandom.nextDouble() * (cfg.canvasHeight - cfg.groundY - 15.0)
            val color = colors[flowerRandom.nextInt(colors.size)]
            flowers.add(Wildflower(x, y, color))
        }
        wildflowers = flowers
    }

    /**
     * Dibuja el fondo completo en el orden correcto:
     * primero las capas lejanas (cielo, sol, nube, colinas),
     * luego las cercanas (pasto, mechones, flores).
     */
    fun drawBackground() {
        // 1. El cielo con su gradiente.
        drawSky()
        // 2. El sol con rayos y brillo.
        drawSun()
        // 3. Una nube suave estilo cartoon.
        drawCloud()
        // 4. Las colinas lejanas detrás del pasto.
        drawHills()
        // 5. El gradiente de pasto.
        drawGrass()
        // 6. Los mechones de pasto (textura).
        drawGrassBlades()
        // 7. Las flores silvestres dispersas.
        drawWildflowers()
    }

    /** Dibuja el gradiente de cielo de 4 paradas (azul profundo a claro). */
    private fun drawSky() {
        gc.fill = skyGradient
        gc.fillRect(0.0, 0.0, cfg.canvasWidth, cfg.groundY)
    }

    /** Dibuja un sol amarillo brillante con rayos precalculados. */
    private fun drawSun() {
        val sunX = cfg.canvasWidth - 130.0
        val sunY = 80.0
        val sunRadius = 40.0

        // --- Rayos del sol: 12 líneas radiales precalculadas ---
        gc.stroke = Color.rgb(255, 210, 60, 0.45)
        gc.lineWidth = 2.0
        for (ray in sunRays) {
            gc.strokeLine(ray.startX, ray.startY, ray.endX, ray.endY)
        }

        // --- Cuerpo del sol: amarillo brillante sólido ---
        gc.fill = Color.rgb(255, 220, 40)
        gc.fillOval(sunX - sunRadius, sunY - sunRadius, sunRadius * 2.0, sunRadius * 2.0)

        // --- Borde/contorno naranja para darle definición contra el cielo ---
        gc.stroke = Color.rgb(255, 180, 0)
        gc.lineWidth = 2.0
        gc.strokeOval(sunX - sunRadius, sunY - sunRadius, sunRadius * 2.0, sunRadius * 2.0)
    }

    /** Dibuja una nube suave estilo cartoon compuesta por 5 círculos superpuestos. */
    private fun drawCloud() {
        gc.fill = Color.rgb(255, 255, 255, 0.95)
        gc.stroke = Color.rgb(220, 230, 240, 0.8)
        gc.lineWidth = 1.5

        val b = 25.0
        for (c in cloudCircles) {
            gc.fillOval(c.cx - b, c.cy - b * 0.7, c.radiusX, c.radiusY)
            gc.strokeOval(c.cx - b, c.cy - b * 0.7, c.radiusX, c.radiusY)
        }
    }

    /** Dibuja dos colinas lejanas con efecto de profundidad. */
    private fun drawHills() {
        // --- Colina 1 (más lejana y más clara) ---
        gc.fill = Color.rgb(100, 170, 100, 0.5)
        gc.beginPath()
        gc.moveTo(0.0, cfg.groundY)
        gc.quadraticCurveTo(cfg.canvasWidth * 0.3, cfg.groundY - 60.0, cfg.canvasWidth * 0.6, cfg.groundY)
        gc.quadraticCurveTo(cfg.canvasWidth * 0.8, cfg.groundY - 40.0, cfg.canvasWidth, cfg.groundY)
        gc.lineTo(cfg.canvasWidth, cfg.canvasHeight)
        gc.lineTo(0.0, cfg.canvasHeight)
        gc.closePath()
        gc.fill()

        // --- Colina 2 (más cercana y más oscura) ---
        gc.fill = Color.rgb(70, 140, 70, 0.6)
        gc.beginPath()
        gc.moveTo(0.0, cfg.groundY)
        gc.quadraticCurveTo(cfg.canvasWidth * 0.4, cfg.groundY - 35.0, cfg.canvasWidth * 0.7, cfg.groundY)
        gc.quadraticCurveTo(cfg.canvasWidth * 0.9, cfg.groundY - 25.0, cfg.canvasWidth, cfg.groundY)
        gc.lineTo(cfg.canvasWidth, cfg.canvasHeight)
        gc.lineTo(0.0, cfg.canvasHeight)
        gc.closePath()
        gc.fill()
    }

    /** Dibuja el gradiente de pasto de 4 paradas (verde brillante a oscuro). */
    private fun drawGrass() {
        gc.fill = grassGradient
        gc.fillRect(0.0, cfg.groundY, cfg.canvasWidth, cfg.canvasHeight - cfg.groundY)
    }

    /** Dibuja mechones de pasto precalculados para máximo rendimiento a 60+ FPS. */
    private fun drawGrassBlades() {
        gc.stroke = Color.rgb(60, 180, 60, 0.4)
        gc.lineWidth = 1.2
        for (blade in grassBlades) {
            gc.beginPath()
            gc.moveTo(blade.xPos, cfg.groundY + 2.0)
            gc.quadraticCurveTo(
                blade.xPos + blade.bend,
                cfg.groundY - blade.height,
                blade.xPos + blade.bend * 0.5,
                cfg.groundY - blade.height - 4.0
            )
            gc.stroke()
        }
    }

    /** Dibuja pequeñas flores silvestres precalculadas dispersas por el pasto. */
    private fun drawWildflowers() {
        val stemColor = Color.rgb(50, 130, 50, 0.7)
        val centerColor = Color.rgb(255, 255, 100)

        for (flower in wildflowers) {
            val x = flower.x
            val y = flower.y

            // --- Tallo: una línea vertical corta ---
            gc.stroke = stemColor
            gc.lineWidth = 1.0
            gc.strokeLine(x, y, x, y - 6.0)

            // --- Pétalos: 4 elipses pequeñas alrededor del centro ---
            gc.fill = flower.color
            gc.fillOval(x + 3.0 - 2.0, y - 8.0 - 2.0, 4.0, 4.0)
            gc.fillOval(x - 2.0, y - 5.0 - 2.0, 4.0, 4.0)
            gc.fillOval(x - 3.0 - 2.0, y - 8.0 - 2.0, 4.0, 4.0)
            gc.fillOval(x - 2.0, y - 11.0 - 2.0, 4.0, 4.0)

            // --- Centro de la flor: un punto amarillo ---
            gc.fill = centerColor
            gc.fillOval(x - 1.5, y - 9.5, 3.0, 3.0)
        }
    }

    /** Dibuja la línea principal y la secundaria que definen el suelo. */
    fun drawGround() {
        // Línea principal del suelo (gruesa y oscura).
        gc.stroke = Color.rgb(60, 80, 40)
        gc.lineWidth = 4.0
        gc.strokeLine(0.0, cfg.groundY, cfg.canvasWidth, cfg.groundY)

        // Segunda línea para dar sensación de profundidad.
        gc.stroke = Color.rgb(40, 60, 30)
        gc.lineWidth = 2.0
        gc.strokeLine(0.0, cfg.groundY + 2.0, cfg.canvasWidth, cfg.groundY + 2.0)
    }
}
