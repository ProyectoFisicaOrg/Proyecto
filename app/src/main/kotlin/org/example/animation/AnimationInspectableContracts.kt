package org.example.animation

/**
 * Contrato de reloj de fotogramas inspeccionable para pruebas (ISP + DIP).
 */
interface IInspectableTicker : FrameTicker {
    fun checkRunning(): Boolean
    fun advance(nowNano: Long)
}

/**
 * Contrato de notificación de eventos del vuelo (ISP + DIP).
 */
interface IFlightEventNotifier {
    fun notifyUpdate()
    fun notifyFinished()
    fun notifyTick(x: Double, y: Double)
    fun notifyOutOfBounds()
}
