package org.example.scrubbing

import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import org.example.model.SimulationState
import org.example.rendering.CoordinateTransformer
import org.example.rendering.SceneConfig
import kotlin.math.hypot

/**
 * Estado inmutable de la interacción de scrubbing (SRP).
 */
data class ScrubbingState(
    val isEnabled: Boolean = false,
    val isDragging: Boolean = false,
    val isHovered: Boolean = false,
    val currentPoint: TrajectoryPoint? = null
)

/**
 * Controlador de eventos de mouse y arrastre (Scrubbing) sobre la trayectoria (SRP + DIP + OCP).
 *
 * Responsabilidad Única (SRP):
 *  - Gestionar exclusivamente el ciclo de interacción con el ratón: captura de eventos
 *    (press, drag, release, move), hit-testing y ajuste / enganche (*snapping*) exacto
 *    del proyectil sobre la curva matemática previa.
 *
 * Inversión de Dependencias (DIP):
 *  - Depende de las abstracciones [ITrajectoryInspector] y [CoordinateTransformer].
 *    No conoce ningún detalle interno de matrices numéricas ni algoritmos de física.
 *
 * Principio Abierto/Cerrado (OCP):
 *  - Ofrece callbacks desacoplados ([onPointSelected], [onRepaint]) para reaccionar a cambios
 *    sin modificar la lógica interna del controlador.
 *
 * @param state Estado mutable compartido de la simulación.
 * @param transformer Conversor de coordenadas metros <-> píxeles.
 * @param cfg Configuración geométrica del escenario.
 * @param onPointSelected Notificación emitida cuando el punto seleccionado cambia.
 * @param onRepaint Notificación para solicitar el redibujado de la escena.
 */
