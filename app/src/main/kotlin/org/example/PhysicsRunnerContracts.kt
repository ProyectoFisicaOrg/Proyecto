package org.example

/**
 * Contrato de resultados tabulados de simulación (ISP + DIP).
 */
interface ITrajectoryData {
    fun parameters(): IPhysicsParams
    fun totalFlightTime(): Double
    fun horizontalRange(): Double
    fun peakHeight(): Double
    fun frameCount(): Int
}

/**
 * Contrato de ejecutor por fotogramas de trayectoria (DIP).
 */
interface ISimulationRunner {
    fun run(params: IPhysicsParams): ITrajectoryData
}
