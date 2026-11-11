package org.example.ui

import javafx.scene.control.Button
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

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
