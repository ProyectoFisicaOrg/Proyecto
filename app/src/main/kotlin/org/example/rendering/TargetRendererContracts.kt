package org.example.rendering

import javafx.scene.canvas.GraphicsContext

/**
 * Contrato para el renderizador del objetivo en el suelo (ISP + DIP).
 */
interface ITargetRenderer {
    fun radiusPixels(): Double
    fun drawTarget(x: Double)
    fun startPulse()
    fun stopPulse()
    fun canMoveTarget(): Boolean
    fun cancelDrag()
    fun getValidXRange(): ClosedFloatingPointRange<Double>
    fun randomizePosition(targetRandomizer: ITargetRandomizer? = null): Boolean
    fun installDrag()
}
