package org.example.telemetry

import org.example.model.ITrajectoryMatrixSummary

/**
 * Proveedor ligero inyectable de [InstantaneousStateApi] conforme a DIP (SRP + DIP).
 */
object DefaultTelemetryProvider : ITelemetryFactory {

    override fun buildApi(): InstantaneousStateApi {
        return createApiWithSearcher(createSearcher())
    }

    fun createSearcher(): TrajectoryNearestNeighborSearcher {
        return EuclideanNearestNeighborSearcher()
    }

    fun createApiWithSearcher(searcher: TrajectoryNearestNeighborSearcher): InstantaneousStateApi {
        return DefaultInstantaneousStateApi(searcher, ::BoundTrajectoryInspector)
    }
}

/**
 * Fábrica para ensamblar componentes de telemetría e inspección interactiva (SRP + DIP).
 */
class TelemetryFactory(
    searcher: TrajectoryNearestNeighborSearcher = DefaultTelemetryProvider.createSearcher(),
    apiBuilder: (TrajectoryNearestNeighborSearcher) -> InstantaneousStateApi = DefaultTelemetryProvider::createApiWithSearcher
) : ITelemetryFactory {
    private val deps: Pair<TrajectoryNearestNeighborSearcher, (TrajectoryNearestNeighborSearcher) -> InstantaneousStateApi> =
        Pair(searcher, apiBuilder)

    override fun buildApi(): InstantaneousStateApi {
        return deps.second(deps.first)
    }

    companion object {
        fun createSearcher(): TrajectoryNearestNeighborSearcher {
            return DefaultTelemetryProvider.createSearcher()
        }

        fun createApi(searcher: TrajectoryNearestNeighborSearcher = createSearcher()): InstantaneousStateApi {
            return DefaultTelemetryProvider.createApiWithSearcher(searcher)
        }

        fun createInspector(matrix: ITrajectoryMatrixSummary, api: InstantaneousStateApi = createApi()): TrajectoryInspector {
            return api.createInspector(matrix)
        }
    }
}
