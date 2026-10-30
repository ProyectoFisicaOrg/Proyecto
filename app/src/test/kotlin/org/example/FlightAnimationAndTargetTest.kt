package org.example

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.example.animation.AnimationEngine
import org.example.animation.CollisionListener
import org.example.animation.ManualFrameTicker
import org.example.collision.CollisionResult
import org.example.collision.CollisionStatus
import org.example.model.SimulationState
import org.example.rendering.CoordinateTransformer
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.PathRenderer
import org.example.rendering.SceneConfig

/**
 * Pruebas unitarias de transformación de coordenadas e inicio/interpolación de vuelo (SRP).
 */
class FlightAnimationAndTargetTest {

    private val cfg = SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)
    private val transformer: CoordinateTransformer = DefaultCoordinateTransformer(cfg, pixelsPerMeter = 2.5)

    private fun createState(vel: Double, ang: Double = 45.0): SimulationState =
        SimulationState().apply {
            angle = ang
            velocity = vel
            gravity = 9.8
        }

    @Test
    fun test_transformer_accurately_converts_origin_between_physics_and_canvas() {
        assertEquals(cfg.cannonBaseX, transformer.toCanvasX(0.0))
        assertEquals(cfg.cannonBaseY, transformer.toCanvasY(0.0))
        assertEquals(0.0, transformer.toPhysicsX(cfg.cannonBaseX))
        assertEquals(0.0, transformer.toPhysicsY(cfg.cannonBaseY))
    }

    @Test
    fun test_transformer_roundtrip_maintains_coordinate_integrity() {
        val canvasX = transformer.toCanvasX(150.0)
        val canvasY = transformer.toCanvasY(45.0)
        assertEquals(150.0, transformer.toPhysicsX(canvasX), 1e-9)
        assertEquals(45.0, transformer.toPhysicsY(canvasY), 1e-9)
    }

    @Test
    fun test_transformer_distance_conversion_respects_scale_factor() {
        val pixels = transformer.toCanvasDistance(20.0)
        assertEquals(50.0, pixels)
        assertEquals(20.0, transformer.toPhysicsDistance(pixels))
    }

    @Test
    fun test_animation_engine_begins_at_launch_point_and_starts_ticker() {
        val state = createState(40.0)
        val ticker = ManualFrameTicker()
        val trailPoints = mutableListOf<Pair<Double, Double>>()
        var updateCalled = false
        val engine = AnimationEngine(
            state = state, cfg = cfg, transformer = transformer,
            onUpdate = { updateCalled = true }, onFinished = {},
            onTick = { x, y -> trailPoints.add(Pair(x, y)) }, frameTicker = ticker
        )
        engine.start()
        assertTrue(state.isAnimating && ticker.isRunning && updateCalled)
        assertEquals(1, trailPoints.size)
        assertEquals(cfg.cannonBaseX, state.projectileX, 1e-6)
        assertEquals(cfg.cannonBaseY, state.projectileY, 1e-6)
    }

    @Test
    fun test_animation_interpolates_smoothly_along_trajectory_time() {
        val state = createState(50.0)
        val ticker = ManualFrameTicker()
        val trailPoints = mutableListOf<Pair<Double, Double>>()
        val engine = AnimationEngine(
            state = state, cfg = cfg, transformer = transformer,
            onUpdate = {}, onFinished = {},
            onTick = { x, y -> trailPoints.add(Pair(x, y)) }, frameTicker = ticker
        )
        engine.start()
        val halfNano = ((engine.currentMatrix!!.timeOfFlight / 2.0) * 1_000_000_000).toLong()
        ticker.advance(1_000_000_000L)
        ticker.advance(1_000_000_000L + halfNano)
        assertTrue(state.projectileX > cfg.cannonBaseX)
        assertTrue(state.projectileY < cfg.cannonBaseY)
        assertTrue(trailPoints.size >= 3)
    }

    @Test
    fun test_animation_engine_defaults_to_240_samples_per_second_for_high_fluid_fps() {
        val engine = AnimationEngine(
            state = createState(20.0), cfg = cfg, transformer = transformer,
            onUpdate = {}, onFinished = {}
        )
        assertEquals(240, engine.getSamplesPerSecond())
    }
}

/**
 * Pruebas unitarias de impacto contra el suelo, timeScale y límites del canvas (SRP).
 */
class FlightImpactAndBoundsTest {

