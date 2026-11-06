package org.example.model

/**
 * Contrato unificado del estado dinámico de la simulación (ISP + DIP).
 */
interface ISimulationState : ILaunchKinematics, IEnvironmentParameters, ITargetAndCollisionState, IFlightMotionState {
    fun resetProjectile(baseX: Double, baseY: Double)
    fun startFlight(nowNano: Long = System.nanoTime())
    fun stopFlight()
    fun clearCollisionResult()
}
