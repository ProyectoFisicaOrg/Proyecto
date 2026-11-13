package org.example.scrubbing

import javafx.scene.canvas.Canvas

/**
 * Contrato abstracto para el controlador de scrubbing sobre la trayectoria (DIP).
 */
interface IScrubbingMouseHandler {
    fun install(canvas: Canvas)
    fun handleMousePressed(canvasX: Double, canvasY: Double): Boolean
    fun handleMouseDragged(canvasX: Double, canvasY: Double): Boolean
    fun handleMouseReleased(): Boolean
    fun handleMouseMoved(canvasX: Double, canvasY: Double): Boolean
}

interface IScrubbingController : IScrubbingMouseHandler {
    fun checkEnabled(): Boolean
    fun checkDragging(): Boolean
    fun checkHovered(): Boolean
    fun selectedTrajectoryPoint(): ISpanishKinematicView?
    fun currentInspector(): ITrajectoryInspector?
    fun currentFlightSummary(): ISpanishFlightSummaryView? = currentInspector()?.getFlightSummary()
    fun getState(): IScrubbingState
    fun enable(inspector: ITrajectoryInspector)
    fun disable()
}
