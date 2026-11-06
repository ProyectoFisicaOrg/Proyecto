package org.example.ui

import javafx.scene.layout.VBox
import org.example.collision.ICollisionResult

/**
 * Contrato abstracto para el panel de controles de usuario (DIP).
 */
interface IControlPanel {
    fun build(): VBox
    fun showCollisionResult(result: ICollisionResult)
    fun showOutOfBoundsMessage()
    fun clearCollisionResult()
    fun setAnimating(animating: Boolean)
}
