package org.example.rendering

import javafx.scene.canvas.GraphicsContext

/**
 * Contratos de punto de estela y sub-componentes temporales/gráficos (ISP + DIP).
 */
interface ITrailPoint {
    fun coordX(): Double
    fun coordY(): Double
    fun createdAtNano(): Long
    fun isVeryCloseTo(otherX: Double, otherY: Double): Boolean
}

interface ITrailComponentsFactory {
    fun createPainter(gc: GraphicsContext?, baseAlpha: Double): ITrailStrokePainter
    fun createClock(fadeDelaySeconds: Double, fadeDurationSeconds: Double): ITrailFadeClock
}
