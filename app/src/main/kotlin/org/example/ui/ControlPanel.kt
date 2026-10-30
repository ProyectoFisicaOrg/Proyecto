package org.example.ui

import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.example.collision.CollisionResult
import org.example.collision.CollisionStatus
import org.example.model.SimulationState

/**
 * Adaptador JavaFX de [ISliderPort] (Adapter + DIP).
 */
private class JavaFxSliderAdapter(
    private val slider: Slider
) : ISliderPort {
    override fun currentValue(): Double = slider.value

    override fun updateValue(newValue: Double) {
        slider.value = newValue
    }

    override fun setDisabled(disabled: Boolean) {
        slider.isDisable = disabled
    }

    override fun onValueChanged(listener: (Double) -> Unit) {
        slider.valueProperty().addListener { _, _, newVal -> listener(newVal.toDouble()) }
    }
}

/**
 * Adaptador JavaFX de [ITextInputPort] con [TextFormatter], Enter y pérdida de foco (Adapter + DIP).
 */
internal class JavaFxTextInputAdapter(
    private val textField: TextField
) : ITextInputPort {
    override fun currentText(): String = textField.text

    override fun updateText(newText: String) {
        textField.text = newText
    }

    override fun setDisabled(disabled: Boolean) {
        textField.isDisable = disabled
    }

    override fun installFilter(predicate: (String) -> Boolean) {
        textField.textFormatter = TextFormatter<String> { change ->
            if (predicate(change.controlNewText)) change else null
        }
    }

    override fun onTextChanged(listener: (String) -> Unit) {
        textField.textProperty().addListener { _, _, txt -> listener(txt) }
    }

    override fun onCommitRequested(action: () -> Unit) {
        textField.setOnAction { action() }
        textField.focusedProperty().addListener { _, _, isFocused -> if (!isFocused) action() }
    }
}

/**
 * Contrato abstracto para el panel de controles de usuario (DIP).
 */
interface IControlPanel {
    fun build(): VBox
    fun showCollisionResult(result: CollisionResult)
    fun showOutOfBoundsMessage()
    fun clearCollisionResult()
    fun setAnimating(animating: Boolean)
}

/**
 * Contrato de fábrica de widgets visuales del panel lateral (ISP + DIP).
 */
interface IControlWidgetFactory {
    fun createSlider(min: Double, max: Double, initial: Double, step: Double): Slider
    fun createValueField(initialText: String): TextField
    fun createControlRow(titleText: String, slider: Slider, valueField: TextField): VBox
    fun createActionButton(text: String, height: Double, css: String, action: () -> Unit): Button
    fun createLegend(): HBox
}

/**
 * Fábrica cohesiva de widgets visuales estilizados mediante CSS para el panel lateral (SRP).
 */
class ControlWidgetFactory : IControlWidgetFactory {

    override fun createSlider(min: Double, max: Double, initial: Double, step: Double): Slider =
        Slider(min, max, initial).apply {
            isShowTickLabels = true
            isShowTickMarks = true
            majorTickUnit = if (step == 1.0) 10.0 else 5.0
            minorTickCount = 4
            blockIncrement = step
            isSnapToTicks = false
            prefWidth = 204.0
            style = "-fx-control-inner-background: #0f3460; -fx-base: #16213e;"
        }

    override fun createValueField(initialText: String): TextField =
        TextField(initialText).apply {
            prefWidth = 64.0
            minWidth = 64.0
            maxWidth = 64.0
            style = "-fx-control-inner-background: #0f172a; -fx-background-color: #0f172a; -fx-text-fill: #e94560; -fx-font-family: 'Monospace'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-alignment: center-right; -fx-border-color: #334155; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 3 6 3 6;"
        }

    override fun createControlRow(titleText: String, slider: Slider, valueField: TextField): VBox {
        val label = Label(titleText).apply {
            style = "-fx-font-family: 'System'; -fx-font-weight: 500; -fx-font-size: 11px; -fx-text-fill: #a0aec0;"
        }
        val row = HBox(10.0, slider, valueField).apply { style = "-fx-alignment: center;" }
        return VBox(8.0, label, row)
    }

    override fun createActionButton(text: String, height: Double, css: String, action: () -> Unit): Button =
        Button(text).apply {
            val weight = if (height > 40.0) "bold" else "600"
            val size = if (height > 40.0) "14px" else "12px"
            prefWidth = 260.0
            prefHeight = height
            style = "$css -fx-font-family: 'System'; -fx-font-weight: $weight; -fx-font-size: $size;"
            setOnAction { action() }
        }

