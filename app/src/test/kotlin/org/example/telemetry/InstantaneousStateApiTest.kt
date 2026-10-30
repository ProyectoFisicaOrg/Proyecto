package org.example.telemetry

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.example.PhysicsParams
import org.example.Position
import org.example.TrajectoryPoint
import org.example.TrajectorySimulationFactory
import org.example.model.TrajectoryMatrix

/**
 * Espía determinista de búsqueda de vecino cercano para verificar DIP y OCP (SRP).
 */
private class SpyNearestNeighborSearcher : TrajectoryNearestNeighborSearcher {
    var invoked: Boolean = false
        private set

    override fun findNearest(points: List<TrajectoryPoint>, x: Double, y: Double): TrajectoryPoint {
        invoked = true
        return points.first()
    }

    override fun findNearestByX(points: List<TrajectoryPoint>, targetX: Double): TrajectoryPoint {
        invoked = true
        return points.first()
    }

    override fun findNearestIndex(points: List<TrajectoryPoint>, x: Double, y: Double): Int = 0

    override fun findNearestIndexByX(points: List<TrajectoryPoint>, targetX: Double): Int = 0
}

/**
 * Pruebas unitarias de modelo cinemático y consultas clave en InstantaneousStateApi (SRP).
 */
class InstantaneousStateApiTest {

    private val testParams = PhysicsParams(angleDeg = 45.0, initialVelocity = 20.0, gravity = 9.8, mass = 1.0)
    private val telemetryApi = TelemetryFactory.createApi()

    private fun buildMatrix(): TrajectoryMatrix =
        TrajectorySimulationFactory.generateMatrix(testParams, samplesPerSecond = 100)

    @Test
    fun test_InstantaneousState_computes_speed_correctly_as_Euclidean_norm_of_velocities() {
        val state = InstantaneousState.fromTrajectoryPoint(TrajectoryPoint(10.0, 5.0, 3.0, 4.0, 1.0))
        assertEquals(5.0, state.speed, 1e-10)
        assertEquals(5.0, state.v, 1e-10)
        assertEquals(1.0, state.t, 1e-10)
        assertEquals(5.0, state.y, 1e-10)
        assertEquals(10.0, state.distance, 1e-10)
        assertEquals(Position(10.0, 5.0), state.position)
    }

    @Test
    fun test_InstantaneousState_format_produces_readable_telemetry_string() {
        val formatted = InstantaneousState.fromTrajectoryPoint(TrajectoryPoint(12.34, 8.56, 10.0, 0.0, 1.25)).format()
        assertTrue(formatted.contains("t=1.25 s") && formatted.contains("h=8.56 m") && formatted.contains("v=10.00 m/s"))
    }

    @Test
    fun test_query_at_launch_coordinates_returns_launch_telemetry_with_initial_velocity() {
        val state = telemetryApi.getInstantaneousState(buildMatrix(), x = 0.0, y = 0.0)
        assertEquals(0.0, state.time, 1e-4)
        assertEquals(0.0, state.height, 1e-4)
        assertEquals(testParams.initialVelocity, state.speed, 0.1)
    }

    @Test
    fun test_query_at_apex_coordinates_returns_max_height_and_horizontal_velocity() {
        val calculator = TrajectorySimulationFactory.createCalculator()
        val expectedMaxHeight = calculator.calculateMaxHeight(testParams)
        val timeToApex = (testParams.initialVelocity * sin(testParams.angleRad)) / testParams.gravity
        val apexPoint = calculator.calculateState(testParams, timeToApex)
        val state = telemetryApi.getInstantaneousState(buildMatrix(), x = apexPoint.x, y = expectedMaxHeight)
        assertEquals(expectedMaxHeight, state.height, 0.1)
        assertEquals(timeToApex, state.time, 0.05)
        assertEquals(testParams.initialVelocity * cos(testParams.angleRad), state.speed, 0.1)
    }

    @Test
    fun test_query_at_impact_point_returns_time_of_flight_and_ground_height() {
        val matrix = buildMatrix()
        val state = telemetryApi.getInstantaneousState(matrix, x = matrix.range, y = 0.0)
        assertEquals(matrix.timeOfFlight, state.time, 0.05)
        assertTrue(abs(state.height) < 0.2)
        assertEquals(testParams.initialVelocity, state.speed, 0.2)
    }

    @Test
    fun test_speed_calculation_satisfies_v_equals_sqrt_vx_squared_plus_vy_squared_for_all_points() {
        for (point in buildMatrix().points) {
            val state = InstantaneousState.fromTrajectoryPoint(point)
            assertEquals(hypot(point.vx, point.vy), state.speed, 1e-10)
        }
    }
}

/**
 * Pruebas unitarias de búsqueda 2D/1D en InstantaneousStateApi (SRP).
 */
class InstantaneousNearestNeighborQueryTest {

    private val testParams = PhysicsParams(angleDeg = 45.0, initialVelocity = 20.0, gravity = 9.8, mass = 1.0)
    private val telemetryApi = TelemetryFactory.createApi()

    private fun buildMatrix(): TrajectoryMatrix =
        TrajectorySimulationFactory.generateMatrix(testParams, samplesPerSecond = 100)

    @Test
    fun test_exact_point_query_on_trajectory_returns_that_point_with_zero_distance() {
        val matrix = buildMatrix()
        val sample = matrix.points[matrix.size / 3]
        val state = telemetryApi.getInstantaneousState(matrix, sample.x, sample.y)
        assertEquals(sample.t, state.time, 1e-10)
        assertEquals(sample.y, state.height, 1e-10)
        assertEquals(0.0, state.distanceToQuery, 1e-10)
    }

