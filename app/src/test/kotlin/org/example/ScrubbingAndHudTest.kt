package org.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.example.model.SimulationState
import org.example.model.TrajectoryMatrix
import org.example.rendering.CoordinateTransformer
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.PathRenderer
import org.example.rendering.SceneConfig
import org.example.scrubbing.FlightSummary
import org.example.scrubbing.ITrajectoryInspector
import org.example.scrubbing.PhysicsTrajectoryInspectorAdapter
import org.example.scrubbing.ResultsHUD
import org.example.scrubbing.ScrubbingController
import org.example.scrubbing.TooltipRenderer
import org.example.scrubbing.TrajectoryPoint

/**
 * Pruebas unitarias de contratos, adaptador e interacción de Scrubbing (SRP).
 */
class ScrubbingAndHudTest {

    private val cfg = SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)
    private val transformer: CoordinateTransformer = DefaultCoordinateTransformer(cfg, pixelsPerMeter = 2.5)
    private val generator = TrajectorySimulationFactory.createGenerator()

    private fun createSampleMatrix(): TrajectoryMatrix =
        generator.generate(PhysicsParams(45.0, 40.0, 9.8, 5.0), samplesPerSecond = 120)

    @Test
    fun test_physics_adapter_correctly_implements_ITrajectoryInspector() {
        val matrix = createSampleMatrix()
        val inspector: ITrajectoryInspector = PhysicsTrajectoryInspectorAdapter(matrix, transformer)
        val nearestPhysical = inspector.findNearestPoint(50.0, 20.0)
        assertTrue(nearestPhysical.speed > 0.0 && nearestPhysical.time > 0.0)

        val nearestFromCursor = inspector.findNearestPointFromCursor(transformer.toCanvasX(50.0), transformer.toCanvasY(20.0))
        assertEquals(nearestPhysical.x, nearestFromCursor.x, 1e-4)
        assertEquals(nearestPhysical.y, nearestFromCursor.y, 1e-4)

        val summary = inspector.getFlightSummary()
        assertEquals(matrix.timeOfFlight, summary.totalFlightTime, 1e-4)
        assertEquals(matrix.range, summary.totalDistance, 1e-4)
    }

    @Test
    fun test_scrubbing_is_disabled_before_projectile_impact_and_only_enabled_post_flight() {
        val controller = ScrubbingController(SimulationState(), transformer, cfg)
        assertFalse(controller.isEnabled || controller.isDragging)
        assertNull(controller.inspector)
        assertFalse(controller.handleMousePressed(100.0, 100.0))

        val matrix = createSampleMatrix()
        controller.enable(PhysicsTrajectoryInspectorAdapter(matrix, transformer))
        assertTrue(controller.isEnabled)
        assertNotNull(controller.currentPoint)
        assertEquals(matrix.timeOfFlight, controller.flightSummary?.totalFlightTime)
    }

    @Test
    fun test_scrubbing_precisely_snaps_projectile_position_to_the_prior_trajectory_curve() {
        val state = SimulationState()
        val controller = ScrubbingController(state, transformer, cfg)
        val matrix = createSampleMatrix()
        controller.enable(PhysicsTrajectoryInspectorAdapter(matrix, transformer))

        val midPt = matrix.points[matrix.points.size / 2]
        val midX = transformer.toCanvasX(midPt.x)
        val midY = transformer.toCanvasY(midPt.y)
        assertTrue(controller.handleMousePressed(midX, midY) && controller.isDragging)
        assertEquals(midX, state.projectileX, 1e-4)
        assertEquals(midY, state.projectileY, 1e-4)

        val quarterPt = matrix.points[matrix.points.size / 4]
        val dragX = transformer.toCanvasX(quarterPt.x)
        val dragY = transformer.toCanvasY(quarterPt.y)
        assertTrue(controller.handleMouseDragged(dragX, dragY))
        assertEquals(dragX, state.projectileX, 1e-4)
        assertTrue(controller.handleMouseReleased() && !controller.isDragging)
    }

    @Test
    fun test_scrubbing_does_not_intercept_clicks_on_the_ground_target_away_from_projectile() {
        val state = SimulationState().apply {
            targetX = 650.0
            targetRadius = 20.0
            projectileX = 100.0
            projectileY = 400.0
        }
        val controller = ScrubbingController(state, transformer, cfg)
        controller.enable(PhysicsTrajectoryInspectorAdapter(createSampleMatrix(), transformer))
        assertFalse(controller.handleMousePressed(state.targetX, cfg.groundY))
        assertFalse(controller.isDragging)
    }

    @Test
    fun test_when_projectile_is_on_top_of_target_clicking_on_the_projectile_selects_the_projectile_because_it_is_in_front() {
        val matrix = createSampleMatrix()
        val impactX = transformer.toCanvasX(matrix.impactPoint.x)
        val impactY = transformer.toCanvasY(matrix.impactPoint.y)
        val state = SimulationState().apply {
            targetX = impactX
            targetRadius = 20.0
            projectileX = impactX
            projectileY = impactY
        }
        val controller = ScrubbingController(state, transformer, cfg)
        controller.enable(PhysicsTrajectoryInspectorAdapter(matrix, transformer))
        assertTrue(controller.handleMousePressed(impactX, impactY))
        assertTrue(controller.isDragging)
    }

    @Test
    fun test_disabling_scrubbing_resets_state_and_clears_inspector() {
        val controller = ScrubbingController(SimulationState(), transformer, cfg)
        controller.enable(PhysicsTrajectoryInspectorAdapter(createSampleMatrix(), transformer))
        assertTrue(controller.isEnabled)
        controller.disable()
        assertFalse(controller.isEnabled)
        assertNull(controller.inspector)
        assertNull(controller.currentPoint)
        assertNull(controller.flightSummary)
    }
}

