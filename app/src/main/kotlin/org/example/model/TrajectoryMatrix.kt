package org.example.model

import org.example.TrajectoryPoint

/**
 * Matriz de trayectoria: contenedor inmutable de los puntos del recorrido del proyectil.
 *
 * Responsabilidad Única (SRP): solo almacena y expone los datos de la trayectoria.
 * No genera puntos (eso lo hace el generador), no busca ni detecta colisiones
 * (eso lo hacen el buscador y el detector).
 *
 * @param points Puntos ordenados cronológicamente (x creciente)
 * @param timeOfFlight Tiempo de vuelo total en segundos
 * @param range Alcance horizontal total en metros
 * @param maxHeight Altura máxima alcanzada en metros
 */
class TrajectoryMatrix(
    val points: List<TrajectoryPoint>,
    val timeOfFlight: Double,
    val range: Double,
    val maxHeight: Double
) {
    init {
        require(points.isNotEmpty()) { "La matriz no puede estar vacía" }
        require(timeOfFlight >= 0) { "El tiempo de vuelo no puede ser negativo" }
        require(range >= 0) { "El alcance no puede ser negativo" }
        require(maxHeight >= 0) { "La altura máxima no puede ser negativa" }
    }

    /** Cantidad de puntos en la matriz */
    val size: Int get() = points.size

    /** Primer punto (lanzamiento) */
    val launchPoint: TrajectoryPoint get() = points.first()

    /** Último punto (impacto en suelo) */
    val impactPoint: TrajectoryPoint get() = points.last()

    /**
     * Subconjunto de puntos de la trayectoria (clamps a los límites).
     *
     * @param fromIndex Índice inicial (inclusivo)
     * @param toIndex Índice final (exclusivo)
     * @return Lista de puntos en el rango
     */
    fun subMatrix(fromIndex: Int, toIndex: Int): List<TrajectoryPoint> {
        val safeFrom = fromIndex.coerceIn(0, points.size)
        val safeTo = toIndex.coerceIn(0, points.size)
        return points.subList(safeFrom, safeTo)
    }

    /**
     * Puntos que están por encima de una altura mínima.
     *
     * @param minHeight Altura mínima en metros
     * @return Lista de puntos por encima de minHeight
     */
    fun pointsAbove(minHeight: Double): List<TrajectoryPoint> {
        return points.filter { it.y >= minHeight }
    }

    override fun toString(): String {
        return buildString {
            appendLine("=== Matriz de Trayectoria ===")
            appendLine("Puntos: ${points.size}")
            appendLine("Tiempo de vuelo: %.4f s".format(timeOfFlight))
            appendLine("Alcance: %.2f m".format(range))
            appendLine("Altura máxima: %.2f m".format(maxHeight))
            appendLine("Punto de lanzamiento: (%.2f, %.2f)".format(launchPoint.x, launchPoint.y))
            appendLine("Punto de impacto: (%.2f, %.2f)".format(impactPoint.x, impactPoint.y))
        }
    }
}
