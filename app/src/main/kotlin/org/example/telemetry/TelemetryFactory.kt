package org.example.telemetry

import org.example.Position
import org.example.TrajectorySimulationFactory
import org.example.model.TrajectoryMatrix

/**
 * Fábrica para ensamblar componentes de telemetría e inspección interactiva (Milestone 3, Issue 3.1).
 *
 * Principio de Responsabilidad Única (SRP): punto centralizado de creación e inyección
 * de dependencias para el módulo de telemetría.
 * Principio de Inversión de Dependencias (DIP): expone abstracciones ([InstantaneousStateApi],
 * [TrajectoryNearestNeighborSearcher], [TrajectoryInspector]) desacoplando a los clientes
 * de las clases concretas.
 */
class TelemetryFactory private constructor() {
    companion object {

    /**
     * Crea un buscador de vecino más cercano (Nearest Neighbor) por defecto.
     */
    fun createSearcher(): TrajectoryNearestNeighborSearcher =
        EuclideanNearestNeighborSearcher()

    /**
     * Crea una instancia de la API de telemetría e inspección instantánea.
     *
     * @param searcher Estrategia de búsqueda inyectada (DIP).
     */
    fun createApi(
        searcher: TrajectoryNearestNeighborSearcher = createSearcher()
    ): InstantaneousStateApi = DefaultInstantaneousStateApi(searcher)

    /**
     * Crea un inspector de trayectoria vinculado a una matriz dada (ideal para Dev 2 / scrubbing).
     *
     * @param matrix Matriz de puntos de la simulación.
     * @param api Instancia de la API de telemetría.
     */
    fun createInspector(
        matrix: TrajectoryMatrix,
        api: InstantaneousStateApi = createApi()
    ): TrajectoryInspector = api.createInspector(matrix)
    }
}

// ─── Funciones de Extensión para integración idiomática en Kotlin ─────────────

/**
 * Extensión para crear la API de telemetría desde [TrajectorySimulationFactory],
 * integrando el Milestone 3 con los Milestones anteriores sin alterar los archivos existentes.
 */
fun TrajectorySimulationFactory.Companion.createInstantaneousStateApi(
    searcher: TrajectoryNearestNeighborSearcher = TelemetryFactory.createSearcher()
): InstantaneousStateApi = TelemetryFactory.createApi(searcher)

/**
 * Extensión para crear un [TrajectoryInspector] directamente desde [TrajectorySimulationFactory].
 */
fun TrajectorySimulationFactory.Companion.createTrajectoryInspector(
    matrix: TrajectoryMatrix
): TrajectoryInspector = TelemetryFactory.createInspector(matrix)

/**
 * Extensión en [TrajectoryMatrix] para consultar el estado instantáneo directamente en (x, y).
 */
fun TrajectoryMatrix.getInstantaneousState(
    x: Double,
    y: Double,
    api: InstantaneousStateApi = TelemetryFactory.createApi()
): InstantaneousState = api.getInstantaneousState(this, x, y)

/**
 * Extensión en [TrajectoryMatrix] para consultar el estado instantáneo a partir de un [Position].
 */
fun TrajectoryMatrix.getInstantaneousState(
    position: Position,
    api: InstantaneousStateApi = TelemetryFactory.createApi()
): InstantaneousState = api.getInstantaneousState(this, position)

/**
 * Extensión en [TrajectoryMatrix] para consultar el estado instantáneo usando la coordenada X.
 */
fun TrajectoryMatrix.getInstantaneousStateByX(
    x: Double,
    api: InstantaneousStateApi = TelemetryFactory.createApi()
): InstantaneousState = api.getInstantaneousStateByX(this, x)
