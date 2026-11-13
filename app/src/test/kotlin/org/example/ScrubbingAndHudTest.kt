package org.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.example.model.ITrajectoryMatrixSummary
import org.example.rendering.PathRenderer
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
class ScrubbingAndHudTest : ISimulationTestSuite {

    private val fixture: Triple<TestSceneGeometry, TestStateBuilderA, TestMatrixSampleBuilder> = Triple(
        TestSceneGeometry(),
        TestStateBuilderA(),
        TestMatrixSampleBuilder()
    )

    override fun suiteName(): String {
        return "ScrubbingAndHudTest"
    }

    private fun createSampleMatrix(): ITrajectoryMatrixSummary {
        return fixture.third.matrix(vel = 40.0, samples = 120)
    }

    @Test
    fun test_physics_adapter_correctly_implements_ITrajectoryInspector() {
        val matrix = createSampleMatrix()
        val transformer = fixture.first.transformer()
        val inspector: ITrajectoryInspector = PhysicsTrajectoryInspectorAdapter(matrix, transformer)
        val nearestPhysical = inspector.findNearestPoint(50.0, 20.0)
        assertTrue(nearestPhysical.speedValue() > 0.0 && nearestPhysical.timeValue() > 0.0)
        val nearestFromCursor = inspector.findNearestPointFromCursor(transformer.toCanvasX(50.0), transformer.toCanvasY(20.0))
        assertEquals(nearestPhysical.coordX(), nearestFromCursor.coordX(), 1e-4)
        assertEquals(nearestPhysical.coordY(), nearestFromCursor.coordY(), 1e-4)
        val summary = inspector.getFlightSummary()
        assertEquals(matrix.totalTimeOfFlight(), summary.flightTimeSeconds(), 1e-4)
        assertEquals(matrix.horizontalRange(), summary.rangeMeters(), 1e-4)
    }

    @Test
    fun test_scrubbing_is_disabled_before_projectile_impact_and_only_enabled_post_flight() {
        val geom = fixture.first
        val controller = ScrubbingController(fixture.second.freshState(), geom.transformer(), geom.config())
        assertFalse(controller.checkEnabled() || controller.checkDragging())
        assertNull(controller.currentInspector())
        assertFalse(controller.handleMousePressed(100.0, 100.0))
        val matrix = createSampleMatrix()
        controller.enable(PhysicsTrajectoryInspectorAdapter(matrix, geom.transformer()))
        assertTrue(controller.checkEnabled())
        assertNotNull(controller.selectedTrajectoryPoint())
        assertEquals(matrix.totalTimeOfFlight(), controller.currentFlightSummary()?.flightTimeSeconds())
    }

    @Test
    fun test_scrubbing_precisely_snaps_projectile_position_to_the_prior_trajectory_curve() {
        val state = fixture.second.freshState()
        val transformer = fixture.first.transformer()
        val controller = ScrubbingController(state, transformer, fixture.first.config())
        val matrix = createSampleMatrix()
        controller.enable(PhysicsTrajectoryInspectorAdapter(matrix, transformer))
        val pts = matrix.trajectoryPoints()
        val midPt = pts[pts.size / 2]
        val midX = transformer.toCanvasX(midPt.posX())
        val midY = transformer.toCanvasY(midPt.posY())
        assertTrue(controller.handleMousePressed(midX, midY) && controller.checkDragging())
        assertEquals(midX, state.currentProjectileCoords().first, 1e-4)
        assertEquals(midY, state.currentProjectileCoords().second, 1e-4)
        val quarterPt = pts[pts.size / 4]
        val dragX = transformer.toCanvasX(quarterPt.posX())
        assertTrue(controller.handleMouseDragged(dragX, transformer.toCanvasY(quarterPt.posY())))
        assertEquals(dragX, state.currentProjectileCoords().first, 1e-4)
        assertTrue(controller.handleMouseReleased() && !controller.checkDragging())
    }

