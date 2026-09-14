package org.example.animation

// === Imports de JavaFX y de los componentes propios ===
import javafx.animation.AnimationTimer   // Bucle de fotogramas de JavaFX.
import org.example.model.SimulationState       // Estado que actualiza.
import org.example.physics.PhysicsEngine       // Abstracción de física (DIP).
import org.example.rendering.SceneConfig       // Configuración geométrica.

/**
 * Motor de animación (SRP).
 *
 * Su única responsabilidad es llevar el "reloj" de la simulación:
 * calcular el tiempo transcurrido, consultar el [PhysicsEngine] para
 * obtener la posición del proyectil y actualizar el estado. Depende de
 * la abstracción [PhysicsEngine] (DIP), no de una implementación
 * concreta. Emite callbacks para que la capa de presentación repinte
 * ([onUpdate]) y para notificar que la animación terminó ([onFinished]).
 *
 * @param state Estado que se actualiza en cada fotograma.
 * @param physics Motor de física (inyectado como abstracción).
 * @param cfg Configuración para conocer los límites del canvas.
 * @param onUpdate Callback para repintar cada fotograma.
 * @param onFinished Callback para rearmar la UI al terminar.
 */
class AnimationEngine(
    private val state: SimulationState,
    private val physics: PhysicsEngine,
    private val cfg: SceneConfig,
    private val onUpdate: () -> Unit,
    private val onFinished: () -> Unit,
) {

    // Bucle de fotogramas de JavaFX: llama a tick(now) en cada frame.
    private val timer = object : AnimationTimer() {
        override fun handle(now: Long) {
            tick(now)
        }
    }

    /**
     * Inicia la animación.
     * Resetea el tiempo de inicio y reposiciona el proyectil en el
     * origen antes de arrancar el bucle.
     */
    fun start() {
        state.isAnimating = true            // Marca que el proyectil está en vuelo.
        state.animationStartTime = 0        // Reinicia el contador de tiempo.
        state.projectileX = cfg.cannonBaseX // Proyectil al origen (horizontal).
        state.projectileY = cfg.cannonBaseY // Proyectil al origen (vertical).
        timer.start()                       // Arranca el bucle de fotogramas.
    }

    /**
     * Detiene la animación.
     * Quita el estado "en vuelo", detiene el bucle y avisa para que la
     * UI rearme los controles.
     */
    fun stop() {
        state.isAnimating = false   // El proyectil deja de estar en vuelo.
        timer.stop()                // Detiene el bucle de fotogramas.
        onFinished()                // Notifica el fin de la animación.
    }

    /**
     * Avanza un fotograma: calcula el tiempo, consulta la física y
     * actualiza la posición del proyectil.
     *
     * @param now Marca de tiempo actual (nanosegundos) del bucle.
     */
    private fun tick(now: Long) {
        // Si ya no se está animando, no hace nada.
        if (!state.isAnimating) return

        // Fija el instante de inicio la primera vez que se ejecuta un fotograma.
        if (state.animationStartTime == 0L) {
            state.animationStartTime = now
        }

        // Tiempo transcurrido en segundos desde el disparo.
        val elapsed = (now - state.animationStartTime) / 1_000_000_000.0
        // Consulta la posición del proyectil al motor de física.
        val pos = physics.getState(elapsed)

        // Convierte coordenadas del modelo a coordenadas de canvas (escala x2.5).
        state.projectileX = cfg.cannonBaseX + pos.x * 2.5
        state.projectileY = cfg.cannonBaseY - pos.y * 2.5

        // Verifica si el proyectil salió de los límites del canvas.
        if (state.projectileX > cfg.canvasWidth - 30.0 ||  // Salió por la derecha.
            state.projectileY > cfg.groundY - 20.0 ||      // Cayó al suelo.
            state.projectileY < 30.0) {                    // Salió por arriba.
            stop()  // Detiene la animación al salir de los límites.
            return
        }

        // Si sigue dentro de los límites, repinta el fotograma.
        onUpdate()
    }
}
