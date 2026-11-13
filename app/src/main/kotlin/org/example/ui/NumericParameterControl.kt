package org.example.ui

import java.util.Locale
import kotlin.math.round

/**
 * Validador estricto de texto numérico que bloquea letras, signos y caracteres especiales (SRP + DIP).
 */
class NumericInputValidator : INumericInputValidator {
    private val decimalPattern = Regex("^\\d*([.,]\\d*)?$")

    override fun isValidPartial(text: String, allowDecimal: Boolean): Boolean {
        if (text.isEmpty()) return true
        return if (allowDecimal) isValidDecimalText(text) else isValidIntegerText(text)
    }

    private fun isValidIntegerText(text: String): Boolean {
        return text.length <= 6 && text.all { it in '0'..'9' }
    }

    private fun isValidDecimalText(text: String): Boolean {
        return text.length <= 9 && decimalPattern.matches(text)
    }

    override fun parseOrNull(text: String): Double? {
        val normalized = text.trim().replace(',', '.')
        if (normalized.isEmpty() || normalized == ".") return null
        return normalized.toDoubleOrNull()
    }
}

/**
 * Instancia compartida por defecto de [INumericInputValidator] (DIP).
 */
object DefaultNumericValidator : INumericInputValidator {
    private val delegate: INumericInputValidator = NumericInputValidator()

    override fun isValidPartial(text: String, allowDecimal: Boolean): Boolean {
        return delegate.isValidPartial(text, allowDecimal)
    }

    override fun parseOrNull(text: String): Double? {
        return delegate.parseOrNull(text)
    }
}

/**
 * Especificación encapsulada de límites, valor por defecto y formato de un parámetro numérico (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class NumericParameterSpec internal constructor(
    private val labelTitle: String,
    private val numericConfig: List<Double>,
    private val defaultValidator: INumericInputValidator
) : INumericParameterSpec {

    constructor(
        labelTitle: String,
        minVal: Double,
        maxVal: Double,
        defaultVal: Double,
        allowDecimal: Boolean,
        defaultValidator: INumericInputValidator = DefaultNumericValidator
    ) : this(labelTitle, listOf(minVal, maxVal, defaultVal, if (allowDecimal) 1.0 else 0.0), defaultValidator)

    override fun title(): String {
        return labelTitle
    }

    override fun min(): Double {
        return numericConfig[0]
    }

    override fun max(): Double {
        return numericConfig[1]
    }

    override fun defaultValue(): Double {
        return numericConfig[2]
    }

    override fun allowsDecimal(): Boolean {
        return numericConfig[3] > 0.5
    }

    override fun step(): Double {
        return if (numericConfig[3] > 0.5) 0.1 else 1.0
    }

    override fun isInRange(value: Double): Boolean {
        return value in numericConfig[0]..numericConfig[1]
    }

    override fun normalize(value: Double): Double {
        return if (numericConfig[3] > 0.5) round(value * 10.0) / 10.0 else round(value)
    }

    override fun resolveOrDefault(rawText: String, validator: INumericInputValidator?): Double {
        val chosen = validator ?: defaultValidator
        val parsed = chosen.parseOrNull(rawText) ?: return numericConfig[2]
        return if (isInRange(parsed)) normalize(parsed) else numericConfig[2]
    }

    override fun format(value: Double): String {
        return if (numericConfig[3] > 0.5) String.format(Locale.US, "%.1f", value) else value.toInt().toString()
    }
}

/**
 * Enlace bidireccional encapsulado entre un [ISliderPort] y un [ITextInputPort] validado (SRP + DIP).
 */
class ParameterControlBinding(
    private val spec: INumericParameterSpec,
    slider: ISliderPort,
    textInput: ITextInputPort,
    private val onValueCommitted: (Double) -> Unit,
    private val validator: INumericInputValidator = DefaultNumericValidator
) : IParameterControlBinding {
    private val ports: Pair<ISliderPort, ITextInputPort> = Pair(slider, textInput)
    private var syncing: Boolean = false

    override fun bind() {
        ports.second.installFilter { candidate -> validator.isValidPartial(candidate, spec.allowsDecimal()) }
        ports.first.onValueChanged { rawVal -> onSliderMoved(rawVal) }
        ports.second.onTextChanged { rawText -> onTextEditedLive(rawText) }
        ports.second.onCommitRequested { commitText() }
    }

    override fun setDisabled(disabled: Boolean) {
        ports.first.setDisabled(disabled)
        ports.second.setDisabled(disabled)
    }

    override fun commitText() {
        if (syncing) return
        val resolved = spec.resolveOrDefault(ports.second.currentText(), validator)
        applySynchronized(resolved, true)
    }

    private fun onSliderMoved(rawSliderValue: Double) {
        if (syncing) return
        val clamped = rawSliderValue.coerceIn(spec.min(), spec.max())
        applySynchronized(spec.normalize(clamped), true)
    }

    private fun onTextEditedLive(rawText: String) {
        if (syncing) return
        val parsed = validator.parseOrNull(rawText) ?: return
        if (!spec.isInRange(parsed)) return
        applySynchronized(spec.normalize(parsed), false)
    }

    private fun applySynchronized(value: Double, updateText: Boolean) {
        syncing = true
        try {
            ports.first.updateValue(value)
            if (updateText) ports.second.updateText(spec.format(value))
            onValueCommitted(value)
        } finally {
            syncing = false
        }
    }
}
