package org.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.example.model.ISimulationState
import org.example.rendering.ITargetRandomizer
import org.example.rendering.TargetDistanceHud
import org.example.ui.ITargetDistanceSpec
import org.example.ui.ITargetOverlayView
import org.example.ui.ITextInputPort
import org.example.ui.TargetDistanceController
import org.example.ui.TargetDistanceSpec

/**
 * Doble de prueba en memoria para el puerto de texto del campo bajo el blanco (DIP).
 */
private data class FakeOverlayTextPort(private val initial: String) : ITextInputPort {
    private val slot: MutableList<Pair<Pair<String, Boolean>, Triple<((String) -> Boolean)?, ((String) -> Unit)?, (() -> Unit)?>>> =
        mutableListOf(Pair(Pair(initial, false), Triple(null, null, null)))

    override fun currentText(): String {
        return slot[0].first.first
    }

    override fun updateText(newText: String) {
        val cur = slot[0]
        val allowed = cur.second.first?.invoke(newText) ?: true
        if (!allowed) return
        slot[0] = Pair(Pair(newText, cur.first.second), cur.second)
        cur.second.second?.invoke(newText)
    }

    override fun setDisabled(disabled: Boolean) {
        val cur = slot[0]
        slot[0] = Pair(Pair(cur.first.first, disabled), cur.second)
    }

    override fun installFilter(predicate: (String) -> Boolean) {
        val cur = slot[0]
        slot[0] = Pair(cur.first, Triple(predicate, cur.second.second, cur.second.third))
    }

    override fun onTextChanged(listener: (String) -> Unit) {
        val cur = slot[0]
        slot[0] = Pair(cur.first, Triple(cur.second.first, listener, cur.second.third))
    }

    override fun onCommitRequested(action: () -> Unit) {
        val cur = slot[0]
        slot[0] = Pair(cur.first, Triple(cur.second.first, cur.second.second, action))
    }

    fun triggerCommit() {
        slot[0].second.third?.invoke()
    }
}

/**
 * Doble de prueba en memoria para la vista flotante bajo el blanco (DIP).
 */
private data class FakeTargetOverlayView(
    private val slot: MutableList<Triple<FakeOverlayTextPort, Double, Boolean>> = mutableListOf(Triple(FakeOverlayTextPort("264.00"), 750.0, false))
) : ITargetOverlayView {

    override fun textPort(): ITextInputPort {
        return slot[0].first
    }

    override fun updateHorizontalPosition(targetCanvasX: Double) {
        val cur = slot[0]
        slot[0] = Triple(cur.first, targetCanvasX, cur.third)
    }

    override fun setLocked(locked: Boolean) {
        val cur = slot[0]
        cur.first.setDisabled(locked)
        slot[0] = Triple(cur.first, cur.second, locked)
    }

    override fun isLocked(): Boolean {
        return slot[0].third
    }

    fun positionedCanvasX(): Double {
        return slot[0].second
    }

    fun typeAndCommit(input: String) {
        val cur = slot[0]
        if (cur.third) return
        cur.first.updateText(input)
        cur.first.triggerCommit()
    }
}

/**
 * Estrategia determinista para probar sincronización con objetivo aleatorio.
 */
private data class FixedTargetRandomizer(
    private val chosenCanvasX: Double
) : ITargetRandomizer {
    override fun calculateRandomX(minX: Double, maxX: Double): Double {
        return chosenCanvasX.coerceIn(minX, maxX)
    }

    fun configuredX(): Double {
        return chosenCanvasX
    }
}

/**
 * Pruebas unitarias de límites [18.00, 312.00], bloqueo en vuelo y sincronización bidireccional del campo del blanco (SRP).
 */
class TargetDistanceEditableFieldTest : ISimulationTestSuite {

    private val fixture: Pair<TestTargetViewBuilder, TestStateBuilderB> = Pair(TestTargetViewBuilder(), TestStateBuilderB())

    override fun suiteName(): String {
        return "TargetDistanceEditableFieldTest"
    }

