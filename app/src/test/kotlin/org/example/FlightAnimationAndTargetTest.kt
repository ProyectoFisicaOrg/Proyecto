package org.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.example.animation.CollisionCallbackAdapter
import org.example.animation.FlightEventHooks
import org.example.animation.ManualFrameTicker
import org.example.collision.CollisionStatus
import org.example.collision.ICollisionResult
import org.example.rendering.PathRenderer

/**
 * Pruebas unitarias de transformación de coordenadas e inicio/interpolación de vuelo (SRP).
 */
class FlightAnimationAndTargetTest : ISimulationTestSuite {

    private val fixture: Pair<TestFlightEngineHarness, TestStateBuilderA> = Pair(TestFlightEngineHarness(), TestStateBuilderA())

    override fun suiteName(): String {
        return "FlightAnimationAndTargetTest"
    }

    @Test
    fun test_transformer_accurately_converts_origin_between_physics_and_canvas() {
        val cfg = fixture.first.geometry().config()
        val transformer = fixture.first.geometry().transformer()
        assertEquals(cfg.basePivotX(), transformer.toCanvasX(0.0))
        assertEquals(cfg.basePivotY(), transformer.toCanvasY(0.0))
        assertEquals(0.0, transformer.toPhysicsX(cfg.basePivotX()))
        assertEquals(0.0, transformer.toPhysicsY(cfg.basePivotY()))
    }

    @Test
    fun test_transformer_roundtrip_maintains_coordinate_integrity() {
        val transformer = fixture.first.geometry().transformer()
        val canvasX = transformer.toCanvasX(150.0)
        val canvasY = transformer.toCanvasY(45.0)
        assertEquals(150.0, transformer.toPhysicsX(canvasX), 1e-9)
        assertEquals(45.0, transformer.toPhysicsY(canvasY), 1e-9)
    }

    @Test
    fun test_transformer_distance_conversion_respects_scale_factor() {
        val transformer = fixture.first.geometry().transformer()
        val pixels = transformer.toCanvasDistance(20.0)
        assertEquals(50.0, pixels)
        assertEquals(20.0, transformer.toPhysicsDistance(pixels))
    }

    @Test
    fun test_animation_engine_begins_at_launch_point_and_starts_ticker() {
        val state = fixture.second.configuredState(40.0)
        val ticker = ManualFrameTicker()
        val trailPoints = mutableListOf<Pair<Double, Double>>()
        var updateCalled = false
        val hooks = FlightEventHooks(onUpdate = { updateCalled = true }, onFinished = {}, onTick = { x, y -> trailPoints.add(Pair(x, y)) })
        val engine = fixture.first.createEngine(state, hooks, ticker)
        engine.start()
        val cfg = fixture.first.geometry().config()
        assertTrue(state.checkAnimating() && ticker.checkRunning() && updateCalled)
        assertEquals(1, trailPoints.size)
        val proj = state.currentProjectileCoords()
        assertEquals(cfg.basePivotX(), proj.first, 1e-6)
        assertEquals(cfg.basePivotY(), proj.second, 1e-6)
    }

    @Test
    fun test_animation_interpolates_smoothly_along_trajectory_time() {
        val state = fixture.second.configuredState(50.0)
        val ticker = ManualFrameTicker()
        val trailPoints = mutableListOf<Pair<Double, Double>>()
        val hooks = FlightEventHooks(onUpdate = {}, onFinished = {}, onTick = { x, y -> trailPoints.add(Pair(x, y)) })
        val engine = fixture.first.createEngine(state, hooks, ticker)
        engine.start()
        val halfNano = ((engine.activeTrajectoryMatrix()!!.totalTimeOfFlight() / 2.0) * 1_000_000_000).toLong()
        ticker.advance(1_000_000_000L)
        ticker.advance(1_000_000_000L + halfNano)
        val cfg = fixture.first.geometry().config()
        val proj = state.currentProjectileCoords()
        assertTrue(proj.first > cfg.basePivotX() && proj.second < cfg.basePivotY())
        assertTrue(trailPoints.size >= 3)
    }

    @Test
    fun test_animation_engine_defaults_to_240_samples_per_second_for_high_fluid_fps() {
        val engine = fixture.first.createEngine(fixture.second.configuredState(20.0), FlightEventHooks({}, {}), ManualFrameTicker())
        assertEquals(240, engine.getSamplesPerSecond())
    }
}

/**
 * Pruebas unitarias de impacto contra el suelo, timeScale y límites del canvas (SRP).
 */
class FlightImpactAndBoundsTest : ISimulationTestSuite {

