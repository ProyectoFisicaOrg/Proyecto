package org.example.trajectory

import org.example.TrajectoryPoint

/**
 * Abstracción de búsqueda de puntos dentro de una trayectoria.
 *
 * Responsabilidad Única (SRP): localizar puntos sobre la curva.
 * Principio de Segregación de Interfaces (ISP): interfaz específica de búsqueda,
 * separada de la generación y de la detección de colisiones.
 */
interface TrajectorySearcher {
    /**
     * Encuentra el índice del punto cuya x es más cercana al valor dado.
     *
     * @param points Puntos de la trayectoria (ordenados por x creciente)
     * @param targetX Coordenada x a buscar
     * @return Índice del punto más cercano
     */
    fun findClosestIndexByX(points: List<TrajectoryPoint>, targetX: Double): Int

    /**
     * Obtiene el punto más cercano a una coordenada x dada.
     *
     * @param points Puntos de la trayectoria
     * @param targetX Coordenada x a buscar
     * @return TrajectoryPoint más cercano
     */
    fun findClosestByX(points: List<TrajectoryPoint>, targetX: Double): TrajectoryPoint
}

/**
 * Búsqueda por búsqueda binaria (O(log n)).
 *
 * Responsabilidad Única (SRP): implementar la estrategia de búsqueda binaria.
 */
class BinaryTrajectorySearcher : TrajectorySearcher {

    override fun findClosestIndexByX(points: List<TrajectoryPoint>, targetX: Double): Int {
        if (points.isEmpty()) return 0

        var low = 0
        var high = points.size - 1

        while (low < high) {
            val mid = (low + high) / 2
            if (points[mid].x < targetX) {
                low = mid + 1
            } else {
                high = mid
            }
        }

        // Comparar low y low-1 para encontrar el más cercano
        if (low > 0) {
            val diffLow = kotlin.math.abs(points[low].x - targetX)
            val diffPrev = kotlin.math.abs(points[low - 1].x - targetX)
            return if (diffPrev < diffLow) low - 1 else low
        }

        return low
    }

    override fun findClosestByX(points: List<TrajectoryPoint>, targetX: Double): TrajectoryPoint {
        return points[findClosestIndexByX(points, targetX)]
    }
}