    override fun createLegend(): HBox {
        val hbox = HBox(20.0).apply { style = "-fx-alignment: center;" }
        val entries = listOf(Pair("#e94560", "Cañón"), Pair("#4a5568", "Suelo"), Pair("#4299e1", "Proyectil"))
        for ((hex, name) in entries) {
            val swatch = Region().apply {
                prefWidth = 16.0
                prefHeight = 4.0
                style = "-fx-background-color: $hex; -fx-background-radius: 2;"
            }
            val lbl = Label(name).apply {
                style = "-fx-font-size: 10px; -fx-text-fill: #718096;"
            }
            hbox.children.add(HBox(6.0, swatch, lbl).apply { style = "-fx-alignment: center;" })
        }
        return hbox
    }
}

/**
 * Contrato de la sección de controles deslizantes y campos de texto sincronizados (ISP + DIP).
 */
interface ISlidersSection {
    fun buildRows(): List<VBox>
    fun setDisabled(disabled: Boolean)
}

/**
 * Grupo cohesivo de sliders y campos editables sincronizados bidireccionalmente con [SimulationState] (SRP + DIP).
 */
class ParameterSlidersSection(
    private val state: SimulationState,
    private val factory: IControlWidgetFactory,
    private val onAngleChange: () -> Unit
) : ISlidersSection {
    private val bindings = mutableListOf<ParameterControlBinding>()

    override fun buildRows(): List<VBox> {
        bindings.clear()
        return listOf(buildAngleControl(), buildVelocityControl(), buildGravityControl(), buildMassControl())
    }

    override fun setDisabled(disabled: Boolean) {
        for (binding in bindings) binding.setDisabled(disabled)
    }

    private fun buildBoundRow(spec: NumericParameterSpec, initial: Double, onCommit: (Double) -> Unit): VBox {
        val slider = factory.createSlider(spec.min(), spec.max(), initial, spec.step())
        val textField = factory.createValueField(spec.format(initial))
        val binding = ParameterControlBinding(spec, JavaFxSliderAdapter(slider), JavaFxTextInputAdapter(textField), onCommit)
        binding.bind()
        bindings.add(binding)
        return factory.createControlRow(spec.title(), slider, textField)
    }

    private fun buildAngleControl(): VBox {
        val spec = NumericParameterSpec("ÁNGULO (0-90°)", 0.0, 90.0, 45.0, allowDecimal = false)
        return buildBoundRow(spec, state.angle) { v ->
            state.angle = v
            if (!state.isAnimating) onAngleChange()
        }
    }

    private fun buildVelocityControl(): VBox {
        val spec = NumericParameterSpec("VELOCIDAD INICIAL (m/s)", 10.0, 100.0, 50.0, allowDecimal = false)
        return buildBoundRow(spec, state.velocity) { v -> state.velocity = v }
    }

    private fun buildGravityControl(): VBox {
        val spec = NumericParameterSpec("GRAVEDAD (m/s²)", 1.0, 30.0, 9.8, allowDecimal = true)
        return buildBoundRow(spec, state.gravity) { v -> state.gravity = v }
    }

    private fun buildMassControl(): VBox {
        val spec = NumericParameterSpec("MASA (kg)", 1.0, 50.0, 10.0, allowDecimal = false)
        return buildBoundRow(spec, state.mass) { v -> state.mass = v }
    }
}

/**
 * Contrato del presentador de estado y resultados de impacto (ISP + DIP).
 */
interface IStatusBannerPresenter {
    fun buildCard(): VBox
    fun showCollision(result: CollisionResult)
    fun showOutOfBounds()
    fun showInFlight()
}

/**
 * Presentador visual de la tarjeta de estado y resultados de colisión (SRP).
 */
class StatusBannerPresenter : IStatusBannerPresenter {
    private val baseStyle = "-fx-font-family: 'System'; -fx-font-weight: 500; -fx-font-size: 11px; -fx-alignment: center; -fx-text-alignment: center;"
    private val statusLabel = Label("Arrastra el objetivo y presiona DISPARAR").apply {
        isWrapText = true
        maxWidth = 240.0
        style = "$baseStyle -fx-text-fill: #94a3b8;"
    }

    override fun buildCard(): VBox =
        VBox(4.0, statusLabel).apply {
            prefWidth = 260.0
            style = "-fx-alignment: center; -fx-padding: 8; -fx-background-color: #0f172a; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 8;"
        }

    override fun showCollision(result: CollisionResult) {
        statusLabel.text = formatCollisionMessage(result)
        statusLabel.style = "$baseStyle -fx-text-fill: ${resolveCollisionHex(result.status)};"
    }