    private val fixture: Triple<TestFlightEngineHarness, TestStateBuilderA, TestMatrixSampleBuilder> = Triple(
        TestFlightEngineHarness(),
        TestStateBuilderA(),
        TestMatrixSampleBuilder()
    )

    override fun suiteName(): String {
        return "FlightImpactAndBoundsTest"
    }

    @Test
    fun test_ground_impact_invokes_collision_detector_with_HIT_when_target_is_placed_at_impact() {
        val state = fixture.second.configuredState(40.0)
        val ticker = ManualFrameTicker()
        var reported: ICollisionResult? = null
        var finished = false
        val hooks = FlightEventHooks(onUpdate = {}, onFinished = { finished = true }, collisionListener = CollisionCallbackAdapter { reported = it })
        val engine = fixture.first.createEngine(state, hooks, ticker)
        val matrix = fixture.third.matrix(vel = 40.0, samples = 240)
        state.changeTargetX(fixture.first.geometry().transformer().toCanvasX(matrix.horizontalRange()))
        state.changeTargetRadius(20.0)
        engine.start(matrix)
        ticker.advance(100_000_000L)
        ticker.advance(110_000_000L + (matrix.totalTimeOfFlight() * 1_000_000_000).toLong())
        assertTrue(!state.checkAnimating() && finished && reported?.collisionStatus() == CollisionStatus.HIT)
        assertEquals(fixture.first.geometry().config().basePivotY(), state.currentProjectileCoords().second, 1e-4)
    }

    @Test
    fun test_ground_impact_invokes_collision_detector_with_MISS_when_target_is_far_away() {
        val state = fixture.second.configuredState(30.0)
        val ticker = ManualFrameTicker()
        var reported: ICollisionResult? = null
        val hooks = FlightEventHooks(onUpdate = {}, onFinished = {}, collisionListener = CollisionCallbackAdapter { reported = it })
        val engine = fixture.first.createEngine(state, hooks, ticker)
        val matrix = fixture.third.matrix(vel = 30.0, samples = 240)
        state.changeTargetX(fixture.first.geometry().transformer().toCanvasX(matrix.horizontalRange() + 100.0))
        engine.start(matrix)
        ticker.advance(100_000_000L)
        ticker.advance(110_000_000L + (matrix.totalTimeOfFlight() * 1_000_000_000).toLong())
        val captured = reported
        assertNotNull(captured)
        assertEquals(CollisionStatus.MISS, captured.collisionStatus())
        assertTrue(captured.statusMessage().contains("fuera del objetivo"))
    }

    @Test
    fun test_trail_points_are_preserved_after_flight_stops() {
        val state = fixture.second.configuredState(30.0)
        val ticker = ManualFrameTicker()
        val trailPoints = mutableListOf<Pair<Double, Double>>()
        val hooks = FlightEventHooks(onUpdate = {}, onFinished = {}, onTick = { x, y -> trailPoints.add(Pair(x, y)) })
        val engine = fixture.first.createEngine(state, hooks, ticker)
        engine.start()
        val flightNano = (engine.activeTrajectoryMatrix()!!.totalTimeOfFlight() * 1_000_000_000).toLong()
        ticker.advance(100_000_000L)
        ticker.advance(100_000_000L + flightNano)
        assertTrue(trailPoints.size >= 2)
        assertEquals(fixture.first.geometry().config().basePivotY(), trailPoints.last().second, 1e-4)
    }
}

/**
 * Pruebas unitarias de reproducción acelerada (timeScale) y reinicio fuera de límites (SRP).
 */
class FlightPlaybackAndBoundsTest : ISimulationTestSuite {

    private val fixture: Pair<TestFlightEngineHarness, TestStateBuilderA> = Pair(TestFlightEngineHarness(), TestStateBuilderA())

    override fun suiteName(): String {
        return "FlightPlaybackAndBoundsTest"
    }

    @Test
    fun test_timeScale_accelerates_simulation_playback() {
        val stateNormal = fixture.second.configuredState(50.0)
        val stateFast = fixture.second.configuredState(50.0)
        val tickerNormal = ManualFrameTicker()
        val tickerFast = ManualFrameTicker()
        val engineNormal = fixture.first.createEngine(stateNormal, FlightEventHooks({}, {}), tickerNormal)
        engineNormal.updateTimeScale(1.0)
        engineNormal.start()
        val engineFast = fixture.first.createEngine(stateFast, FlightEventHooks({}, {}), tickerFast)
        engineFast.updateTimeScale(2.0)
        engineFast.start()
        tickerNormal.advance(1_000_000_000L)
        tickerNormal.advance(2_000_000_000L)
        tickerFast.advance(1_000_000_000L)
        tickerFast.advance(2_000_000_000L)
        assertTrue(stateFast.currentProjectileCoords().first > stateNormal.currentProjectileCoords().first)
    }

