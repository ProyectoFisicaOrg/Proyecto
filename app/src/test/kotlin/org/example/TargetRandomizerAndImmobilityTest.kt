package org.example

import org.example.model.SimulationState
import org.example.rendering.ITargetRandomizer
import org.example.rendering.SceneConfig
import org.example.rendering.TargetRenderer
import org.example.rendering.UniformTargetRandomizer
import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pruebas unitarias para la funcionalidad de objetivo aleatorio e inmovilidad durante el vuelo.
 *
 * Valida los requerimientos:
 *  1. Cálculo de posición aleatoria del objetivo dentro de los límites válidos del suelo (ITargetRandomizer + UniformTargetRandomizer).
 *  2. Inmovilidad del objetivo: cuando la pelota sale disparada (state.isAnimating == true),
 *     el objetivo NO se puede mover (ni por arrastre ni por aleatorización).
 *  3. Inversión de dependencias (DIP) y extensibilidad (OCP) permitiendo inyectar generadores deterministas.
 */
class TargetRandomizerAndImmobilityTest {

    private val cfg = SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)

    // ─── 1. Pruebas de ITargetRandomizer (SRP + DIP) ───────────────────────────

    @Test
    fun `uniform target randomizer produces values strictly within bounds`() {
        val randomizer = UniformTargetRandomizer(Random(42))
        val minX = 100.0
        val maxX = 800.0

        for (i in 0..100) {
            val randomX = randomizer.calculateRandomX(minX, maxX)
            assertTrue(randomX >= minX, "randomX ($randomX) no puede ser menor que minX ($minX)")
            assertTrue(randomX <= maxX, "randomX ($randomX) no puede ser mayor que maxX ($maxX)")
        }
    }

    @Test
    fun `uniform target randomizer with seeded random is deterministic`() {
        val random1 = Random(12345)
        val random2 = Random(12345)
        val randomizer1 = UniformTargetRandomizer(random1)
        val randomizer2 = UniformTargetRandomizer(random2)

        val val1 = randomizer1.calculateRandomX(50.0, 500.0)
        val val2 = randomizer2.calculateRandomX(50.0, 500.0)

        assertEquals(val1, val2, 1e-9, "Con la misma semilla, el resultado debe ser exactamente reproducible")
    }

    // ─── 2. Pruebas de Límites e Inmovilidad del Objetivo en TargetRenderer ────

    @Test
    fun `target renderer valid X range respects cannon margin and right edge margin`() {
        val state = SimulationState()
        val dummyCanvas = javafx.scene.canvas.Canvas(cfg.canvasWidth, cfg.canvasHeight)
        val targetRenderer = TargetRenderer(
            gc = dummyCanvas.graphicsContext2D,
            cfg = cfg,
            canvas = dummyCanvas,
            state = state,
            onTargetPositionChanged = {},
            onRepaint = {}
        )

        val range = targetRenderer.getValidXRange()
        val expectedMin = cfg.cannonBaseX + 45.0
        val expectedMax = cfg.canvasWidth - targetRenderer.targetRadius - 10.0

        assertEquals(expectedMin, range.start, 1e-4)
        assertEquals(expectedMax, range.endInclusive, 1e-4)
        assertTrue(range.start < range.endInclusive, "El rango debe ser válido y positivo")
    }

    @Test
    fun `randomizePosition updates targetX within valid range when not animating`() {
        val state = SimulationState().apply {
            isAnimating = false
            targetX = 200.0
        }
        val dummyCanvas = javafx.scene.canvas.Canvas(cfg.canvasWidth, cfg.canvasHeight)
        var notifiedPosition: Double? = null
        var repainted = false

        val targetRenderer = TargetRenderer(
            gc = dummyCanvas.graphicsContext2D,
            cfg = cfg,
            canvas = dummyCanvas,
            state = state,
            onTargetPositionChanged = { notifiedPosition = it },
            onRepaint = { repainted = true }
        )

        val success = targetRenderer.randomizePosition()

        assertTrue(success, "Debe retornar true al reubicar con éxito cuando no hay animación activa")
        val range = targetRenderer.getValidXRange()
        assertTrue(state.targetX in range.start..range.endInclusive, "La nueva posición debe estar en el rango válido")
        assertEquals(state.targetX, notifiedPosition, "Debe notificar la nueva coordenada X")
        assertTrue(repainted, "Debe solicitar el redibujado de la escena")
    }

    @Test
    fun `randomizePosition is strictly rejected when projectile is in flight`() {
        val initialX = 500.0
        val state = SimulationState().apply {
            isAnimating = true // Pelota disparada en vuelo
            targetX = initialX
        }
        val dummyCanvas = javafx.scene.canvas.Canvas(cfg.canvasWidth, cfg.canvasHeight)
        var wasNotified = false

        val targetRenderer = TargetRenderer(
            gc = dummyCanvas.graphicsContext2D,
            cfg = cfg,
            canvas = dummyCanvas,
            state = state,
            onTargetPositionChanged = { wasNotified = true },
            onRepaint = {}
        )

        // Intentar aleatorizar la posición mientras la pelota está en vuelo
        val success = targetRenderer.randomizePosition()

        assertFalse(success, "Debe retornar false y rechazar la acción cuando la pelota está en vuelo")
        assertEquals(initialX, state.targetX, "La posición del objetivo NO debe cambiar mientras la pelota está en vuelo")
        assertFalse(wasNotified, "No debe emitir notificación de cambio")
        assertFalse(targetRenderer.canMoveTarget(), "canMoveTarget debe ser false durante el vuelo")
    }

    @Test
    fun `custom target randomizer can be injected conforming to OCP and DIP`() {
        val state = SimulationState().apply {
            isAnimating = false
            targetX = 100.0
        }
        val dummyCanvas = javafx.scene.canvas.Canvas(cfg.canvasWidth, cfg.canvasHeight)

        val targetRenderer = TargetRenderer(
            gc = dummyCanvas.graphicsContext2D,
            cfg = cfg,
            canvas = dummyCanvas,
            state = state,
            onTargetPositionChanged = {},
            onRepaint = {}
        )

        // Mock / Estrategia determinista inyectada que siempre devuelve el punto medio
        val midpointRandomizer = object : ITargetRandomizer {
            override fun calculateRandomX(minX: Double, maxX: Double): Double = (minX + maxX) / 2.0
        }

        val success = targetRenderer.randomizePosition(midpointRandomizer)
        assertTrue(success)

        val expectedMidpoint = (targetRenderer.getValidXRange().start + targetRenderer.getValidXRange().endInclusive) / 2.0
        assertEquals(expectedMidpoint, state.targetX, 1e-4)
    }

    @Test
    fun `canMoveTarget becomes true once flight finishes or projectile is at rest`() {
        val state = SimulationState().apply {
            isAnimating = true
        }
        val dummyCanvas = javafx.scene.canvas.Canvas(cfg.canvasWidth, cfg.canvasHeight)
        val targetRenderer = TargetRenderer(
            gc = dummyCanvas.graphicsContext2D,
            cfg = cfg,
            canvas = dummyCanvas,
            state = state,
            onTargetPositionChanged = {},
            onRepaint = {}
        )

        assertFalse(targetRenderer.canMoveTarget(), "Durante el vuelo debe ser inamovible")

        // Finaliza el vuelo
        state.isAnimating = false
        assertTrue(targetRenderer.canMoveTarget(), "Al terminar el vuelo vuelve a ser movible")
    }
}
