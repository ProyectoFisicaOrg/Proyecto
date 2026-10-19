package org.example.rendering

import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.shape.StrokeLineCap
import javafx.scene.shape.StrokeLineJoin

/**
 * Punto individual de la estela con coordenadas y marca temporal de creación.
 */
data class TrailPoint(
    val x: Double,
    val y: Double,
    val timestampNano: Long,
)

/**
 * Renderizador de la estela o rastro continuo del proyectil (SRP + DIP + OCP).
 *
 * Responsabilidad Única:
 *  - Almacenar los puntos del recorrido del proyectil con su timestamp exacto.
 *  - Renderizar la estela continua y desvanecerla progresivamente DESDE EL PUNTO DE PARTIDA (boca del cañón)
 *    hacia la cabeza del proyectil a partir de los 2 segundos posteriores al disparo.
 *
 * Principios SOLID:
 *  - SRP: Se encarga únicamente del almacenamiento, ciclo de vida temporal y dibujo de la estela.
 *  - OCP: Tiempos de retardo (fadeDelaySeconds), duración de desvanecimiento (fadeDurationSeconds),
 *    opacidad base y estilos son configurables por constructor.
 *  - LSP: Comportamiento determinista y compatible con implementaciones de testing.
 *  - ISP: Expone métodos específicos y mínimos (addPoint, clear, recordLaunch, getOpacity, drawPath).
 *  - DIP: [GraphicsContext] desacoplado y nullable; marcas de tiempo inyectables para pruebas unitarias sin GUI.
 *
 * @param gc Contexto gráfico del canvas (opcional para permitir pruebas unitarias sin JavaFX).
 * @param fadeDelaySeconds Tiempo en segundos antes de comenzar el desvanecimiento (default: 2.0s).
 * @param fadeDurationSeconds Duración en segundos que tarda la estela en disolverse a 0.0 (default: 1.2s).
 * @param baseAlpha Opacidad inicial del trazo (default: 0.55).
 */
