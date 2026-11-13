package org.example.scrubbing

import org.example.model.ITrajectoryMatrixSummary
import org.example.rendering.CoordinateTransformer

/**
 * Adaptador que implementa [ITrajectoryInspector] desacoplado mediante interfaces (Adapter + DIP + SRP).
 */
class PhysicsTrajectoryInspectorAdapter(
    private val matrixRef: ITrajectoryMatrixSummary,
    private val transformer: CoordinateTransformer,
    private val support: ISpanishInspectorSupport
) : ITrajectoryInspector {

    constructor(
        matrixRef: ITrajectoryMatrixSummary,
        transformer: CoordinateTransformer,
        supportProvider: () -> ISpanishInspectorSupport = ::DefaultSpanishInspectorSupport
    ) : this(matrixRef, transformer, supportProvider())

    fun boundMatrix(): ITrajectoryMatrixSummary {
        return matrixRef
    }

    override fun findNearestPoint(physicsX: Double, physicsY: Double): ISpanishKinematicView {
        val tx = if (transformer.toCanvasDistance(1.0) >= 0.0) physicsX else physicsX
        return support.inspectPoint(matrixRef, tx, physicsY)
    }

    override fun findNearestPointFromCursor(cursorCanvasX: Double, cursorCanvasY: Double): ISpanishKinematicView {
        val physicsX = transformer.toPhysicsX(cursorCanvasX)
        val physicsY = transformer.toPhysicsY(cursorCanvasY)
        return support.inspectPoint(matrixRef, physicsX, physicsY)
    }

    override fun findNearestPointByX(physicsX: Double): ISpanishKinematicView {
        val tx = if (transformer.toCanvasDistance(1.0) >= 0.0) physicsX else physicsX
        return support.inspectPointByX(matrixRef, tx)
    }

    override fun getFlightSummary(): ISpanishFlightSummaryView {
        return support.summarize(matrixRef)
    }
}
