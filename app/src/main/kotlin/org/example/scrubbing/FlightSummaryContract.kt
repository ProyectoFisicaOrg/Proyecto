package org.example.scrubbing

/**
 * Contrato bilingüe para métricas finales del vuelo (ISP + DIP).
 */
interface ISpanishFlightSummaryView {
    fun flightTimeSeconds(): Double
    fun rangeMeters(): Double
    fun formattedTime(): String
    fun formattedDistance(): String
}
