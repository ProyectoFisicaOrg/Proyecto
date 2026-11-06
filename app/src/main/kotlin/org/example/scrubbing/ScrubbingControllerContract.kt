package org.example.scrubbing

import javafx.scene.canvas.Canvas

/**
 * Contrato abstracto para el controlador de scrubbing sobre la trayectoria (DIP).
 */
interface IScrubbingController {
    fun checkEnabled(): Boolean
    fun checkDragging(): Boolean
    fun checkHovered(): Boolean
    fun selectedTrajectoryPoint(): ISpanishKinematicView?
    fun currentInspector(): ITrajectoryInspector?
    fun getState(): IScrubbingState
    fun enable(inspector: ITrajectoryInspector)
    fun disable()
    fun install(canvas: Canvas)
    fun handleMousePressed(canvasX: Double, canvasY: Double): Boolean
    fun handleMouseDragged(canvasX: Double, canvasY: Double): Boolean
    fun handleMouseReleased(): Boolean
    fun handleMouseMoved(canvasX: Double, canvasY: Double): Boolean

    val isEnabled: Boolean get() = checkEnabled()
    val isDragging: Boolean get() = checkDragging()
    val isHovered: Boolean get() = checkHovered()
    val currentPoint: ISpanishKinematicView? get() = selectedTrajectoryPoint()
    val inspector: ITrajectoryInspector? get() = currentInspector()
    val flightSummary: ISpanishFlightSummaryView? get() = currentInspector()?.getFlightSummary()
}
