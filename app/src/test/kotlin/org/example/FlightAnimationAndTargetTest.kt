package org.example

import org.example.animation.AnimationEngine
import org.example.animation.CollisionListener
import org.example.animation.ManualFrameTicker
import org.example.collision.CollisionResult
import org.example.collision.CollisionStatus
import org.example.model.SimulationState
import org.example.model.Target
import org.example.rendering.CoordinateTransformer
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.PathRenderer
import org.example.rendering.SceneConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pruebas unitarias para el Issue 2.2: Renderizado de Vuelo y Objetivo Desplazable.
 *
 * Valida los 4 requisitos:
 *  1. Animación de Vuelo fluida recorriendo la trayectoria de Dev 1.
 *  2. Rastro Continuo: acumulación de puntos durante el avance y al finalizar.
 *  3. Objetivo Arrastrable: transformación de coordenadas y límites en eje X.
 *  4. Notificación de Colisión: invocación del validador de Dev 1 exactamente al tocar el suelo.
 */
class FlightAnimationAndTargetTest {

    private val cfg = SceneConfig(canvasWidth = 900.0, canvasHeight = 550.0)
    private val transformer: CoordinateTransformer = DefaultCoordinateTransformer(cfg, pixelsPerMeter = 2.5)

    // ─── 1. Pruebas de Transformación de Coordenadas (Metros <-> Canvas) ──────

    @Test
    fun `transformer accurately converts origin between physics and canvas`() {
        // En física (0, 0) debe coincidir con (cannonBaseX, cannonBaseY) en el canvas
        assertEquals(cfg.cannonBaseX, transformer.toCanvasX(0.0))
        assertEquals(cfg.cannonBaseY, transformer.toCanvasY(0.0))

        assertEquals(0.0, transformer.toPhysicsX(cfg.cannonBaseX))
        assertEquals(0.0, transformer.toPhysicsY(cfg.cannonBaseY))
    }

    @Test
    fun `transformer roundtrip maintains coordinate integrity`() {
        val physicsX = 150.0
        val physicsY = 45.0

        val canvasX = transformer.toCanvasX(physicsX)
        val canvasY = transformer.toCanvasY(physicsY)

        assertEquals(physicsX, transformer.toPhysicsX(canvasX), 1e-9)
        assertEquals(physicsY, transformer.toPhysicsY(canvasY), 1e-9)
    }

    @Test
    fun `transformer distance conversion respects scale factor`() {
        val meters = 20.0
        val pixels = transformer.toCanvasDistance(meters)
        assertEquals(50.0, pixels) // 20m * 2.5 px/m = 50px
        assertEquals(meters, transformer.toPhysicsDistance(pixels))
    }

    // ─── 2. Pruebas de Animación de Vuelo y Rastro Continuo ────────────────────

    @Test
    fun `animation engine begins at launch point and starts ticker`() {
        val state = SimulationState().apply {
            angle = 45.0
            velocity = 40.0
            gravity = 9.8
        }

        val ticker = ManualFrameTicker()
        val trailPoints = mutableListOf<Pair<Double, Double>>()
        var updateCalled = false

        val engine = AnimationEngine(
            state = state,
            cfg = cfg,
            transformer = transformer,
            onUpdate = { updateCalled = true },
            onFinished = {},
            onTick = { x, y -> trailPoints.add(Pair(x, y)) },
            frameTicker = ticker
        )

        engine.start()

        assertTrue(state.isAnimating, "La simulación debe marcarse en animación")
        assertTrue(ticker.isRunning, "El ticker de fotogramas debe estar corriendo")
        assertTrue(updateCalled, "Debe invocarse onUpdate al iniciar")

        // El primer punto del rastro debe ser la boca del cañón
        assertEquals(1, trailPoints.size)
        assertEquals(cfg.cannonBaseX, state.projectileX, 1e-6)
        assertEquals(cfg.cannonBaseY, state.projectileY, 1e-6)
    }

    @Test
    fun `animation interpolates smoothly along trajectory time`() {
        val state = SimulationState().apply {
            angle = 45.0
            velocity = 50.0
            gravity = 9.8
        }

        val ticker = ManualFrameTicker()
        val trailPoints = mutableListOf<Pair<Double, Double>>()

        val engine = AnimationEngine(
            state = state,
            cfg = cfg,
            transformer = transformer,
            onUpdate = {},
            onFinished = {},
            onTick = { x, y -> trailPoints.add(Pair(x, y)) },
            frameTicker = ticker
        )

        engine.start()

        val matrix = engine.currentMatrix!!
        val halfTime = matrix.timeOfFlight / 2.0
        val halfNano = (halfTime * 1_000_000_000).toLong()

        // Fotograma 1: t = 0
        ticker.advance(1_000_000_000L)
        // Fotograma 2: t = halfTime (ápice de la parábola)
        ticker.advance(1_000_000_000L + halfNano)

        // El proyectil debe haberse desplazado horizontalmente y ganado altura
        assertTrue(state.projectileX > cfg.cannonBaseX, "El proyectil debe avanzar en X")
        assertTrue(state.projectileY < cfg.cannonBaseY, "El proyectil debe elevarse (menor Y en canvas)")
        assertTrue(trailPoints.size >= 3, "Deben haberse acumulado puntos continuos en la estela")
    }