    private fun formatCollisionMessage(result: CollisionResult): String = when (result.status) {
        CollisionStatus.HIT -> "🎯 ¡IMPACTO EN EL BLANCO!\n${result.message}"
        CollisionStatus.MISS -> "❌ FUERA DEL OBJETIVO\n${result.message}"
        CollisionStatus.UNKNOWN -> "⚠️ ${result.message}"
    }

    private fun resolveCollisionHex(status: CollisionStatus): String = when (status) {
        CollisionStatus.HIT -> "#4ade80"
        CollisionStatus.MISS -> "#f87171"
        CollisionStatus.UNKNOWN -> "#fb923c"
    }

    override fun showOutOfBounds() {
        statusLabel.text = "⚠️ ¡FUERA DEL CUADRADO!\nJuego reiniciado"
        statusLabel.style = "$baseStyle -fx-text-fill: #fb923c;"
    }

    override fun showInFlight() {
        statusLabel.text = "🚀 Proyectil en vuelo..."
        statusLabel.style = "$baseStyle -fx-text-fill: #94a3b8;"
    }
}

/**
 * Contrato de la botonera de acciones del panel lateral (ISP + DIP).
 */
interface IActionButtonsSection {
    fun buildButtons(): List<Button>
    fun setAnimating(animating: Boolean)
}

/**
 * Grupo de botones de acción principal y secundarios del panel de control (SRP + DIP).
 */
class ActionButtonsSection(
    private val factory: IControlWidgetFactory,
    private val onFire: () -> Unit,
    private val onReset: (() -> Unit)?,
    private val onRandomizeTarget: (() -> Unit)?
) : IActionButtonsSection {
    private lateinit var fireButton: Button
    private val secondaryButtons = mutableListOf<Button>()

    override fun buildButtons(): List<Button> {
        secondaryButtons.clear()
        fireButton = factory.createActionButton(
            "DISPARAR", 45.0,
            "-fx-background-color: linear-gradient(to right, #e94560, #c73650); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"
        ) { onFire() }

        val randomBtn = factory.createActionButton(
            "🎯 OBJETIVO ALEATORIO", 34.0,
            "-fx-background-color: #1e3a8a; -fx-text-fill: #93c5fd; -fx-background-radius: 8; -fx-border-color: #3b82f6; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"
        ) { onRandomizeTarget?.invoke() }

        val resetBtn = factory.createActionButton(
            "REINICIAR", 34.0,
            "-fx-background-color: #334155; -fx-text-fill: #e2e8f0; -fx-background-radius: 8; -fx-cursor: hand;"
        ) { onReset?.invoke() }

        secondaryButtons.addAll(listOf(randomBtn, resetBtn))
        return listOf(fireButton, randomBtn, resetBtn)
    }

    override fun setAnimating(animating: Boolean) {
        fireButton.isDisable = animating
        fireButton.text = if (animating) "DISPARANDO..." else "DISPARAR"
        for (btn in secondaryButtons) btn.isDisable = animating
    }
}

/**
 * Panel principal de controles de la simulación desacoplado mediante interfaces (SRP + DIP).
 */
class ControlPanel(
    state: SimulationState,
    onFire: () -> Unit,
    onAngleChange: () -> Unit,
    onReset: (() -> Unit)? = null,
    onRandomizeTarget: (() -> Unit)? = null,
    private val widgetFactory: IControlWidgetFactory = ControlWidgetFactory(),
    private val statusBanner: IStatusBannerPresenter = StatusBannerPresenter()
) : IControlPanel {

    private val slidersSection: ISlidersSection = ParameterSlidersSection(state, widgetFactory, onAngleChange)
    private val buttonsSection: IActionButtonsSection = ActionButtonsSection(widgetFactory, onFire, onReset, onRandomizeTarget)

    override fun build(): VBox {
        val title = Label("CONTROLES").apply {
            style = "-fx-font-family: 'System'; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;"
        }
        return VBox(14.0).apply {
            children.add(title)
            children.addAll(slidersSection.buildRows())
            children.addAll(buttonsSection.buildButtons())
            children.addAll(statusBanner.buildCard(), widgetFactory.createLegend())
            prefWidth = 320.0
            style = "-fx-alignment: top-center; -fx-padding: 20; -fx-background-color: #16213e; -fx-background-radius: 12; -fx-border-color: #0f3460; -fx-border-width: 1; -fx-border-radius: 12;"
        }
    }

    override fun showCollisionResult(result: CollisionResult) = statusBanner.showCollision(result)

    override fun showOutOfBoundsMessage() = statusBanner.showOutOfBounds()

    override fun clearCollisionResult() = statusBanner.showInFlight()

    override fun setAnimating(animating: Boolean) {
        slidersSection.setDisabled(animating)
        buttonsSection.setAnimating(animating)
    }
}
