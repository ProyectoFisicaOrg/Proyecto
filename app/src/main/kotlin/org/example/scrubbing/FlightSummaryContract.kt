package org.example.scrubbing

/**
 * Contrato bilingüe para métricas finales del vuelo (ISP + DIP).
 */
interface ISpanishFlightSummaryView {
    fun flightTimeSeconds(): Double
    fun rangeMeters(): Double
    fun formattedTime(): String
    fun formattedDistance(): String

    val totalFlightTime: Double get() = flightTimeSeconds()
    val totalDistance: Double get() = rangeMeters()
    val tiempoTotalCaida: Double get() = flightTimeSeconds()
    val distanciaRecorrida: Double get() = rangeMeters()
}
