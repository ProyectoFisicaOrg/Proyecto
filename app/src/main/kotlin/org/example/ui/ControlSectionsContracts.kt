package org.example.ui

import javafx.scene.control.Button
import javafx.scene.layout.VBox
import org.example.collision.ICollisionResult
import org.example.model.ISimulationState

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

interface ISliderRowWiring {
    fun bindRow(factory: IControlWidgetFactory, spec: INumericParameterSpec, initial: Double, onCommit: (Double) -> Unit): Pair<VBox, IParameterControlBinding>
}

interface IControlInitSpec {
    fun stateModel(): ISimulationState
    fun triggerFire()
    fun triggerAngleChange()
    fun triggerReset()
    fun triggerRandomizeTarget()
}

interface IControlSectionsWiring {
    fun createPair(spec: IControlInitSpec, factory: IControlWidgetFactory): Pair<ISlidersSection, IActionButtonsSection>
}

interface IControlPanelWiring {
    fun assemble(spec: IControlInitSpec): IControlPanel
}
