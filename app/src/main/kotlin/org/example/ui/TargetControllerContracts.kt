package org.example.ui

/**
 * Contratos de sincronización manual y coordinación de interacción con el blanco (DIP + ISP).
 */
interface ITargetDistanceController {
    fun bind()
    fun syncFromCanvasX(targetCanvasX: Double)
    fun prepareForLaunch()
    fun unlockForEditing()
    fun commitText()
}

interface ITargetInteractionCoordinator {
    fun install()
    fun randomize()
    fun prepareForLaunch()
    fun unlockEditing()
}
