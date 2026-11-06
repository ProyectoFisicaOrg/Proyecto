package org.example.ui

import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import org.example.rendering.ISceneConfig

/**
 * Pastilla interactiva JavaFX con [TextField] editable superpuesta debajo de la diana en el escenario (SRP + DIP).
 */
class JavaFxTargetOverlayPill(
    cfg: ISceneConfig,
    initialText: String = "264.00",
    portFactory: (TextField) -> ITextInputPort = ::JavaFxTextInputAdapter
) : ITargetOverlayView {
    private val slot: MutableList<Triple<Pair<ISceneConfig, Double>, Pair<TextField, HBox>, Pair<ITextInputPort, Boolean>>> =
        createInitialSlot(cfg, initialText, portFactory)

    private fun createInitialSlot(cfg: ISceneConfig, initialText: String, portFactory: (TextField) -> ITextInputPort): MutableList<Triple<Pair<ISceneConfig, Double>, Pair<TextField, HBox>, Pair<ITextInputPort, Boolean>>> {
        val widgets = buildWidgets(initialText, 96.0)
        return mutableListOf(Triple(Pair(cfg, 96.0), widgets, Pair(portFactory(widgets.first), false)))
    }

    fun node(): HBox {
        return slot[0].second.second
    }

    override fun textPort(): ITextInputPort {
        return slot[0].third.first
    }

    override fun updateHorizontalPosition(targetCanvasX: Double) {
        val cfgAndW = slot[0].first
        val box = slot[0].second.second
        val clampedX = (targetCanvasX - cfgAndW.second / 2.0).coerceIn(10.0, cfgAndW.first.canvasWidth - cfgAndW.second - 10.0)
        box.layoutX = clampedX
        box.layoutY = cfgAndW.first.groundY + 26.0
    }

    override fun setLocked(locked: Boolean) {
        val cur = slot[0]
        cur.second.first.isEditable = !locked
        cur.second.first.isDisable = locked
        cur.second.second.style = pillStyle(locked)
        slot[0] = Triple(cur.first, cur.second, Pair(cur.third.first, locked))
    }

    override fun isLocked(): Boolean {
        return slot[0].third.second
    }

    private fun buildWidgets(initialText: String, pillW: Double): Pair<TextField, HBox> {
        val field = TextField(initialText).apply {
            prefWidth = 66.0
            minWidth = 66.0
            maxWidth = 66.0
            style = "-fx-control-inner-background: transparent; -fx-background-color: transparent; -fx-text-fill: #facc15; -fx-font-family: 'System'; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-alignment: center-right; -fx-padding: 1 2 1 2; -fx-opacity: 0.95;"
        }
        val unitLabel = Label("m").apply {
            style = "-fx-text-fill: #facc15; -fx-font-family: 'System'; -fx-font-weight: bold; -fx-font-size: 11.5px;"
        }
        val baseCss = "-fx-background-color: rgba(15, 23, 42, 0.92); -fx-border-color: rgba(250, 204, 21, 0.88); -fx-border-width: 1.3; -fx-background-radius: 11; -fx-border-radius: 11; -fx-alignment: center; -fx-padding: 0 6 0 4;"
        val box = HBox(2.0, field, unitLabel).apply {
            prefWidth = pillW
            minWidth = pillW
            maxWidth = pillW
            prefHeight = 24.0
            style = baseCss
        }
        return Pair(field, box)
    }

    private fun pillStyle(isLockedState: Boolean): String {
        val borderHex = if (isLockedState && slot.isNotEmpty()) "rgba(148, 163, 184, 0.55)" else "rgba(250, 204, 21, 0.88)"
        return "-fx-background-color: rgba(15, 23, 42, 0.92); -fx-border-color: $borderHex; -fx-border-width: 1.3; -fx-background-radius: 11; -fx-border-radius: 11; -fx-alignment: center; -fx-padding: 0 6 0 4;"
    }
}
