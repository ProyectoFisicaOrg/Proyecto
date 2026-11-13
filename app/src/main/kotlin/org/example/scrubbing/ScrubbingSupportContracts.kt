package org.example.scrubbing

import org.example.model.ISimulationState
import org.example.rendering.CoordinateTransformer
import org.example.rendering.ISceneConfig

/**
 * Contratos de estado e hit-testing para sesión de scrubbing (ISP + DIP).
 */
interface IScrubbingState {
    fun selectedPoint(): ISpanishKinematicView?
    fun isInteracting(): Boolean
    fun hasSelection(): Boolean
}

interface IScrubbingSession {
    fun checkEnabled(): Boolean
    fun checkDragging(): Boolean
    fun checkHovered(): Boolean
    fun selectedTrajectoryPoint(): ISpanishKinematicView?
    fun currentInspector(): ITrajectoryInspector?
    fun getState(): IScrubbingState
    fun activateSession(newInspector: ITrajectoryInspector)
    fun clearSession()
    fun updateDraggingFlag(dragging: Boolean)
    fun updateHoveredFlag(hovered: Boolean)
    fun updateSelectedPoint(point: ISpanishKinematicView?)
    fun activeInspector(): ITrajectoryInspector?
}

interface IScrubbingHitTester {
    fun isOverProjectile(canvasX: Double, canvasY: Double): Boolean
    fun isOverTarget(canvasX: Double, canvasY: Double): Boolean
    fun findCurvePointWithinThreshold(inspector: ITrajectoryInspector, canvasX: Double, canvasY: Double): ISpanishKinematicView?
}

interface IScrubbingSyncPort {
    fun projectileCoords(): Pair<Double, Double>
    fun commitSelectedPoint(point: ISpanishKinematicView)
    fun requestRepaint()
}

interface IScrubbingInitSpec {
    fun stateModel(): ISimulationState
    fun coordTransformer(): CoordinateTransformer
    fun sceneConfig(): ISceneConfig
    fun hitThreshold(): Double
    fun projectileRadius(): Double
    fun notifyPointSelected(point: ISpanishKinematicView)
    fun notifyRepaint()
}

interface IScrubbingPortsWiring {
    fun createPorts(spec: IScrubbingInitSpec): Pair<IScrubbingHitTester, IScrubbingSyncPort>
}

interface IScrubbingWiring {
    fun createController(spec: IScrubbingInitSpec): IScrubbingController
}
