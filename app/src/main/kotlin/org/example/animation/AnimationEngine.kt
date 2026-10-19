package org.example.animation

import org.example.PhysicsParams
import org.example.TrajectorySimulationFactory
import org.example.collision.CollisionDetector
import org.example.model.SimulationState
import org.example.model.Target
import org.example.model.TrajectoryMatrix
import org.example.rendering.CoordinateTransformer
import org.example.rendering.DefaultCoordinateTransformer
import org.example.rendering.SceneConfig
import org.example.trajectory.TrajectoryMatrixGenerator

/**
 * Motor de animación de vuelo (SRP, DIP, OCP).
 *
 * Responsabilidad Única:
 *  - Controlar el reloj temporal del vuelo del proyectil.
 *  - Recorrer de forma fluida la trayectoria entregada por el módulo de Dev 1 ([TrajectoryMatrix]).
 *  - Actualizar las coordenadas de visualización en el canvas a través de [CoordinateTransformer].
 *  - Invocar el validador de colisión del Issue 2.1 ([CollisionDetector]) exactamente al impactar el suelo.
 *
 * Inversión de Dependencias (DIP):
 *  - Depende de abstracciones ([TrajectoryMatrixGenerator], [CollisionDetector],
 *    [CoordinateTransformer], [FrameTicker], [CollisionListener]).
 *
 * Issue 2.2 Requisitos:
 *  1. Animación de Vuelo fluida recorriendo la matriz de trayectoria.
 *  2. Rastro Continuo: notifica cada punto mediante [onTick].
 *  4. Notificación de Colisión: evalúa colisión exactamente al impactar el suelo (t >= timeOfFlight).
 */