class PathRenderer(
    private val gc: GraphicsContext? = null,
    val fadeDelaySeconds: Double = 2.0,
    val fadeDurationSeconds: Double = 1.2,
    val baseAlpha: Double = 0.55,
) {
    // Puntos acumulados de la estela en orden cronológico
    private val points = mutableListOf<TrailPoint>()

    // Marca temporal del momento del disparo
    private var launchTimestampNano: Long = 0L

    /** Indica si el desvanecimiento está congelado (por ejemplo, para permitir scrubbing tras el impacto). */
    var isFrozen: Boolean = false

    /** Congela el desvanecimiento para mantener la trayectoria completamente visible tras el impacto */
    fun freeze() {
        isFrozen = true
    }

    /** Descongela el desvanecimiento */
    fun unfreeze() {
        isFrozen = false
    }

    /**
     * Registra el instante exacto del disparo para iniciar el conteo del desvanecimiento temporal.
     *
     * @param nowNano Marca de tiempo en nanosegundos (por defecto System.nanoTime()).
     */
    fun recordLaunch(nowNano: Long = System.nanoTime()) {
        launchTimestampNano = nowNano
        isFrozen = false
    }

    /**
     * Agrega un nuevo punto a la trayectoria registrando la marca de tiempo actual.
     */
    fun addPoint(x: Double, y: Double, nowNano: Long = System.nanoTime()) {
        if (points.isNotEmpty()) {
            val last = points.last()
            if (kotlin.math.abs(last.x - x) < 0.5 && kotlin.math.abs(last.y - y) < 0.5) {
                return
            }
        }
        points.add(TrailPoint(x, y, nowNano))
    }

    /**
     * Limpia la trayectoria acumulada y reinicia la marca temporal del lanzamiento.
     */
    fun clear() {
        points.clear()
        launchTimestampNano = 0L
        isFrozen = false
    }

    /** Cantidad de puntos actualmente activos en la trayectoria. */
    val pointCount: Int get() = points.size

    /** Retorna una copia de los puntos acumulados como pares (x, y). */
    fun getPoints(): List<Pair<Double, Double>> = points.map { Pair(it.x, it.y) }

    /** Retorna los puntos con su metadata interna. */
    fun getTrailPoints(): List<TrailPoint> = points.toList()

    /**
     * Marca temporal efectiva del inicio del disparo.
     * Si no se invocó recordLaunch, se utiliza la marca del primer punto disponible.
     */
    private fun effectiveLaunchNano(): Long =
        if (launchTimestampNano != 0L) launchTimestampNano else (points.firstOrNull()?.timestampNano ?: 0L)

    /**
     * Calcula el progreso del desvanecimiento en el rango [0.0, 1.0]:
     *  - Si está congelado ([isFrozen]), progreso 0.0 (opacidad máxima).
     *  - t <= fadeDelaySeconds (2.0s): progreso 0.0 (opacidad máxima sin desvanecimiento).
     *  - fadeDelaySeconds < t < fadeDelaySeconds + fadeDurationSeconds: progreso proporcional de 0.0 a 1.0.
     *  - t >= fadeDelaySeconds + fadeDurationSeconds: progreso 1.0 (disuelto por completo).
     */
    fun getFadeProgress(nowNano: Long = System.nanoTime()): Double {
        if (isFrozen) return 0.0
        val launch = effectiveLaunchNano()
        if (launch == 0L) return 0.0
        val age = (nowNano - launch) / 1_000_000_000.0
        if (age <= fadeDelaySeconds) {
            return 0.0
        }
        return ((age - fadeDelaySeconds) / fadeDurationSeconds).coerceIn(0.0, 1.0)
    }

    /**
     * Retorna la opacidad general de la estela en función de la antigüedad desde el disparo:
     *  - Antigüedad <= [fadeDelaySeconds] (2.0s): opacidad máxima [baseAlpha].
     *  - Antigüedad entre 2.0s y 2.0s + [fadeDurationSeconds]: desvanecimiento suave a 0.0.
     *  - Antigüedad >= 2.0s + [fadeDurationSeconds]: disuelto (0.0).
     */
    fun getOpacity(nowNano: Long = System.nanoTime()): Double {
        val progress = getFadeProgress(nowNano)
        return (baseAlpha * (1.0 - progress)).coerceAtLeast(0.0)
    }

    /**
     * Calcula la opacidad individual de un punto en función del desvanecimiento progresivo
     * desde el punto de partida hacia el proyectil.
     */
    fun getPointAlpha(point: TrailPoint, nowNano: Long = System.nanoTime()): Double {
        val progress = getFadeProgress(nowNano)
        if (progress >= 1.0) return 0.0
        if (progress <= 0.0) return baseAlpha
        val idx = points.indexOf(point)
        if (idx < 0 || points.size <= 1) return getOpacity(nowNano)
        val u = idx.toDouble() / (points.size - 1.0)
        if (u <= progress) return 0.0
        val factor = ((u - progress) / (1.0 - progress)).coerceIn(0.0, 1.0)
        return (baseAlpha * factor).coerceIn(0.0, baseAlpha)
    }

    /**
     * Dibuja la línea continua de la trayectoria aplicando el desvanecimiento progresivo
     * que comienza a disolver el rastro DESDE EL PUNTO DE PARTIDA (boca del cañón) hacia el proyectil
     * a partir de los 2 segundos posteriores al disparo.
     *
     * Al alcanzar el tiempo de disolución total (progreso >= 1.0), purga los puntos automáticamente.
     */
    fun drawPath(nowNano: Long = System.nanoTime()) {
        val fadeProgress = getFadeProgress(nowNano)

        // Limpiar puntos si expiró por completo el tiempo de permanencia
        if (fadeProgress >= 1.0) {
            points.clear()
            return
        }

        if (points.size < 2) {
            return
        }

        val context = gc ?: return

        context.lineWidth = 3.5
        context.lineCap = StrokeLineCap.ROUND
        context.lineJoin = StrokeLineJoin.ROUND
        context.setLineDashes()

        // 1. Antes de los 2 segundos: trazo continuo de alto rendimiento (1 solo path batch, 60+ FPS sin stutter)
        if (fadeProgress <= 0.0) {
            context.stroke = Color.rgb(66, 153, 225, baseAlpha)
            context.beginPath()
            context.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                context.lineTo(points[i].x, points[i].y)
            }
            context.stroke()
            return
        }

        // 2. A partir de los 2 segundos: desvanecimiento progresivo DESDE EL PUNTO DE PARTIDA (origen)
        // Los primeros puntos emitidos en la boca del cañón se disuelven primero
        val totalSegments = points.size - 1
        for (i in 0 until totalSegments) {
            val u2 = (i + 1).toDouble() / totalSegments
            if (u2 <= fadeProgress) {
                // Segmento disuelto por completo en el origen
                continue
            }

            val uMid = (i + 0.5) / totalSegments
            val factor = ((uMid - fadeProgress) / (1.0 - fadeProgress)).coerceIn(0.0, 1.0)
            val segmentAlpha = (baseAlpha * factor).coerceIn(0.0, baseAlpha)

            if (segmentAlpha > 0.01) {
                context.stroke = Color.rgb(66, 153, 225, segmentAlpha)
                context.strokeLine(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y)
            }
        }
    }
}