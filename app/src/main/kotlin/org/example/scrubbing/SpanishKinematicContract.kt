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

    val x: Double get() = coordX()
    val y: Double get() = coordY()
    val time: Double get() = timeValue()
    val height: Double get() = heightValue()
    val speed: Double get() = speedValue()
    val tiempo: Double get() = timeValue()
    val altura: Double get() = heightValue()
    val velocidad: Double get() = speedValue()
}
