package org.example.ui

import javafx.scene.control.Button
import javafx.scene.layout.VBox
import org.example.collision.ICollisionResult

/**
 * Contratos de las sub-secciones del panel de controles (ISP + DIP).
 */
interface ISlidersSection {
    fun buildRows(): List<VBox>
    fun setDisabled(disabled: Boolean)
}

interface IStatusBannerPresenter {
    fun buildCard(): VBox
    fun showCollision(result: ICollisionResult)
    fun showOutOfBounds()
    fun showInFlight()
}

interface IActionButtonsSection {
    fun buildButtons(): List<Button>
    fun setAnimating(animating: Boolean)
}
