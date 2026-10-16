package org.example.animation

import org.example.collision.CollisionResult

/**
 * Listener para eventos de colisión (ISP + DIP).
 *
 * Permite que los observadores (UI, panel de control, sonido, efectos)
 * reciban la notificación del resultado de la colisión calculado
 * por el módulo del Issue 2.1 exactamente al impactar el suelo,
 * sin acoplar la física a la presentación.
 */
fun interface CollisionListener {
    /**
     * Invocado exactamente al impactar el suelo con el resultado del validador de colisión (Issue 2.1).
     *
     * @param result Resultado de la colisión (HIT, MISS, detalles de coordenadas y distancia).
     */
    fun onCollision(result: CollisionResult)
}

/**
 * Abstracción del reloj de fotogramas para animación (DIP + SRP).
 *
 * Desacopla la lógica temporal de la animación del toolkit gráfico de JavaFX ([AnimationTimer]).
 * Esto permite pruebas unitarias puras y deterministas en JUnit sin requerir
 * la inicialización de la JVM gráfica.
 */
interface FrameTicker {
    /** Inicia el envío de pulsos de fotogramas al callback suministrado. */
    fun start(onTick: (now: Long) -> Unit)

    /** Detiene el bucle de fotogramas. */
    fun stop()
}

/**
 * Implementación de producción de [FrameTicker] usando [javafx.animation.AnimationTimer].
 */
class JavaFxFrameTicker : FrameTicker {
    private var timer: javafx.animation.AnimationTimer? = null

    override fun start(onTick: (now: Long) -> Unit) {
        timer = object : javafx.animation.AnimationTimer() {
            override fun handle(now: Long) {
                onTick(now)
            }
        }.also { it.start() }
    }

    override fun stop() {
        timer?.stop()
        timer = null
    }
}

/**
 * Implementación de [FrameTicker] para pruebas unitarias deterministas sin entorno gráfico.
 */
class ManualFrameTicker : FrameTicker {
    private var tickCallback: ((Long) -> Unit)? = null
    var isRunning: Boolean = false
        private set

    override fun start(onTick: (now: Long) -> Unit) {
        isRunning = true
        tickCallback = onTick
    }

    override fun stop() {
        isRunning = false
        tickCallback = null
    }

    /** Simula el paso de un fotograma en el instante [nowNano]. */
    fun advance(nowNano: Long) {
        if (isRunning) {
            tickCallback?.invoke(nowNano)
        }
    }
}

