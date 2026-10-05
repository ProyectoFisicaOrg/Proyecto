package org.example.telemetry

import org.example.PhysicsParams
import org.example.Position
import org.example.TrajectoryPoint
import org.example.TrajectorySimulationFactory
import org.example.model.TrajectoryMatrix
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InstantaneousStateApiTest {

    private val testParams = PhysicsParams(
        angleDeg = 45.0,
        initialVelocity = 20.0,
        gravity = 9.8,
        mass = 1.0
    )

    private val matrix: TrajectoryMatrix by lazy {
        TrajectorySimulationFactory.generateMatrix(testParams, samplesPerSecond = 100)
    }

    private val telemetryApi = TelemetryFactory.createApi()
    private val searcher = TelemetryFactory.createSearcher()

    // ─── 1. InstantaneousState Data Model ───────────────────────────────────

    @Test
    fun `InstantaneousState computes speed correctly as Euclidean norm of velocities`() {
        val point = TrajectoryPoint(x = 10.0, y = 5.0, vx = 3.0, vy = 4.0, t = 1.0)
        val state = InstantaneousState.fromTrajectoryPoint(point)

        assertEquals(5.0, state.speed, 1e-10) // sqrt(3^2 + 4^2) = 5.0
        assertEquals(5.0, state.v, 1e-10)
        assertEquals(1.0, state.time, 1e-10)
        assertEquals(1.0, state.t, 1e-10)
        assertEquals(5.0, state.height, 1e-10)
        assertEquals(5.0, state.y, 1e-10)
        assertEquals(10.0, state.x, 1e-10)
        assertEquals(10.0, state.distance, 1e-10)
        assertEquals(Position(10.0, 5.0), state.position)
    }

    @Test
    fun `InstantaneousState format produces readable telemetry string`() {
        val point = TrajectoryPoint(x = 12.34, y = 8.56, vx = 10.0, vy = 0.0, t = 1.25)
        val state = InstantaneousState.fromTrajectoryPoint(point)
        val formatted = state.format()

        assertTrue(formatted.contains("t=1.25 s"), "Debe contener el tiempo format")
        assertTrue(formatted.contains("h=8.56 m"), "Debe contener la altura format")
        assertTrue(formatted.contains("v=10.00 m/s"), "Debe contener la velocidad format")
    }

    // ─── 2. Consultas en Puntos Clave de la Cinemática Parabólica ───────────

    @Test
    fun `query at launch coordinates returns launch telemetry with initial velocity`() {
        val state = telemetryApi.getInstantaneousState(matrix, x = 0.0, y = 0.0)

        assertEquals(0.0, state.time, 1e-4, "El tiempo en el origen debe ser ~0")
        assertEquals(0.0, state.height, 1e-4, "La altura en el origen debe ser ~0")
        assertEquals(testParams.initialVelocity, state.speed, 0.1, "La velocidad al inicio debe ser v0")
        assertEquals(0.0, state.distanceToQuery, 1e-4)
    }

    @Test
    fun `query at apex coordinates returns max height and horizontal velocity`() {
        val calculator = TrajectorySimulationFactory.createCalculator()
        val expectedMaxHeight = calculator.calculateMaxHeight(testParams)
        val timeToApex = (testParams.initialVelocity * sin(testParams.angleRad)) / testParams.gravity
        val apexPoint = calculator.calculateState(testParams, timeToApex)

        val state = telemetryApi.getInstantaneousState(matrix, x = apexPoint.x, y = expectedMaxHeight)

        assertEquals(expectedMaxHeight, state.height, 0.1, "La altura debe ser cercana a h_max")
        assertEquals(timeToApex, state.time, 0.05, "El tiempo debe ser el tiempo al ápice")

        // En el punto más alto, vy = 0, por lo que v = vx = v0 * cos(theta)
        val expectedVx = testParams.initialVelocity * cos(testParams.angleRad)
        assertEquals(expectedVx, state.speed, 0.1, "La velocidad en el ápice debe ser vx")
    }

    @Test
    fun `query at impact point returns time of flight and ground height`() {
        val state = telemetryApi.getInstantaneousState(matrix, x = matrix.range, y = 0.0)

        assertEquals(matrix.timeOfFlight, state.time, 0.05, "El tiempo debe ser timeOfFlight")
        assertTrue(abs(state.height) < 0.2, "La altura al impacto debe ser ~0")
        assertEquals(testParams.initialVelocity, state.speed, 0.2, "La velocidad de impacto en suelo simétrico debe ser ~v0")
    }

    // ─── 3. Nearest Neighbor 2D (Euclidean) ──────────────────────────────────

    @Test
    fun `exact point query on trajectory returns that point with zero distance`() {
        val samplePoint = matrix.points[matrix.size / 3]
        val state = telemetryApi.getInstantaneousState(matrix, samplePoint.x, samplePoint.y)

        assertEquals(samplePoint.t, state.time, 1e-10)
        assertEquals(samplePoint.y, state.height, 1e-10)
        assertEquals(samplePoint.speed(), state.speed, 1e-10)
        assertEquals(0.0, state.distanceToQuery, 1e-10)
    }

    @Test
    fun `mouse coordinate near trajectory finds closest point on curve`() {
        val midPoint = matrix.points[matrix.size / 2]
        // Coordenada del mouse ligeramente desplazada en Y (+1.5m arriba de la curva)
        val mouseX = midPoint.x
        val mouseY = midPoint.y + 1.5

        val state = telemetryApi.getInstantaneousState(matrix, mouseX, mouseY)

        assertEquals(midPoint.x, state.x, 0.5, "Debe aproximar al punto central")
        assertTrue(state.distanceToQuery <= 1.6, "La distancia debe ser cercana al offset del mouse")
    }

    @Test
    fun `query using Position object works identically`() {
        val pos = Position(15.0, 7.0)
        val stateFromPosition = telemetryApi.getInstantaneousState(matrix, pos)
        val stateFromXY = telemetryApi.getInstantaneousState(matrix, pos.x, pos.y)

        assertEquals(stateFromXY.point, stateFromPosition.point)
        assertEquals(stateFromXY.speed, stateFromPosition.speed, 1e-10)
        assertEquals(stateFromXY.time, stateFromPosition.time, 1e-10)
        assertEquals(stateFromXY.height, stateFromPosition.height, 1e-10)
    }

    // ─── 4. Nearest Neighbor by X (1D Projection) ───────────────────────────

    @Test
    fun `getInstantaneousStateByX finds point matching horizontal coordinate`() {
        val targetX = matrix.range * 0.4
        val state = telemetryApi.getInstantaneousStateByX(matrix, targetX)

        assertTrue(abs(state.x - targetX) < 0.3, "El punto devuelto debe tener x cercano a targetX")
        assertTrue(state.height > 0.0, "La altura debe ser positiva en mitad del trayecto")
        assertTrue(state.speed > 0.0, "La velocidad debe ser positiva")
    }

    @Test
    fun `clamping when querying before launch point returns first point`() {
        val state = telemetryApi.getInstantaneousStateByX(matrix, -50.0)

        assertEquals(matrix.launchPoint.x, state.x, 1e-10)
        assertEquals(matrix.launchPoint.t, state.time, 1e-10)
    }

    @Test
    fun `clamping when querying beyond range returns impact point`() {
        val state = telemetryApi.getInstantaneousStateByX(matrix, matrix.range + 100.0)

        assertEquals(matrix.impactPoint.x, state.x, 1e-10)
        assertEquals(matrix.impactPoint.t, state.time, 1e-10)
    }

    // ─── 5. TrajectoryInspector (Scrubbing / Drag & Drop para Dev 2) ─────────

    @Test
    fun `TrajectoryInspector enables fast consecutive scrubbing inspections`() {
        val inspector = telemetryApi.createInspector(matrix)
        assertEquals(matrix, inspector.matrix)

        // Simular arrastre del mouse (scrubbing) a través de 20 posiciones
        val step = matrix.range / 20.0
        var previousTime = -1.0

        for (i in 0..20) {
            val scrubX = i * step
            val state = inspector.inspectByX(scrubX)

            assertTrue(state.time >= previousTime, "El tiempo debe ser no-decreciente durante el scrubbing hacia adelante")
            assertTrue(state.height >= -0.1, "La altura debe ser no-negativa")
            assertTrue(state.speed > 0, "La velocidad debe ser positiva")
            previousTime = state.time
        }
    }

    @Test
    fun `inspector 2D inspect matches api getInstantaneousState`() {
        val inspector = TelemetryFactory.createInspector(matrix)
        val expected = telemetryApi.getInstantaneousState(matrix, 12.0, 6.0)
        val actual = inspector.inspect(12.0, 6.0)

        assertEquals(expected.point, actual.point)
        assertEquals(expected.speed, actual.speed, 1e-10)
        assertEquals(expected.height, actual.height, 1e-10)
        assertEquals(expected.time, actual.time, 1e-10)
    }

    // ─── 6. Principios SOLID e Inversión de Dependencias (DIP) ──────────────

    @Test
    fun `custom searcher can be injected into api conforming to DIP and OCP`() {
        // Implementación personalizada mock/espía del buscador
        var searcherInvoked = false
        val customSearcher = object : TrajectoryNearestNeighborSearcher {
            override fun findNearest(points: List<TrajectoryPoint>, x: Double, y: Double): TrajectoryPoint {
                searcherInvoked = true
                return points.first()
            }
            override fun findNearestByX(points: List<TrajectoryPoint>, targetX: Double): TrajectoryPoint {
                searcherInvoked = true
                return points.first()
            }
            override fun findNearestIndex(points: List<TrajectoryPoint>, x: Double, y: Double): Int = 0
            override fun findNearestIndexByX(points: List<TrajectoryPoint>, targetX: Double): Int = 0
        }

        val customApi = TelemetryFactory.createApi(customSearcher)
        val result = customApi.getInstantaneousState(matrix, 50.0, 50.0)

        assertTrue(searcherInvoked, "El buscador inyectado debe haber sido invocado")
        assertEquals(matrix.launchPoint, result.point)
    }

    // ─── 7. Extension Functions ─────────────────────────────────────────────

    @Test
    fun `TrajectoryMatrix extension functions provide ergonomic access`() {
        val stateXY = matrix.getInstantaneousState(10.0, 5.0)
        val stateByX = matrix.getInstantaneousStateByX(10.0)
        val statePos = matrix.getInstantaneousState(Position(10.0, 5.0))

        assertNotNull(stateXY)
        assertNotNull(stateByX)
        assertNotNull(statePos)
        assertEquals(stateXY.point, statePos.point)
    }

    @Test
    fun `TrajectorySimulationFactory extension functions assemble telemetry components`() {
        val factoryApi = TrajectorySimulationFactory.createInstantaneousStateApi()
        val factoryInspector = TrajectorySimulationFactory.createTrajectoryInspector(matrix)

        assertNotNull(factoryApi)
        assertNotNull(factoryInspector)
        assertEquals(matrix, factoryInspector.matrix)
    }

    // ─── 8. Robustez y Manejo de Errores ─────────────────────────────────────

    @Test
    fun `api rejects empty points list`() {
        assertFailsWith<IllegalArgumentException> {
            telemetryApi.getInstantaneousState(emptyList(), 0.0, 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            telemetryApi.getInstantaneousStateByX(emptyList(), 0.0)
        }
    }

    @Test
    fun `searcher handles single point trajectory safely`() {
        val singlePoint = listOf(TrajectoryPoint(5.0, 5.0, 10.0, 10.0, 0.5))
        val nearest2D = searcher.findNearest(singlePoint, 100.0, 100.0)
        val nearestX = searcher.findNearestByX(singlePoint, 100.0)

        assertEquals(singlePoint.first(), nearest2D)
        assertEquals(singlePoint.first(), nearestX)
    }

    @Test
    fun `speed calculation satisfies v equals sqrt vx squared plus vy squared for all points`() {
        matrix.points.forEach { point ->
            val state = InstantaneousState.fromTrajectoryPoint(point)
            val expectedSpeed = hypot(point.vx, point.vy)
            assertEquals(expectedSpeed, state.speed, 1e-10)
        }
    }
}