    // ─── 3. Pruebas de Notificación de Colisión al Impactar el Suelo ──────────

    @Test
    fun `ground impact invokes collision detector with HIT when target is placed at impact`() {
        val state = SimulationState().apply {
            angle = 45.0
            velocity = 40.0
            gravity = 9.8
        }

        val ticker = ManualFrameTicker()
        var reportedResult: CollisionResult? = null
        var finishedCalled = false

        val engine = AnimationEngine(
            state = state,
            cfg = cfg,
            transformer = transformer,
            onUpdate = {},
            onFinished = { finishedCalled = true },
            onTick = { _, _ -> },
            collisionListener = CollisionListener { result ->
                reportedResult = result
            },
            frameTicker = ticker
        )

        // Precalcular la matriz para conocer el punto de impacto exacto
        val matrix = TrajectorySimulationFactory.generateMatrix(
            PhysicsParams(state.angle, state.velocity, state.gravity, state.mass)
        )
        // Ubicar el objetivo en canvas exactamente donde impacta el proyectil
        state.targetX = transformer.toCanvasX(matrix.range)
        state.targetRadius = 20.0

        engine.start(matrix)

        // Simular que el tiempo transcurre hasta alcanzar el tiempo de vuelo
        val flightNano = (matrix.timeOfFlight * 1_000_000_000).toLong()
        ticker.advance(100_000_000L) // t = 0
        ticker.advance(100_000_000L + flightNano + 10_000_000L) // t >= timeOfFlight (Impacto)

        // Verificaciones
        assertFalse(state.isAnimating, "La animación debe detenerse tras el impacto")
        assertTrue(finishedCalled, "Debe invocarse onFinished tras el impacto")
        assertNotNull(reportedResult, "El CollisionListener debe haber recibido el resultado")
        assertEquals(CollisionStatus.HIT, reportedResult.status, "Debe ser un impacto positivo (HIT)")
        assertEquals(CollisionStatus.HIT, state.lastCollisionResult?.status)

        // El proyectil debe reposar en el suelo
        assertEquals(cfg.cannonBaseY, state.projectileY, 1e-4)
        assertEquals(transformer.toCanvasX(matrix.range), state.projectileX, 1.0)
    }

    @Test
    fun `ground impact invokes collision detector with MISS when target is far away`() {
        val state = SimulationState().apply {
            angle = 45.0
            velocity = 30.0
            gravity = 9.8
        }

        val ticker = ManualFrameTicker()
        var reportedResult: CollisionResult? = null

        val engine = AnimationEngine(
            state = state,
            cfg = cfg,
            transformer = transformer,
            onUpdate = {},
            onFinished = {},
            onTick = { _, _ -> },
            collisionListener = { result -> reportedResult = result },
            frameTicker = ticker
        )

        val matrix = TrajectorySimulationFactory.generateMatrix(
            PhysicsParams(state.angle, state.velocity, state.gravity, state.mass)
        )
        // Colocar el objetivo muy lejos del alcance real
        state.targetX = transformer.toCanvasX(matrix.range + 100.0)

        engine.start(matrix)

        val flightNano = (matrix.timeOfFlight * 1_000_000_000).toLong()
        ticker.advance(100_000_000L)
        ticker.advance(100_000_000L + flightNano + 10_000_000L) // Impacto

        assertNotNull(reportedResult, "Debe emitirse la notificación de colisión")
        assertEquals(CollisionStatus.MISS, reportedResult.status, "Debe registrarse como tiro fallido (MISS)")
        assertTrue(reportedResult.message.contains("fuera del objetivo"))
    }

