package org.example

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.example.collision.CollisionDetector
import org.example.collision.CollisionStatus
import org.example.model.ITrajectoryMatrixSummary
import org.example.model.Target
import org.example.model.TrajectoryMatrix
import org.example.trajectory.TrajectorySearcher

/**
 * Pruebas unitarias de generación cinemática de la matriz de trayectoria (SRP).
 */
class TrajectoryMatrixTest : ISimulationTestSuite {

    private val builder: TestParamsBuilderA = TestParamsBuilderA()

    override fun suiteName(): String {
        return "TrajectoryMatrixTest"
    }

    private fun defaultParams(): IPhysicsParams {
        return builder.params()
    }

    private fun generate(samples: Int = 120): ITrajectoryMatrixSummary {
        return builder.kinematics().generator().generate(defaultParams(), samples)
    }

    @Test
    fun test_generator_creates_matrix_with_correct_number_of_points() {
        val matrix = generate(samples = 100)
        assertTrue(matrix.size >= 100)
    }

    @Test
    fun test_generator_stores_correct_time_of_flight() {
        val expected = builder.kinematics().calculator().calculateTimeOfFlight(defaultParams())!!
        assertEquals(expected, generate().timeOfFlight, 1e-10)
    }

    @Test
    fun test_generator_stores_correct_range() {
        val expected = builder.kinematics().calculator().calculateRange(defaultParams())!!
        assertEquals(expected, generate().range, 1e-10)
    }

    @Test
    fun test_generator_stores_correct_max_height() {
        val expected = builder.kinematics().calculator().calculateMaxHeight(defaultParams())
        assertEquals(expected, generate().maxHeight, 1e-10)
    }

    @Test
    fun test_launch_point_is_at_origin() {
        val launch = generate().launchPoint
        assertEquals(0.0, launch.x, 1e-10)
        assertEquals(0.0, launch.y, 1e-10)
        assertEquals(0.0, launch.t, 1e-10)
    }

    @Test
    fun test_impact_point_has_y_approximately_zero() {
        assertTrue(abs(generate().impactPoint.y) < 0.1)
    }

    @Test
    fun test_points_are_ordered_chronologically() {
        val pts = generate().points
        for (i in 1 until pts.size) assertTrue(pts[i].t >= pts[i - 1].t)
    }

    @Test
    fun test_x_coordinates_are_monotonically_increasing() {
        val pts = generate().points
        for (i in 1 until pts.size) assertTrue(pts[i].x >= pts[i - 1].x)
    }
}

/**
 * Pruebas unitarias de búsqueda binaria y consultas sobre TrajectoryMatrix (SRP).
 */
class TrajectorySearchAndQueryTest : ISimulationTestSuite {

    private val fixture: Pair<TrajectorySearcher, TestParamsBuilderA> = Pair(
        DefaultSearcherBuilder().buildSearcher(),
        TestParamsBuilderA()
    )

    override fun suiteName(): String {
        return "TrajectorySearchAndQueryTest"
    }

    private fun generate(samples: Int = 120): ITrajectoryMatrixSummary {
        return fixture.second.kinematics().generator().generate(fixture.second.params(), samples)
    }

    @Test
    fun test_findClosestByX_returns_correct_point() {
        val closest = fixture.first.findClosestByX(generate(100).points, 10.0)
        assertTrue(abs(closest.x - 10.0) < 0.5)
    }

    @Test
    fun test_findClosestByX_at_exact_point_returns_that_point() {
        val matrix = generate(100)
        val targetX = matrix.points[matrix.size / 2].x
        assertEquals(targetX, fixture.first.findClosestByX(matrix.points, targetX).x, 1e-10)
    }

    @Test
    fun test_findClosestByX_at_start_returns_first_point() {
        val matrix = generate()
        assertEquals(matrix.points.first().x, fixture.first.findClosestByX(matrix.points, 0.0).x, 1e-10)
    }

    @Test
    fun test_findClosestByX_at_end_returns_last_point() {
        val matrix = generate()
        assertEquals(matrix.points.last().x, fixture.first.findClosestByX(matrix.points, matrix.range).x, 1e-10)
    }

    @Test
    fun test_findClosestIndexByX_returns_a_valid_index() {
        val matrix = generate(100)
        assertTrue(fixture.first.findClosestIndexByX(matrix.points, 10.0) in 0 until matrix.size)
    }

    @Test
    fun test_subMatrix_returns_correct_range() {
        val matrix = generate(10)
        val sub = matrix.subMatrix(2, 5)
        assertEquals(3, sub.size)
        assertEquals(matrix.points[2].x, sub[0].x, 1e-10)
        assertEquals(matrix.points[4].x, sub[2].x, 1e-10)
    }

    @Test
    fun test_subMatrix_clamps_indices_out_of_range() {
        val matrix = generate(10)
        assertEquals(matrix.size, matrix.subMatrix(-5, matrix.size + 10).size)
    }

    @Test
    fun test_pointsAbove_filters_correctly() {
        val aboveZero = generate().pointsAbove(0.0)
        assertTrue(aboveZero.isNotEmpty() && aboveZero.all { it.y >= 0.0 })
    }
}

/**
 * Pruebas unitarias de detección de colisión contra el objetivo en suelo (SRP).
 */
class TrajectoryCollisionDetectionTest : ISimulationTestSuite {

    private val fixture: Pair<CollisionDetector, TestParamsBuilderA> = Pair(
        DefaultCollisionBuilder().buildCollisionDetector(),
        TestParamsBuilderA()
    )

