package org.example.ui

import javafx.scene.layout.HBox

/**
 * Contratos de especificación, vista flotante y controlador de distancia al blanco (DIP + ISP).
 */
interface ITargetDistanceSpec {
    fun minMeters(): Double
    fun maxMeters(): Double
    fun defaultMeters(): Double
    fun clampMeters(value: Double): Double
    fun resolveCommittedMeters(rawText: String, validator: INumericInputValidator? = null): Double
    fun formatNumber(meters: Double): String
}

interface ITargetOverlayView {
    fun textPort(): ITextInputPort
    fun updateHorizontalPosition(targetCanvasX: Double)
    fun setLocked(locked: Boolean)
    fun isLocked(): Boolean
}