    @Test
    fun test_spec_clamps_below_18_and_above_312_and_restores_default_when_empty_or_invalid() {
        val spec: ITargetDistanceSpec = TargetDistanceSpec(minVal = 18.0, maxVal = 312.0, defaultVal = 264.0)
        assertEquals(18.0, spec.resolveCommittedMeters("0"), 1e-4)
        assertEquals(18.0, spec.resolveCommittedMeters("12.40"), 1e-4)
        assertEquals(312.0, spec.resolveCommittedMeters("350"), 1e-4)
        assertEquals(312.0, spec.resolveCommittedMeters("999.99"), 1e-4)
        assertEquals(150.25, spec.resolveCommittedMeters("150,25"), 1e-4)
        assertEquals(264.0, spec.resolveCommittedMeters(""), 1e-4)
        assertEquals(264.0, spec.resolveCommittedMeters("   "), 1e-4)
        assertEquals(264.0, spec.resolveCommittedMeters("."), 1e-4)
    }

    private fun newController(state: ISimulationState, overlay: FakeTargetOverlayView): TargetDistanceController {
        return TargetDistanceController(TargetDistanceSpec(), fixture.first.geometry().transformer(), overlay, { state.changeTargetX(it) })
    }

    @Test
    fun test_typing_distance_moves_target_and_updates_top_hud_and_rejects_letters() {
        val geom = fixture.first.geometry()
        val state = fixture.second.freshState()
        state.changeTargetX(750.0)
        val overlay = FakeTargetOverlayView()
        val topHud = TargetDistanceHud(geom.config(), geom.transformer())
        newController(state, overlay).bind()
        overlay.textPort().updateText("abc$#@")
        assertEquals("264.00", overlay.textPort().currentText())
        overlay.typeAndCommit("100")
        assertEquals("100.00", overlay.textPort().currentText())
        assertEquals(340.0, state.currentTargetX(), 1e-4)
        assertEquals("100.00 m", topHud.formatDistance(state.currentTargetX()))
        assertClampedDistances(overlay, state)
    }

    private fun assertClampedDistances(overlay: FakeTargetOverlayView, state: ISimulationState) {
        overlay.typeAndCommit("5")
        assertEquals("18.00", overlay.textPort().currentText())
        assertEquals(135.0, state.currentTargetX(), 1e-4)
        overlay.typeAndCommit("500")
        assertEquals("312.00", overlay.textPort().currentText())
        assertEquals(870.0, state.currentTargetX(), 1e-4)
    }

    @Test
    fun test_dragging_or_randomizing_target_updates_editable_field_in_real_time() {
        val state = fixture.second.freshState()
        state.changeTargetX(750.0)
        val overlay = FakeTargetOverlayView()
        val controller = newController(state, overlay)
        controller.bind()
        val renderer = fixture.first.createObservedRenderer(state, { newX -> state.changeTargetX(newX); controller.syncFromCanvasX(newX) }, {})
        val randomizer = FixedTargetRandomizer(590.0)
        assertTrue(renderer.randomizePosition(randomizer) && randomizer.configuredX() == 590.0)
        assertEquals("200.00", overlay.textPort().currentText())
        assertEquals(590.0, overlay.positionedCanvasX(), 1e-4)
    }

    @Test
    fun test_field_locks_on_launch_and_unlocks_only_after_scene_reset() {
        val state = fixture.second.freshState()
        state.changeTargetX(590.0)
        val overlay = FakeTargetOverlayView()
        overlay.textPort().updateText("200.00")
        val controller = newController(state, overlay)
        controller.bind()
        assertFalse(overlay.isLocked())
        controller.prepareForLaunch()
        assertTrue(overlay.isLocked())
        overlay.typeAndCommit("120")
        assertEquals("200.00", overlay.textPort().currentText())
        assertEquals(590.0, state.currentTargetX(), 1e-4)
        controller.unlockForEditing()
        assertFalse(overlay.isLocked())
        overlay.typeAndCommit("120")
        assertEquals("120.00", overlay.textPort().currentText())
        assertEquals(390.0, state.currentTargetX(), 1e-4)
    }
}
