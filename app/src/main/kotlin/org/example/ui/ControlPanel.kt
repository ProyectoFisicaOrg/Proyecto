package org.example.ui

// === Imports de JavaFX para construir la interfaz de controles ===
import javafx.geometry.Insets         // Márgenes (padding).
import javafx.geometry.Pos            // Alineación.
import javafx.scene.control.Button    // Botón de disparo.
import javafx.scene.control.Label     // Etiquetas.
import javafx.scene.control.Slider    // Sliders para los parámetros.
import javafx.scene.layout.HBox       // Layout horizontal.
import javafx.scene.layout.Region     // Elemento base para la leyenda.
import javafx.scene.layout.VBox       // Layout vertical.
import javafx.scene.paint.Color       // Colores de los textos.
import javafx.scene.text.Font         // Tipografías.
import javafx.scene.text.FontWeight   // Grusos de tipografía.

// Import del estado de la simulación.
import org.example.model.SimulationState

/**
 * Panel de controles de la simulación (SRP).
 *
 * Su única responsabilidad es construir y gestionar la interfaz de
 * controles: los sliders (ángulo, velocidad, gravedad y masa) y el
 * botón de disparo. Traduce los cambios de los sliders al
 * [SimulationState] y delega el disparo en el callback [onFire].
 * Permite habilitar/deshabilitar los controles durante la animación.
 *
 * @param state Estado al que se escriben los valores de los sliders.
 * @param onFire Callback que se invoca al presionar DISPARAR.
 * @param onAngleChange Callback que se invoca al mover el slider de ángulo.
 */
