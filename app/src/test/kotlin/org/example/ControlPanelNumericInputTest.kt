package org.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.example.model.SimulationState
import org.example.ui.ISliderPort
import org.example.ui.ITextInputPort
import org.example.ui.NumericInputValidator
import org.example.ui.NumericParameterSpec
import org.example.ui.ParameterControlBinding

/**
 * Doble de prueba en memoria para [ISliderPort] (DIP).
 */
private class FakeSliderPort(
    initial: Double
) : ISliderPort {
    private var value: Double = initial
    private var disabled: Boolean = false
    private var listener: ((Double) -> Unit)? = null

    override fun currentValue(): Double = value

    override fun updateValue(newValue: Double) {
        value = newValue
    }

    override fun setDisabled(disabled: Boolean) {
        this.disabled = disabled
    }

    override fun onValueChanged(listener: (Double) -> Unit) {
        this.listener = listener
    }

    fun isDisabled(): Boolean = disabled

    fun dragTo(newValue: Double) {
        value = newValue
        listener?.invoke(newValue)
    }
}

/**
 * Doble de prueba en memoria para [ITextInputPort] que simula filtro en vivo, Enter y focusLost (DIP).
 */
private class FakeTextInputPort(
    initial: String
) : ITextInputPort {
    private var text: String = initial
    private var disabled: Boolean = false
    private var filter: ((String) -> Boolean)? = null
    private var changeListener: ((String) -> Unit)? = null
    private var commitAction: (() -> Unit)? = null

    override fun currentText(): String = text

    override fun updateText(newText: String) {
        val allowed = filter?.invoke(newText) ?: true
        if (!allowed) return
        text = newText
        changeListener?.invoke(newText)
    }

    override fun setDisabled(disabled: Boolean) {
        this.disabled = disabled
    }

    override fun installFilter(predicate: (String) -> Boolean) {
        filter = predicate
    }

    override fun onTextChanged(listener: (String) -> Unit) {
        changeListener = listener
    }

    override fun onCommitRequested(action: () -> Unit) {
        commitAction = action
    }

    fun isDisabled(): Boolean = disabled

    fun typeUserText(candidate: String) = updateText(candidate)

    fun pressEnterOrLoseFocus() {
        commitAction?.invoke()
    }
}

/**
 * Pruebas unitarias de validación léxica y valores por defecto para entradas numéricas (SRP).
 */
class NumericInputValidationTest {

    private val validator = NumericInputValidator()

    @Test
    fun test_validator_rejects_letters_symbols_and_signs_in_integer_and_decimal_modes() {
        assertTrue(validator.isValidPartial("", allowDecimal = false))
        assertTrue(validator.isValidPartial("45", allowDecimal = false))
        assertFalse(validator.isValidPartial("45a", allowDecimal = false))
        assertFalse(validator.isValidPartial("-10", allowDecimal = false))
        assertFalse(validator.isValidPartial("45.5", allowDecimal = false))
        assertFalse(validator.isValidPartial("45°", allowDecimal = false))
    }

    @Test
    fun test_validator_allows_comma_or_dot_only_in_decimal_parameters_like_gravity() {
        assertTrue(validator.isValidPartial("9.8", allowDecimal = true))
        assertTrue(validator.isValidPartial("9,8", allowDecimal = true))
        assertTrue(validator.isValidPartial("12,", allowDecimal = true))
        assertFalse(validator.isValidPartial("9.8.1", allowDecimal = true))
        assertFalse(validator.isValidPartial("9,8,1", allowDecimal = true))
        assertFalse(validator.isValidPartial("g=9.8", allowDecimal = true))
    }