    private val cfg = SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)
    private val transformer: CoordinateTransformer = DefaultCoordinateTransformer(cfg, pixelsPerMeter = 2.5)

    private fun createState(vel: Double, ang: Double = 45.0): SimulationState =
        SimulationState().apply {
            angle = ang
            velocity = vel
            gravity = 9.8
        }

    @Test
    fun test_ground_impact_invokes_collision_detector_with_HIT_when_target_is_placed_at_impact() {
        val state = createState(40.0)
        val ticker = ManualFrameTicker()
        var reportedResult: CollisionResult? = null
        var finishedCalled = false
        val engine = AnimationEngine(
            state = state, cfg = cfg, transformer = transformer,
            onUpdate = {}, onFinished = { finishedCalled = true },
            collisionListener = CollisionListener { result -> reportedResult = result },
            frameTicker = ticker
        )
        val matrix = TrajectorySimulationFactory.generateMatrix(state.toPhysicsParams())
        state.targetX = transformer.toCanvasX(matrix.range)
        state.targetRadius = 20.0
        engine.start(matrix)
        val flightNano = (matrix.timeOfFlight * 1_000_000_000).toLong()
        ticker.advance(100_000_000L)
        ticker.advance(110_000_000L + flightNano)
        assertFalse(state.isAnimating)
        assertTrue(finishedCalled)
        assertEquals(CollisionStatus.HIT, reportedResult?.status)
        assertEquals(cfg.cannonBaseY, state.projectileY, 1e-4)
    }

    @Test
    fun test_ground_impact_invokes_collision_detector_with_MISS_when_target_is_far_away() {
        val state = createState(30.0)
        val ticker = ManualFrameTicker()
        var reportedResult: CollisionResult? = null
        val engine = AnimationEngine(
            state = state, cfg = cfg, transformer = transformer,
            onUpdate = {}, onFinished = {},
            collisionListener = CollisionListener { result -> reportedResult = result },
            frameTicker = ticker
        )
        val matrix = TrajectorySimulationFactory.generateMatrix(state.toPhysicsParams())
        state.targetX = transformer.toCanvasX(matrix.range + 100.0)
        engine.start(matrix)
        val flightNano = (matrix.timeOfFlight * 1_000_000_000).toLong()
        ticker.advance(100_000_000L)
        ticker.advance(110_000_000L + flightNano)
        val captured = reportedResult
        assertNotNull(captured)
        assertEquals(CollisionStatus.MISS, captured.status)
        assertTrue(captured.message.contains("fuera del objetivo"))
    }

    @Test
    fun test_trail_points_are_preserved_after_flight_stops() {
        val state = createState(30.0)
        val ticker = ManualFrameTicker()
        val trailPoints = mutableListOf<Pair<Double, Double>>()
        val engine = AnimationEngine(
            state = state, cfg = cfg, transformer = transformer,
            onUpdate = {}, onFinished = {},
            onTick = { x, y -> trailPoints.add(Pair(x, y)) }, frameTicker = ticker
        )
        engine.start()
        val flightNano = (engine.currentMatrix!!.timeOfFlight * 1_000_000_000).toLong()
        ticker.advance(100_000_000L)
        ticker.advance(100_000_000L + flightNano)
        assertTrue(trailPoints.size >= 2)
        assertEquals(cfg.cannonBaseY, trailPoints.last().second, 1e-4)
    }
}

/**
 * Pruebas unitarias de reproducción acelerada (timeScale) y reinicio fuera de límites (SRP).
 */
class FlightPlaybackAndBoundsTest {

    private val cfg = SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)
    private val transformer: CoordinateTransformer = DefaultCoordinateTransformer(cfg, pixelsPerMeter = 2.5)

    private fun createState(vel: Double, ang: Double = 45.0): SimulationState =
        SimulationState().apply {
            angle = ang
            velocity = vel
            gravity = 9.8
        }

    @Test
    fun test_timeScale_accelerates_simulation_playback() {
        val stateNormal = createState(50.0)
        val stateFast = createState(50.0)
        val tickerNormal = ManualFrameTicker()
        val tickerFast = ManualFrameTicker()
        AnimationEngine(state = stateNormal, cfg = cfg, transformer = transformer, onUpdate = {}, onFinished = {}, timeScale = 1.0, frameTicker = tickerNormal).start()
        AnimationEngine(state = stateFast, cfg = cfg, transformer = transformer, onUpdate = {}, onFinished = {}, timeScale = 2.0, frameTicker = tickerFast).start()
        tickerNormal.advance(1_000_000_000L)
        tickerNormal.advance(2_000_000_000L)
        tickerFast.advance(1_000_000_000L)
        tickerFast.advance(2_000_000_000L)
        assertTrue(stateFast.projectileX > stateNormal.projectileX)
    }

    @Test
    fun test_projectile_leaving_canvas_square_automatically_resets_simulation_and_restores_cannon_position() {
        val state = createState(90.0, ang = 20.0)
        val ticker = ManualFrameTicker()
        var outOfBoundsTriggered = false
        val engine = AnimationEngine(
            state = state, cfg = cfg, transformer = transformer,
            onUpdate = {}, onFinished = {},
            onOutOfBounds = { outOfBoundsTriggered = true }, timeScale = 1.0, frameTicker = ticker
        )
        engine.start()
        ticker.advance(1_000_000_000L)
        ticker.advance(3_000_000_000L)
        ticker.advance(6_000_000_000L)
        assertTrue(outOfBoundsTriggered && !state.isAnimating)
        assertEquals(cfg.cannonBaseX, state.projectileX, 1e-4)
        assertEquals(cfg.cannonBaseY, state.projectileY, 1e-4)
    }

    @Test
    fun test_manual_reset_restores_state_and_triggers_callback() {
        val state = SimulationState().apply {
            projectileX = 500.0
            projectileY = 200.0
            isAnimating = true
        }
        var callbackCalled = false
        val engine = AnimationEngine(
            state = state, cfg = cfg, transformer = transformer,
            onUpdate = {}, onFinished = {}, onOutOfBounds = { callbackCalled = true }
        )
        engine.reset()
        assertFalse(state.isAnimating)
        assertEquals(cfg.cannonBaseX, state.projectileX)
        assertEquals(cfg.cannonBaseY, state.projectileY)
        assertTrue(callbackCalled)
    }
}

/**
 * Pruebas unitarias de desvanecimiento progresivo de la estela (PathRenderer) (SRP).
 */
class PathTrailFadeTest {

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
        assertEquals(0, pathRenderer.pointCount)
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
