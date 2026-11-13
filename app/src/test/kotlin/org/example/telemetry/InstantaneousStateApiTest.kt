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
import org.example.ISimulationTestSuite
import org.example.ITrajectoryPoint
import org.example.Position
import org.example.TestPointBuilder
import org.example.TestTelemetryBuilder
import org.example.TrajectorySimulationFactory
import org.example.model.ITrajectoryMatrixSummary
import org.example.model.TrajectoryMatrix

/**
 * Espía determinista de búsqueda de vecino cercano para verificar DIP y OCP (SRP).
 */
private class SpyNearestNeighborSearcher : TrajectoryNearestNeighborSearcher {
    private var invokedFlag: Boolean = false

    fun wasInvoked(): Boolean {
        return invokedFlag
    }

    override fun findNearest(points: List<ITrajectoryPoint>, x: Double, y: Double): ITrajectoryPoint {
        invokedFlag = true
        return points.first()
    }

    override fun findNearestByX(points: List<ITrajectoryPoint>, targetX: Double): ITrajectoryPoint {
        invokedFlag = true
        return points.first()
    }

    override fun findNearestIndex(points: List<ITrajectoryPoint>, x: Double, y: Double): Int {
        invokedFlag = true
        return 0
    }

    override fun findNearestIndexByX(points: List<ITrajectoryPoint>, targetX: Double): Int {
        invokedFlag = true
        return 0
    }
}

/**
 * Pruebas unitarias de modelo cinemático y consultas clave en InstantaneousStateApi (SRP).
 */
class InstantaneousStateApiTest : ISimulationTestSuite {

    private val fixture: Pair<TestTelemetryBuilder, TestPointBuilder> = Pair(
        TestTelemetryBuilder(),
        TestPointBuilder()
    )

    override fun suiteName(): String {
        return "InstantaneousStateApiTest"
    }

    private fun buildMatrix(): ITrajectoryMatrixSummary {
        return fixture.first.matrix()
    }

    @Test
    fun test_InstantaneousState_computes_speed_correctly_as_Euclidean_norm_of_velocities() {
        val state = InstantaneousState.fromTrajectoryPoint(fixture.second.unitTimePoint(10.0, 5.0, 3.0, 4.0))
        assertEquals(5.0, state.scalarSpeed(), 1e-10)
        assertEquals(1.0, state.elapsedTime(), 1e-10)
        assertEquals(5.0, state.altitude(), 1e-10)
        assertEquals(10.0, state.horizontalCoord(), 1e-10)
        assertEquals(Position(10.0, 5.0), state.currentPosition())
    }

    @Test
    fun test_InstantaneousState_format_produces_readable_telemetry_string() {
        val formatted = InstantaneousState.fromTrajectoryPoint(fixture.second.horizontalPoint(12.34, 8.56, 10.0, 1.25)).format()
        assertTrue(formatted.contains("t=1.25 s") && formatted.contains("h=8.56 m") && formatted.contains("v=10.00 m/s"))
    }

    @Test
    fun test_query_at_launch_coordinates_returns_launch_telemetry_with_initial_velocity() {
        val params = fixture.first.defaultParams()
        val state = fixture.first.telemetry().buildApi().getInstantaneousState(buildMatrix(), x = 0.0, y = 0.0)
        assertEquals(0.0, state.elapsedTime(), 1e-4)
        assertEquals(0.0, state.altitude(), 1e-4)
        assertEquals(params.launchVelocity(), state.scalarSpeed(), 0.1)
    }

    @Test
    fun test_query_at_apex_coordinates_returns_max_height_and_horizontal_velocity() {
        val params = fixture.first.defaultParams()
        val calculator = fixture.first.kinematics().calculator()
        val expectedMaxHeight = calculator.calculateMaxHeight(params)
        val timeToApex = (params.launchVelocity() * sin(params.angleRadians())) / params.gravityAcceleration()
        val apexPoint = calculator.calculateState(params, timeToApex)
        val state = fixture.first.telemetry().buildApi().getInstantaneousState(buildMatrix(), x = apexPoint.posX(), y = expectedMaxHeight)
        assertEquals(expectedMaxHeight, state.altitude(), 0.1)
        assertEquals(timeToApex, state.elapsedTime(), 0.05)
        assertEquals(params.launchVelocity() * cos(params.angleRadians()), state.scalarSpeed(), 0.1)
    }

