package org.example.telemetry

import org.example.Position
import org.example.TrajectoryPoint
import org.example.model.TrajectoryMatrix
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Inspector interactivo acoplado a una trayectoria concreta.
 *
 * Especialmente diseñado para la interacción fluida con mouse (Scrubbing e inspección HUD en Dev 2):
 * se vincula a la [TrajectoryMatrix] del lanzamiento y permite consultar el estado instantáneo
 * en cada evento de movimiento del mouse sin reenviar la matriz en cada frame.
 *
 * Principio de Responsabilidad Única (SRP): consultar telemetría sobre una trayectoria dada.
 */
interface TrajectoryInspector {
    /** La matriz de trayectoria vinculada a este inspector. */
    val matrix: TrajectoryMatrix

    /**
     * Consulta el estado instantáneo en la coordenada 2D (x, y) más cercana sobre la trayectoria.
     *
     * @param x Coordenada horizontal (en metros).
     * @param y Coordenada vertical (en metros).
     * @return [InstantaneousState] correspondiente.
     */
    fun inspect(x: Double, y: Double): InstantaneousState

    /**
     * Sobrecarga para consultar usando un objeto [Position].
     *
     * @param position Coordenada empaquetada.
     * @return [InstantaneousState] correspondiente.
     */
    fun inspect(position: Position): InstantaneousState = inspect(position.x, position.y)

    /**
     * Consulta el estado instantáneo usando una coordenada horizontal X (proyección sobre la curva).
     *
     * @param x Coordenada horizontal (en metros).
     * @return [InstantaneousState] correspondiente.
     */
    fun inspectByX(x: Double): InstantaneousState
}

/**
 * Contrato de la API de Intersección y Estado Instantáneo (Issue 3.1).
 *
 * Principio de Inversión de Dependencias (DIP): desacopla a los consumidores (HUD y scrubbing)
 * de las implementaciones del algoritmo de búsqueda.
 * Principio de Responsabilidad Única (SRP): proveer la telemetría cinemática (t, y, v)
 * en el punto más cercano a una coordenada dada sobre la curva.
 * Principio de Segregación de Interfaces (ISP): expone métodos concisos tanto para [TrajectoryMatrix]
 * como para listas de [TrajectoryPoint].
 */
interface InstantaneousStateApi {

    /**
     * Obtiene el estado instantáneo a partir de una coordenada 2D (x, y) sobre la trayectoria.
     *
     * @param matrix Matriz de trayectoria generada tras el disparo.
     * @param x Coordenada X (metros).
     * @param y Coordenada Y (metros).
     * @return [InstantaneousState] con tiempo transcurrido, altura y velocidad escalar v = sqrt(vx^2 + vy^2).
     */
    fun getInstantaneousState(matrix: TrajectoryMatrix, x: Double, y: Double): InstantaneousState

    /**
     * Sobrecarga que recibe un [Position].
     *
     * @param matrix Matriz de trayectoria generada.
     * @param position Coordenada empaquetada como [Position].
     * @return [InstantaneousState] más cercano.
     */
    fun getInstantaneousState(matrix: TrajectoryMatrix, position: Position): InstantaneousState =
        getInstantaneousState(matrix, position.x, position.y)

    /**
     * Obtiene el estado instantáneo a partir de una coordenada horizontal X sobre la curva.
     *
     * @param matrix Matriz de trayectoria generada.
     * @param x Coordenada X sobre la curva.
     * @return [InstantaneousState] correspondiente al punto con X más cercano.
     */
    fun getInstantaneousStateByX(matrix: TrajectoryMatrix, x: Double): InstantaneousState

    /**
     * Obtiene el estado instantáneo operando directamente sobre una lista de [TrajectoryPoint].
     *
     * @param points Lista de puntos de la trayectoria.
     * @param x Coordenada X.
     * @param y Coordenada Y.
     * @return [InstantaneousState] correspondiente.
     */
    fun getInstantaneousState(points: List<TrajectoryPoint>, x: Double, y: Double): InstantaneousState

    /**
     * Obtiene el estado instantáneo operando sobre una lista de [TrajectoryPoint] por coordenada X.
     *
     * @param points Lista de puntos de la trayectoria.
     * @param x Coordenada X.
     * @return [InstantaneousState] correspondiente.
     */
    fun getInstantaneousStateByX(points: List<TrajectoryPoint>, x: Double): InstantaneousState

    /**
     * Crea un [TrajectoryInspector] vinculado a la matriz provista para inspección continua.
     *
     * @param matrix Matriz de trayectoria.
     * @return [TrajectoryInspector] vinculado a la matriz.
     */
    fun createInspector(matrix: TrajectoryMatrix): TrajectoryInspector
}

/**
 * Implementación estándar de [InstantaneousStateApi].
 *
 * Principio de Responsabilidad Única (SRP): orquestar la búsqueda del punto más cercano
 * usando la estrategia inyectada y computar los valores de telemetría instantáneos.
 * Principio de Inversión de Dependencias (DIP): depende de la abstracción [TrajectoryNearestNeighborSearcher].
 */
class DefaultInstantaneousStateApi(
    private val searcher: TrajectoryNearestNeighborSearcher = EuclideanNearestNeighborSearcher()
) : InstantaneousStateApi {

    override fun getInstantaneousState(matrix: TrajectoryMatrix, x: Double, y: Double): InstantaneousState =
        getInstantaneousState(matrix.points, x, y)

    override fun getInstantaneousStateByX(matrix: TrajectoryMatrix, x: Double): InstantaneousState =
        getInstantaneousStateByX(matrix.points, x)

    override fun getInstantaneousState(points: List<TrajectoryPoint>, x: Double, y: Double): InstantaneousState {
        require(points.isNotEmpty()) { "La lista de puntos de trayectoria no puede estar vacía" }
        val nearestPoint = searcher.findNearest(points, x, y)
        val distance = hypot(nearestPoint.x - x, nearestPoint.y - y)
        return InstantaneousState.fromTrajectoryPoint(nearestPoint, distanceToQuery = distance)
    }

    override fun getInstantaneousStateByX(points: List<TrajectoryPoint>, x: Double): InstantaneousState {
        require(points.isNotEmpty()) { "La lista de puntos de trayectoria no puede estar vacía" }
        val nearestPoint = searcher.findNearestByX(points, x)
        val distance = abs(nearestPoint.x - x)
        return InstantaneousState.fromTrajectoryPoint(nearestPoint, distanceToQuery = distance)
    }

    override fun createInspector(matrix: TrajectoryMatrix): TrajectoryInspector =
        BoundTrajectoryInspector(matrix, this)
}

/**
 * Implementación concreta de [TrajectoryInspector] vinculada a una [TrajectoryMatrix].
 *
 * Responsabilidad Única (SRP): mantener la referencia a la matriz y delegar la consulta
 * a la API de telemetría.
 */
class BoundTrajectoryInspector(
    override val matrix: TrajectoryMatrix,
    private val api: InstantaneousStateApi
) : TrajectoryInspector {

    override fun inspect(x: Double, y: Double): InstantaneousState =
        api.getInstantaneousState(matrix, x, y)

    override fun inspectByX(x: Double): InstantaneousState =
        api.getInstantaneousStateByX(matrix, x)
}
