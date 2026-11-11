package org.example.telemetry

import org.example.model.ITrajectoryMatrixSummary

/**
 * Proveedor ligero inyectable de [InstantaneousStateApi] conforme a DIP (SRP + DIP).
 */
class DefaultTelemetryProvider : ITelemetryFactory {

    override fun buildApi(): InstantaneousStateApi {
        return DefaultInstantaneousStateApi(EuclideanNearestNeighborSearcher())
    }
}

/**
 * Fábrica para ensamblar componentes de telemetría e inspección interactiva (SRP + DIP).
 */
class TelemetryFactory(
    searcher: TrajectoryNearestNeighborSearcher = EuclideanNearestNeighborSearcher(),
    apiBuilder: (TrajectoryNearestNeighborSearcher) -> InstantaneousStateApi = ::createDefaultApi
) : ITelemetryFactory {
    private val deps: Pair<TrajectoryNearestNeighborSearcher, (TrajectoryNearestNeighborSearcher) -> InstantaneousStateApi> =
        Pair(searcher, apiBuilder)

    override fun buildApi(): InstantaneousStateApi {
        return deps.second(deps.first)
    }

    companion object {
        private fun createDefaultApi(searcher: TrajectoryNearestNeighborSearcher): InstantaneousStateApi {
            return DefaultInstantaneousStateApi(searcher)
        }

        fun createSearcher(): TrajectoryNearestNeighborSearcher {
            return EuclideanNearestNeighborSearcher()
        }

        fun createApi(searcher: TrajectoryNearestNeighborSearcher = createSearcher()): InstantaneousStateApi {
            return createDefaultApi(searcher)
        }

        fun createInspector(matrix: ITrajectoryMatrixSummary, api: InstantaneousStateApi = createApi()): TrajectoryInspector {
            return api.createInspector(matrix)
        }
    }
}
