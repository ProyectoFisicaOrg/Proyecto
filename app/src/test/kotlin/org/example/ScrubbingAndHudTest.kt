package org.example

import org.example.model.SimulationState
import org.example.rendering.CoordinateTransformer
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.PathRenderer
import org.example.rendering.SceneConfig
import org.example.scrubbing.FlightSummary
import org.example.scrubbing.ITrajectoryInspector
import org.example.scrubbing.PhysicsTrajectoryInspectorAdapter
import org.example.scrubbing.ResultsHUD
import org.example.scrubbing.ScrubbingController
import org.example.scrubbing.TooltipRenderer
import org.example.scrubbing.TrajectoryPoint
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Suite de pruebas unitarias para la Issue 3.2: Arrastre con Mouse (Scrubbing) y HUD.
 *
 * Valida los 4 requerimientos principales y el cumplimiento de POO y Principios SOLID:
 *  1. Definición de Contratos (TrajectoryPoint, FlightSummary, ITrajectoryInspector).
 *  2. Mecanismo de Scrubbing (arrastre, hit-testing, enganche exacto y ciclo de activación).
 *  3. HUD / Tarjeta de Resultados Finales (tiempo total de caída y distancia recorrida).
 *  4. Etiqueta Flotante Interactiva (Tooltip con velocidad instantánea y altura Y).
 */
class ScrubbingAndHudTest {

