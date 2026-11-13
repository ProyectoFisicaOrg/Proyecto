package org.example.scrubbing

/**
 * Contrato de vista cinemática bilingüe para un punto discreto de la trayectoria (ISP + DIP).
 */
interface ISpanishKinematicView {
    fun coordX(): Double
    fun coordY(): Double
    fun timeValue(): Double
    fun heightValue(): Double
    fun speedValue(): Double
    fun formatTelemetry(): String
}