    @Test
    fun test_spec_falls_back_to_default_on_empty_blank_or_out_of_bounds_values() {
        val angleSpec = NumericParameterSpec("ÁNGULO", 0.0, 90.0, 45.0, allowDecimal = false)
        val gravitySpec = NumericParameterSpec("GRAVEDAD", 1.0, 30.0, 9.8, allowDecimal = true)

        assertEquals(45.0, angleSpec.resolveOrDefault(""))
        assertEquals(45.0, angleSpec.resolveOrDefault("   "))
        assertEquals(45.0, angleSpec.resolveOrDefault("120"))
        assertEquals(30.0, angleSpec.resolveOrDefault("30"))

        assertEquals(9.8, gravitySpec.resolveOrDefault(""))
        assertEquals(9.8, gravitySpec.resolveOrDefault(","))
        assertEquals(9.8, gravitySpec.resolveOrDefault("0.2"))
        assertEquals(9.8, gravitySpec.resolveOrDefault("99.9"))
        assertEquals(1.6, gravitySpec.resolveOrDefault("1,6"))
        assertNull(validator.parseOrNull("abc"))
    }
}

/**
 * Pruebas de integración de sincronización bidireccional Slider <-> TextField (SRP).
 */
class ControlPanelBidirectionalSyncTest {

    @Test
    fun test_moving_slider_updates_text_field_and_state_in_real_time() {
        val state = SimulationState()
        val angleSpec = NumericParameterSpec("ÁNGULO (0-90°)", 0.0, 90.0, 45.0, allowDecimal = false)
        val slider = FakeSliderPort(45.0)
        val field = FakeTextInputPort("45")
        var angleRepaints = 0
        val binding = ParameterControlBinding(angleSpec, slider, field, { v -> state.angle = v; angleRepaints++ })
        binding.bind()

        slider.dragTo(62.0)
        assertEquals("62", field.currentText())
        assertEquals(62.0, state.angle, 1e-4)
        assertEquals(1, angleRepaints)
    }

    @Test
    fun test_typing_valid_value_moves_slider_and_blocks_invalid_characters() {
        val state = SimulationState()
        val velocitySpec = NumericParameterSpec("VELOCIDAD INICIAL (m/s)", 10.0, 100.0, 50.0, allowDecimal = false)
        val velSlider = FakeSliderPort(50.0)
        val velField = FakeTextInputPort("50")
        ParameterControlBinding(velocitySpec, velSlider, velField, { state.velocity = it }).bind()

        velField.typeUserText("75")
        assertEquals(75.0, velSlider.currentValue(), 1e-4)
        assertEquals(75.0, state.velocity, 1e-4)

        velField.typeUserText("abc!@#")
        assertEquals("75", velField.currentText())

        val gravitySpec = NumericParameterSpec("GRAVEDAD (m/s²)", 1.0, 30.0, 9.8, allowDecimal = true)
        val gravSlider = FakeSliderPort(9.8)
        val gravField = FakeTextInputPort("9.8")
        ParameterControlBinding(gravitySpec, gravSlider, gravField, { state.gravity = it }).bind()

        gravField.typeUserText("3,7")
        gravField.pressEnterOrLoseFocus()
        assertEquals(3.7, gravSlider.currentValue(), 1e-4)
        assertEquals("3.7", gravField.currentText())
        assertEquals(3.7, state.gravity, 1e-4)
    }

    @Test
    fun test_empty_or_out_of_range_text_restores_default_value_on_focus_lost_or_enter() {
        val state = SimulationState()
        val massSpec = NumericParameterSpec("MASA (kg)", 1.0, 50.0, 10.0, allowDecimal = false)
        val slider = FakeSliderPort(10.0)
        val field = FakeTextInputPort("10")
        val binding = ParameterControlBinding(massSpec, slider, field, { state.mass = it })
        binding.bind()

        field.typeUserText("28")
        assertEquals(28.0, state.mass, 1e-4)

        field.typeUserText("")
        field.pressEnterOrLoseFocus()
        assertEquals("10", field.currentText())
        assertEquals(10.0, slider.currentValue(), 1e-4)
        assertEquals(10.0, state.mass, 1e-4)

        field.typeUserText("999")
        field.pressEnterOrLoseFocus()
        assertEquals("10", field.currentText())
        assertEquals(10.0, slider.currentValue(), 1e-4)
        assertEquals(10.0, state.mass, 1e-4)

        binding.setDisabled(true)
        assertTrue(slider.isDisabled())
        assertTrue(field.isDisabled())
    }
}