class AnimationEngine(
    private val state: SimulationState,
    private val cfg: SceneConfig,
    private val transformer: CoordinateTransformer = DefaultCoordinateTransformer(cfg),
    private val trajectoryGenerator: TrajectoryMatrixGenerator = TrajectorySimulationFactory.createGenerator(),
    private val collisionDetector: CollisionDetector = TrajectorySimulationFactory.createCollisionDetector(),
    private val onUpdate: () -> Unit,
    private val onFinished: () -> Unit,
    private val onTick: (Double, Double) -> Unit = { _, _ -> },
    private val collisionListener: CollisionListener? = null,
    private val onOutOfBounds: (() -> Unit)? = null,
    private val onFlightCompleted: ((TrajectoryMatrix) -> Unit)? = null,
    var timeScale: Double = 1.75,
    private val frameTicker: FrameTicker = JavaFxFrameTicker(),
    val samplesPerSecond: Int = 240,
) {

    // Matriz de trayectoria actual calculada por Dev 1
    var currentMatrix: TrajectoryMatrix? = null
        private set

    /**
     * Inicia la animación de vuelo.
     * Si no se suministra una matriz precalculada, se genera una nueva
     * a partir de los parámetros físicos actuales del estado.
     */
    fun start(matrix: TrajectoryMatrix? = null) {
        val flightMatrix = matrix ?: run {
            val params = PhysicsParams(
                angleDeg = state.angle,
                initialVelocity = state.velocity,
                gravity = state.gravity,
                mass = state.mass
            )
            trajectoryGenerator.generate(params, samplesPerSecond = samplesPerSecond)
        }

        currentMatrix = flightMatrix
        state.isAnimating = true
        state.animationStartTime = 0L

        // Posicionar proyectil en el origen del lanzamiento (coordenadas de canvas)
        val launch = flightMatrix.launchPoint
        state.projectileX = transformer.toCanvasX(launch.x)
        state.projectileY = transformer.toCanvasY(launch.y)

        // Registrar el primer punto del rastro continuo
        onTick(state.projectileX, state.projectileY)

        // Iniciar el bucle de fotogramas
        frameTicker.start { now -> tick(now) }
        onUpdate()
    }

    /**
     * Detiene la animación y notifica a los observadores.
     */
    fun stop() {
        if (!state.isAnimating) return
        state.isAnimating = false
        frameTicker.stop()
        onFinished()
    }

    /**
     * Reinicia el juego colocando la pelota en su posición inicial en la base del cañón.
     */
    fun reset() {
        stop()
        state.projectileX = cfg.cannonBaseX
        state.projectileY = cfg.cannonBaseY
        onOutOfBounds?.invoke()
        onUpdate()
    }

    /**
     * Determina si la pelota salió de los límites del cuadrado (área del canvas).
     */
    fun isOutOfBounds(canvasX: Double, canvasY: Double): Boolean {
        val margin = 15.0
        return canvasX < -margin ||
               canvasX > cfg.canvasWidth + margin ||
               canvasY < -margin ||
               canvasY > cfg.canvasHeight + margin
    }

    /**
     * Procesa un fotograma temporal de la simulación.
     * Método público para permitir pruebas unitarias deterministas.
     *
     * @param now Marca de tiempo actual en nanosegundos.
     */
    fun tick(now: Long) {
        if (!state.isAnimating) return
        val matrix = currentMatrix ?: return

        if (state.animationStartTime == 0L) {
            state.animationStartTime = now
        }

        // Progreso temporal escalado para mayor fluidez y velocidad
        val elapsed = ((now - state.animationStartTime) / 1_000_000_000.0) * timeScale

        // ─── Impacto en el suelo ──────────────────────────────────────────
        // Si el tiempo transcurrido alcanza o supera el tiempo total de vuelo,
        // el proyectil impacta exactamente en el suelo.
        if (elapsed >= matrix.timeOfFlight) {
            handleGroundImpact(matrix)
            return
        }

        // ─── Vuelo en curso: interpolación fluida ─────────────────────────
        val (currentPhysicsX, currentPhysicsY) = interpolatePosition(matrix, elapsed)

        // Transformación matemática de metros a píxeles de pantalla
        val canvasX = transformer.toCanvasX(currentPhysicsX)
        val canvasY = transformer.toCanvasY(currentPhysicsY)

        // Si la pelota se sale del cuadrado del canvas, se reinicia el juego automáticamente
        if (isOutOfBounds(canvasX, canvasY)) {
            reset()
            return
        }

        state.projectileX = canvasX
        state.projectileY = canvasY

        // Notificar al acumulador de la estela continua
        onTick(canvasX, canvasY)

        onUpdate()
    }

    /**
     * Maneja el impacto en el suelo exactamente al finalizar el tiempo de vuelo.
     * Invoca el validador de colisiones del Issue 2.1 y emite la notificación.
     */
    private fun handleGroundImpact(matrix: TrajectoryMatrix) {
        // 1. Posicionar proyectil exactamente en el punto de impacto final
        val impactPoint = matrix.impactPoint
        val canvasX = transformer.toCanvasX(impactPoint.x)
        val canvasY = transformer.toCanvasY(impactPoint.y)

        state.projectileX = canvasX
        state.projectileY = canvasY

        // 2. Registrar el punto final en el rastro continuo
        onTick(canvasX, canvasY)

        // 3. Construir el Target en coordenadas de física (metros)
        val targetPhysicsX = transformer.toPhysicsX(state.targetX)
        val targetPhysicsRadius = transformer.toPhysicsDistance(state.targetRadius)
        val target = Target(centerX = targetPhysicsX, radius = targetPhysicsRadius)

        // 4. Invocar el validador de colisión del Issue 2.1
        val result = collisionDetector.checkCollision(matrix, target)
        state.lastCollisionResult = result

        // 5. Notificar la colisión al listener desacoplado
        collisionListener?.onCollision(result)

        // 6. Notificar finalización de vuelo para habilitar la interacción de scrubbing (Issue 3.2)
        onFlightCompleted?.invoke(matrix)

        // 7. Finalizar la animación y refrescar el fotograma final
        stop()
        onUpdate()
    }

    /**
     * Interpola suavemente las coordenadas (x, y) de la trayectoria de Dev 1
     * en el instante temporal [elapsed].
     */
    fun interpolatePosition(matrix: TrajectoryMatrix, elapsed: Double): Pair<Double, Double> {
        val points = matrix.points
        if (points.size <= 1) return Pair(points[0].x, points[0].y)

        // Búsqueda binaria O(log n) para ubicar el intervalo temporal
        var low = 0
        var high = points.size - 1
        while (low <= high) {
            val mid = (low + high) / 2
            if (points[mid].t <= elapsed) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        val idx = (low - 1).coerceIn(0, points.size - 2)
        val p1 = points[idx]
        val p2 = points[idx + 1]

        val ratio = if (p2.t > p1.t) {
            ((elapsed - p1.t) / (p2.t - p1.t)).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        val x = p1.x + ratio * (p2.x - p1.x)
        val y = p1.y + ratio * (p2.y - p1.y)
        return Pair(x, y)
    }
}
