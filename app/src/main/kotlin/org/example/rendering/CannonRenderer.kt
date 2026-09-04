package org.example.rendering

// --- Imports de JavaFX usados para dibujar el cañón ---
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.paint.CycleMethod
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Stop
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment

/**
 * Dibuja el cañón estilo "Kick Buttowski" (SRP).
 *
 * Su única responsabilidad es pintar el cañón: tubo, boca, recámara,
 * calavera, llamas y base giratoria. Recibe el [GraphicsContext] y la
 * [SceneConfig] por constructor (inyección de dependencias) y dibuja
 * según el ángulo que se le pasa a [drawCannon].
 */
class CannonRenderer(
    // Contexto gráfico del canvas sobre el que se dibuja.
    private val gc: GraphicsContext,
    // Configuración geométrica (posición y dimensiones del cañón).
    private val cfg: SceneConfig,
) {

    /**
     * Dibuja el cañón completo rotado según [angle] y, al final, su base fija.
     *
     * Usa save/translate/rotate para dibujar el cañón orientado hacia el
     * ángulo dado y luego restore para volver al sistema de coordenadas
     * original antes de pintar la base.
     */
    fun drawCannon(angle: Double) {
        // Guarda el estado gráfico y lo desplaza/rota hasta el pivote del cañón.
        gc.save()
        gc.translate(cfg.cannonBaseX, cfg.cannonBaseY)
        gc.rotate(-angle)  // Rota en sentido horario según el ángulo.

        // ===== 1. SOMBRA DEL TUBO =====
        // Sombra desplazada ligeramente para dar volumen.
        gc.fill = Color.rgb(40, 40, 40, 0.6)
        gc.fillRoundRect(3.0, -cfg.cannonWidth / 2.0 + 3.0, cfg.cannonLength, cfg.cannonWidth, 6.0, 6.0)

        // ===== 2. TUBO PRINCIPAL (metal gris acero) =====
        // Gradiente vertical para simular el reflejo metálico del tubo.
        val tubeGradient = LinearGradient(
            0.0, -cfg.cannonWidth / 2.0, 0.0, cfg.cannonWidth / 2.0,
            true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(180, 185, 190)),   // Claro arriba (luz).
            Stop(0.5, Color.rgb(120, 125, 130)),   // Medio.
            Stop(1.0, Color.rgb(60, 65, 70))       // Oscuro abajo (sombra).
        )
        gc.fill = tubeGradient
        // Dibuja el tubo como un rectángulo redondeado horizontal.
        gc.fillRoundRect(0.0, -cfg.cannonWidth / 2.0, cfg.cannonLength, cfg.cannonWidth, 6.0, 6.0)

        // --- Brillos en el tubo: un reflejo claro en la parte superior ---
        gc.fill = Color.rgb(220, 225, 230, 0.4)
        gc.fillRoundRect(0.0, -cfg.cannonWidth / 2.0, cfg.cannonLength, cfg.cannonWidth * 0.3, 6.0, 6.0)

        // ===== 3. ANILLOS DE REFUERZO (bandas metálicas) =====
        // Líneas verticales oscuras a lo largo del tubo.
        gc.stroke = Color.rgb(80, 85, 90)
        gc.lineWidth = 3.0
        for (i in 1..3) {
            val x = i * cfg.cannonLength / 4.0  // Posición horizontal del anillo.
            gc.strokeLine(x, -cfg.cannonWidth / 2.0 - 2.0, x, cfg.cannonWidth / 2.0 + 2.0)
        }
        // Líneas claras superpuestas para dar efecto de brillo.
        gc.lineWidth = 1.5
        gc.stroke = Color.rgb(160, 165, 170)
        for (i in 1..3) {
            val x = i * cfg.cannonLength / 4.0
            gc.strokeLine(x, -cfg.cannonWidth / 2.0 - 2.0, x, cfg.cannonWidth / 2.0 + 2.0)
        }

        // ===== 4. BOCA DEL CAÑÓN (ensanchada, estilo "bazooka") =====
        // La boca es más ancha que el tubo y sobresale al final.
        val muzzleWidth = cfg.cannonWidth * 1.8
        val muzzleLength = 12.0

        // Sombra de la boca.
        gc.fill = Color.rgb(30, 30, 30, 0.5)
        gc.fillOval(cfg.cannonLength + 3.0, -muzzleWidth / 2.0 + 3.0, muzzleLength, muzzleWidth)

        // Boca metálica con su propio gradiente.
        val muzzleGradient = LinearGradient(
            cfg.cannonLength, -muzzleWidth / 2.0,
            cfg.cannonLength + muzzleLength, muzzleWidth / 2.0,
            true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(140, 145, 150)),
            Stop(0.5, Color.rgb(90, 95, 100)),
            Stop(1.0, Color.rgb(40, 45, 50))
        )
        gc.fill = muzzleGradient
        // Dibuja la boca como una elipse vertical al final del tubo.
        gc.fillOval(cfg.cannonLength, -muzzleWidth / 2.0, muzzleLength, muzzleWidth)

        // --- Anillo final cromado: contorno exterior e interior ---
        gc.stroke = Color.rgb(200, 205, 210)
        gc.lineWidth = 2.5
        gc.strokeOval(cfg.cannonLength, -muzzleWidth / 2.0, muzzleLength, muzzleWidth)
        gc.stroke = Color.rgb(100, 105, 110)
        gc.lineWidth = 1.0
        gc.strokeOval(cfg.cannonLength + 2.0, -muzzleWidth / 2.0 + 2.0, muzzleLength - 4.0, muzzleWidth - 4.0)

        // ===== 5. RECÁMARA (parte trasera, más ancha, con detalles) =====
        // La recámara es más ancha que el tubo y va detrás del pivote.
        val chamberWidth = cfg.cannonWidth * 2.2
        val chamberLength = 22.0

        // Base de la recámara en tono madera oscura.
        gc.fill = Color.rgb(70, 50, 35)
        gc.fillRoundRect(-chamberLength, -chamberWidth / 2.0, chamberLength, chamberWidth, 8.0, 8.0)

        // --- Detalles de madera (vetas horizontales) ---
        gc.stroke = Color.rgb(50, 35, 25, 0.6)
        gc.lineWidth = 1.0
        for (i in 1..5) {
            val y = -chamberWidth / 2.0 + i * chamberWidth / 6.0  // Alta de cada veta.
            gc.strokeLine(-chamberLength + 2.0, y, 2.0, y + 3.0)
        }

        // --- Tornillos/pernos en la recámara: una cuadrícula de 2x3 ---
        gc.fill = Color.rgb(180, 185, 190)
        for (row in 0..1) {      // Dos filas.
            for (col in 0..2) {  // Tres columnas.
                // Posición de cada tornillo.
                val sx = -chamberLength + 5.0 + col * 7.0
                val sy = -chamberWidth / 2.0 + 6.0 + row * (chamberWidth - 12.0)
                // Cabeza del tornillo (clara).
                gc.fillOval(sx - 2.5, sy - 2.5, 5.0, 5.0)
                // Agujero del tornillo (oscuro).
                gc.fill = Color.rgb(120, 125, 130)
                gc.fillOval(sx - 1.5, sy - 1.5, 3.0, 3.0)
                // Vuelve a dejar el color claro para el siguiente tornillo.
                gc.fill = Color.rgb(180, 185, 190)
            }
        }

        // ===== 6. CALAVERA KICK BUTTKOWSKI en el lateral =====
        drawSkull(-chamberLength / 2.0, -2.0, 0.7)

        // ===== 7. LLAMAS decorativas en la base =====
        drawFlames(-chamberLength - 5.0, 0.0)

        // Restaura el sistema de coordenadas original (quita la rotación).
        gc.restore()

        // ===== 8. BASE FIJA (plataforma giratoria estilo "rampa de stunt") =====
        drawCannonBase()
    }

    /** Dibuja la calavera estilo Kick Buttowski (simplificada y cartoon). */
    private fun drawSkull(cx: Double, cy: Double, scale: Double) {
        // Tamaño base de la calavera (escalado).
        val s = 8.0 * scale
        // Relleno blanco hueso.
        gc.fill = Color.rgb(240, 240, 230)

        // --- Cráneo: un óvalo ---
        gc.fillOval(cx - s, cy - s * 1.1, s * 2.0, s * 2.2)

        // --- Ojos: dos huecos negros ---
        gc.fill = Color.BLACK
        gc.fillOval(cx - s * 0.5, cy - s * 0.3, s * 0.6, s * 0.7)  // Ojo izquierdo.
        gc.fillOval(cx + s * 0.2, cy - s * 0.3, s * 0.6, s * 0.7)  // Ojo derecho.

        // --- Nariz: un triángulo negro ---
        gc.fill = Color.BLACK
        gc.beginPath()
        gc.moveTo(cx, cy + s * 0.1)             // Vértice superior.
        gc.lineTo(cx - s * 0.25, cy + s * 0.4)  // Base izquierda.
        gc.lineTo(cx + s * 0.25, cy + s * 0.4)  // Base derecha.
        gc.closePath()
        gc.fill()

        // --- Dientes: 4 líneas verticales negras ---
        gc.stroke = Color.BLACK
        gc.lineWidth = 1.0
        for (i in 0..3) {
            val tx = cx - s * 0.5 + i * s * 0.33  // Posición horizontal de cada diente.
            gc.strokeLine(tx, cy + s * 0.6, tx, cy + s * 1.0)
        }

        // --- Cruz en la frente (estilo "peligro") ---
        gc.stroke = Color.rgb(255, 50, 50)
        gc.lineWidth = 1.5
        gc.strokeLine(cx, cy - s * 1.2, cx, cy - s * 0.8)      // Parte vertical de la cruz.
        gc.strokeLine(cx - s * 0.2, cy - s * 1.0, cx + s * 0.2, cy - s * 1.0)  // Parte horizontal.
    }

    /** Dibuja llamas estilo cartoon en la recámara. */
    private fun drawFlames(baseX: Double, baseY: Double) {
        // Capas de color de la llama: rojo, naranja y amarillo.
        val flameColors = listOf(
            Color.rgb(255, 80, 20),   // Rojo intenso (fondo).
            Color.rgb(255, 160, 20),  // Naranja (medio).
            Color.rgb(255, 230, 40)   // Amarillo (centro).
        )
        // Semilla fija para un patrón determinista de llamas.
        val random = java.util.Random(999)

        // Dibuja 5 llamas repartidas con variación aleatoria.
        for (i in 0..4) {
            // Posición de la llama con dispersión aleatoria.
            val fx = baseX - 3.0 - random.nextDouble() * 6.0
            val fy = baseY - 6.0 + (random.nextDouble() - 0.5) * 12.0
            // Alto y ancho de la llama.
            val h = 10.0 + random.nextDouble() * 8.0
            val w = 6.0 + random.nextDouble() * 4.0

            // Dibuja 3 capas concéntricas para dar sensación de fuego.
            for (layer in 0..2) {
                gc.fill = flameColors[layer]
                // Cada capa interior es más pequeña.
                val lw = w * (1.0 - layer * 0.25)
                val lh = h * (1.0 - layer * 0.2)
                // Una forma de gota enroscada hacia arriba.
                gc.beginPath()
                gc.moveTo(fx, fy)
                gc.quadraticCurveTo(fx - lw / 2.0, fy - lh / 3.0, fx, fy - lh)
                gc.quadraticCurveTo(fx + lw / 2.0, fy - lh / 3.0, fx, fy)
                gc.closePath()
                gc.fill()
            }
        }
    }

    /** Dibuja la base del cañón: una plataforma giratoria estilo rampa de stunt. */
    private fun drawCannonBase() {
        // Radios de la plataforma y del pivote central.
        val baseRadius = 32.0
        val pivotRadius = 12.0
        // Centro de la base (coincide con el pivote del cañón).
        val bx = cfg.cannonBaseX
        val by = cfg.cannonBaseY

        // --- Sombra de la plataforma ---
        gc.fill = Color.rgb(0, 0, 0, 0.3)
        gc.fillOval(bx - baseRadius + 3.0, by - baseRadius + 8.0, baseRadius * 2.0, baseRadius * 2.0)

        // --- Base de madera: círculo con gradiente marrón ---
        val woodGradient = LinearGradient(
            bx - baseRadius, by - baseRadius,
            bx + baseRadius, by + baseRadius,
            true, CycleMethod.NO_CYCLE,
            Stop(0.0, Color.rgb(130, 90, 50)),
            Stop(0.5, Color.rgb(100, 70, 40)),
            Stop(1.0, Color.rgb(70, 50, 30))
        )
        gc.fill = woodGradient
        gc.fillOval(bx - baseRadius, by - baseRadius, baseRadius * 2.0, baseRadius * 2.0)

        // --- Vetillas de madera: líneas radiales ---
        gc.stroke = Color.rgb(50, 35, 25, 0.4)
        gc.lineWidth = 1.0
        for (i in 0..6) {
            val angle = i * Math.PI / 3.0  // Espaciado de 60°.
            // Punto interno y externo de cada veta.
            val x1 = bx + Math.cos(angle) * baseRadius * 0.3
            val y1 = by + Math.sin(angle) * baseRadius * 0.3
            val x2 = bx + Math.cos(angle) * baseRadius * 0.9
            val y2 = by + Math.sin(angle) * baseRadius * 0.9
            gc.strokeLine(x1, y1, x2, y2)
        }

        // --- Anillo metálico exterior (doble contorno) ---
        gc.stroke = Color.rgb(100, 105, 110)
        gc.lineWidth = 4.0
        gc.strokeOval(bx - baseRadius, by - baseRadius, baseRadius * 2.0, baseRadius * 2.0)
        gc.stroke = Color.rgb(160, 165, 170)
        gc.lineWidth = 1.0
        gc.strokeOval(bx - baseRadius + 2.0, by - baseRadius + 2.0, baseRadius * 2.0 - 4.0, baseRadius * 2.0 - 4.0)

        // --- PIVOTE CENTRAL (mecánico) ---
        gc.fill = Color.rgb(60, 65, 70)
        gc.fillOval(bx - pivotRadius, by - pivotRadius, pivotRadius * 2.0, pivotRadius * 2.0)

        // --- Tornillos en el pivote: 6 pernos alrededor ---
        gc.fill = Color.rgb(180, 185, 190)
        for (i in 0..5) {
            val angle = i * Math.PI / 3.0
            val sx = bx + Math.cos(angle) * (pivotRadius * 0.6)
            val sy = by + Math.sin(angle) * (pivotRadius * 0.6)
            gc.fillOval(sx - 3.0, sy - 3.0, 6.0, 6.0)   // Cabeza del tornillo.
            gc.fill = Color.rgb(120, 125, 130)
            gc.fillOval(sx - 1.5, sy - 1.5, 3.0, 3.0)   // Agujero del tornillo.
            gc.fill = Color.rgb(180, 185, 190)
        }

        // --- Centro del pivote: un agujero oscuro ---
        gc.fill = Color.rgb(20, 20, 20)
        gc.fillOval(bx - 5.0, by - 5.0, 10.0, 10.0)

        // --- Marcas de grados en la base (como un transportador) ---
        gc.stroke = Color.rgb(200, 205, 210, 0.6)
        gc.lineWidth = 1.0
        // Dibuja marcas cada 10° (de 0 a 80).
        for (deg in 0..8) {
            val a = deg * 10.0  // 0, 10, 20 ... 80.
            val rad = Math.toRadians(a)
            // Desde el radio interior hasta el exterior (resta 90° porque 0° es arriba).
            val r1 = baseRadius * 0.7
            val r2 = baseRadius * 0.9
            val x1 = bx + Math.cos(rad - Math.PI / 2.0) * r1
            val y1 = by + Math.sin(rad - Math.PI / 2.0) * r1
            val x2 = bx + Math.cos(rad - Math.PI / 2.0) * r2
            val y2 = by + Math.sin(rad - Math.PI / 2.0) * r2
            gc.strokeLine(x1, y1, x2, y2)

            // --- Números en las marcas cada 20° (excepto 0 y 90) ---
            if (deg % 2 == 0 && deg > 0 && deg < 90) {
                // Posición del texto sobre la marca.
                val tx = bx + Math.cos(rad - Math.PI / 2.0) * (baseRadius * 1.15)
                val ty = by + Math.sin(rad - Math.PI / 2.0) * (baseRadius * 1.15)
                gc.fill = Color.rgb(200, 205, 210, 0.7)
                gc.font = Font.font("Monospace", FontWeight.BOLD, 9.0)
                gc.textAlign = TextAlignment.CENTER
                gc.fillText("${deg}°", tx, ty + 3.0)
            }
        }
    }
}