    @Test
    fun test_scrubbing_does_not_intercept_clicks_on_the_ground_target_away_from_projectile() {
        val state = fixture.second.freshState()
        state.changeTargetX(650.0)
        state.changeTargetRadius(20.0)
        state.updateProjectilePosition(100.0, 400.0)
        val geom = fixture.first
        val controller = ScrubbingController(state, geom.transformer(), geom.config())
        controller.enable(PhysicsTrajectoryInspectorAdapter(createSampleMatrix(), geom.transformer()))
        assertFalse(controller.handleMousePressed(state.currentTargetX(), geom.config().groundLevelY()))
        assertFalse(controller.checkDragging())
    }

    @Test
    fun test_when_projectile_is_on_top_of_target_clicking_on_the_projectile_selects_the_projectile_because_it_is_in_front() {
        val matrix = createSampleMatrix()
        val geom = fixture.first
        val impactX = geom.transformer().toCanvasX(matrix.finalPoint().posX())
        val impactY = geom.transformer().toCanvasY(matrix.finalPoint().posY())
        val state = fixture.second.freshState()
        state.changeTargetX(impactX)
        state.changeTargetRadius(20.0)
        state.updateProjectilePosition(impactX, impactY)
        val controller = ScrubbingController(state, geom.transformer(), geom.config())
        controller.enable(PhysicsTrajectoryInspectorAdapter(matrix, geom.transformer()))
        assertTrue(controller.handleMousePressed(impactX, impactY) && controller.checkDragging())
    }

    @Test
    fun test_disabling_scrubbing_resets_state_and_clears_inspector() {
        val geom = fixture.first
        val controller = ScrubbingController(fixture.second.freshState(), geom.transformer(), geom.config())
        controller.enable(PhysicsTrajectoryInspectorAdapter(createSampleMatrix(), geom.transformer()))
        assertTrue(controller.checkEnabled())
        controller.disable()
        assertFalse(controller.checkEnabled())
        assertNull(controller.currentInspector())
        assertNull(controller.selectedTrajectoryPoint())
        assertNull(controller.currentFlightSummary())
    }
}

/**
 * Pruebas unitarias de modelos de telemetría, ResultsHUD, TooltipRenderer y congelación de estela (SRP).
 */
class HudAndTooltipFormattingTest : ISimulationTestSuite {

    override fun suiteName(): String {
        return "HudAndTooltipFormattingTest"
    }

    @Test
    fun test_trajectory_point_encapsulates_kinematic_state_and_provides_bilingual_accessors() {
        val point = TrajectoryPoint(x = 120.5, y = 35.8, time = 2.15, height = 35.8, speed = 28.4)
        assertEquals(120.5, point.coordX())
        assertEquals(2.15, point.timeValue())
        assertEquals(35.8, point.heightValue())
        assertEquals(28.4, point.speedValue())
        assertTrue(point.formatTelemetry().contains("v = 28.40 m/s"))
        assertTrue(point.formatTelemetry().contains("h = 35.80 m"))
    }

    @Test
    fun test_flight_summary_encapsulates_total_flight_time_and_total_distance() {
        val summary = FlightSummary(totalFlightTime = 5.77, totalDistance = 163.26)
        assertEquals(5.77, summary.flightTimeSeconds())
        assertEquals(163.26, summary.rangeMeters())
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
        assertTrue(boundsTop.topY() >= 10.0 && boundsTop.topY() > 5.0)
        val boundsRight = tooltip.calculateTooltipBounds(895.0, 300.0, 900.0, 550.0)
        assertTrue(boundsRight.right() <= 890.0)
        val boundsLeft = tooltip.calculateTooltipBounds(2.0, 300.0, 900.0, 550.0)
        assertTrue(boundsLeft.leftX() >= 10.0)
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
        assertTrue(pathRenderer.checkFrozen())
        assertEquals(0.55, pathRenderer.getOpacity(laterNano))
        pathRenderer.clear()
        assertFalse(pathRenderer.checkFrozen())
    }
}
