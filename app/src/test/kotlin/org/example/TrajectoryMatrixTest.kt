package org.example

import org.example.collision.CollisionDetector
import org.example.collision.CollisionStatus
import org.example.model.Target
import org.example.model.TrajectoryMatrix
import org.example.trajectory.TrajectoryMatrixGenerator
import org.example.trajectory.TrajectorySearcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TrajectoryMatrixTest {

    private val testParams = PhysicsParams(
        angleDeg = 45.0,
        initialVelocity = 20.0,
        gravity = 9.8,
        mass = 1.0
    )

    // Dependencias obtenidas a través de la fábrica (DIP)
    private val generator = TrajectorySimulationFactory.createGenerator()
    private val searcher = TrajectorySimulationFactory.createSearcher()
    private val collisionDetector = TrajectorySimulationFactory.createCollisionDetector()

    private fun generate(params: PhysicsParams = testParams, samples: Int = 120): TrajectoryMatrix =
        generator.generate(params, samples)

    // ─── Generación ─────────────────────────────────────────────────────────

    @Test
    fun `generator creates matrix with correct number of points`() {
        val matrix = generate(samples = 100)

        assertTrue(matrix.size > 0, "La matriz debe tener puntos")
        // Para 45°, 20 m/s, 9.8 m/s²: t_vuelo ≈ 2.886 s → ~289 puntos con 100 samples/s
        assertTrue(matrix.size >= 100, "Debe haber al menos 100 puntos con 100 samples/s")
    }

    @Test
    fun `generator stores correct time of flight`() {
        val matrix = generate()

        val calculator = TrajectorySimulationFactory.createCalculator()
        val expectedToF = calculator.calculateTimeOfFlight(testParams)!!

        assertEquals(expectedToF, matrix.timeOfFlight, 1e-10)
    }

    @Test
    fun `generator stores correct range`() {
        val matrix = generate()

        val calculator = TrajectorySimulationFactory.createCalculator()
        val expectedRange = calculator.calculateRange(testParams)!!

        assertEquals(expectedRange, matrix.range, 1e-10)
    }

    @Test
    fun `generator stores correct max height`() {
        val matrix = generate()

        val calculator = TrajectorySimulationFactory.createCalculator()
        val expectedMaxHeight = calculator.calculateMaxHeight(testParams)

        assertEquals(expectedMaxHeight, matrix.maxHeight, 1e-10)
    }

    @Test
    fun `launch point is at origin`() {
        val matrix = generate()

        assertEquals(0.0, matrix.launchPoint.x, 1e-10)
        assertEquals(0.0, matrix.launchPoint.y, 1e-10)
        assertEquals(0.0, matrix.launchPoint.t, 1e-10)
    }

    @Test
    fun `impact point has y approximately zero`() {
        val matrix = generate()

        assertTrue(
            kotlin.math.abs(matrix.impactPoint.y) < 0.1,
            "El punto de impacto debe estar cerca del suelo (y≈0), pero y=${matrix.impactPoint.y}"
        )
    }

    @Test
    fun `points are ordered chronologically`() {
        val matrix = generate()

        for (i in 1 until matrix.points.size) {
            assertTrue(
                matrix.points[i].t >= matrix.points[i - 1].t,
                "Los puntos deben estar ordenados por tiempo"
            )
        }
    }

    @Test
    fun `x coordinates are monotonically increasing`() {
        val matrix = generate()

        for (i in 1 until matrix.points.size) {
            assertTrue(
                matrix.points[i].x >= matrix.points[i - 1].x,
                "Las coordenadas x deben ser crecientes"
            )
        }
    }

    // ─── Búsqueda ───────────────────────────────────────────────────────────

    @Test
    fun `findClosestByX returns correct point`() {
        val matrix = generate(samples = 100)

        val closest = searcher.findClosestByX(matrix.points, 10.0)
        assertTrue(
            kotlin.math.abs(closest.x - 10.0) < 0.5,
            "El punto más cercano a x=10 debe tener x≈10, pero x=${closest.x}"
        )
    }

    @Test
    fun `findClosestByX at exact point returns that point`() {
        val matrix = generate(samples = 100)

        val midIndex = matrix.size / 2
        val targetX = matrix.points[midIndex].x

        val closest = searcher.findClosestByX(matrix.points, targetX)
        assertEquals(targetX, closest.x, 1e-10)
    }

    @Test
    fun `findClosestByX at start returns first point`() {
        val matrix = generate()

        val closest = searcher.findClosestByX(matrix.points, 0.0)
        assertEquals(matrix.points.first().x, closest.x, 1e-10)
    }

    @Test
    fun `findClosestByX at end returns last point`() {
        val matrix = generate()

        val closest = searcher.findClosestByX(matrix.points, matrix.range)
        assertEquals(matrix.points.last().x, closest.x, 1e-10)
    }

    @Test
    fun `findClosestIndexByX returns a valid index`() {
        val matrix = generate(samples = 100)

        val index = searcher.findClosestIndexByX(matrix.points, 10.0)
        assertTrue(index in 0 until matrix.size, "Índice debe estar en rango")
    }

    // ─── Colisión ───────────────────────────────────────────────────────────

    @Test
    fun `checkCollision returns HIT when target is at impact point`() {
        val matrix = generate()

        val target = Target(centerX = matrix.range, radius = 2.0)
        val collision = collisionDetector.checkCollision(matrix, target)

        assertEquals(CollisionStatus.HIT, collision.status)
        assertNotNull(collision.impact)
        assertEquals(matrix.range, collision.impact.point.x, 2.0)
    }

    @Test
    fun `checkCollision returns MISS when target is far from impact`() {
        val matrix = generate()

        val target = Target(centerX = matrix.range + 100.0, radius = 1.0)
        val collision = collisionDetector.checkCollision(matrix, target)

        assertEquals(CollisionStatus.MISS, collision.status)
    }

    @Test
    fun `checkCollision with wide target near impact always hits`() {
        val matrix = generate()

        val target = Target(centerX = matrix.range, radius = 50.0)
        val collision = collisionDetector.checkCollision(matrix, target)

        assertEquals(CollisionStatus.HIT, collision.status)
    }

    @Test
    fun `matrix rejects empty points list`() {
        assertFailsWith<IllegalArgumentException> {
            TrajectoryMatrix(emptyList(), 0.0, 0.0, 0.0)
        }
    }

    @Test
    fun `matrix rejects negative time of flight`() {
        assertFailsWith<IllegalArgumentException> {
            TrajectoryMatrix(listOf(TrajectoryPoint(0.0, 0.0, 1.0, 1.0, 0.0)), -1.0, 0.0, 0.0)
        }
    }

    @Test
    fun `impact result exposes correct target info`() {
        val matrix = generate()

        val target = Target(centerX = matrix.range, radius = 2.0)
        val collision = collisionDetector.checkCollision(matrix, target)

        assertEquals(CollisionStatus.HIT, collision.status)
        val impact = collision.impact
        assertNotNull(impact)
        assertEquals(target, impact.target)
        assertEquals(target.centerX, impact.target.centerX, 1e-10)
    }

    @Test
    fun `isInsideTarget returns true when inside`() {
        val target = Target(centerX = 50.0, radius = 2.0)

        assertTrue(collisionDetector.isInsideTarget(50.0, target))
        assertTrue(collisionDetector.isInsideTarget(51.0, target))
        assertTrue(collisionDetector.isInsideTarget(49.0, target))
    }

    @Test
    fun `isInsideTarget returns false when outside`() {
        val target = Target(centerX = 50.0, radius = 2.0)

        assertFalse(collisionDetector.isInsideTarget(55.0, target))
        assertFalse(collisionDetector.isInsideTarget(45.0, target))
    }

    @Test
    fun `isInsideTarget returns true at exact boundary`() {
        val target = Target(centerX = 50.0, radius = 2.0)

        assertTrue(collisionDetector.isInsideTarget(52.0, target))
        assertTrue(collisionDetector.isInsideTarget(48.0, target))
    }

    // ─── Consultas del contenedor ───────────────────────────────────────────

    @Test
    fun `subMatrix returns correct range`() {
        val matrix = generate(samples = 10)

        val sub = matrix.subMatrix(2, 5)

        assertEquals(3, sub.size)
        assertEquals(matrix.points[2].x, sub[0].x, 1e-10)
        assertEquals(matrix.points[4].x, sub[2].x, 1e-10)
    }

    @Test
    fun `subMatrix clamps indices out of range`() {
        val matrix = generate(samples = 10)

        val sub = matrix.subMatrix(-5, matrix.size + 10)

        assertEquals(matrix.size, sub.size)
    }

    @Test
    fun `pointsAbove filters correctly`() {
        val matrix = generate()

        val aboveZero = matrix.pointsAbove(0.0)
        assertTrue(aboveZero.isNotEmpty(), "Debe haber puntos por encima de y=0")

        aboveZero.forEach { point ->
            assertTrue(point.y >= 0.0, "Todos los puntos deben tener y >= 0")
        }
    }

    // ─── Validaciones ───────────────────────────────────────────────────────

    @Test
    fun `generator throws on horizontal launch`() {
        val horizontalParams = testParams.copy(angleDeg = 0.0)

        assertFailsWith<IllegalArgumentException> {
            generate(horizontalParams)
        }
    }

    @Test
    fun `generator throws on negative samples per second`() {
        assertFailsWith<IllegalArgumentException> {
            generate(samples = 0)
        }
    }

    @Test
    fun `Target rejects negative radius`() {
        assertFailsWith<IllegalArgumentException> {
            Target(centerX = 10.0, radius = -1.0)
        }
    }

    @Test
    fun `matrix works with different angles`() {
        val angles = listOf(15.0, 30.0, 45.0, 60.0, 75.0)

        angles.forEach { angle ->
            val params = testParams.copy(angleDeg = angle)
            val matrix = generate(params)

            assertTrue(matrix.size > 0, "Ángulo $angle°: debe tener puntos")
            assertTrue(matrix.timeOfFlight > 0, "Ángulo $angle°: tiempo de vuelo > 0")
            assertTrue(matrix.range > 0, "Ángulo $angle°: alcance > 0")
            assertTrue(matrix.maxHeight > 0, "Ángulo $angle°: altura máxima > 0")
        }
    }

    @Test
    fun `matrix works with different velocities`() {
        val velocities = listOf(5.0, 10.0, 20.0, 50.0)

        velocities.forEach { v0 ->
            val params = testParams.copy(initialVelocity = v0)
            val matrix = generate(params)

            assertTrue(matrix.size > 0)
            assertTrue(matrix.timeOfFlight > 0)
            assertTrue(matrix.range > 0)
        }
    }

    // ─── Mensajes ───────────────────────────────────────────────────────────

    @Test
    fun `collision message contains useful info on HIT`() {
        val matrix = generate()
        val target = Target(centerX = matrix.range, radius = 2.0)
        val collision = collisionDetector.checkCollision(matrix, target)

        assertTrue(collision.message.contains("Impacto"), "El mensaje debe indicar impacto")
        assertTrue(collision.message.contains("x="), "El mensaje debe contener coordenada x")
        assertTrue(collision.message.contains("t="), "El mensaje debe contener tiempo")
    }

    @Test
    fun `collision message contains useful info on MISS`() {
        val matrix = generate()
        val target = Target(centerX = matrix.range + 100.0, radius = 1.0)
        val collision = collisionDetector.checkCollision(matrix, target)

        assertTrue(collision.message.contains("fuera del objetivo"), "El mensaje debe indicar que está fuera")
    }
}
