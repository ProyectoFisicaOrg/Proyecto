package org.example.rendering

import javafx.scene.canvas.GraphicsContext

/**
 * Contratos de elementos gráficos elementales del escenario natural (ISP + DIP).
 */
interface ICanvasDrawableElement {
    fun drawOn(gc: GraphicsContext)
}

interface ISkyBackdropPainter {
    fun renderSkyLayers()
}

interface IGroundFoliagePainter {
    fun renderFoliageLayers()
    fun renderGroundLine()
}
