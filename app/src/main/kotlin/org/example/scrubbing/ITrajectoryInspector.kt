package org.example.scrubbing

import org.example.model.ITrajectoryMatrixSummary

/**
 * Contrato de inspección de trayectoria para la interacción visual (DIP + ISP).
 */
interface ITrajectoryInspector {
    fun findNearestPoint(physicsX: Double, physicsY: Double): ISpanishKinematicView
    fun findNearestPointFromCursor(cursorCanvasX: Double, cursorCanvasY: Double): ISpanishKinematicView
    fun findNearestPointByX(physicsX: Double): ISpanishKinematicView = findNearestPoint(physicsX, 0.0)
    fun getFlightSummary(): ISpanishFlightSummaryView
}

interface ISpanishInspectorSupport {
    fun inspectPoint(matrix: ITrajectoryMatrixSummary, physicsX: Double, physicsY: Double): ISpanishKinematicView
    fun inspectPointByX(matrix: ITrajectoryMatrixSummary, physicsX: Double): ISpanishKinematicView
    fun summarize(matrix: ITrajectoryMatrixSummary): ISpanishFlightSummaryView
}
