package org.example.ui

import java.util.Locale
import kotlin.math.round

/**
 * Contrato de validación léxica y parseo seguro de entradas numéricas (ISP + DIP).
 */
interface INumericInputValidator {
    fun isValidPartial(text: String, allowDecimal: Boolean): Boolean
    fun parseOrNull(text: String): Double?
}

/**
 * Validador estricto de texto numérico que bloquea letras, signos y caracteres especiales (SRP).
 */
class NumericInputValidator : INumericInputValidator {
    private val decimalPattern = Regex("^\\d*([.,]\\d*)?$")

    override fun isValidPartial(text: String, allowDecimal: Boolean): Boolean {
        if (text.isEmpty()) return true
        return if (allowDecimal) isValidDecimalText(text) else isValidIntegerText(text)
    }

    private fun isValidIntegerText(text: String): Boolean =
        text.length <= 6 && text.all { it in '0'..'9' }

    private fun isValidDecimalText(text: String): Boolean =
        text.length <= 9 && decimalPattern.matches(text)

    override fun parseOrNull(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty() || normalized == ".") return null
        return normalized.toDoubleOrNull()
    }
}

/**
 * Especificación encapsulada de límites, valor por defecto y formato de un parámetro numérico (SRP + Encapsulación).
 */
class NumericParameterSpec(
    private val labelTitle: String,
    private val minVal: Double,
    private val maxVal: Double,
    private val defaultVal: Double,
    private val allowDecimal: Boolean
) {
    fun title(): String = labelTitle

    fun min(): Double = minVal

    fun max(): Double = maxVal

    fun defaultValue(): Double = defaultVal

    fun allowsDecimal(): Boolean = allowDecimal

    fun step(): Double = if (allowDecimal) 0.1 else 1.0

    fun isInRange(value: Double): Boolean = value in minVal..maxVal

    fun normalize(value: Double): Double =
        if (allowDecimal) round(value * 10.0) / 10.0 else round(value)

    fun resolveOrDefault(rawText: String, validator: INumericInputValidator = NumericInputValidator()): Double {
        val parsed = validator.parseOrNull(rawText) ?: return defaultVal
        return if (isInRange(parsed)) normalize(parsed) else defaultVal
    }

    fun format(value: Double): String =
        if (allowDecimal) String.format(Locale.US, "%.1f", value) else value.toInt().toString()
}

/**
 * Puerto abstracto para controles deslizantes desacoplados del toolkit gráfico (DIP + ISP).
 */
interface ISliderPort {
    fun currentValue(): Double
    fun updateValue(newValue: Double)
    fun setDisabled(disabled: Boolean)
    fun onValueChanged(listener: (Double) -> Unit)
}

/**
 * Puerto abstracto para campos de entrada de texto con filtro y eventos de confirmación (DIP + ISP).
 */
interface ITextInputPort {
    fun currentText(): String
    fun updateText(newText: String)
    fun setDisabled(disabled: Boolean)
    fun installFilter(predicate: (String) -> Boolean)
    fun onTextChanged(listener: (String) -> Unit)
    fun onCommitRequested(action: () -> Unit)
}

/**
 * Enlace bidireccional encapsulado entre un [ISliderPort] y un [ITextInputPort] validado (SRP + DIP).
 */
class ParameterControlBinding(
    private val spec: NumericParameterSpec,
    private val slider: ISliderPort,
    private val textInput: ITextInputPort,
    private val onValueCommitted: (Double) -> Unit,
    private val validator: INumericInputValidator = NumericInputValidator()
) {
    private var syncing: Boolean = false

    fun parameterSpec(): NumericParameterSpec = spec

    fun bind() {
        textInput.installFilter { candidate -> validator.isValidPartial(candidate, spec.allowsDecimal()) }
        slider.onValueChanged { rawVal -> onSliderMoved(rawVal) }
        textInput.onTextChanged { rawText -> onTextEditedLive(rawText) }
        textInput.onCommitRequested { commitText() }
    }

    fun setDisabled(disabled: Boolean) {
        slider.setDisabled(disabled)
        textInput.setDisabled(disabled)
    }

    fun commitText() {
        if (syncing) return
        val resolved = spec.resolveOrDefault(textInput.currentText(), validator)
        applySynchronized(resolved, updateText = true)
    }

    private fun onSliderMoved(rawSliderValue: Double) {
        if (syncing) return
        val clamped = rawSliderValue.coerceIn(spec.min(), spec.max())
        applySynchronized(spec.normalize(clamped), updateText = true)
    }

    private fun onTextEditedLive(rawText: String) {
        if (syncing) return
        val parsed = validator.parseOrNull(rawText) ?: return
        if (!spec.isInRange(parsed)) return
        applySynchronized(spec.normalize(parsed), updateText = false)
    }

    private fun applySynchronized(value: Double, updateText: Boolean) {
        syncing = true
        try {
            slider.updateValue(value)
            if (updateText) textInput.updateText(spec.format(value))
            onValueCommitted(value)
        } finally {
            syncing = false
        }
    }
}