    @Test
    fun test_mouse_coordinate_near_trajectory_finds_closest_point_on_curve() {
        val matrix = buildMatrix()
        val mid = matrix.points[matrix.size / 2]
        val state = telemetryApi.getInstantaneousState(matrix, mid.x, mid.y + 1.5)
        assertEquals(mid.x, state.x, 0.5)
        assertTrue(state.distanceToQuery <= 1.6)
    }

    @Test
    fun test_query_using_Position_object_works_identically() {
        val matrix = buildMatrix()
        val pos = Position(15.0, 7.0)
        val fromPos = telemetryApi.getInstantaneousState(matrix, pos)
        val fromXY = telemetryApi.getInstantaneousState(matrix, pos.x, pos.y)
        assertEquals(fromXY.point, fromPos.point)
        assertEquals(fromXY.speed, fromPos.speed, 1e-10)
    }

    @Test
    fun test_getInstantaneousStateByX_finds_point_matching_horizontal_coordinate() {
        val matrix = buildMatrix()
        val targetX = matrix.range * 0.4
        val state = telemetryApi.getInstantaneousStateByX(matrix, targetX)
        assertTrue(abs(state.x - targetX) < 0.3 && state.height > 0.0 && state.speed > 0.0)
    }

    @Test
    fun test_clamping_when_querying_before_launch_point_returns_first_point() {
        val matrix = buildMatrix()
        val state = telemetryApi.getInstantaneousStateByX(matrix, -50.0)
        assertEquals(matrix.launchPoint.x, state.x, 1e-10)
        assertEquals(matrix.launchPoint.t, state.time, 1e-10)
    }

    @Test
    fun test_clamping_when_querying_beyond_range_returns_impact_point() {
        val matrix = buildMatrix()
        val state = telemetryApi.getInstantaneousStateByX(matrix, matrix.range + 100.0)
        assertEquals(matrix.impactPoint.x, state.x, 1e-10)
        assertEquals(matrix.impactPoint.t, state.time, 1e-10)
    }
}

/**
 * Pruebas unitarias de TrajectoryInspector, DIP/OCP y robustez de telemetría (SRP).
 */
class TrajectoryInspectorAndDipTest {

    private val testParams = PhysicsParams(angleDeg = 45.0, initialVelocity = 20.0, gravity = 9.8, mass = 1.0)
    private val telemetryApi = TelemetryFactory.createApi()
    private val searcher = TelemetryFactory.createSearcher()

    private fun buildMatrix(): TrajectoryMatrix =
        TrajectorySimulationFactory.generateMatrix(testParams, samplesPerSecond = 100)

    @Test
    fun test_TrajectoryInspector_enables_fast_consecutive_scrubbing_inspections() {
        val matrix = buildMatrix()
        val inspector = telemetryApi.createInspector(matrix)
        val step = matrix.range / 20.0
        var previousTime = -1.0
        for (i in 0..20) {
            val state = inspector.inspectByX(i * step)
            assertTrue(state.time >= previousTime && state.height >= -0.1 && state.speed > 0)
            previousTime = state.time
        }
    }

    @Test
    fun test_inspector_2D_inspect_matches_api_getInstantaneousState() {
        val matrix = buildMatrix()
        val inspector = TelemetryFactory.createInspector(matrix)
        val expected = telemetryApi.getInstantaneousState(matrix, 12.0, 6.0)
        val actual = inspector.inspect(12.0, 6.0)
        assertEquals(expected.point, actual.point)
    }

    @Test
    fun test_custom_searcher_can_be_injected_into_api_conforming_to_DIP_and_OCP() {
        val matrix = buildMatrix()
        val spy = SpyNearestNeighborSearcher()
        val result = TelemetryFactory.createApi(spy).getInstantaneousState(matrix, 50.0, 50.0)
        assertTrue(spy.invoked)
        assertEquals(matrix.launchPoint, result.point)
    }

    @Test
    fun test_TrajectoryMatrix_extension_functions_provide_ergonomic_access() {
        val matrix = buildMatrix()
        val stateXY = matrix.getInstantaneousState(10.0, 5.0)
        val stateByX = matrix.getInstantaneousStateByX(10.0)
        val statePos = matrix.getInstantaneousState(Position(10.0, 5.0))
        assertNotNull(stateByX)
        assertEquals(stateXY.point, statePos.point)
    }

    @Test
    fun test_TrajectorySimulationFactory_extension_functions_assemble_telemetry_components() {
        val matrix = buildMatrix()
        val factoryApi = TrajectorySimulationFactory.createInstantaneousStateApi()
        val factoryInspector = TrajectorySimulationFactory.createTrajectoryInspector(matrix)
        assertNotNull(factoryApi)
        assertEquals(matrix, factoryInspector.matrix)
    }

    @Test
    fun test_api_rejects_empty_points_list() {
        assertFailsWith<IllegalArgumentException> {
            telemetryApi.getInstantaneousState(emptyList(), 0.0, 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            telemetryApi.getInstantaneousStateByX(emptyList(), 0.0)
        }
    }

    @Test
    fun test_searcher_handles_single_point_trajectory_safely() {
        val singlePoint = listOf(TrajectoryPoint(5.0, 5.0, 10.0, 10.0, 0.5))
        assertEquals(singlePoint.first(), searcher.findNearest(singlePoint, 100.0, 100.0))
        assertEquals(singlePoint.first(), searcher.findNearestByX(singlePoint, 100.0))
    }
}
