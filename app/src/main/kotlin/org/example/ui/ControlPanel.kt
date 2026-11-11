package org.example.ui

import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.example.collision.ICollisionResult
import org.example.model.ISimulationState

/**
 * Adaptador JavaFX de [ISliderPort] (Adapter + DIP).
 */
internal class JavaFxSliderAdapter(
    private val slider: Slider
) : ISliderPort {

    override fun currentValue(): Double {
        return slider.value
    }

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

    override fun currentText(): String {
        return textField.text
    }

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
 * Fábrica cohesiva de widgets visuales estilizados mediante CSS para el panel lateral (SRP + DIP).
 */
class ControlWidgetFactory : IControlWidgetFactory {

    override fun createSlider(min: Double, max: Double, initial: Double, step: Double): Slider {
        return Slider(min, max, initial).apply {
            isShowTickLabels = true
            isShowTickMarks = true
            majorTickUnit = if (step == 1.0) 10.0 else 5.0
            minorTickCount = 4
            blockIncrement = step
            isSnapToTicks = false
            prefWidth = 204.0
            style = "-fx-control-inner-background: #0f3460; -fx-base: #16213e;"
        }
    }

    override fun createValueField(initialText: String): TextField {
        return TextField(initialText).apply {
            prefWidth = 64.0
            minWidth = 64.0
            maxWidth = 64.0
            style = "-fx-control-inner-background: #0f172a; -fx-background-color: #0f172a; -fx-text-fill: #e94560; -fx-font-family: 'Monospace'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-alignment: center-right; -fx-border-color: #334155; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 3 6 3 6;"
        }
    }

    override fun createControlRow(titleText: String, slider: Slider, valueField: TextField): VBox {
        val label = Label(titleText).apply {
            style = "-fx-font-family: 'System'; -fx-font-weight: 500; -fx-font-size: 11px; -fx-text-fill: #a0aec0;"
        }
        val row = HBox(10.0, slider, valueField).apply { style = "-fx-alignment: center;" }
        return VBox(8.0, label, row)
    }

    override fun createActionButton(text: String, height: Double, css: String, action: () -> Unit): Button {
        return Button(text).apply {
            val weight = if (height > 40.0) "bold" else "600"
            val size = if (height > 40.0) "14px" else "12px"
            prefWidth = 260.0
            prefHeight = height
            style = "$css -fx-font-family: 'System'; -fx-font-weight: $weight; -fx-font-size: $size;"
            setOnAction { action() }
        }
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
            val lbl = Label(name).apply { style = "-fx-font-size: 10px; -fx-text-fill: #718096;" }
            hbox.children.add(HBox(6.0, swatch, lbl).apply { style = "-fx-alignment: center;" })
        }
        return hbox
    }
}

/**
 * Grupo cohesivo de sliders y campos editables sincronizados bidireccionalmente con [ISimulationState] (SRP + DIP).
 */
class ParameterSlidersSection(
    state: ISimulationState,
    factory: IControlWidgetFactory,
    onAngleChange: () -> Unit
) : ISlidersSection {
    private val ctx: Pair<Triple<ISimulationState, IControlWidgetFactory, () -> Unit>, MutableList<IParameterControlBinding>> =
        Pair(Triple(state, factory, onAngleChange), mutableListOf())

    override fun buildRows(): List<VBox> {
        ctx.second.clear()
        return listOf(buildAngleControl(), buildVelocityControl(), buildGravityControl(), buildMassControl())
    }

    override fun setDisabled(disabled: Boolean) {
        for (binding in ctx.second) binding.setDisabled(disabled)
    }

    private fun buildBoundRow(spec: INumericParameterSpec, initial: Double, onCommit: (Double) -> Unit): VBox {
        val factory = ctx.first.second
        val slider = factory.createSlider(spec.min(), spec.max(), initial, spec.step())
        val textField = factory.createValueField(spec.format(initial))
        val binding: IParameterControlBinding =
            ParameterControlBinding(spec, JavaFxSliderAdapter(slider), JavaFxTextInputAdapter(textField), onCommit)
        binding.bind()
        ctx.second.add(binding)
        return factory.createControlRow(spec.title(), slider, textField)
    }

    private fun buildAngleControl(): VBox {
        val st = ctx.first.first
        val spec: INumericParameterSpec = NumericParameterSpec("ÁNGULO (0-90°)", 0.0, 90.0, 45.0, false)
        return buildBoundRow(spec, st.angle) { v ->
            st.angle = v
            if (!st.isAnimating) ctx.first.third.invoke()
        }
    }

    private fun buildVelocityControl(): VBox {
        val st = ctx.first.first
        val spec: INumericParameterSpec = NumericParameterSpec("VELOCIDAD INICIAL (m/s)", 10.0, 100.0, 50.0, false)
        return buildBoundRow(spec, st.velocity) { v -> st.velocity = v }
    }

    private fun buildGravityControl(): VBox {
        val st = ctx.first.first
        val spec: INumericParameterSpec = NumericParameterSpec("GRAVEDAD (m/s²)", 1.0, 30.0, 9.8, true)
        return buildBoundRow(spec, st.gravity) { v -> st.gravity = v }
    }

    private fun buildMassControl(): VBox {
        val st = ctx.first.first
        val spec: INumericParameterSpec = NumericParameterSpec("MASA (kg)", 1.0, 50.0, 10.0, false)
        return buildBoundRow(spec, st.mass) { v -> st.mass = v }
    }
}