    override fun suiteName(): String {
        return "TrajectoryCollisionDetectionTest"
    }

    private fun generate(): ITrajectoryMatrixSummary {
        return fixture.second.kinematics().generator().generate(fixture.second.params(), 120)
    }

    @Test
    fun test_checkCollision_returns_HIT_when_target_is_at_impact_point() {
        val matrix = generate()
        val collision = fixture.first.checkCollision(matrix, Target(centerX = matrix.range, radius = 2.0))
        assertEquals(CollisionStatus.HIT, collision.status)
        val impact = collision.impactResult()
        assertNotNull(impact)
        assertEquals(matrix.range, impact.point.x, 2.0)
    }

    @Test
    fun test_checkCollision_returns_MISS_when_target_is_far_from_impact() {
        val matrix = generate()
        val collision = fixture.first.checkCollision(matrix, Target(centerX = matrix.range + 100.0, radius = 1.0))
        assertEquals(CollisionStatus.MISS, collision.status)
    }

    @Test
    fun test_checkCollision_with_wide_target_near_impact_always_hits() {
        val matrix = generate()
        val collision = fixture.first.checkCollision(matrix, Target(centerX = matrix.range, radius = 50.0))
        assertEquals(CollisionStatus.HIT, collision.status)
    }

    @Test
    fun test_impact_result_exposes_correct_target_info() {
        val matrix = generate()
        val target = Target(centerX = matrix.range, radius = 2.0)
        val collision = fixture.first.checkCollision(matrix, target)
        assertEquals(CollisionStatus.HIT, collision.status)
        assertEquals(target, collision.impactResult()?.target)
    }

    @Test
    fun test_isInsideTarget_returns_true_when_inside() {
        val target = Target(centerX = 50.0, radius = 2.0)
        assertTrue(fixture.first.isInsideTarget(50.0, target))
        assertTrue(fixture.first.isInsideTarget(51.0, target))
        assertTrue(fixture.first.isInsideTarget(49.0, target))
    }

    @Test
    fun test_isInsideTarget_returns_false_when_outside() {
        val target = Target(centerX = 50.0, radius = 2.0)
        assertFalse(fixture.first.isInsideTarget(55.0, target))
        assertFalse(fixture.first.isInsideTarget(45.0, target))
    }

    @Test
    fun test_isInsideTarget_returns_true_at_exact_boundary() {
        val target = Target(centerX = 50.0, radius = 2.0)
        assertTrue(fixture.first.isInsideTarget(52.0, target))
        assertTrue(fixture.first.isInsideTarget(48.0, target))
    }

    @Test
    fun test_collision_message_contains_useful_info_on_HIT() {
        val matrix = generate()
        val collision = fixture.first.checkCollision(matrix, Target(centerX = matrix.range, radius = 2.0))
        assertTrue(collision.message.contains("Impacto") && collision.message.contains("x=") && collision.message.contains("t="))
    }

    @Test
    fun test_collision_message_contains_useful_info_on_MISS() {
        val matrix = generate()
        val collision = fixture.first.checkCollision(matrix, Target(centerX = matrix.range + 100.0, radius = 1.0))
        assertTrue(collision.message.contains("fuera del objetivo"))
    }
}

/**
 * Pruebas unitarias de invariantes, validaciones de dominio y variación paramétrica (SRP).
 */
class TrajectoryDomainValidationTest : ISimulationTestSuite {

    private val fixture: Pair<TestParamsBuilderA, TestPointBuilder> = Pair(TestParamsBuilderA(), TestPointBuilder())

    override fun suiteName(): String {
        return "TrajectoryDomainValidationTest"
    }

    @Test
    fun test_matrix_rejects_empty_points_list() {
        assertFailsWith<IllegalArgumentException> {
            TrajectoryMatrix(emptyList(), 0.0, 0.0, 0.0)
        }
    }

    @Test
    fun test_matrix_rejects_negative_time_of_flight() {
        assertFailsWith<IllegalArgumentException> {
            TrajectoryMatrix(listOf(fixture.second.originPoint(0.0, 0.0, 1.0, 1.0)), -1.0, 0.0, 0.0)
        }
    }

    @Test
    fun test_generator_throws_on_horizontal_launch() {
        assertFailsWith<IllegalArgumentException> {
            fixture.first.kinematics().generator().generate(fixture.first.params(angleDeg = 0.0), 120)
        }
    }

    @Test
    fun test_generator_throws_on_negative_samples_per_second() {
        assertFailsWith<IllegalArgumentException> {
            fixture.first.kinematics().generator().generate(fixture.first.params(), 0)
        }
    }

    @Test
    fun test_Target_rejects_negative_radius() {
        assertFailsWith<IllegalArgumentException> {
            Target(centerX = 10.0, radius = -1.0)
        }
    }

    @Test
    fun test_matrix_works_with_different_angles() {
        for (angle in listOf(15.0, 30.0, 45.0, 60.0, 75.0)) {
            val matrix = fixture.first.kinematics().generator().generate(fixture.first.params(angleDeg = angle), 120)
            assertTrue(matrix.size > 0 && matrix.timeOfFlight > 0 && matrix.range > 0 && matrix.maxHeight > 0)
        }
    }

    @Test
    fun test_matrix_works_with_different_velocities() {
        for (v0 in listOf(5.0, 10.0, 20.0, 50.0)) {
            val matrix = fixture.first.kinematics().generator().generate(fixture.first.params(initialVelocity = v0), 120)
            assertTrue(matrix.size > 0 && matrix.timeOfFlight > 0 && matrix.range > 0)
        }
    }
}