    @Test
    fun test_query_at_impact_point_returns_time_of_flight_and_ground_height() {
        val params = fixture.first.defaultParams()
        val matrix = buildMatrix()
        val state = fixture.first.telemetry().buildApi().getInstantaneousState(matrix, x = matrix.horizontalRange(), y = 0.0)
        assertEquals(matrix.totalTimeOfFlight(), state.elapsedTime(), 0.05)
        assertTrue(abs(state.altitude()) < 0.2)
        assertEquals(params.launchVelocity(), state.scalarSpeed(), 0.2)
    }

    @Test
    fun test_speed_calculation_satisfies_v_equals_sqrt_vx_squared_plus_vy_squared_for_all_points() {
        for (point in buildMatrix().trajectoryPoints()) {
            val state = InstantaneousState.fromTrajectoryPoint(point)
            assertEquals(hypot(point.velX(), point.velY()), state.scalarSpeed(), 1e-10)
        }
    }
}

/**
 * Pruebas unitarias de búsqueda 2D/1D en InstantaneousStateApi (SRP).
 */
class InstantaneousNearestNeighborQueryTest : ISimulationTestSuite {

    private val builder: TestTelemetryBuilder = TestTelemetryBuilder()

    override fun suiteName(): String {
        return "InstantaneousNearestNeighborQueryTest"
    }

    private fun buildMatrix(): ITrajectoryMatrixSummary {
        return builder.matrix()
    }

    @Test
    fun test_exact_point_query_on_trajectory_returns_that_point_with_zero_distance() {
        val matrix = buildMatrix()
        val sample = matrix.trajectoryPoints()[matrix.pointCount() / 3]
        val state = builder.telemetry().buildApi().getInstantaneousState(matrix, sample.posX(), sample.posY())
        assertEquals(sample.timeSeconds(), state.elapsedTime(), 1e-10)
        assertEquals(sample.posY(), state.altitude(), 1e-10)
        assertEquals(0.0, state.queryDistance(), 1e-10)
    }

    @Test
    fun test_mouse_coordinate_near_trajectory_finds_closest_point_on_curve() {
        val matrix = buildMatrix()
        val mid = matrix.trajectoryPoints()[matrix.pointCount() / 2]
        val state = builder.telemetry().buildApi().getInstantaneousState(matrix, mid.posX(), mid.posY() + 1.5)
        assertEquals(mid.posX(), state.horizontalCoord(), 0.5)
        assertTrue(state.queryDistance() <= 1.6)
    }

    @Test
    fun test_query_using_Position_object_works_identically() {
        val matrix = buildMatrix()
        val pos = Position(15.0, 7.0)
        val fromPos = builder.telemetry().buildApi().getInstantaneousState(matrix, pos)
        val fromXY = builder.telemetry().buildApi().getInstantaneousState(matrix, pos.horizontal(), pos.vertical())
        assertEquals(fromXY.sourcePoint(), fromPos.sourcePoint())
        assertEquals(fromXY.scalarSpeed(), fromPos.scalarSpeed(), 1e-10)
    }

    @Test
    fun test_getInstantaneousStateByX_finds_point_matching_horizontal_coordinate() {
        val matrix = buildMatrix()
        val targetX = matrix.horizontalRange() * 0.4
        val state = builder.telemetry().buildApi().getInstantaneousStateByX(matrix, targetX)
        assertTrue(abs(state.horizontalCoord() - targetX) < 0.3 && state.altitude() > 0.0 && state.scalarSpeed() > 0.0)
    }

    @Test
    fun test_clamping_when_querying_before_launch_point_returns_first_point() {
        val matrix = buildMatrix()
        val state = builder.telemetry().buildApi().getInstantaneousStateByX(matrix, -50.0)
        assertEquals(matrix.firstPoint().posX(), state.horizontalCoord(), 1e-10)
        assertEquals(matrix.firstPoint().timeSeconds(), state.elapsedTime(), 1e-10)
    }

