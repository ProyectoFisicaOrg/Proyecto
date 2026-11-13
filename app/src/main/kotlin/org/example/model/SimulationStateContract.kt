package org.example.model

interface IStateSnapshot {
    fun isSnapshotReady(): Boolean
}

interface ILaunchParametersState : ILaunchKinematics, IEnvironmentParameters {
    fun hasValidLaunchConfig(): Boolean
}

/**
 * Contrato unificado del estado dinámico de la simulación (ISP + DIP).
 */
interface ISimulationState : ILaunchParametersState, ITargetAndCollisionState, IFlightMotionState {
    fun resetProjectile(baseX: Double, baseY: Double)
    fun startFlight(nowNano: Long = System.nanoTime())
    fun stopFlight()
    fun clearCollisionResult()
}