    @Test
    fun `trail points are preserved after flight stops`() {
        val state = SimulationState().apply {
            angle = 45.0
            velocity = 30.0
            gravity = 9.8
        }

        val ticker = ManualFrameTicker()
        val trailPoints = mutableListOf<Pair<Double, Double>>()

        val engine = AnimationEngine(
            state = state,
            cfg = cfg,
            transformer = transformer,
            onUpdate = {},
            onFinished = {},
            onTick = { x, y -> trailPoints.add(Pair(x, y)) },
            frameTicker = ticker
        )

        engine.start()

        val matrix = engine.currentMatrix!!
        val flightNano = (matrix.timeOfFlight * 1_000_000_000).toLong()
        ticker.advance(100_000_000L)
        ticker.advance(100_000_000L + flightNano)

        // Tras detenerse la animación, los puntos del rastro no deben borrarse
        assertTrue(trailPoints.size >= 2, "La estela continua debe conservar todos los puntos del vuelo")
        val lastPoint = trailPoints.last()
        assertEquals(cfg.cannonBaseY, lastPoint.second, 1e-4, "El último punto del rastro debe estar en el suelo")
    }

    // ─── 4. Pruebas de Velocidad Acelerada (timeScale) y Reinicio al Salir ────

    @Test
    fun `timeScale accelerates simulation playback`() {
        val stateNormal = SimulationState().apply { angle = 45.0; velocity = 50.0; gravity = 9.8 }
        val stateFast = SimulationState().apply { angle = 45.0; velocity = 50.0; gravity = 9.8 }

        val tickerNormal = ManualFrameTicker()
        val tickerFast = ManualFrameTicker()

        val engineNormal = AnimationEngine(
            state = stateNormal, cfg = cfg, transformer = transformer,
            onUpdate = {}, onFinished = {}, onTick = { _, _ -> },
            timeScale = 1.0, frameTicker = tickerNormal
        )

        val engineFast = AnimationEngine(
            state = stateFast, cfg = cfg, transformer = transformer,
            onUpdate = {}, onFinished = {}, onTick = { _, _ -> },
            timeScale = 2.0, frameTicker = tickerFast
        )

        engineNormal.start()
        engineFast.start()

        // Avanzar el mismo segundo en tiempo real
        tickerNormal.advance(1_000_000_000L)
        tickerNormal.advance(2_000_000_000L) // 1 seg transcurrido

        tickerFast.advance(1_000_000_000L)
        tickerFast.advance(2_000_000_000L) // 1 seg real = 2 seg simulación

        // El motor acelerado debe haber recorrido más distancia en el mismo tiempo real
        assertTrue(
            stateFast.projectileX > stateNormal.projectileX,
            "El proyectil con timeScale acelerado debe haber avanzado más distancia horizontal"
        )
    }

    @Test
    fun `projectile leaving canvas square automatically resets simulation and restores cannon position`() {
        val state = SimulationState().apply {
            angle = 20.0
            velocity = 90.0 // Gran velocidad horizontal que saldrá por la derecha del canvas (x > 900)
            gravity = 9.8
        }

        val ticker = ManualFrameTicker()
        var outOfBoundsTriggered = false

        val engine = AnimationEngine(
            state = state,
            cfg = cfg,
            transformer = transformer,
            onUpdate = {},
            onFinished = {},
            onTick = { _, _ -> },
            onOutOfBounds = { outOfBoundsTriggered = true },
            timeScale = 1.0,
            frameTicker = ticker
        )

        engine.start()

        // Simular avance hasta que salga por la derecha del canvas (> 900 px)
        ticker.advance(1_000_000_000L)
        ticker.advance(3_000_000_000L) // 2 segundos a 90 m/s * 2.5 px/m = ~450 px + inicio...
        ticker.advance(6_000_000_000L) // Sale del canvas (> 900 px)

        // Verificaciones de reinicio
        assertTrue(outOfBoundsTriggered, "Debe haberse invocado el callback onOutOfBounds al salir del cuadrado")
        assertFalse(state.isAnimating, "La animación debe haberse detenido")
        assertEquals(cfg.cannonBaseX, state.projectileX, 1e-4, "La pelota debe haberse reiniciado a la base del cañón")
        assertEquals(cfg.cannonBaseY, state.projectileY, 1e-4, "La pelota debe haberse reiniciado al nivel del suelo")
    }

    @Test
    fun `manual reset restores state and triggers callback`() {
        val state = SimulationState().apply {
            projectileX = 500.0
            projectileY = 200.0
            isAnimating = true
        }

        var callbackCalled = false
        val engine = AnimationEngine(
            state = state, cfg = cfg, transformer = transformer,
            onUpdate = {}, onFinished = {},
            onOutOfBounds = { callbackCalled = true }
        )

        engine.reset()

        assertFalse(state.isAnimating)
        assertEquals(cfg.cannonBaseX, state.projectileX)
        assertEquals(cfg.cannonBaseY, state.projectileY)
        assertTrue(callbackCalled)
    }

