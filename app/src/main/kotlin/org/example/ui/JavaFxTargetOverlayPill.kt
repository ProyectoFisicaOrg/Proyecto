package org.example.ui

import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import org.example.rendering.ISceneConfig

/**
 * Pastilla interactiva JavaFX con [TextField] editable superpuesta debajo de la diana en el escenario (SRP + DIP).
 */
class JavaFxTargetOverlayPill(
    private val cfg: ISceneConfig,
    initialText: String = "264.00"
) : ITargetOverlayView {
    private val pillWidth: Double = 96.0
    private var locked: Boolean = false
    private val textField: TextField = createTextField(initialText)
    private val pillBox: HBox = createPillContainer()
    private val inputPort: ITextInputPort = JavaFxTextInputAdapter(textField)

    fun node(): HBox = pillBox

    override fun textPort(): ITextInputPort = inputPort

    override fun updateHorizontalPosition(targetCanvasX: Double) {
        val clampedX = (targetCanvasX - pillWidth / 2.0).coerceIn(10.0, cfg.canvasWidth - pillWidth - 10.0)
        pillBox.layoutX = clampedX
        pillBox.layoutY = cfg.groundY + 26.0
    }

    override fun setLocked(locked: Boolean) {
        this.locked = locked
        textField.isEditable = !locked
        textField.isDisable = locked
        pillBox.style = pillStyle(locked)
    }

    override fun isLocked(): Boolean = locked

    private fun createTextField(initialText: String): TextField =
        TextField(initialText).apply {
            prefWidth = 66.0
            minWidth = 66.0
            maxWidth = 66.0
            style = "-fx-control-inner-background: transparent; -fx-background-color: transparent; -fx-text-fill: #facc15; -fx-font-family: 'System'; -fx-font-weight: bold; -fx-font-size: 11.5px; -fx-alignment: center-right; -fx-padding: 1 2 1 2; -fx-opacity: 0.95;"
        }

    private fun createPillContainer(): HBox {
        val unitLabel = Label("m").apply {
            style = "-fx-text-fill: #facc15; -fx-font-family: 'System'; -fx-font-weight: bold; -fx-font-size: 11.5px;"
        }
        return HBox(2.0, textField, unitLabel).apply {
            prefWidth = pillWidth
            minWidth = pillWidth
            maxWidth = pillWidth
            prefHeight = 24.0
            style = pillStyle(locked = false)
        }
    }

    private fun pillStyle(locked: Boolean): String {
        val borderHex = if (locked) "rgba(148, 163, 184, 0.55)" else "rgba(250, 204, 21, 0.88)"
        return "-fx-background-color: rgba(15, 23, 42, 0.92); -fx-border-color: $borderHex; -fx-border-width: 1.3; -fx-background-radius: 11; -fx-border-radius: 11; -fx-alignment: center; -fx-padding: 0 6 0 4;"
    }
}
