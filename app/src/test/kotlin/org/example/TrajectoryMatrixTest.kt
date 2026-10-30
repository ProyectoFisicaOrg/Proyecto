package org.example

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.example.collision.CollisionStatus
import org.example.model.Target
import org.example.model.TrajectoryMatrix

/**
 * Pruebas unitarias de generación cinemática de la matriz de trayectoria (SRP).
 */
class TrajectoryMatrixTest {

    private val testParams = PhysicsParams(angleDeg = 45.0, initialVelocity = 20.0, gravity = 9.8, mass = 1.0)
    private val generator = TrajectorySimulationFactory.createGenerator()

    private fun generate(params: PhysicsParams = testParams, samples: Int = 120): TrajectoryMatrix =
        generator.generate(params, samples)

    @Test
    fun test_generator_creates_matrix_with_correct_number_of_points() {
        val matrix = generate(samples = 100)
        assertTrue(matrix.size >= 100)
    }

    @Test
    fun test_generator_stores_correct_time_of_flight() {
        val expected = TrajectorySimulationFactory.createCalculator().calculateTimeOfFlight(testParams)!!
        assertEquals(expected, generate().timeOfFlight, 1e-10)
    }

    @Test
    fun test_generator_stores_correct_range() {
        val expected = TrajectorySimulationFactory.createCalculator().calculateRange(testParams)!!
        assertEquals(expected, generate().range, 1e-10)
    }

