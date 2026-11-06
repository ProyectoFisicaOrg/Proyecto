package org.example

import javafx.scene.canvas.Canvas
import javafx.scene.layout.Pane
import org.example.model.ISimulationState
import org.example.model.ITrajectoryMatrixSummary
import org.example.rendering.CoordinateTransformer
import org.example.rendering.ISceneConfig

/**
 * Contrato abstracto del espacio de trabajo visual e interactivo de la escena (ISP + DIP).
 */
interface ISimulationWorkspace {
    fun stateModel(): ISimulationState
    fun sceneConfig(): ISceneConfig
    fun coordTransformer(): CoordinateTransformer
    fun canvasLayer(): Pane
    fun renderScene()
    fun recordTrailPoint(x: Double, y: Double)
    fun randomizeTarget()
    fun installOn(canvas: Canvas)
    fun prepareForLaunch()
    fun resetInteractionAndTrail()
    fun onFlightFinishedWithImpact(matrix: ITrajectoryMatrixSummary)
}