class ScrubbingController(
    private val state: SimulationState,
    private val transformer: CoordinateTransformer,
    private val cfg: SceneConfig,
    var onPointSelected: ((TrajectoryPoint) -> Unit)? = null,
    var onRepaint: (() -> Unit)? = null,
    val hitThresholdPixels: Double = 40.0,
    val projectileHitRadius: Double = 22.0
) {
    /** Inspector de trayectoria actualmente activo (suministrado tras el impacto). */
    var inspector: ITrajectoryInspector? = null
        private set

    /** Indica si el modo de interacción está habilitado (únicamente tras completar el vuelo). */
    var isEnabled: Boolean = false
        private set

    /** Indica si el usuario está actualmente arrastrando el proyectil a lo largo de la curva. */
    var isDragging: Boolean = false
        private set

    /** Indica si el mouse se encuentra flotando sobre o cerca de la trayectoria interactiva. */
    var isHovered: Boolean = false
        private set

    /** Punto cinemático actualmente seleccionado sobre la trayectoria. */
    var currentPoint: TrajectoryPoint? = null
        private set

    /** Resumen consolidado del vuelo actual, disponible cuando está habilitado. */
    val flightSummary: FlightSummary? get() = inspector?.getFlightSummary()

    /** Retorna una captura inmutable del estado actual de interacción. */
    fun getState(): ScrubbingState = ScrubbingState(
        isEnabled = isEnabled,
        isDragging = isDragging,
        isHovered = isHovered,
        currentPoint = currentPoint
    )

    /**
     * Habilita la interacción de arrastre sobre la trayectoria vinculando el inspector provisto.
     * Solo debe invocarse cuando el proyectil haya completado su vuelo e impactado el suelo.
     *
     * @param inspector Inspector de la trayectoria recién finalizada.
     */
    fun enable(inspector: ITrajectoryInspector) {
        this.inspector = inspector
        this.isEnabled = true
        this.isDragging = false
        this.isHovered = false

        // Posicionar inicialmente en el punto de impacto final o en la posición actual
        val initialPoint = inspector.findNearestPointFromCursor(state.projectileX, state.projectileY)
        snapToPoint(initialPoint)
        onRepaint?.invoke()
    }

    /**
     * Deshabilita el modo de interacción (por ejemplo al iniciar un nuevo disparo o resetear).
     */
    fun disable() {
        this.isEnabled = false
        this.isDragging = false
        this.isHovered = false
        this.inspector = null
        this.currentPoint = null
        onRepaint?.invoke()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Procesamiento de Eventos de Mouse (Agnóstico a JavaFX para Pruebas Unitarias)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Procesa la pulsación del mouse en las coordenadas del canvas dadas.
     * Si la pelota está por delante del objetivo, tiene máxima prioridad de selección.
     *
     * @param canvasX Coordenada X del cursor en el canvas.
     * @param canvasY Coordenada Y del cursor en el canvas.
     * @return true si el evento fue consumido por el mecanismo de scrubbing.
     */
    fun handleMousePressed(canvasX: Double, canvasY: Double): Boolean {
        if (!isEnabled) return false
        val currentInspector = inspector ?: return false

        val distToProjectile = hypot(canvasX - state.projectileX, canvasY - state.projectileY)

        // 1. PRIORIDAD: Si el clic es sobre la pelota (que está por delante del objetivo), seleccionar la pelota
        if (distToProjectile <= projectileHitRadius) {
            isDragging = true
            val nearest = currentInspector.findNearestPointFromCursor(canvasX, canvasY)
            snapToPoint(nearest)
            onRepaint?.invoke()
            return true
        }

        // 2. Si el usuario hace clic sobre la diana pero fuera del área de la pelota, permitir arrastrar la diana
        if (isOverTarget(canvasX, canvasY)) {
            return false
        }

        // 3. Hit-test sobre la curva de la trayectoria previa
        val nearest = currentInspector.findNearestPointFromCursor(canvasX, canvasY)
        val nearestCanvasX = transformer.toCanvasX(nearest.x)
        val nearestCanvasY = transformer.toCanvasY(nearest.y)
        val distToCurve = hypot(canvasX - nearestCanvasX, canvasY - nearestCanvasY)

        if (distToCurve <= hitThresholdPixels) {
            isDragging = true
            snapToPoint(nearest)
            onRepaint?.invoke()
            return true
        }

        return false
    }

    /**
     * Procesa el arrastre del mouse. Ajusta y "engancha" la posición del proyectil
     * exactamente a la curva de la trayectoria previa.
     *
     * @param canvasX Coordenada X del cursor en el canvas.
     * @param canvasY Coordenada Y del cursor en el canvas.
     * @return true si se procesó el arrastre.
     */
    fun handleMouseDragged(canvasX: Double, canvasY: Double): Boolean {
        if (!isEnabled || !isDragging) return false
        val currentInspector = inspector ?: return false

        // Ajustar y enganchar exactamente a la curva matemática
        val nearest = currentInspector.findNearestPointFromCursor(canvasX, canvasY)
        snapToPoint(nearest)
        onRepaint?.invoke()
        return true
    }

    /**
     * Procesa la liberación del botón del mouse finalizando el arrastre.
     *
     * @return true si se estaba arrastrando.
     */
    fun handleMouseReleased(): Boolean {
        if (isDragging) {
            isDragging = false
            onRepaint?.invoke()
            return true
        }
        return false
    }

    /**
     * Procesa el movimiento del mouse sin presionar para detectar si se está sobrevolando
     * la trayectoria interactiva (hover).
     *
     * @param canvasX Coordenada X del cursor.
     * @param canvasY Coordenada Y del cursor.
     * @return true si el cursor está sobre la zona interactiva.
     */
    fun handleMouseMoved(canvasX: Double, canvasY: Double): Boolean {
        if (!isEnabled || isDragging) return false
        val currentInspector = inspector ?: return false

        val distToProjectile = hypot(canvasX - state.projectileX, canvasY - state.projectileY)

        // Si el cursor está sobre la pelota (que está al frente), activar el hover de la pelota
        if (distToProjectile <= projectileHitRadius) {
            if (!isHovered) {
                isHovered = true
                onRepaint?.invoke()
            }
            return true
        }

        // Si está sobre la diana pero fuera de la pelota, desactivar hover de la pelota para que la diana responda
        if (isOverTarget(canvasX, canvasY)) {
            if (isHovered) {
                isHovered = false
                onRepaint?.invoke()
            }
            return false
        }

        val nearest = currentInspector.findNearestPointFromCursor(canvasX, canvasY)
        val nearestCanvasX = transformer.toCanvasX(nearest.x)
        val nearestCanvasY = transformer.toCanvasY(nearest.y)
        val distToCurve = hypot(canvasX - nearestCanvasX, canvasY - nearestCanvasY)

        val newHovered = distToCurve <= hitThresholdPixels
        if (newHovered != isHovered) {
            isHovered = newHovered
            onRepaint?.invoke()
        }
        return isHovered
    }

    /**
     * Engancha el proyectil al punto de la trayectoria provisto y actualiza el estado.
     */
    private fun snapToPoint(point: TrajectoryPoint) {
        currentPoint = point
        state.projectileX = transformer.toCanvasX(point.x)
        state.projectileY = transformer.toCanvasY(point.y)
        onPointSelected?.invoke(point)
    }

    /**
     * Verifica si una coordenada del mouse se encuentra sobre la diana del suelo.
     */
    private fun isOverTarget(canvasX: Double, canvasY: Double): Boolean {
        val dx = canvasX - state.targetX
        val dy = canvasY - cfg.groundY
        return hypot(dx, dy) <= state.targetRadius + 6.0
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Integración con JavaFX Canvas
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Instala los controladores de eventos de mouse en el canvas mediante `addEventHandler`,
     * permitiendo coexistir pacíficamente con otros listeners como [TargetRenderer].
     */
    fun install(canvas: Canvas) {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED) { event ->
            val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
            if (handleMousePressed(local.x, local.y)) {
                canvas.cursor = Cursor.CLOSED_HAND
                event.consume()
            }
        }

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED) { event ->
            if (isDragging) {
                val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
                if (handleMouseDragged(local.x, local.y)) {
                    canvas.cursor = Cursor.CLOSED_HAND
                    event.consume()
                }
            }
        }

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED) { event ->
            if (handleMouseReleased()) {
                canvas.cursor = if (isHovered) Cursor.HAND else Cursor.DEFAULT
                event.consume()
            }
        }

        canvas.addEventHandler(MouseEvent.MOUSE_MOVED) { event ->
            if (isEnabled && !isDragging) {
                val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
                val hovering = handleMouseMoved(local.x, local.y)
                if (hovering) {
                    canvas.cursor = Cursor.HAND
                } else if (!isOverTarget(local.x, local.y)) {
                    canvas.cursor = Cursor.DEFAULT
                }
            }
        }
    }
}
