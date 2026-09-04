package org.example.rendering

// --- Imports de JavaFX usados para dibujar el escenario ---
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop

/**
 * Dibuja el escenario de fondo del canvas.
 *
 * SRP: su única responsabilidad es pintar el fondo (cielo, sol, nube,
 * colinas, pasto y suelo). No conoce nada del cañón ni del proyectil.
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
        drawCloud(180.0, 100.0, 1.0)
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
        // Crea un gradiente vertical desde la parte alta del canvas hasta el suelo.
        val skyGradient = LinearGradient(
            0.0, 0.0, 0.0, cfg.groundY, true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(30, 60, 120)),       // Azul profundo en la parte alta.
            Stop(0.3, Color.rgb(60, 110, 180)),      // Azul medio.
            Stop(0.6, Color.rgb(100, 160, 220)),     // Azul claro.
            Stop(1.0, Color.rgb(170, 210, 240))      // Casi blanco en el horizonte.
        )
        // Asigna el gradiente como relleno.
        gc.fill = skyGradient
        // Pinta un rectángulo que ocupa toda la zona del cielo (desde arriba hasta el suelo).
        gc.fillRect(0.0, 0.0, cfg.canvasWidth, cfg.groundY)
    }

    /** Dibuja el sol con sus rayos y un brillo interno. */
    private fun drawSun() {
        // Posición y tamaño del sol en la esquina superior derecha.
        val sunX = cfg.canvasWidth - 130.0
        val sunY = 80.0
        val sunRadius = 40.0

        // --- Rayos del sol: 12 líneas radiales alrededor ---
        gc.stroke = Color.rgb(255, 230, 100, 0.4)
        gc.lineWidth = 2.0
        // Recorre 12 ángulos para repartir los rayos en un círculo completo.
        for (i in 0..11) {
            val angle = i * Math.PI / 6.0
            // Longitud del rayo con una leve variación aleatoria.
            val rayLength = 15.0 + Math.random() * 10.0
            // Punto inicial del rayo (justo fuera del borde del sol).
            val startX = sunX + Math.cos(angle) * (sunRadius + 5.0)
            val startY = sunY + Math.sin(angle) * (sunRadius + 5.0)
            // Punto final del rayo (extendido hacia afuera).
            val endX = sunX + Math.cos(angle) * (sunRadius + 5.0 + rayLength)
            val endY = sunY + Math.sin(angle) * (sunRadius + 5.0 + rayLength)
            // Traza la línea del rayo.
            gc.strokeLine(startX, startY, endX, endY)
        }

        // --- Cuerpo del sol: círculo con gradiente amarillo ---
        val sunGradient = LinearGradient(
            sunX - sunRadius, sunY - sunRadius,
            sunX + sunRadius, sunY + sunRadius,
            true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(255, 255, 200)),  // Borde claro.
            Stop(0.5, Color.rgb(255, 230, 80)),   // Forma principal del sol.
            Stop(1.0, Color.rgb(255, 200, 40))    // Borde oscuro.
        )
        gc.fill = sunGradient
        // Dibuja el círculo del sol centrado en (sunX, sunY).
        gc.fillOval(sunX - sunRadius, sunY - sunRadius, sunRadius * 2.0, sunRadius * 2.0)

        // --- Brillo interno: un óvalo más claro y translúcido en el centro ---
        gc.fill = Color.rgb(255, 255, 230, 0.6)
        gc.fillOval(sunX - sunRadius * 0.6, sunY - sunRadius * 0.6, sunRadius * 1.2, sunRadius * 1.2)
    }

    /** Dibuja una nube suave estilo cartoon compuesta por 5 círculos superpuestos. */
    private fun drawCloud(cx: Double, cy: Double, scale: Double) {
        // Relleno blanco casi opaco y contorno suave.
        gc.fill = Color.rgb(255, 255, 255, 0.95)
        gc.stroke = Color.rgb(220, 230, 240, 0.8)
        gc.lineWidth = 1.5

        // Tamaño base de cada círculo, escalado.
        val b = 25.0 * scale
        // Posiciones relativas de los 5 círculos que forman la nube.
        val circles = listOf(
            Pair(cx - 1.5 * b, cy),          // Izquierda (base).
            Pair(cx - 0.5 * b, cy - 0.7 * b), // Arriba izquierda.
            Pair(cx + 0.5 * b, cy - 0.8 * b), // Arriba derecha.
            Pair(cx + 1.5 * b, cy - 0.3 * b), // Derecha (base).
            Pair(cx + 0.8 * b, cy + 0.4 * b)  // Abajo centro.
        )
        // Dibuja cada círculo con su relleno y contorno.
        for ((x, y) in circles) {
            gc.fillOval(x - b, y - b * 0.7, b * 2.0, b * 1.4)
            gc.strokeOval(x - b, y - b * 0.7, b * 2.0, b * 1.4)
        }
    }

    /** Dibuja dos colinas lejanas con efecto de profundidad. */
    private fun drawHills() {
        // --- Colina 1 (más lejana y más clara) ---
        gc.fill = Color.rgb(100, 170, 100, 0.5)
        // Traza el contorno de la colina con curvas.
        gc.beginPath()
        gc.moveTo(0.0, cfg.groundY)
        gc.quadraticCurveTo(cfg.canvasWidth * 0.3, cfg.groundY - 60.0, cfg.canvasWidth * 0.6, cfg.groundY)
        gc.quadraticCurveTo(cfg.canvasWidth * 0.8, cfg.groundY - 40.0, cfg.canvasWidth, cfg.groundY)
        // Cierra la forma bajando hasta el borde inferior del canvas.
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
        // Gradiente vertical desde la línea del suelo hasta el borde inferior.
        val grassGradient = LinearGradient(
            0.0, cfg.groundY, 0.0, cfg.canvasHeight, true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(45, 150, 50)),   // Verde brillante junto al suelo.
            Stop(0.4, Color.rgb(35, 120, 40)),   // Verde medio.
            Stop(0.7, Color.rgb(25, 90, 30)),    // Verde oscuro.
            Stop(1.0, Color.rgb(18, 70, 22))     // Muy oscuro abajo.
        )
        // Asigna y pinta el rectángulo del pasto (desde el suelo hasta el fondo).
        gc.fill = grassGradient
        gc.fillRect(0.0, cfg.groundY, cfg.canvasWidth, cfg.canvasHeight - cfg.groundY)
    }

    /** Dibuja mechones de pasto con una semilla fija para que sea determinista. */
    private fun drawGrassBlades() {
        gc.stroke = Color.rgb(60, 180, 60, 0.4)
        gc.lineWidth = 1.2
        // Semilla fija: siempre genera el mismo patrón de pasto.
        val random = java.util.Random(12345)
        // Distribuye mechones a lo ancho del canvas, uno cada 6 píxeles.
        for (x in 0..(cfg.canvasWidth / 6.0).toInt()) {
            // Posición horizontal con una pequeña dispersión aleatoria.
            val xPos = x * 6.0 + (random.nextDouble() * 3.0 - 1.5)
            // Altura del mechón.
            val height = 8.0 + random.nextDouble() * 14.0
            // Inclinación del mechón para darle forma orgánica.
            val bend = (random.nextDouble() - 0.5) * 6.0
            // Traza el mechón como una curva con punta.
            gc.beginPath()
            gc.moveTo(xPos, cfg.groundY + 2.0)
            gc.quadraticCurveTo(xPos + bend, cfg.groundY - height, xPos + bend * 0.5, cfg.groundY - height - 4.0)
            gc.stroke()
        }
    }

    /** Dibuja pequeñas flores silvestres dispersas por el pasto. */
    private fun drawWildflowers() {
        // Semilla fija para un patrón determinista.
        val random = java.util.Random(5555)
        // Paleta de colores posibles para los pétalos.
        val colors = listOf(
            Color.rgb(255, 100, 100),   // Rojo.
            Color.rgb(255, 200, 50),    // Amarillo.
            Color.rgb(180, 100, 255),   // Violeta.
            Color.rgb(100, 200, 255),   // Azul claro.
            Color.rgb(255, 150, 200)    // Rosa.
        )
        // Dibuja 31 flores en posiciones aleatorias dentro de la zona de pasto.
        for (i in 0..30) {
            // Posición de la flor (aleatoria, pero dentro del área del pasto).
            val x = 50.0 + random.nextDouble() * (cfg.canvasWidth - 100.0)
            val y = cfg.groundY + 5.0 + random.nextDouble() * (cfg.canvasHeight - cfg.groundY - 15.0)
            // Elige un color de pétalo al azar.
            val color = colors[random.nextInt(colors.size)]
            // --- Tallo: una línea vertical corta ---
            gc.stroke = Color.rgb(50, 130, 50, 0.7)
            gc.lineWidth = 1.0
            gc.strokeLine(x, y, x, y - 6.0)
            // --- Pétalos: 4 elipses pequeñas alrededor del centro ---
            gc.fill = color
            for (j in 0..3) {
                // Ángulo de cada pétalo a 90° de distancia.
                val angle = j * Math.PI / 2.0
                val px = x + Math.cos(angle) * 3.0
                val py = y - 8.0 + Math.sin(angle) * 3.0
                gc.fillOval(px - 2.0, py - 2.0, 4.0, 4.0)
            }
            // --- Centro de la flor: un punto amarillo ---
            gc.fill = Color.rgb(255, 255, 100)
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
