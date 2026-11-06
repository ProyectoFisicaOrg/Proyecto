package org.example.scrubbing

import javafx.scene.canvas.Canvas

/**
 * Contratos de estado e hit-testing para sesión de scrubbing (ISP + DIP).
 */
interface IScrubbingState {
    fun selectedPoint(): ISpanishKinematicView?
    fun isInteracting(): Boolean
    fun hasSelection(): Boolean

    val currentPoint: ISpanishKinematicView? get() = selectedPoint()
}

interface IScrubbingHitTester {
    fun isOverProjectile(canvasX: Double, canvasY: Double): Boolean
    fun isOverTarget(canvasX: Double, canvasY: Double): Boolean
    fun findCurvePointWithinThreshold(inspector: ITrajectoryInspector, canvasX: Double, canvasY: Double): ISpanishKinematicView?
}