    @Test
    fun test_clamping_when_querying_beyond_range_returns_impact_point() {
        val matrix = buildMatrix()
        val state = builder.telemetry().buildApi().getInstantaneousStateByX(matrix, matrix.horizontalRange() + 100.0)
        assertEquals(matrix.finalPoint().posX(), state.horizontalCoord(), 1e-10)
        assertEquals(matrix.finalPoint().timeSeconds(), state.elapsedTime(), 1e-10)
    }
}

/**
 * Pruebas unitarias de TrajectoryInspector, DIP/OCP y robustez de telemetría (SRP).
 */
class TrajectoryInspectorAndDipTest : ISimulationTestSuite {

    private val fixture: Pair<InstantaneousStateApi, TestTelemetryBuilder> = Pair(
        TelemetryFactory.createApi(),
        TestTelemetryBuilder()
    )

    override fun suiteName(): String {
        return "TrajectoryInspectorAndDipTest"
    }

    private fun buildMatrix(): TrajectoryMatrix {
        return fixture.second.matrix()
    }

    @Test
    fun test_TrajectoryInspector_enables_fast_consecutive_scrubbing_inspections() {
        val matrix = buildMatrix()
        val inspector = fixture.first.createInspector(matrix)
        val step = matrix.horizontalRange() / 20.0
        var previousTime = -1.0
        for (i in 0..20) {
            val state = inspector.inspectByX(i * step)
            assertTrue(state.elapsedTime() >= previousTime && state.altitude() >= -0.1 && state.scalarSpeed() > 0)
            previousTime = state.elapsedTime()
        }
    }

    @Test
    fun test_inspector_2D_inspect_matches_api_getInstantaneousState() {
        val matrix = buildMatrix()
        val inspector = TelemetryFactory.createInspector(matrix)
        val expected = fixture.first.getInstantaneousState(matrix, 12.0, 6.0)
        val actual = inspector.inspect(12.0, 6.0)
        assertEquals(expected.sourcePoint(), actual.sourcePoint())
    }

    @Test
    fun test_custom_searcher_can_be_injected_into_api_conforming_to_DIP_and_OCP() {
        val matrix = buildMatrix()
        val spy = SpyNearestNeighborSearcher()
        val result = TelemetryFactory.createApi(spy).getInstantaneousState(matrix, 50.0, 50.0)
        assertTrue(spy.wasInvoked())
        assertEquals(matrix.firstPoint(), result.sourcePoint())
    }

    @Test
    fun test_TrajectoryMatrix_extension_functions_provide_ergonomic_access() {
        val matrix = buildMatrix()
        val stateXY = matrix.getInstantaneousState(10.0, 5.0)
        val stateByX = matrix.getInstantaneousStateByX(10.0)
        val statePos = matrix.getInstantaneousState(Position(10.0, 5.0))
        assertNotNull(stateByX)
        assertEquals(stateXY.sourcePoint(), statePos.sourcePoint())
    }

    @Test
    fun test_TrajectorySimulationFactory_extension_functions_assemble_telemetry_components() {
        val matrix = buildMatrix()
        val factoryApi = TrajectorySimulationFactory.createInstantaneousStateApi()
        val factoryInspector = TrajectorySimulationFactory.createTrajectoryInspector(matrix)
        assertNotNull(factoryApi)
        assertEquals(matrix, factoryInspector.boundMatrix())
    }

    @Test
    fun test_api_rejects_empty_points_list() {
        assertFailsWith<IllegalArgumentException> {
            fixture.first.getInstantaneousState(emptyList(), 0.0, 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            fixture.first.getInstantaneousStateByX(emptyList(), 0.0)
        }
    }

    @Test
    fun test_searcher_handles_single_point_trajectory_safely() {
        val searcher = TelemetryFactory.createSearcher()
        val singlePoint = buildMatrix().subMatrix(0, 1)
        assertEquals(singlePoint.first(), searcher.findNearest(singlePoint, 100.0, 100.0))
        assertEquals(singlePoint.first(), searcher.findNearestByX(singlePoint, 100.0))
    }
}