    // ─── 5. Pruebas de Desvanecimiento de la Línea (Fade-out tras 2s) ─────────

    @Test
    fun `path renderer preserves full opacity during first 2 seconds`() {
        val pathRenderer = PathRenderer(gc = null, fadeDelaySeconds = 2.0, fadeDurationSeconds = 1.5, baseAlpha = 0.50)
        val launchNano = 10_000_000_000L
        pathRenderer.recordLaunch(launchNano)

        // En t = 1.0s (1 segundo después de disparar): opacidad intacta
        val opacityAt1s = pathRenderer.getOpacity(launchNano + 1_000_000_000L)
        assertEquals(0.50, opacityAt1s, 1e-4, "La opacidad debe ser máxima durante el primer segundo")

        // En t = 2.0s exactos (2 segundos después de disparar): opacidad aún intacta
        val opacityAt2s = pathRenderer.getOpacity(launchNano + 2_000_000_000L)
        assertEquals(0.50, opacityAt2s, 1e-4, "A los 2 segundos exactos no debe haber comenzado la pérdida de opacidad")
    }

    @Test
    fun `path renderer fades progressively after 2 seconds and clears on expiration`() {
        val pathRenderer = PathRenderer(gc = null, fadeDelaySeconds = 2.0, fadeDurationSeconds = 1.0, baseAlpha = 0.50)
        val launchNano = 10_000_000_000L
        pathRenderer.recordLaunch(launchNano)
        pathRenderer.addPoint(10.0, 10.0)
        pathRenderer.addPoint(20.0, 20.0)

        // En t = 2.5s (0.5s en fade de 1.0s total -> 50% progreso del desvanecimiento):
        val opacityAt2Half = pathRenderer.getOpacity(launchNano + 2_500_000_000L)
        assertEquals(0.25, opacityAt2Half, 1e-4, "A los 2.5 segundos debe estar a la mitad de su opacidad")

        // En t = 3.0s (2s espera + 1s duración -> desvanecido total):
        val opacityAt3s = pathRenderer.getOpacity(launchNano + 3_000_000_000L)
        assertEquals(0.0, opacityAt3s, 1e-4, "A los 3.0 segundos debe haberse desvanecido por completo (opacidad 0)")

        // Al dibujar pasado el tiempo de expiración, limpia los puntos automáticamente
        pathRenderer.drawPath(launchNano + 3_100_000_000L)
        assertEquals(0, pathRenderer.pointCount)
    }

    @Test
    fun `path renderer fades progressively from the starting point towards the head`() {
        val pathRenderer = PathRenderer(gc = null, fadeDelaySeconds = 2.0, fadeDurationSeconds = 1.0, baseAlpha = 0.50)
        val launchNano = 10_000_000_000L
        pathRenderer.recordLaunch(launchNano)

        // Simular 5 puntos a lo largo de la trayectoria (origen en índice 0)
        pathRenderer.addPoint(0.0, 0.0)
        pathRenderer.addPoint(25.0, 10.0)
        pathRenderer.addPoint(50.0, 20.0)
        pathRenderer.addPoint(75.0, 15.0)
        pathRenderer.addPoint(100.0, 0.0)

        val points = pathRenderer.getTrailPoints()
        assertEquals(5, points.size)

        // A los 2.4s (40% de desvanecimiento transcurrido):
        val timeAtFade = launchNano + 2_400_000_000L
        val progress = pathRenderer.getFadeProgress(timeAtFade)
        assertEquals(0.40, progress, 1e-4)

        // El punto de partida (índice 0) ya debe haberse disuelto por completo
        val alphaOrigin = pathRenderer.getPointAlpha(points[0], timeAtFade)
        assertEquals(0.0, alphaOrigin, 1e-4, "El punto de origen debe disolverse primero")

        // El punto en la cabeza (índice 4) aún conserva opacidad visible
        val alphaHead = pathRenderer.getPointAlpha(points[4], timeAtFade)
        assertTrue(alphaHead > 0.0, "La cabeza de la estela aún debe ser visible")
        assertTrue(alphaHead > alphaOrigin, "El punto de partida debe tener menor opacidad que la cabeza de la trayectoria")
    }

    @Test
    fun `animation engine defaults to 240 samples per second for high fluid fps`() {
        val state = SimulationState().apply {
            angle = 45.0
            velocity = 20.0
            gravity = 9.8
        }
        val engine = AnimationEngine(
            state = state,
            cfg = cfg,
            transformer = transformer,
            onUpdate = {},
            onFinished = {}
        )
        assertEquals(240, engine.samplesPerSecond, "Debe muestrear a 240 muestras por segundo para máxima fluidez")
    }
}

