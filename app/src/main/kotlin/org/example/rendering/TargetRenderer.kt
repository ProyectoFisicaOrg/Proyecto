package org.example.rendering

import javafx.animation.AnimationTimer
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import org.example.collision.CollisionStatus
import org.example.model.SimulationState

/**
 * Renderizador del objetivo en el suelo y gestor de arrastre sobre el eje X (SRP + DIP).
 *
 * Responsabilidad Única:
 *  - Dibujar el objetivo circular (diana) sobre la línea del suelo (eje Y fijo en groundY).
 *  - Gestionar el arrastre con el ratón limitando el desplazamiento estrictamente al eje X.
 *
 * Inversión de Dependencias (DIP):
 *  - Depende de abstracciones ([GraphicsContext], [SceneConfig], [Canvas]) y notifica
 *    cambios a través de callbacks sin acoplarse al motor de física.
 *
 * Issue 2.2 Requisito 3:
 *  - Dibujar un objetivo (círculo) en el suelo.
 *  - Permitir arrastrarlo con el mouse únicamente sobre el eje X.
 */
class TargetRenderer(
    private val gc: GraphicsContext,
    private val cfg: SceneConfig,
    private val canvas: Canvas,
    private val state: SimulationState,
    private val onTargetPositionChanged: (Double) -> Unit,
    private val onRepaint: () -> Unit,
) {
    // Radio del objetivo en píxeles de canvas
    val targetRadius: Double = 20.0

    // Radio de detección para seleccionar el objetivo con el mouse
    private val hitRadius: Double = 26.0

    // Radio de detección del proyectil (que está por delante)
    val projectileHitRadius: Double = 22.0

    // Estado interno del gesto de arrastre
    private var isDragging: Boolean = false
    private var dragOffsetX: Double = 0.0

    // Bucle para animación suave del pulso visual
    private var pulseTimer: AnimationTimer? = null

    init {
        // Asegurar que el estado refleje el radio del objetivo
        state.targetRadius = targetRadius
    }

    /**
     * Dibuja el objetivo circular en la posición X dada (siempre sobre la línea del suelo).
     */
    fun drawTarget(x: Double) {
        val cx = x
        val cy = cfg.groundY

        // Factor de pulso sutil cuando está en reposo
        val time = System.nanoTime() / 1_000_000_000.0
        val pulseFactor = if (state.isAnimating) 1.0 else 1.0 + 0.06 * Math.sin(time * 3.0)
        val r = targetRadius * pulseFactor

        // Comprobar si hubo un impacto exitoso reciente para retroalimentación visual
        val lastCollision = state.lastCollisionResult
        val isHit = lastCollision?.status == CollisionStatus.HIT

        // 1. Sombra bajo el objetivo sobre el suelo
        gc.fill = Color.rgb(0, 0, 0, 0.22)
        gc.fillOval(cx - r * 1.1, cy - r * 0.4 + 4.0, r * 2.2, r * 0.8)

        // 2. Anillo exterior (Verde si acertó, rojo estándar)
        val outerColor = when {
            isHit -> Color.rgb(46, 204, 113)      // Verde esmeralda si hubo impacto
            isDragging -> Color.rgb(231, 76, 60) // Rojo brillante mientras se arrastra
            else -> Color.rgb(217, 30, 24)        // Rojo clásico
        }
        gc.fill = outerColor
        gc.fillOval(cx - r, cy - r, r * 2.0, r * 2.0)

        // 3. Anillo intermedio blanco
        gc.fill = Color.rgb(255, 255, 255)
        val midR = r * 0.65
        gc.fillOval(cx - midR, cy - midR, midR * 2.0, midR * 2.0)

        // 4. Centro del objetivo (diana)
        gc.fill = if (isHit) Color.rgb(39, 174, 96) else Color.rgb(192, 57, 43)
        val centerR = r * 0.35
        gc.fillOval(cx - centerR, cy - centerR, centerR * 2.0, centerR * 2.0)

        // 5. Cruz / Mira central
        gc.stroke = if (isHit) Color.WHITE else Color.rgb(240, 240, 240, 0.9)
        gc.lineWidth = 1.5
        gc.strokeLine(cx - centerR * 1.2, cy, cx + centerR * 1.2, cy)
        gc.strokeLine(cx, cy - centerR * 1.2, cx, cy + centerR * 1.2)

        // 6. Halo visual mientras el usuario mantiene presionado y arrastra
        if (isDragging) {
            gc.stroke = Color.rgb(255, 230, 0, 0.8)
            gc.lineWidth = 2.0
            gc.strokeOval(cx - r - 4.0, cy - r - 4.0, (r + 4.0) * 2.0, (r + 4.0) * 2.0)
        }
    }

    /**
     * Inicia el pulso visual del objetivo cuando no hay proyectil en vuelo.
     */
    fun startPulse() {
        if (pulseTimer == null) {
            pulseTimer = object : AnimationTimer() {
                override fun handle(now: Long) {
                    if (!state.isAnimating) {
                        onRepaint()
                    }
                }
            }
        }
        pulseTimer?.start()
    }

    /**
     * Detiene el timer de pulso.
     */
    fun stopPulse() {
        pulseTimer?.stop()
        pulseTimer = null
    }

    /**
     * Instala los controladores de eventos de mouse en el canvas para permitir
     * arrastrar el objetivo ÚNICAMENTE a lo largo del eje X.
     * Cede la interacción al proyectil cuando el cursor está sobre la pelota (por estar al frente).
     */
    fun installDrag() {
        // Al presionar el mouse: hit-test
        canvas.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED) { event ->
            if (event.isConsumed) return@addEventHandler

            val local = canvas.sceneToLocal(event.sceneX, event.sceneY)

            // Si el proyectil está por delante y el clic fue sobre él, ceder la selección al proyectil
            val distToProjectile = Math.hypot(local.x - state.projectileX, local.y - state.projectileY)
            if (distToProjectile <= projectileHitRadius) {
                return@addEventHandler
            }

            val dx = local.x - state.targetX
            val dy = local.y - cfg.groundY
            val dist = Math.sqrt(dx * dx + dy * dy)

            if (dist <= hitRadius) {
                isDragging = true
                dragOffsetX = dx
                canvas.cursor = Cursor.CLOSED_HAND
                onRepaint()
                event.consume()
            }
        }

        // Al arrastrar: actualización de X únicamente
        canvas.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_DRAGGED) { event ->
            if (event.isConsumed || !isDragging) return@addEventHandler

            val local = canvas.sceneToLocal(event.sceneX, event.sceneY)
            val requestedX = local.x - dragOffsetX

            // Límites en X: margen derecho del cañón y margen derecho del canvas
            val minX = cfg.cannonBaseX + 45.0
            val maxX = cfg.canvasWidth - targetRadius - 10.0
            val clampedX = requestedX.coerceIn(minX, maxX)

            state.targetX = clampedX
            onTargetPositionChanged(clampedX)
            onRepaint()
            event.consume()
        }

        // Al soltar el mouse
        canvas.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_RELEASED) { event ->
            if (isDragging) {
                isDragging = false
                canvas.cursor = Cursor.HAND
                onRepaint()
                event.consume()
            }
        }

        // Cursor de mano al pasar por encima del objetivo
        canvas.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED) { event ->
            if (event.isConsumed || isDragging) return@addEventHandler
            val local = canvas.sceneToLocal(event.sceneX, event.sceneY)

            // Si el cursor está sobre el proyectil (que está adelante), no cambiar cursor a target
            val distToProjectile = Math.hypot(local.x - state.projectileX, local.y - state.projectileY)
            if (distToProjectile <= projectileHitRadius) {
                return@addEventHandler
            }

            val dx = local.x - state.targetX
            val dy = local.y - cfg.groundY
            val dist = Math.sqrt(dx * dx + dy * dy)

            if (dist <= hitRadius) {
                canvas.cursor = Cursor.HAND
            }
        }
    }
}