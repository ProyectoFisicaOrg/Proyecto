package org.example.rendering

// --- Imports de JavaFX usados para dibujar el proyectil ---
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.scene.text.TextAlignment

/**
 * Dibuja el proyectil, su estela y el indicador de ángulo (SRP).
 *
 * Su única responsabilidad es pintar los elementos dinámicos que se
 * superponen al escenario. Recibe el [GraphicsContext] y la [SceneConfig]
 * por constructor (inyección de dependencias), y dibuja a partir de las
 * posiciones que se le pasan en cada método.
 */
class ProjectileRenderer(
    // Contexto gráfico del canvas sobre el que se dibuja.
    private val gc: GraphicsContext,
    // Configuración geométrica (se usa para el indicador de ángulo).
    private val cfg: SceneConfig,
) {

    /**
     * Dibuja el proyectil (una esfera azul) con un brillo/reflejo.
     *
     * @param x Coordenada horizontal del centro del proyectil.
     * @param y Coordenada vertical del centro del proyectil.
     */
    fun drawProjectile(x: Double, y: Double) {
        // Cuerpo principal del proyectil: un círculo azul centrado en (x, y).
        gc.fill = Color.rgb(66, 153, 225)
        gc.fillOval(x - 10.0, y - 10.0, 20.0, 20.0)

        // Brillo/reflejo: un círculo más claro y pequeño en la parte superior izquierda.
        gc.fill = Color.rgb(99, 179, 237)
        gc.fillOval(x - 12.0, y - 12.0, 8.0, 8.0)
    }

    /**
     * Dibuja la estela discontinua desde la boca del cañón hasta el proyectil.
     *
     * @param fromX, fromY Origen de la estela (boca del cañón).
     * @param toX, toY     Extremo de la estela (proyectil).
     */
    fun drawTrail(fromX: Double, fromY: Double, toX: Double, toY: Double) {
        // Color azul translúcido para la línea de la estela.
        gc.stroke = Color.rgb(66, 153, 225, 0.25)
        gc.lineWidth = 3.0
        // Línea discontinua (8 px de trazo, 8 px de espacio).
        gc.setLineDashes(8.0, 8.0)
        // Traza la línea que une cañón y proyectil.
        gc.strokeLine(fromX, fromY, toX, toY)
        // Restablece el estilo de línea discontinuo a contínuo.
        gc.setLineDashes()
    }

    /** Dibuja el arco de ángulo y la etiqueta con el ángulo actual. */
    fun drawAngleIndicator(angle: Double) {
        // Convierte el ángulo a radianes para los cálculos de posición.
        val angleRad = Math.toRadians(angle)
        // Radio del arco que se dibuja alrededor del cañón.
        val radius = 60.0
        // Centro del arco (coincide con el pivote del cañón).
        val bx = cfg.cannonBaseX
        val by = cfg.cannonBaseY

        // --- Arco discontinuo que indica el sector del ángulo ---
        gc.stroke = Color.rgb(233, 69, 96, 0.5)
        gc.lineWidth = 2.0
        gc.setLineDashes(6.0, 6.0)
        // Dibuja un arco que va desde 90° (arriba) decreciendo en `angle` grados.
        gc.strokeArc(
            bx - radius, by - radius,
            radius * 2.0, radius * 2.0,
            90.0, -angle,
            ArcType.OPEN
        )
        gc.setLineDashes()

        // --- Etiqueta de texto con el valor del ángulo ---
        // Calcula el ángulo a mitad del arco para colocar el texto en el centro.
        val labelAngle = angleRad / 2.0 - Math.PI / 2.0
        // Posición del texto, desplazada un poco hacia afuera del arco.
        val labelX = bx + Math.cos(labelAngle) * (radius + 25.0)
        val labelY = by + Math.sin(labelAngle) * (radius + 25.0)

        // Configura el relleno y la fuente del texto.
        gc.fill = Color.rgb(233, 69, 96)
        gc.font = Font.font("Monospace", FontWeight.BOLD, 14.0)
        gc.textAlign = TextAlignment.CENTER
        // Dibuja el texto del ángulo, centrado horizontalmente.
        gc.fillText("${angle.toInt()}°", labelX - 15.0, labelY + 5.0)
    }
}
