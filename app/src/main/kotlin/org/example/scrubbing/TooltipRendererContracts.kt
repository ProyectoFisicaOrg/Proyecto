package org.example.scrubbing

import javafx.scene.canvas.GraphicsContext

/**
 * Contratos de renderizado de tooltip flotante y feedback visual del proyectil (ISP + DIP).
 */
interface ITooltipRenderer {
    fun calculateTooltipBounds(targetCanvasX: Double, targetCanvasY: Double, canvasWidth: Double, canvasHeight: Double): ITooltipBounds
    fun formatLines(point: ISpanishKinematicView): List<String>
    fun render(gc: GraphicsContext, point: ISpanishKinematicView, anchor: ITooltipAnchor)
}

interface IInteractiveProjectileRenderer {
    fun drawInteractiveEffects(gc: GraphicsContext, effectState: IInteractiveEffectState)
}
