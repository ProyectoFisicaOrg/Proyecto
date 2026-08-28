package org.example

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhysicsTest {

    private val calculator = TrajectoryCalculatorFactory.createParabolic()

    // Parámetros de prueba estándar (45° en grados)
    private val testParams = PhysicsParams(
        angleDeg = 45.0,
        initialVelocity = 20.0,
        gravity = 9.8,
        mass = 1.0
    )

    // Helper: ángulo en radianes para cálculos esperados
    private val testAngleRad = testParams.angleRad

    @Test
    fun `calculateState at t=0 returns initial position and velocity`() {
        val point = calculator.calculateState(testParams, 0.0)

        assertEquals(0.0, point.x, 1e-10, "x(0) debe ser 0")
        assertEquals(0.0, point.y, 1e-10, "y(0) debe ser 0")
        assertEquals(testParams.initialVelocity * cos(testAngleRad), point.vx, 1e-10, "vx(0) = v0 * cos(theta)")
        assertEquals(testParams.initialVelocity * sin(testAngleRad), point.vy, 1e-10, "vy(0) = v0 * sin(theta)")
        assertEquals(0.0, point.t, 1e-10, "t debe ser 0")
    }

    @Test
    fun `calculateState at t=1 returns correct position and velocity`() {
        val point = calculator.calculateState(testParams, 1.0)

        val expectedX = testParams.initialVelocity * cos(testAngleRad) * 1.0
        val expectedY = testParams.initialVelocity * sin(testAngleRad) * 1.0 - 0.5 * testParams.gravity * 1.0 * 1.0
        val expectedVx = testParams.initialVelocity * cos(testAngleRad)
        val expectedVy = testParams.initialVelocity * sin(testAngleRad) - testParams.gravity * 1.0

        assertEquals(expectedX, point.x, 1e-10, "x(1) incorrecto")
        assertEquals(expectedY, point.y, 1e-10, "y(1) incorrecto")
        assertEquals(expectedVx, point.vx, 1e-10, "vx(1) incorrecto")
        assertEquals(expectedVy, point.vy, 1e-10, "vy(1) incorrecto")
        assertEquals(1.0, point.t, 1e-10, "t debe ser 1")
    }

    @Test
    fun `calculateState at t=2 returns correct position and velocity`() {
        val point = calculator.calculateState(testParams, 2.0)

        val expectedX = testParams.initialVelocity * cos(testAngleRad) * 2.0
        val expectedY = testParams.initialVelocity * sin(testAngleRad) * 2.0 - 0.5 * testParams.gravity * 2.0 * 2.0
        val expectedVx = testParams.initialVelocity * cos(testAngleRad)
        val expectedVy = testParams.initialVelocity * sin(testAngleRad) - testParams.gravity * 2.0

        assertEquals(expectedX, point.x, 1e-10, "x(2) incorrecto")
        assertEquals(expectedY, point.y, 1e-10, "y(2) incorrecto")
        assertEquals(expectedVx, point.vx, 1e-10, "vx(2) incorrecto")
        assertEquals(expectedVy, point.vy, 1e-10, "vy(2) incorrecto")
        assertEquals(2.0, point.t, 1e-10, "t debe ser 2")
    }

    @Test
    fun `calculateTimeOfFlight returns correct time for 45 degree launch`() {
        val timeOfFlight = calculator.calculateTimeOfFlight(testParams)

        assertNotNull(timeOfFlight)
        // t = 2 * v0 * sin(theta) / g
        val expected = 2.0 * testParams.initialVelocity * sin(testAngleRad) / testParams.gravity
        assertEquals(expected, timeOfFlight, 1e-10)
    }

    @Test
    fun `calculateTimeOfFlight returns null for horizontal launch from ground`() {
        val horizontalParams = testParams.copy(angleDeg = 0.0)
        val timeOfFlight = calculator.calculateTimeOfFlight(horizontalParams)
        assertNull(timeOfFlight)
    }

    @Test
    fun `calculateTimeOfFlight returns null for downward launch from ground`() {
        val downwardParams = testParams.copy(angleDeg = -45.0)
        val timeOfFlight = calculator.calculateTimeOfFlight(downwardParams)
        assertNull(timeOfFlight)
    }

    @Test
    fun `calculateRange returns correct range for 45 degree launch`() {
        val range = calculator.calculateRange(testParams)

        assertNotNull(range)
        // R = v0^2 * sin(2*theta) / g
        // Para theta = 45°, sin(90°) = 1, así que R = v0^2 / g
        val expected = (testParams.initialVelocity * testParams.initialVelocity) / testParams.gravity
        assertEquals(expected, range, 1e-10)
    }

    @Test
    fun `calculateMaxHeight returns correct max height for 45 degree launch`() {
        val maxHeight = calculator.calculateMaxHeight(testParams)

        // h_max = (v0 * sin(theta))^2 / (2g)
        val v0y = testParams.initialVelocity * sin(testAngleRad)
        val expected = (v0y * v0y) / (2.0 * testParams.gravity)
        assertEquals(expected, maxHeight, 1e-10)
    }

    @Test
    fun `calculateMaxHeight returns 0 for horizontal launch`() {
        val horizontalParams = testParams.copy(angleDeg = 0.0)
        val maxHeight = calculator.calculateMaxHeight(horizontalParams)
        assertEquals(0.0, maxHeight, 1e-10)
    }

    @Test
    fun `calculateMaxHeight returns 0 for downward launch`() {
        val downwardParams = testParams.copy(angleDeg = -45.0)
        val maxHeight = calculator.calculateMaxHeight(downwardParams)
        assertEquals(0.0, maxHeight, 1e-10)
    }

    @Test
    fun `TrajectoryPoint speed calculates magnitude correctly`() {
        val point = TrajectoryPoint(3.0, 4.0, 3.0, 4.0, 1.0)
        assertEquals(5.0, point.speed(), 1e-10)
    }

    @Test
    fun `TrajectoryPoint height returns y coordinate`() {
        val point = TrajectoryPoint(10.0, 5.0, 1.0, 2.0, 1.0)
        assertEquals(5.0, point.height(), 1e-10)
    }

    @Test
    fun `TrajectoryPoint distance returns x coordinate`() {
        val point = TrajectoryPoint(10.0, 5.0, 1.0, 2.0, 1.0)
        assertEquals(10.0, point.distance(), 1e-10)
    }

    @Test
    fun `PhysicsParams validation rejects negative initial velocity`() {
        assertTrue({
            try {
                PhysicsParams(angleDeg = 0.0, initialVelocity = -1.0, gravity = 9.8, mass = 1.0)
                false
            } catch (e: IllegalArgumentException) {
                true
            }
        }())
    }

    @Test
    fun `PhysicsParams validation rejects zero gravity`() {
        assertTrue({
            try {
                PhysicsParams(angleDeg = 0.0, initialVelocity = 10.0, gravity = 0.0, mass = 1.0)
                false
            } catch (e: IllegalArgumentException) {
                true
            }
        }())
    }

    @Test
    fun `PhysicsParams validation rejects negative gravity`() {
        assertTrue({
            try {
                PhysicsParams(angleDeg = 0.0, initialVelocity = 10.0, gravity = -9.8, mass = 1.0)
                false
            } catch (e: IllegalArgumentException) {
                true
            }
        }())
    }

    @Test
    fun `PhysicsParams validation rejects zero mass`() {
        assertTrue({
            try {
                PhysicsParams(angleDeg = 0.0, initialVelocity = 10.0, gravity = 9.8, mass = 0.0)
                false
            } catch (e: IllegalArgumentException) {
                true
            }
        }())
    }

    @Test
    fun `PhysicsParams validation rejects angle greater than 90 degrees`() {
        assertTrue({
            try {
                PhysicsParams(angleDeg = 95.0, initialVelocity = 10.0, gravity = 9.8, mass = 1.0)
                false
            } catch (e: IllegalArgumentException) {
                true
            }
        }())
    }

    @Test
    fun `PhysicsParams validation rejects angle less than -90 degrees`() {
        assertTrue({
            try {
                PhysicsParams(angleDeg = -95.0, initialVelocity = 10.0, gravity = 9.8, mass = 1.0)
                false
            } catch (e: IllegalArgumentException) {
                true
            }
        }())
    }

    @Test
    fun `calculateState throws on negative time`() {
        assertTrue({
            try {
                calculator.calculateState(testParams, -1.0)
                false
            } catch (e: IllegalArgumentException) {
                true
            }
        }())
    }

    @Test
    fun `verify trajectory is parabolic - y follows quadratic equation`() {
        // Verificar que la trayectoria sigue una parábola
        val tValues = doubleArrayOf(0.0, 0.5, 1.0, 1.5, 2.0)
        val points = tValues.map { calculator.calculateState(testParams, it) }

        // Verificar que x es lineal (vx constante)
        val vx = points.first().vx
        points.forEach { assertEquals(vx, it.vx, 1e-10) }

        // Verificar que y sigue la ecuación cuadrática
        points.forEach { point ->
            val expectedY = testParams.initialVelocity * sin(testAngleRad) * point.t -
                0.5 * testParams.gravity * point.t * point.t
            assertEquals(expectedY, point.y, 1e-10)
        }
    }

    @Test
    fun `angleRad property converts degrees to radians correctly`() {
        val params = PhysicsParams(angleDeg = 90.0, initialVelocity = 10.0, gravity = 9.8, mass = 1.0)
        assertEquals(PI / 2.0, params.angleRad, 1e-10)

        val params2 = PhysicsParams(angleDeg = 45.0, initialVelocity = 10.0, gravity = 9.8, mass = 1.0)
        assertEquals(PI / 4.0, params2.angleRad, 1e-10)

        val params3 = PhysicsParams(angleDeg = -45.0, initialVelocity = 10.0, gravity = 9.8, mass = 1.0)
        assertEquals(-PI / 4.0, params3.angleRad, 1e-10)

        val params4 = PhysicsParams(angleDeg = 0.0, initialVelocity = 10.0, gravity = 9.8, mass = 1.0)
        assertEquals(0.0, params4.angleRad, 1e-10)
    }
}