class ControlPanel(
    private val state: SimulationState,
    private val onFire: () -> Unit,
    private val onAngleChange: () -> Unit,
) {

    // Referencias a los controles, inicializadas dentro de build().
    private lateinit var angleSlider: Slider      // Slider del ángulo.
    private lateinit var velocitySlider: Slider   // Slider de la velocidad.
    private lateinit var gravitySlider: Slider    // Slider de la gravedad.
    private lateinit var massSlider: Slider       // Slider de la masa.
    private lateinit var fireButton: Button       // Botón de disparo.
    private lateinit var angleLabel: Label        // Etiqueta del valor del ángulo.
    private lateinit var velocityLabel: Label     // Etiqueta del valor de la velocidad.
    private lateinit var gravityLabel: Label      // Etiqueta del valor de la gravedad.
    private lateinit var massLabel: Label         // Etiqueta del valor de la masa.

    /**
     * Construye y devuelve el panel completo de controles (VBox).
     * Crea los controles, los agrupa en filas y devuelve el panel armado.
     */
    fun build(): VBox {
        // Crea sliders, etiquetas, botón y listeners.
        createControls()

        // Título del panel.
        val title = Label("CONTROLES").apply {
            font = Font.font("System", FontWeight.BOLD, 16.0)  // Tipografía del título.
            textFill = Color.WHITE                              // Color blanco.
        }

        // Una fila por cada parámetro (etiqueta + slider + valor).
        val angleBox = createControlRow("ÁNGULO (0-90°)", angleSlider, angleLabel)
        val velocityBox = createControlRow("VELOCIDAD INICIAL (m/s)", velocitySlider, velocityLabel)
        val gravityBox = createControlRow("GRAVEDAD (m/s²)", gravitySlider, gravityLabel)
        val massBox = createControlRow("MASA (kg)", massSlider, massLabel)

        // Leyenda de colores de la escena.
        val legend = createLegend()

        // Compone el panel vertical con todos los elementos.
        val panel = VBox(20.0, title, angleBox, velocityBox, gravityBox, massBox, fireButton, legend)
        panel.alignment = Pos.TOP_CENTER   // Alinea los elementos arriba y centrados.
        panel.padding = Insets(25.0)       // Márgenes internos.
        panel.prefWidth = 320.0            // Ancho del panel.
        // Estilo visual: fondo oscuro, bordes redondeados y sombra.
        panel.style = """
            -fx-background-color: #16213e;
            -fx-background-radius: 12;
            -fx-border-color: #0f3460;
            -fx-border-width: 1;
            -fx-border-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 4);
        """.trimIndent()

        return panel
    }

    /**
     * Habilita o deshabilita los controles según el estado de la animación.
     *
     * @param animating Si es true, deshabilita sliders y botón y cambia el
     *                  texto del botón a "DISPARANDO..."; si es false,
     *                  los habilita y restaura el texto "DISPARAR".
     */
    fun setAnimating(animating: Boolean) {
        // Deshabilita/habilita los sliders de parámetros.
        angleSlider.isDisable = animating
        velocitySlider.isDisable = animating
        gravitySlider.isDisable = animating
        massSlider.isDisable = animating

        // Deshabilita/habilita el botón y cambia su texto según el estado.
        fireButton.isDisable = animating
        fireButton.text = if (animating) "DISPARANDO..." else "DISPARAR"
    }

    /** Crea los sliders, etiquetas, botón y sus listeners de cambio. */
    private fun createControls() {
        // --- Creación de los sliders (min, max, valor inicial, incremento) ---
        angleSlider = createSlider(0.0, 90.0, state.angle, 1.0)        // 0-90 grados.
        velocitySlider = createSlider(10.0, 100.0, state.velocity, 1.0) // 10-100 m/s.
        gravitySlider = createSlider(1.0, 30.0, state.gravity, 0.1)     // 1-30 m/s².
        massSlider = createSlider(1.0, 50.0, state.mass, 1.0)           // 1-50 kg.

        // --- Etiquetas de los valores iniciales ---
        angleLabel = createValueLabel("${state.angle.toInt()}°")            // Ángulo inicial.
        velocityLabel = createValueLabel(state.velocity.toInt().toString()) // Velocidad inicial.
        gravityLabel = createValueLabel(String.format("%.1f", state.gravity)) // Gravedad inicial.
        massLabel = createValueLabel(state.mass.toInt().toString())          // Masa inicial.

        // --- Botón de disparo ---
        fireButton = Button("DISPARAR").apply {
            font = Font.font("System", FontWeight.BOLD, 14.0)  // Tipografía del botón.
            prefWidth = 260.0                                  // Ancho del botón.
            prefHeight = 45.0                                  // Alto del botón.
            // Estilo: gradiente de color y texto blanco.
            style = """
                -fx-background-color: linear-gradient(to right, #e94560, #c73650);
                -fx-text-fill: white;
                -fx-background-radius: 8;
                -fx-cursor: hand;
            """.trimIndent()
            // Al presionarlo invoca al callback de disparo.
            setOnAction { onFire() }
        }

        // --- Listeners: mantienen el estado sincronizado con los sliders ---
        // Al mover el ángulo: actualiza estado, etiqueta y repinta si no está animando.
        angleSlider.valueProperty().addListener { _, _, newVal ->
            state.angle = newVal.toDouble()          // Guarda el ángulo en el estado.
            angleLabel.text = "${state.angle.toInt()}°"  // Actualiza la etiqueta.
            // Si no hay proyectil en vuelo, repinta (rotación del cañón en vivo).
            if (!state.isAnimating) onAngleChange()
        }
        // Al mover la velocidad: actualiza estado y etiqueta.
        velocitySlider.valueProperty().addListener { _, _, newVal ->
            state.velocity = newVal.toDouble()                     // Guarda la velocidad.
            velocityLabel.text = state.velocity.toInt().toString() // Actualiza la etiqueta.
        }
        // Al mover la gravedad: actualiza estado y etiqueta (con 1 decimal).
        gravitySlider.valueProperty().addListener { _, _, newVal ->
            state.gravity = newVal.toDouble()                       // Guarda la gravedad.
            gravityLabel.text = String.format("%.1f", state.gravity) // Actualiza la etiqueta.
        }
        // Al mover la masa: actualiza estado y etiqueta.
        massSlider.valueProperty().addListener { _, _, newVal ->
            state.mass = newVal.toDouble()                     // Guarda la masa.
            massLabel.text = state.mass.toInt().toString()     // Actualiza la etiqueta.
        }
    }

    /**
     * Crea un slider con estilo consistente.
     *
     * @param min Valor mínimo.
     * @param max Valor máximo.
     * @param initial Valor inicial.
     * @param blockIncrement Incremento al usar las flechas.
     */
    private fun createSlider(min: Double, max: Double, initial: Double, blockIncrement: Double): Slider {
        return Slider(min, max, initial).apply {
            setShowTickLabels(true)                  // Muestra números en la pista.
            setShowTickMarks(true)                  // Muestra marcas.
            majorTickUnit = if (blockIncrement == 1.0) 10.0 else 5.0  // Gran división.
            minorTickCount = 4                       // Subdivisiones entre marcas.
            this.blockIncrement = blockIncrement     // Incremento con las flechas.
            setSnapToTicks(blockIncrement == 1.0)   // Ajusta a marcas en valores enteros.
            prefWidth = 260.0                        // Ancho del slider.
            // Estilo de colores del slider.
            style = """
                -fx-control-inner-background: #0f3460;
                -fx-base: #16213e;
            """.trimIndent()
        }
    }

    /** Crea una etiqueta de valor en color rojo y fuente monospace. */
    private fun createValueLabel(text: String): Label {
        return Label(text).apply {
            font = Font.font("Monospace", FontWeight.BOLD, 13.0)  // Fuente de valor.
            textFill = Color.rgb(233, 69, 96)                      // Color rojo del valor.
            minWidth = 55.0                                        // Ancho mínimo fijo.
            style = "-fx-alignment: center-right;"                  // Alinea el texto a la derecha.
        }
    }

    /** Crea una fila de control: etiqueta de descripción + slider + valor. */
    private fun createControlRow(labelText: String, slider: Slider, valueLabel: Label): VBox {
        // Etiqueta de descripción del parámetro.
        val label = Label(labelText).apply {
            font = Font.font("System", FontWeight.MEDIUM, 11.0)  // Tipografía pequeña.
            textFill = Color.rgb(160, 174, 192)                   // Color gris claro.
        }
        // Fila horizontal: slider a la izquierda y valor a la derecha.
        val sliderRow = HBox(12.0, slider, valueLabel)
        sliderRow.alignment = Pos.CENTER   // Centra la fila.
        // Devuelve la fila completa en un VBox (descripción arriba, fila abajo).
        return VBox(8.0, label, sliderRow)
    }

    /** Crea la leyenda de colores de la escena (cañón, suelo, proyectil). */
    private fun createLegend(): HBox {
        // Defines los colores y sus textos correspondientes.
        val items = listOf(
            Pair("#e94560", "Cañón"),
            Pair("#4a5568", "Suelo"),
            Pair("#4299e1", "Proyectil")
        )
        // Contenedor horizontal de la leyenda.
        val hbox = HBox(20.0)
        hbox.alignment = Pos.CENTER   // Centra la leyenda.
        // Crea un ítem (caja de color + texto) por cada color de la lista.
        for ((color, text) in items) {
            val itemBox = HBox(6.0)   // Caja del ítem.
            itemBox.alignment = Pos.CENTER
            // Rectángulo pequeño de muestra del color.
            val colorBox = Region().apply {
                prefWidth = 16.0      // Ancho de la muestra.
                prefHeight = 4.0      // Alto de la muestra.
                style = "-fx-background-color: $color; -fx-background-radius: 2;"
            }
            // Texto que acompaña a la muestra de color.
            val textLabel = Label(text).apply {
                font = Font.font(10.0)        // Texto pequeño.
                textFill = Color.rgb(113, 128, 150)  // Color gris.
            }
            // Agrega la muestra y el texto al ítem.
            itemBox.children.addAll(colorBox, textLabel)
            // Agrega el ítem al contenedor de la leyenda.
            hbox.children.add(itemBox)
        }
        return hbox
    }
}
