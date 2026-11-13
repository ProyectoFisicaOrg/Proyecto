package org.example.scrubbing

import javafx.scene.canvas.GraphicsContext

/**
 * Contratos de estado de efecto y renderizadores HUD/Tooltip/Efectos de scrubbing (ISP + DIP).
 */
interface IInteractiveEffectState {
    fun isActive(): Boolean
    fun isDraggingNow(): Boolean
    fun centerX(): Double
    fun centerY(): Double
    fun projectileRadius(): Double
}

interface IHudRenderer {
    fun checkVisible(): Boolean
    fun formatLines(summary: ISpanishFlightSummaryView): List<String>
    fun render(gc: GraphicsContext, summary: ISpanishFlightSummaryView)
}