    private val cfg = SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)
    private val transformer: CoordinateTransformer = DefaultCoordinateTransformer(cfg, pixelsPerMeter = 2.5)

    // Generador de física de Dev 1 para crear trayectorias reales en los tests
    private val generator = TrajectorySimulationFactory.createGenerator()

    private fun createSampleMatrix(): org.example.model.TrajectoryMatrix {
        val params = PhysicsParams(
            angleDeg = 45.0,
            initialVelocity = 40.0,
            gravity = 9.8,
            mass = 5.0
        )
        return generator.generate(params, samplesPerSecond = 120)
    }

    // ─── 1. Pruebas de Contratos y Modelos de Datos (SRP + Inmutabilidad) ────────

    @Test
    fun `trajectory point encapsulates kinematic state and provides bilingual accessors`() {
        val point = TrajectoryPoint(
            x = 120.5,
            y = 35.8,
            time = 2.15,
            height = 35.8,
            speed = 28.4
        )

        assertEquals(120.5, point.x)
        assertEquals(35.8, point.y)
        assertEquals(2.15, point.time)
        assertEquals(35.8, point.height)
        assertEquals(28.4, point.speed)

        // Aliases en español requeridos explícitamente
        assertEquals(point.time, point.tiempo)
        assertEquals(point.height, point.altura)
        assertEquals(point.speed, point.velocidad)

        assertTrue(point.formatTelemetry().contains("v = 28.40 m/s"))
        assertTrue(point.formatTelemetry().contains("h = 35.80 m"))
    }

    @Test
    fun `flight summary encapsulates total flight time and total distance`() {
        val summary = FlightSummary(
            totalFlightTime = 5.77,
            totalDistance = 163.26
        )

        assertEquals(5.77, summary.totalFlightTime)
        assertEquals(163.26, summary.totalDistance)
        assertEquals(5.77, summary.tiempoTotalCaida)
        assertEquals(163.26, summary.distanciaRecorrida)

        assertEquals("5.77 s", summary.formattedTime())
        assertEquals("163.26 m", summary.formattedDistance())
    }

    // ─── 2. Pruebas de Inversión de Dependencias (DIP) y Adaptador ───────────────

    @Test
    fun `physics adapter correctly implements ITrajectoryInspector`() {
        val matrix = createSampleMatrix()
        val inspector: ITrajectoryInspector = PhysicsTrajectoryInspectorAdapter(matrix, transformer)

        // Consultar por coordenadas físicas
        val nearestPhysical = inspector.findNearestPoint(50.0, 20.0)
        assertNotNull(nearestPhysical)
        assertTrue(nearestPhysical.speed > 0.0)
        assertTrue(nearestPhysical.time > 0.0)

        // Consultar desde coordenadas del cursor en píxeles del canvas
        val cursorCanvasX = transformer.toCanvasX(50.0)
        val cursorCanvasY = transformer.toCanvasY(20.0)
        val nearestFromCursor = inspector.findNearestPointFromCursor(cursorCanvasX, cursorCanvasY)

        assertEquals(nearestPhysical.x, nearestFromCursor.x, 1e-4)
        assertEquals(nearestPhysical.y, nearestFromCursor.y, 1e-4)
        assertEquals(nearestPhysical.speed, nearestFromCursor.speed, 1e-4)

        // Resumen del vuelo a través del inspector
        val summary = inspector.getFlightSummary()
        assertEquals(matrix.timeOfFlight, summary.totalFlightTime, 1e-4)
        assertEquals(matrix.range, summary.totalDistance, 1e-4)
    }

    // ─── 3. Pruebas del Mecanismo de Scrubbing (ScrubbingController) ─────────────

    @Test
    fun `scrubbing is disabled before projectile impact and only enabled post flight`() {
        val state = SimulationState()
        val controller = ScrubbingController(state, transformer, cfg)

        assertFalse(controller.isEnabled, "El scrubbing debe estar deshabilitado inicialmente")
        assertFalse(controller.isDragging)
        assertNull(controller.inspector)

        // Intentar arrastrar cuando no está habilitado no debe tener efecto
        val consumed = controller.handleMousePressed(100.0, 100.0)
        assertFalse(consumed, "No debe consumir eventos de mouse cuando está deshabilitado")

        // Habilitar tras completar el vuelo
        val matrix = createSampleMatrix()
        val inspector = PhysicsTrajectoryInspectorAdapter(matrix, transformer)
        controller.enable(inspector)

        assertTrue(controller.isEnabled, "Debe estar habilitado tras invocar enable con el inspector")
        assertNotNull(controller.currentPoint)
        assertEquals(matrix.timeOfFlight, controller.flightSummary?.totalFlightTime)
    }

    @Test
    fun `scrubbing precisely snaps projectile position to the prior trajectory curve`() {
        val state = SimulationState()
        val controller = ScrubbingController(state, transformer, cfg)
        val matrix = createSampleMatrix()
        val inspector = PhysicsTrajectoryInspectorAdapter(matrix, transformer)

        controller.enable(inspector)

        // Tomar un punto arbitrario de la trayectoria generada por Dev 1
        val targetTrajectoryPoint = matrix.points[matrix.points.size / 2]
        val expectedCanvasX = transformer.toCanvasX(targetTrajectoryPoint.x)
        val expectedCanvasY = transformer.toCanvasY(targetTrajectoryPoint.y)

        // Simular un clic exactamente en ese punto en el canvas
        val clickCanvasX = expectedCanvasX
        val clickCanvasY = expectedCanvasY

        val pressed = controller.handleMousePressed(clickCanvasX, clickCanvasY)
        assertTrue(pressed, "El clic dentro del umbral de hit-test debe iniciar el arrastre")
        assertTrue(controller.isDragging, "Debe pasar al estado de arrastre")

        // El proyectil debe haberse 'enganchado' exactamente a la curva matemática
        assertEquals(expectedCanvasX, state.projectileX, 1e-4, "La coordenada X del proyectil debe ajustarse a la curva")
        assertEquals(expectedCanvasY, state.projectileY, 1e-4, "La coordenada Y del proyectil debe ajustarse a la curva")

        // Simular arrastre hacia otro sector de la trayectoria
        val anotherPoint = matrix.points[matrix.points.size / 4]
        val dragCanvasX = transformer.toCanvasX(anotherPoint.x)
        val dragCanvasY = transformer.toCanvasY(anotherPoint.y)

        val dragged = controller.handleMouseDragged(dragCanvasX, dragCanvasY)
        assertTrue(dragged)
        assertEquals(dragCanvasX, state.projectileX, 1e-4)
        assertEquals(dragCanvasY, state.projectileY, 1e-4)

        // Al soltar el mouse, finaliza el arrastre pero conserva la posición enganchada
        val released = controller.handleMouseReleased()
        assertTrue(released)
        assertFalse(controller.isDragging)
        assertEquals(dragCanvasX, state.projectileX, 2.5)
    }

    @Test
    fun `scrubbing does not intercept clicks on the ground target away from projectile`() {
        val state = SimulationState().apply {
            targetX = 650.0
            targetRadius = 20.0
            projectileX = 100.0
            projectileY = 400.0
        }
        val controller = ScrubbingController(state, transformer, cfg)
        val matrix = createSampleMatrix()
        controller.enable(PhysicsTrajectoryInspectorAdapter(matrix, transformer))

        // Clic exactamente en el centro de la diana sobre el suelo (alejado de la pelota)
        val consumed = controller.handleMousePressed(state.targetX, cfg.groundY)
        assertFalse(consumed, "ScrubbingController debe ceder el evento a TargetRenderer cuando el clic es sobre la diana lejos del proyectil")
        assertFalse(controller.isDragging)
    }

    @Test
    fun `when projectile is on top of target, clicking on the projectile selects the projectile because it is in front`() {
        val matrix = createSampleMatrix()
        // Punto de impacto final de la trayectoria
        val impactX = transformer.toCanvasX(matrix.impactPoint.x)
        val impactY = transformer.toCanvasY(matrix.impactPoint.y)

        val state = SimulationState().apply {
            // La diana se encuentra justo en el punto de impacto
            targetX = impactX
            targetRadius = 20.0
            projectileX = impactX
            projectileY = impactY
        }
        val controller = ScrubbingController(state, transformer, cfg)
        controller.enable(PhysicsTrajectoryInspectorAdapter(matrix, transformer))

        // Clic directamente sobre la pelota que está posada encima / al frente de la diana
        val consumed = controller.handleMousePressed(impactX, impactY)
        assertTrue(consumed, "Al hacer clic sobre la pelota que está al frente, debe seleccionarse la pelota con máxima prioridad")
        assertTrue(controller.isDragging, "Debe iniciar el arrastre del proyectil (capa frontal)")
    }

    @Test
    fun `disabling scrubbing resets state and clears inspector`() {
        val state = SimulationState()
        val controller = ScrubbingController(state, transformer, cfg)
        val matrix = createSampleMatrix()
        controller.enable(PhysicsTrajectoryInspectorAdapter(matrix, transformer))

        assertTrue(controller.isEnabled)
        assertNotNull(controller.currentPoint)

        controller.disable()

        assertFalse(controller.isEnabled)
        assertNull(controller.inspector)
        assertNull(controller.currentPoint)
        assertNull(controller.flightSummary)
    }

    // ─── 4. Pruebas de la Tarjeta de Resultados Finales (ResultsHUD) ─────────────

    @Test
    fun `results HUD formats total flight time and range clearly`() {
        val hud = ResultsHUD(x = 30.0, y = 30.0)
        val summary = FlightSummary(totalFlightTime = 4.25, totalDistance = 120.80)

        val lines = hud.formatLines(summary)
        assertEquals(3, lines.size)
        assertEquals("RESULTADOS DEL VUELO", lines[0])
        assertTrue(lines[1].contains("4.25 s"), "Debe mostrar el tiempo total de caída formateado")
        assertTrue(lines[2].contains("120.80 m"), "Debe mostrar la distancia recorrida formateada")
    }

    // ─── 5. Pruebas de la Etiqueta Flotante Interactiva (TooltipRenderer) ─────────

    @Test
    fun `tooltip renderer formats instantaneous speed and height in real time`() {
        val tooltip = TooltipRenderer()
        val point = TrajectoryPoint(x = 40.0, y = 25.5, time = 1.35, height = 25.5, speed = 31.85)

        val lines = tooltip.formatLines(point)
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("31.85 m/s"), "Debe mostrar la velocidad instantánea")
        assertTrue(lines[1].contains("25.50 m"), "Debe mostrar la altura instantánea")
    }

    @Test
    fun `tooltip bounds stay clamped within canvas boundaries`() {
        val tooltip = TooltipRenderer(width = 150.0, height = 50.0, verticalOffset = 15.0)

        // Caso 1: Proyectil muy arriba (y = 5.0). El tooltip debe reubicarse debajo para no salirse de pantalla
        val boundsTop = tooltip.calculateTooltipBounds(
            targetCanvasX = 200.0,
            targetCanvasY = 5.0,
            canvasWidth = 900.0,
            canvasHeight = 550.0
        )
        assertTrue(boundsTop.y >= 10.0, "El tooltip no debe quedar cortado por el borde superior")
        assertTrue(boundsTop.y > 5.0, "Debe colocarse debajo del proyectil cuando está muy pegado arriba")

        // Caso 2: Proyectil en el extremo derecho (x = 895.0)
        val boundsRight = tooltip.calculateTooltipBounds(
            targetCanvasX = 895.0,
            targetCanvasY = 300.0,
            canvasWidth = 900.0,
            canvasHeight = 550.0
        )
        assertTrue(boundsRight.x + boundsRight.width <= 890.0, "El tooltip no debe salirse por la derecha")

        // Caso 3: Proyectil en el extremo izquierdo (x = 2.0)
        val boundsLeft = tooltip.calculateTooltipBounds(
            targetCanvasX = 2.0,
            targetCanvasY = 300.0,
            canvasWidth = 900.0,
            canvasHeight = 550.0
        )
        assertTrue(boundsLeft.x >= 10.0, "El tooltip no debe salirse por la izquierda")
    }

    // ─── 6. Pruebas de Congelación de la Trayectoria (PathRenderer Freeze) ────────

    @Test
    fun `path renderer freeze maintains trail opacity without fading during scrubbing`() {
        val pathRenderer = PathRenderer(gc = null, fadeDelaySeconds = 2.0, fadeDurationSeconds = 1.0, baseAlpha = 0.55)
        val launchNano = 10_000_000_000L
        pathRenderer.recordLaunch(launchNano)
        pathRenderer.addPoint(10.0, 10.0)
        pathRenderer.addPoint(50.0, 50.0)

        // Tras 5 segundos (bien pasado el tiempo de desvanecimiento)
        val laterNano = launchNano + 5_000_000_000L

        // Sin congelar, el progreso es 1.0 y la opacidad es 0.0
        assertEquals(1.0, pathRenderer.getFadeProgress(laterNano))
        assertEquals(0.0, pathRenderer.getOpacity(laterNano))

        // Al congelar (modo scrubbing tras impacto):
        pathRenderer.freeze()
        assertTrue(pathRenderer.isFrozen)
        assertEquals(0.0, pathRenderer.getFadeProgress(laterNano), "El progreso debe mantenerse en 0.0 mientras está congelado")
        assertEquals(0.55, pathRenderer.getOpacity(laterNano), "La opacidad debe mantenerse en el valor base (visible)")

        // Al limpiar para un nuevo disparo, se descongela automáticamente
        pathRenderer.clear()
        assertFalse(pathRenderer.isFrozen)
        assertEquals(0, pathRenderer.pointCount)
    }
}
