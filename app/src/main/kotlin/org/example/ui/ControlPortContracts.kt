package org.example.ui

/**
 * Puertos abstractos para sliders, campos de texto y enlaces bidireccionales (DIP + ISP).
 */
interface ISliderPort {
    fun currentValue(): Double
    fun updateValue(newValue: Double)
    fun setDisabled(disabled: Boolean)
    fun onValueChanged(listener: (Double) -> Unit)
}

interface ITextInputPort {
    fun currentText(): String
    fun updateText(newText: String)
    fun setDisabled(disabled: Boolean)
    fun installFilter(predicate: (String) -> Boolean)
    fun onTextChanged(listener: (String) -> Unit)
    fun onCommitRequested(action: () -> Unit)
}

interface IParameterControlBinding {
    fun bind()
    fun setDisabled(disabled: Boolean)
    fun commitText()
}
