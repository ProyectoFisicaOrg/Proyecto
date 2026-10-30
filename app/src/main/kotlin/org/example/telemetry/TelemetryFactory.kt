package org.example.telemetry

import org.example.model.TrajectoryMatrix

/**
 * Fábrica para ensamblar componentes de telemetría e inspección interactiva (SRP + DIP).
 */
class TelemetryFactory(
    private val searcher: TrajectoryNearestNeighborSearcher = EuclideanNearestNeighborSearcher()
) {
    /** Construye una instancia de [InstantaneousStateApi] con el buscador inyectado. */
    fun buildApi(): InstantaneousStateApi = DefaultInstantaneousStateApi(searcher)

    companion object {
        fun createSearcher(): TrajectoryNearestNeighborSearcher = EuclideanNearestNeighborSearcher()

        fun createApi(searcher: TrajectoryNearestNeighborSearcher = createSearcher()): InstantaneousStateApi =
            TelemetryFactory(searcher).buildApi()

        fun createInspector(matrix: TrajectoryMatrix, api: InstantaneousStateApi = createApi()): TrajectoryInspector =
            api.createInspector(matrix)
    }
}
