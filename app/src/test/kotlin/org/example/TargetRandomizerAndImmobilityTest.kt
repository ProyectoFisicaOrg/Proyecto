package org.example

import java.util.Random
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
import org.example.rendering.UniformTargetRandomizer

/**
 * Estrategia determinista de prueba para validar DIP/OCP en el reposicionamiento del objetivo.
 */
private class MidpointTargetRandomizer : ITargetRandomizer {
    override fun calculateRandomX(minX: Double, maxX: Double): Double = (minX + maxX) / 2.0
}

/**
 * Pruebas unitarias de objetivo aleatorio e inmovilidad durante el vuelo (SRP).
 */
class TargetRandomizerAndImmobilityTest {

    private val cfg = SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)

    private fun buildRenderer(state: SimulationState, onMoved: (Double) -> Unit = {}, onRepaint: () -> Unit = {}): TargetRenderer {
        val dummyCanvas = Canvas(cfg.canvasWidth, cfg.canvasHeight)
        return TargetRenderer(dummyCanvas.graphicsContext2D, cfg, dummyCanvas, state, onMoved, onRepaint)
    }

    @Test
    fun test_uniform_target_randomizer_produces_values_strictly_within_bounds() {
        val randomizer = UniformTargetRandomizer(Random(42))
        for (i in 0..100) {
            val randomX = randomizer.calculateRandomX(100.0, 800.0)
            assertTrue(randomX in 100.0..800.0)
        }
    }

    @Test
    fun test_uniform_target_randomizer_with_seeded_random_is_deterministic() {
        val val1 = UniformTargetRandomizer(Random(12345)).calculateRandomX(50.0, 500.0)
        val val2 = UniformTargetRandomizer(Random(12345)).calculateRandomX(50.0, 500.0)
        assertEquals(val1, val2, 1e-9)
    }

    @Test
    fun test_target_renderer_valid_X_range_respects_cannon_margin_and_right_edge_margin() {
        val renderer = buildRenderer(SimulationState())
        val range = renderer.getValidXRange()
        assertEquals(cfg.cannonBaseX + 45.0, range.start, 1e-4)
        assertEquals(cfg.canvasWidth - renderer.targetRadius - 10.0, range.endInclusive, 1e-4)
        assertTrue(range.start < range.endInclusive)
    }

    @Test
    fun test_randomizePosition_updates_targetX_within_valid_range_when_not_animating() {
        val state = SimulationState().apply {
            isAnimating = false
            targetX = 200.0
        }
        var notifiedPosition: Double? = null
        var repainted = false
        val renderer = buildRenderer(state, onMoved = { notifiedPosition = it }, onRepaint = { repainted = true })
        assertTrue(renderer.randomizePosition())
        val range = renderer.getValidXRange()
        assertTrue(state.targetX in range.start..range.endInclusive)
        assertEquals(state.targetX, notifiedPosition)
        assertTrue(repainted)
    }

    @Test
    fun test_randomizePosition_is_strictly_rejected_when_projectile_is_in_flight() {
        val state = SimulationState().apply {
            isAnimating = true
            targetX = 500.0
        }
        var wasNotified = false
        val renderer = buildRenderer(state, onMoved = { wasNotified = true })
        assertFalse(renderer.randomizePosition())
        assertEquals(500.0, state.targetX)
        assertFalse(wasNotified)
        assertFalse(renderer.canMoveTarget())
    }

    @Test
    fun test_custom_target_randomizer_can_be_injected_conforming_to_OCP_and_DIP() {
        val state = SimulationState().apply {
            isAnimating = false
            targetX = 100.0
        }
        val renderer = buildRenderer(state)
        assertTrue(renderer.randomizePosition(MidpointTargetRandomizer()))
        val range = renderer.getValidXRange()
        assertEquals((range.start + range.endInclusive) / 2.0, state.targetX, 1e-4)
    }

    @Test
    fun test_canMoveTarget_becomes_true_once_flight_finishes_or_projectile_is_at_rest() {
        val state = SimulationState().apply { isAnimating = true }
        val renderer = buildRenderer(state)
        assertFalse(renderer.canMoveTarget())
        state.isAnimating = false
        assertTrue(renderer.canMoveTarget())
    }
}

/**
 * Pruebas unitarias del HUD dinámico de distancia al objetivo al arrastrar y aleatorizar (SRP).
 */
class TargetDistanceHudTest {

    private val cfg = SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)
    private val transformer = DefaultCoordinateTransformer(cfg, pixelsPerMeter = 2.5)

    @Test
    fun test_target_distance_hud_calculates_and_formats_dynamic_distance_on_move_and_randomize() {
        val state = SimulationState().apply { targetX = 750.0 }
        val hud = TargetDistanceHud(cfg, transformer)
        assertEquals(264.0, hud.distanceMeters(state.targetX), 1e-4)
        assertEquals("264.00 m", hud.formatDistance(state.targetX))

        state.targetX = 340.0
        assertEquals(100.0, hud.distanceMeters(state.targetX), 1e-4)
        assertEquals("100.00 m", hud.formatDistance(state.targetX))

        val dummyCanvas = Canvas(cfg.canvasWidth, cfg.canvasHeight)
        val renderer = TargetRenderer(dummyCanvas.graphicsContext2D, cfg, dummyCanvas, state, {}, {})
        assertTrue(renderer.randomizePosition(MidpointTargetRandomizer()))
        val expectedMeters = transformer.toPhysicsX(state.targetX)
        assertEquals(expectedMeters, hud.distanceMeters(state.targetX), 1e-4)
    }
}