/**
 * Presentador visual polimórfico de la tarjeta de estado y resultados de colisión (SRP + OCP).
 */
class StatusBannerPresenter : IStatusBannerPresenter {
    private val baseStyle = "-fx-font-family: 'System'; -fx-font-weight: 500; -fx-font-size: 11px; -fx-alignment: center; -fx-text-alignment: center;"
    private val statusLabel = Label("Arrastra el objetivo y presiona DISPARAR").apply {
        isWrapText = true
        maxWidth = 240.0
        style = "$baseStyle -fx-text-fill: #94a3b8;"
    }

    override fun buildCard(): VBox {
        return VBox(4.0, statusLabel).apply {
            prefWidth = 260.0
            style = "-fx-alignment: center; -fx-padding: 8; -fx-background-color: #0f172a; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 8;"
        }
    }

    override fun showCollision(result: ICollisionResult) {
        statusLabel.text = result.formatBannerText()
        statusLabel.style = "$baseStyle -fx-text-fill: ${result.bannerColorHex()};"
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
 * Grupo cohesivo de botones de acción del panel de control (SRP + DIP).
 */
class ActionButtonsSection(
    private val factory: IControlWidgetFactory,
    onFire: () -> Unit,
    onReset: (() -> Unit)?,
    onRandomizeTarget: (() -> Unit)?
) : IActionButtonsSection {
    private val actions: Triple<() -> Unit, (() -> Unit)?, (() -> Unit)?> = Triple(onFire, onReset, onRandomizeTarget)
    private val buttons = mutableListOf<Button>()

    override fun buildButtons(): List<Button> {
        buttons.clear()
        val fireBtn = factory.createActionButton(
            "DISPARAR", 45.0,
            "-fx-background-color: linear-gradient(to right, #e94560, #c73650); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"
        ) { actions.first.invoke() }
        val randomBtn = factory.createActionButton(
            "🎯 OBJETIVO ALEATORIO", 34.0,
            "-fx-background-color: #1e3a8a; -fx-text-fill: #93c5fd; -fx-background-radius: 8; -fx-border-color: #3b82f6; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"
        ) { actions.third?.invoke() }
        val resetBtn = factory.createActionButton(
            "REINICIAR", 34.0,
            "-fx-background-color: #334155; -fx-text-fill: #e2e8f0; -fx-background-radius: 8; -fx-cursor: hand;"
        ) { actions.second?.invoke() }
        buttons.addAll(listOf(fireBtn, randomBtn, resetBtn))
        return buttons.toList()
    }

    override fun setAnimating(animating: Boolean) {
        if (buttons.isEmpty()) return
        buttons[0].isDisable = animating
        buttons[0].text = if (animating) "DISPARANDO..." else "DISPARAR"
        for (i in 1 until buttons.size) buttons[i].isDisable = animating
    }
}

/**
 * Panel principal de controles de la simulación desacoplado mediante interfaces (SRP + DIP).
 */
class ControlPanel(
    private val widgetFactory: IControlWidgetFactory,
    private val statusBanner: IStatusBannerPresenter,
    slidersSection: ISlidersSection,
    buttonsSection: IActionButtonsSection
) : IControlPanel {

    constructor(
        state: ISimulationState,
        onFire: () -> Unit,
        onAngleChange: () -> Unit,
        onReset: (() -> Unit)? = null,
        onRandomizeTarget: (() -> Unit)? = null,
        widgetFactory: IControlWidgetFactory = ControlWidgetFactory(),
        statusBanner: IStatusBannerPresenter = StatusBannerPresenter()
    ) : this(
        widgetFactory = widgetFactory,
        statusBanner = statusBanner,
        slidersSection = ParameterSlidersSection(state, widgetFactory, onAngleChange),
        buttonsSection = ActionButtonsSection(widgetFactory, onFire, onReset, onRandomizeTarget)
    )

    private val interactiveSections: Pair<ISlidersSection, IActionButtonsSection> = Pair(slidersSection, buttonsSection)

    override fun build(): VBox {
        val title = Label("CONTROLES").apply {
            style = "-fx-font-family: 'System'; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: white;"
        }
        return VBox(14.0).apply {
            children.add(title)
            children.addAll(interactiveSections.first.buildRows())
            children.addAll(interactiveSections.second.buildButtons())
            children.addAll(statusBanner.buildCard(), widgetFactory.createLegend())
            prefWidth = 320.0
            style = "-fx-alignment: top-center; -fx-padding: 20; -fx-background-color: #16213e; -fx-background-radius: 12; -fx-border-color: #0f3460; -fx-border-width: 1; -fx-border-radius: 12;"
        }
    }

    override fun showCollisionResult(result: ICollisionResult) {
        statusBanner.showCollision(result)
    }

    override fun showOutOfBoundsMessage() {
        statusBanner.showOutOfBounds()
    }

    override fun clearCollisionResult() {
        statusBanner.showInFlight()
    }

    override fun setAnimating(animating: Boolean) {
        interactiveSections.first.setDisabled(animating)
        interactiveSections.second.setAnimating(animating)
    }
}