    @Test
    fun test_generator_stores_correct_max_height() {
        val expected = TrajectorySimulationFactory.createCalculator().calculateMaxHeight(testParams)
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
class TrajectorySearchAndQueryTest {

    private val testParams = PhysicsParams(angleDeg = 45.0, initialVelocity = 20.0, gravity = 9.8, mass = 1.0)
    private val generator = TrajectorySimulationFactory.createGenerator()
    private val searcher = TrajectorySimulationFactory.createSearcher()

    private fun generate(samples: Int = 120): TrajectoryMatrix = generator.generate(testParams, samples)

    @Test
    fun test_findClosestByX_returns_correct_point() {
        val closest = searcher.findClosestByX(generate(100).points, 10.0)
        assertTrue(abs(closest.x - 10.0) < 0.5)
    }

    @Test
    fun test_findClosestByX_at_exact_point_returns_that_point() {
        val matrix = generate(100)
        val targetX = matrix.points[matrix.size / 2].x
        assertEquals(targetX, searcher.findClosestByX(matrix.points, targetX).x, 1e-10)
    }

    @Test
    fun test_findClosestByX_at_start_returns_first_point() {
        val matrix = generate()
        assertEquals(matrix.points.first().x, searcher.findClosestByX(matrix.points, 0.0).x, 1e-10)
    }

    @Test
    fun test_findClosestByX_at_end_returns_last_point() {
        val matrix = generate()
        assertEquals(matrix.points.last().x, searcher.findClosestByX(matrix.points, matrix.range).x, 1e-10)
    }

    @Test
    fun test_findClosestIndexByX_returns_a_valid_index() {
        val matrix = generate(100)
        assertTrue(searcher.findClosestIndexByX(matrix.points, 10.0) in 0 until matrix.size)
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
class TrajectoryCollisionDetectionTest {

    private val testParams = PhysicsParams(angleDeg = 45.0, initialVelocity = 20.0, gravity = 9.8, mass = 1.0)
    private val generator = TrajectorySimulationFactory.createGenerator()
    private val collisionDetector = TrajectorySimulationFactory.createCollisionDetector()

    private fun generate(): TrajectoryMatrix = generator.generate(testParams, 120)

    @Test
    fun test_checkCollision_returns_HIT_when_target_is_at_impact_point() {
        val matrix = generate()
        val collision = collisionDetector.checkCollision(matrix, Target(centerX = matrix.range, radius = 2.0))
        assertEquals(CollisionStatus.HIT, collision.status)
        val impact = collision.impactResult()
        assertNotNull(impact)
        assertEquals(matrix.range, impact.point.x, 2.0)
    }

    @Test
    fun test_checkCollision_returns_MISS_when_target_is_far_from_impact() {
        val matrix = generate()
        val collision = collisionDetector.checkCollision(matrix, Target(centerX = matrix.range + 100.0, radius = 1.0))
        assertEquals(CollisionStatus.MISS, collision.status)
    }

    @Test
    fun test_checkCollision_with_wide_target_near_impact_always_hits() {
        val matrix = generate()
        val collision = collisionDetector.checkCollision(matrix, Target(centerX = matrix.range, radius = 50.0))
        assertEquals(CollisionStatus.HIT, collision.status)
    }

    @Test
    fun test_impact_result_exposes_correct_target_info() {
        val matrix = generate()
        val target = Target(centerX = matrix.range, radius = 2.0)
        val collision = collisionDetector.checkCollision(matrix, target)
        assertEquals(CollisionStatus.HIT, collision.status)
        assertEquals(target, collision.impactResult()?.target)
    }

    @Test
    fun test_isInsideTarget_returns_true_when_inside() {
        val target = Target(centerX = 50.0, radius = 2.0)
        assertTrue(collisionDetector.isInsideTarget(50.0, target))
        assertTrue(collisionDetector.isInsideTarget(51.0, target))
        assertTrue(collisionDetector.isInsideTarget(49.0, target))
    }

    @Test
    fun test_isInsideTarget_returns_false_when_outside() {
        val target = Target(centerX = 50.0, radius = 2.0)
        assertFalse(collisionDetector.isInsideTarget(55.0, target))
        assertFalse(collisionDetector.isInsideTarget(45.0, target))
    }

    @Test
    fun test_isInsideTarget_returns_true_at_exact_boundary() {
        val target = Target(centerX = 50.0, radius = 2.0)
        assertTrue(collisionDetector.isInsideTarget(52.0, target))
        assertTrue(collisionDetector.isInsideTarget(48.0, target))
    }

    @Test
    fun test_collision_message_contains_useful_info_on_HIT() {
        val matrix = generate()
        val collision = collisionDetector.checkCollision(matrix, Target(centerX = matrix.range, radius = 2.0))
        assertTrue(collision.message.contains("Impacto") && collision.message.contains("x=") && collision.message.contains("t="))
    }

    @Test
    fun test_collision_message_contains_useful_info_on_MISS() {
        val matrix = generate()
        val collision = collisionDetector.checkCollision(matrix, Target(centerX = matrix.range + 100.0, radius = 1.0))
        assertTrue(collision.message.contains("fuera del objetivo"))
    }
}

/**
 * Pruebas unitarias de invariantes, validaciones de dominio y variación paramétrica (SRP).
 */
class TrajectoryDomainValidationTest {

    private val testParams = PhysicsParams(angleDeg = 45.0, initialVelocity = 20.0, gravity = 9.8, mass = 1.0)
    private val generator = TrajectorySimulationFactory.createGenerator()

    @Test
    fun test_matrix_rejects_empty_points_list() {
        assertFailsWith<IllegalArgumentException> {
            TrajectoryMatrix(emptyList(), 0.0, 0.0, 0.0)
        }
    }

    @Test
    fun test_matrix_rejects_negative_time_of_flight() {
        assertFailsWith<IllegalArgumentException> {
            TrajectoryMatrix(listOf(TrajectoryPoint(0.0, 0.0, 1.0, 1.0, 0.0)), -1.0, 0.0, 0.0)
        }
    }

    @Test
    fun test_generator_throws_on_horizontal_launch() {
        assertFailsWith<IllegalArgumentException> {
            generator.generate(testParams.copy(angleDeg = 0.0), 120)
        }
    }

    @Test
    fun test_generator_throws_on_negative_samples_per_second() {
        assertFailsWith<IllegalArgumentException> {
            generator.generate(testParams, 0)
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
            val matrix = generator.generate(testParams.copy(angleDeg = angle), 120)
            assertTrue(matrix.size > 0 && matrix.timeOfFlight > 0 && matrix.range > 0 && matrix.maxHeight > 0)
        }
    }

    @Test
    fun test_matrix_works_with_different_velocities() {
        for (v0 in listOf(5.0, 10.0, 20.0, 50.0)) {
            val matrix = generator.generate(testParams.copy(initialVelocity = v0), 120)
            assertTrue(matrix.size > 0 && matrix.timeOfFlight > 0 && matrix.range > 0)
        }
    }
}
