package org.example.telemetry

import org.example.Position
import org.example.TrajectoryPoint
import kotlin.math.abs

/**
 * Abstracción para el algoritmo de búsqueda de vecino más cercano (Nearest Neighbor).
 *
 * Principio de Responsabilidad Única (SRP): localizar el punto de la trayectoria más próximo
 * a una coordenada dada.
 * Principio de Segregación de Interfaces (ISP): métodos precisos para consultas 2D y 1D sin
 * acoplar detalles de presentación ni lógica de negocio externa.
 * Principio Abierto/Cerrado (OCP): extensible con nuevas estrategias de búsqueda (KD-Tree,
 * búsqueda espacial, interpolación continua) sin alterar a los consumidores.
 */
interface TrajectoryNearestNeighborSearcher {

    /**
     * Encuentra el punto más cercano a una coordenada 2D (x, y) según la distancia geométrica.
     *
     * @param points Lista de puntos de la trayectoria (no vacía).
     * @param x Coordenada X buscada.
     * @param y Coordenada Y buscada.
     * @return El [TrajectoryPoint] más cercano.
     */
    fun findNearest(points: List<TrajectoryPoint>, x: Double, y: Double): TrajectoryPoint

    /**
     * Sobrecarga para buscar a partir de un objeto [Position].
     *
     * @param points Lista de puntos de la trayectoria.
     * @param position Coordenada empaquetada como [Position].
     * @return El [TrajectoryPoint] más cercano.
     */
    fun findNearest(points: List<TrajectoryPoint>, position: Position): TrajectoryPoint =
        findNearest(points, position.x, position.y)

    /**
     * Encuentra el punto más cercano a una coordenada horizontal X (proyección sobre la curva).
     *
     * @param points Lista de puntos de la trayectoria (no vacía, ordenados cronológicamente por x).
     * @param targetX Coordenada horizontal X buscada.
     * @return El [TrajectoryPoint] con coordenada x más cercana.
     */
    fun findNearestByX(points: List<TrajectoryPoint>, targetX: Double): TrajectoryPoint

    /**
     * Encuentra el índice del punto más cercano a una coordenada 2D (x, y).
     *
     * @param points Lista de puntos de la trayectoria (no vacía).
     * @param x Coordenada X buscada.
     * @param y Coordenada Y buscada.
     * @return Índice del punto más cercano en la lista.
     */
    fun findNearestIndex(points: List<TrajectoryPoint>, x: Double, y: Double): Int

    /**
     * Encuentra el índice del punto con coordenada X más cercana.
     *
     * @param points Lista de puntos de la trayectoria (no vacía, ordenados cronológicamente por x).
     * @param targetX Coordenada horizontal X buscada.
     * @return Índice del punto más cercano en la lista.
     */
    fun findNearestIndexByX(points: List<TrajectoryPoint>, targetX: Double): Int
}

/**
 * Implementación de Nearest Neighbor basada en distancia euclidiana 2D y búsqueda binaria en 1D.
 *
 * Responsabilidad Única (SRP): ejecutar el algoritmo de minimización de distancia euclidiana cuadrática
 * y búsqueda binaria O(log n) en 1D.
 * Principio de Sustitución de Liskov (LSP): cumple estrictamente el contrato de [TrajectoryNearestNeighborSearcher].
 */
class EuclideanNearestNeighborSearcher : TrajectoryNearestNeighborSearcher {

    override fun findNearest(points: List<TrajectoryPoint>, x: Double, y: Double): TrajectoryPoint {
        val index = findNearestIndex(points, x, y)
        return points[index]
    }

    override fun findNearestIndex(points: List<TrajectoryPoint>, x: Double, y: Double): Int {
        require(points.isNotEmpty()) { "La lista de puntos no puede estar vacía" }
        if (points.size == 1) return 0

        var bestIndex = 0
        var minDistanceSq = Double.MAX_VALUE

        for (i in points.indices) {
            val p = points[i]
            val dx = p.x - x
            val dy = p.y - y
            val distSq = dx * dx + dy * dy

            if (distSq < minDistanceSq) {
                minDistanceSq = distSq
                bestIndex = i
                // Coincidencia exacta (umbral de precisión flotante)
                if (distSq < 1e-14) break
            }
        }

        return bestIndex
    }

    override fun findNearestByX(points: List<TrajectoryPoint>, targetX: Double): TrajectoryPoint {
        val index = findNearestIndexByX(points, targetX)
        return points[index]
    }

    override fun findNearestIndexByX(points: List<TrajectoryPoint>, targetX: Double): Int {
        require(points.isNotEmpty()) { "La lista de puntos no puede estar vacía" }
        if (points.size == 1) return 0

        // Clamping si el valor solicitado está fuera del rango del recorrido
        if (targetX <= points.first().x) return 0
        if (targetX >= points.last().x) return points.size - 1

        // Búsqueda binaria O(log n)
        var low = 0
        var high = points.size - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midVal = points[mid].x

            when {
                midVal < targetX -> low = mid + 1
                midVal > targetX -> high = mid - 1
                else -> return mid // Coincidencia exacta
            }
        }

        val candidate1 = high.coerceIn(0, points.size - 1)
        val candidate2 = low.coerceIn(0, points.size - 1)

        val diff1 = abs(points[candidate1].x - targetX)
        val diff2 = abs(points[candidate2].x - targetX)

        return if (diff1 <= diff2) candidate1 else candidate2
    }
}
