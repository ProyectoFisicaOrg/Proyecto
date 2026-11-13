package org.example.rendering

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import org.example.model.ISimulationState

/**
 * Contratos visuales y de arrastre del objetivo en el suelo (ISP + DIP).
 */
interface ITargetVisualFlags {
    fun isAnimating(): Boolean
    fun isHit(): Boolean
    fun isDragging(): Boolean
    fun outerRingColor(): Color
}

interface ITargetCanvasPainter {
    fun draw(x: Double, flags: ITargetVisualFlags)
}

interface ITargetDragInteraction {
    fun checkDragging(): Boolean
    fun validXRange(targetRadius: Double): ClosedFloatingPointRange<Double>
    fun applyTargetX(newX: Double)
    fun cancel()
    fun install(targetRadius: Double)
}

interface ITargetInitSpec {
    fun graphics(): GraphicsContext
    fun canvasRef(): Canvas
    fun sceneConfig(): ISceneConfig
    fun stateModel(): ISimulationState
    fun notifyMoved(newX: Double)
    fun notifyRepaint()
    fun customRandomizer(): ITargetRandomizer?
}

interface ITargetComponentsBundle {
    fun drawAt(x: Double, animating: Boolean, isHit: Boolean)
    fun startPulse()
    fun stopPulse()
    fun cancelDrag()
    fun validRange(radiusPx: Double): ClosedFloatingPointRange<Double>
    fun applyRandomX(radiusPx: Double, overrideRandomizer: ITargetRandomizer?)
    fun installDrag(radiusPx: Double)
}

interface ITargetVisualWiring {
    fun createPainter(spec: ITargetInitSpec): ITargetCanvasPainter
}

interface ITargetInteractionWiring {
    fun createDragAndPulse(spec: ITargetInitSpec): Pair<ITargetDragInteraction, IPulseController>
}

interface ITargetBundleWiring {
    fun build(spec: ITargetInitSpec): ITargetComponentsBundle
}