/**
 * Pruebas unitarias de modelos de telemetría, ResultsHUD, TooltipRenderer y congelación de estela (SRP).
 */
class HudAndTooltipFormattingTest {

    @Test
    fun test_trajectory_point_encapsulates_kinematic_state_and_provides_bilingual_accessors() {
        val point = TrajectoryPoint(x = 120.5, y = 35.8, time = 2.15, height = 35.8, speed = 28.4)
        assertEquals(120.5, point.x)
        assertEquals(point.time, point.tiempo)
        assertEquals(point.height, point.altura)
        assertEquals(point.speed, point.velocidad)
        assertTrue(point.formatTelemetry().contains("v = 28.40 m/s"))
        assertTrue(point.formatTelemetry().contains("h = 35.80 m"))
    }

    @Test
    fun test_flight_summary_encapsulates_total_flight_time_and_total_distance() {
        val summary = FlightSummary(totalFlightTime = 5.77, totalDistance = 163.26)
        assertEquals(5.77, summary.tiempoTotalCaida)
        assertEquals(163.26, summary.distanciaRecorrida)
        assertEquals("5.77 s", summary.formattedTime())
        assertEquals("163.26 m", summary.formattedDistance())
    }

    @Test
    fun test_results_HUD_formats_total_flight_time_and_range_clearly() {
        val hud = ResultsHUD(x = 30.0, y = 30.0)
        val lines = hud.formatLines(FlightSummary(totalFlightTime = 4.25, totalDistance = 120.80))
        assertEquals(3, lines.size)
        assertEquals("RESULTADOS DEL VUELO", lines[0])
        assertTrue(lines[1].contains("4.25 s"))
        assertTrue(lines[2].contains("120.80 m"))
    }

    @Test
    fun test_tooltip_renderer_formats_instantaneous_speed_and_height_in_real_time() {
        val tooltip = TooltipRenderer()
        val lines = tooltip.formatLines(TrajectoryPoint(x = 40.0, y = 25.5, time = 1.35, height = 25.5, speed = 31.85))
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("31.85 m/s"))
        assertTrue(lines[1].contains("25.50 m"))
    }

    @Test
    fun test_tooltip_bounds_stay_clamped_within_canvas_boundaries() {
        val tooltip = TooltipRenderer(width = 150.0, height = 50.0, verticalOffset = 15.0)
        val boundsTop = tooltip.calculateTooltipBounds(200.0, 5.0, 900.0, 550.0)
        assertTrue(boundsTop.y >= 10.0 && boundsTop.y > 5.0)
        val boundsRight = tooltip.calculateTooltipBounds(895.0, 300.0, 900.0, 550.0)
        assertTrue(boundsRight.right() <= 890.0)
        val boundsLeft = tooltip.calculateTooltipBounds(2.0, 300.0, 900.0, 550.0)
        assertTrue(boundsLeft.x >= 10.0)
    }

    @Test
    fun test_path_renderer_freeze_maintains_trail_opacity_without_fading_during_scrubbing() {
        val pathRenderer = PathRenderer(gc = null, fadeDelaySeconds = 2.0, fadeDurationSeconds = 1.0, baseAlpha = 0.55)
        val launchNano = 10_000_000_000L
        pathRenderer.recordLaunch(launchNano)
        pathRenderer.addPoint(10.0, 10.0)
        pathRenderer.addPoint(50.0, 50.0)
        val laterNano = launchNano + 5_000_000_000L
        assertEquals(1.0, pathRenderer.getFadeProgress(laterNano))
        pathRenderer.freeze()
        assertTrue(pathRenderer.isFrozen)
        assertEquals(0.55, pathRenderer.getOpacity(laterNano))
        pathRenderer.clear()
        assertFalse(pathRenderer.isFrozen)
    }
}
