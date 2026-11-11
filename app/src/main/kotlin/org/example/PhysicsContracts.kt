package org.example

/**
 * Interfaz para calculadores de trayectoria (DIP + ISP).
 */
interface TrajectoryCalculator {
    fun calculateState(params: IPhysicsParams, t: Double): TrajectoryPoint
    fun calculateTimeOfFlight(params: IPhysicsParams): Double?
    fun calculateRange(params: IPhysicsParams): Double?
    fun calculateMaxHeight(params: IPhysicsParams): Double
}

/**
 * Contrato de fábrica y ejecución de trayectoria física (DIP).
 */
interface ITrajectoryCalculatorFactory {
    fun createParabolic(): TrajectoryCalculator
    fun createCustom(calculator: TrajectoryCalculator): TrajectoryCalculator
}
