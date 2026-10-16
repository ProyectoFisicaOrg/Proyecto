package org.example.rendering

/**
 * Abstracción para transformar coordenadas entre el modelo físico (metros)
 * y el espacio gráfico del canvas (píxeles) (SRP + DIP).
 *
 * Responsabilidad Única: Encapsula exclusivamente las fórmulas
 * de conversión matemática de coordenadas y escalado.
 * Inversión de Dependencias: Los módulos de renderizado y animación
 * dependen de esta interfaz y no de constantes mágicas embebidas.
 */
interface CoordinateTransformer {
    /** Convierte una coordenada X del modelo físico (metros) a píxeles de canvas. */
    fun toCanvasX(physicsX: Double): Double

    /** Convierte una coordenada Y del modelo físico (metros) a píxeles de canvas. */
    fun toCanvasY(physicsY: Double): Double

    /** Convierte una coordenada X del canvas (píxeles) a metros del modelo físico. */
    fun toPhysicsX(canvasX: Double): Double

    /** Convierte una coordenada Y del canvas (píxeles) a metros del modelo físico. */
    fun toPhysicsY(canvasY: Double): Double

    /** Convierte una distancia escalar de metros a píxeles. */
    fun toCanvasDistance(physicsMeters: Double): Double

    /** Convierte una distancia escalar de píxeles a metros. */
    fun toPhysicsDistance(canvasPixels: Double): Double
}

/**
 * Implementación estándar de transformación de coordenadas ortogonales.
 *
 * Sistema del modelo físico:
 *  - Origen (0, 0) en la base del cañón sobre el suelo.
 *  - Eje X positivo hacia la derecha (alcance en metros).
 *  - Eje Y positivo hacia arriba (altura en metros).
 *
 * Sistema del canvas (JavaFX):
 *  - Origen (0, 0) en la esquina superior izquierda.
 *  - Eje X positivo hacia la derecha.
 *  - Eje Y positivo hacia abajo.
 *
 * @param cfg Configuración geométrica del escenario (posiciones de base y suelo).
 * @param pixelsPerMeter Factor de escala (píxeles por cada metro físico).
 */
class DefaultCoordinateTransformer(
    private val cfg: SceneConfig,
    val pixelsPerMeter: Double = 2.5
) : CoordinateTransformer {

    override fun toCanvasX(physicsX: Double): Double {
        return cfg.cannonBaseX + physicsX * pixelsPerMeter
    }

    override fun toCanvasY(physicsY: Double): Double {
        return cfg.cannonBaseY - physicsY * pixelsPerMeter
    }

    override fun toPhysicsX(canvasX: Double): Double {
        return (canvasX - cfg.cannonBaseX) / pixelsPerMeter
    }

    override fun toPhysicsY(canvasY: Double): Double {
        return (cfg.cannonBaseY - canvasY) / pixelsPerMeter
    }

    override fun toCanvasDistance(physicsMeters: Double): Double {
        return physicsMeters * pixelsPerMeter
    }

    override fun toPhysicsDistance(canvasPixels: Double): Double {
        return canvasPixels / pixelsPerMeter
    }
}
