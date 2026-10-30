package org.example

import javafx.scene.canvas.Canvas
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.example.model.SimulationState
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.ITargetRandomizer
import org.example.rendering.SceneConfig
import org.example.rendering.TargetDistanceHud
import org.example.rendering.TargetRenderer
import org.example.ui.ITargetOverlayView
import org.example.ui.ITextInputPort
import org.example.ui.TargetDistanceController
import org.example.ui.TargetDistanceSpec

/**
 * Doble de prueba en memoria para el puerto de texto del campo bajo el blanco (DIP).
 */
private class FakeOverlayTextPort(
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

    fun triggerCommit() {
        commitAction?.invoke()
    }
}

/**
 * Doble de prueba en memoria para la vista flotante bajo el blanco (DIP).
 */
private class FakeTargetOverlayView(
    initialText: String = "264.00"
) : ITargetOverlayView {
    private val port = FakeOverlayTextPort(initialText)
    private var lastCanvasX: Double = 750.0
    private var lockedState: Boolean = false

    override fun textPort(): ITextInputPort = port

    override fun updateHorizontalPosition(targetCanvasX: Double) {
        lastCanvasX = targetCanvasX
    }

    override fun setLocked(locked: Boolean) {
        lockedState = locked
        port.setDisabled(locked)
    }

    override fun isLocked(): Boolean = lockedState

    fun positionedCanvasX(): Double = lastCanvasX

    fun typeAndCommit(input: String) {
        if (lockedState) return
        port.updateText(input)
        port.triggerCommit()
    }
}

/**
 * Estrategia determinista para probar sincronización con objetivo aleatorio.
 */
private class FixedTargetRandomizer(
    private val chosenCanvasX: Double
) : ITargetRandomizer {
    override fun calculateRandomX(minX: Double, maxX: Double): Double =
        chosenCanvasX.coerceIn(minX, maxX)
}

/**
 * Pruebas unitarias de límites [18.00, 312.00], bloqueo en vuelo y sincronización bidireccional del campo del blanco (SRP).
 */
class TargetDistanceEditableFieldTest {

    private val cfg = SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)
    private val transformer = DefaultCoordinateTransformer(cfg, pixelsPerMeter = 2.5)

    @Test
    fun test_spec_clamps_below_18_and_above_312_and_restores_default_when_empty_or_invalid() {
        val spec = TargetDistanceSpec(minVal = 18.0, maxVal = 312.0, defaultVal = 264.0)

        assertEquals(18.0, spec.resolveCommittedMeters("0"), 1e-4)
        assertEquals(18.0, spec.resolveCommittedMeters("12.40"), 1e-4)
        assertEquals(312.0, spec.resolveCommittedMeters("350"), 1e-4)
        assertEquals(312.0, spec.resolveCommittedMeters("999.99"), 1e-4)
        assertEquals(150.25, spec.resolveCommittedMeters("150,25"), 1e-4)

        assertEquals(264.0, spec.resolveCommittedMeters(""), 1e-4)
        assertEquals(264.0, spec.resolveCommittedMeters("   "), 1e-4)
        assertEquals(264.0, spec.resolveCommittedMeters("."), 1e-4)
    }

    @Test
    fun test_typing_distance_moves_target_and_updates_top_hud_and_rejects_letters() {
        val state = SimulationState().apply { targetX = 750.0 }
        val overlay = FakeTargetOverlayView("264.00")
        val topHud = TargetDistanceHud(cfg, transformer)
        TargetDistanceController(TargetDistanceSpec(), transformer, overlay, { state.targetX = it }).bind()

        overlay.textPort().updateText("abc$#@")
        assertEquals("264.00", overlay.textPort().currentText())

        overlay.typeAndCommit("100")
        assertEquals("100.00", overlay.textPort().currentText())
        assertEquals(340.0, state.targetX, 1e-4)
        assertEquals("100.00 m", topHud.formatDistance(state.targetX))

        overlay.typeAndCommit("5")
        assertEquals("18.00", overlay.textPort().currentText())
        assertEquals(135.0, state.targetX, 1e-4)

        overlay.typeAndCommit("500")
        assertEquals("312.00", overlay.textPort().currentText())
        assertEquals(870.0, state.targetX, 1e-4)
    }

    @Test
    fun test_dragging_or_randomizing_target_updates_editable_field_in_real_time() {
        val state = SimulationState().apply { targetX = 750.0 }
        val overlay = FakeTargetOverlayView("264.00")
        val controller = TargetDistanceController(TargetDistanceSpec(), transformer, overlay, { state.targetX = it })
        controller.bind()

        val canvas = Canvas(cfg.canvasWidth, cfg.canvasHeight)
        val renderer = TargetRenderer(
            canvas.graphicsContext2D, cfg, canvas, state,
            onTargetPositionChanged = { newX -> state.targetX = newX; controller.syncFromCanvasX(newX) },
            onRepaint = {}
        )
        assertTrue(renderer.randomizePosition(FixedTargetRandomizer(590.0)))
        assertEquals("200.00", overlay.textPort().currentText())
        assertEquals(590.0, overlay.positionedCanvasX(), 1e-4)
    }

    @Test
    fun test_field_locks_on_launch_and_unlocks_only_after_scene_reset() {
        val state = SimulationState().apply { targetX = 590.0 }
        val overlay = FakeTargetOverlayView("200.00")
        val controller = TargetDistanceController(TargetDistanceSpec(), transformer, overlay, { state.targetX = it })
        controller.bind()

        assertFalse(overlay.isLocked())
        controller.prepareForLaunch()
        assertTrue(overlay.isLocked())

        overlay.typeAndCommit("120")
        assertEquals("200.00", overlay.textPort().currentText())
        assertEquals(590.0, state.targetX, 1e-4)

        controller.unlockForEditing()
        assertFalse(overlay.isLocked())
        overlay.typeAndCommit("120")
        assertEquals("120.00", overlay.textPort().currentText())
        assertEquals(390.0, state.targetX, 1e-4)
    }
}