    @Test
    fun test_projectile_leaving_canvas_square_automatically_resets_simulation_and_restores_cannon_position() {
        val state = fixture.second.configuredState(90.0, ang = 20.0)
        val ticker = ManualFrameTicker()
        var outOfBoundsTriggered = false
        val hooks = FlightEventHooks(onUpdate = {}, onFinished = {}, onOutOfBounds = { outOfBoundsTriggered = true })
        val engine = fixture.first.createEngine(state, hooks, ticker)
        engine.updateTimeScale(1.0)
        engine.start()
        ticker.advance(1_000_000_000L)
        ticker.advance(3_000_000_000L)
        ticker.advance(6_000_000_000L)
        val cfg = fixture.first.geometry().config()
        val proj = state.currentProjectileCoords()
        assertTrue(outOfBoundsTriggered && !state.checkAnimating())
        assertEquals(cfg.basePivotX(), proj.first, 1e-4)
        assertEquals(cfg.basePivotY(), proj.second, 1e-4)
    }

    @Test
    fun test_manual_reset_restores_state_and_triggers_callback() {
        val state = fixture.second.freshState()
        state.updateProjectilePosition(500.0, 200.0)
        state.changeAnimating(true)
        var callbackCalled = false
        val hooks = FlightEventHooks(onUpdate = {}, onFinished = {}, onOutOfBounds = { callbackCalled = true })
        val engine = fixture.first.createEngine(state, hooks, ManualFrameTicker())
        engine.reset()
        val cfg = fixture.first.geometry().config()
        val proj = state.currentProjectileCoords()
        assertFalse(state.checkAnimating())
        assertEquals(cfg.basePivotX(), proj.first)
        assertEquals(cfg.basePivotY(), proj.second)
        assertTrue(callbackCalled)
    }
}

/**
 * Pruebas unitarias de desvanecimiento progresivo de la estela (PathRenderer) (SRP).
 */
class PathTrailFadeTest : ISimulationTestSuite {

    override fun suiteName(): String {
        return "PathTrailFadeTest"
    }

    @Test
    fun test_path_renderer_preserves_full_opacity_during_first_2_seconds() {
        val pathRenderer = PathRenderer(gc = null, fadeDelaySeconds = 2.0, fadeDurationSeconds = 1.5, baseAlpha = 0.50)
        val launchNano = 10_000_000_000L
        pathRenderer.recordLaunch(launchNano)
        assertEquals(0.50, pathRenderer.getOpacity(launchNano + 1_000_000_000L), 1e-4)
        assertEquals(0.50, pathRenderer.getOpacity(launchNano + 2_000_000_000L), 1e-4)
    }

    @Test
    fun test_path_renderer_fades_progressively_after_2_seconds_and_clears_on_expiration() {
        val pathRenderer = PathRenderer(gc = null, fadeDelaySeconds = 2.0, fadeDurationSeconds = 1.0, baseAlpha = 0.50)
        val launchNano = 10_000_000_000L
        pathRenderer.recordLaunch(launchNano)
        pathRenderer.addPoint(10.0, 10.0)
        pathRenderer.addPoint(20.0, 20.0)
        assertEquals(0.25, pathRenderer.getOpacity(launchNano + 2_500_000_000L), 1e-4)
        assertEquals(0.0, pathRenderer.getOpacity(launchNano + 3_000_000_000L), 1e-4)
        pathRenderer.drawPath(launchNano + 3_100_000_000L)
        assertEquals(0, pathRenderer.currentPointCount())
    }

    @Test
    fun test_path_renderer_fades_progressively_from_the_starting_point_towards_the_head() {
        val pathRenderer = PathRenderer(gc = null, fadeDelaySeconds = 2.0, fadeDurationSeconds = 1.0, baseAlpha = 0.50)
        val launchNano = 10_000_000_000L
        pathRenderer.recordLaunch(launchNano)
        listOf(0.0 to 0.0, 25.0 to 10.0, 50.0 to 20.0, 75.0 to 15.0, 100.0 to 0.0).forEach { (x, y) ->
            pathRenderer.addPoint(x, y)
        }
        val points = pathRenderer.getTrailPoints()
        val timeAtFade = launchNano + 2_400_000_000L
        assertEquals(0.40, pathRenderer.getFadeProgress(timeAtFade), 1e-4)
        val alphaOrigin = pathRenderer.getPointAlpha(points[0], timeAtFade)
        val alphaHead = pathRenderer.getPointAlpha(points[4], timeAtFade)
        assertEquals(0.0, alphaOrigin, 1e-4)
        assertTrue(alphaHead > alphaOrigin)
    }
}
