package org.example

import org.example.model.Target
import org.example.telemetry.DefaultTelemetryProvider
import org.example.telemetry.ITelemetryFactory

/**
 * Demostración de consola desacoplada por constructores e interfaces (SRP + DIP).
 */
class App(
    factory: ITrajectorySimulationFactory = SimulationFactoryWiring,
    telemetry: ITelemetryFactory = DefaultTelemetryProvider
) : ISimulationCliDemo {
    private val deps: Pair<ITrajectorySimulationFactory, ITelemetryFactory> = Pair(factory, telemetry)

    override fun executeDemo() {
        val params = PhysicsParams(45.0, 25.0, 9.8, 2.0)
        val matrix = deps.first.buildGenerator().generate(params, 120)
        println(matrix)
        val target = Target(50.0, 2.0)
        val collision = deps.first.buildCollisionDetector().checkCollision(matrix, target)
        println("Colisión con objetivo en x=${target.centerPosition()}: ${collision.collisionStatus()}")
        println(collision.statusMessage())
        val state = deps.second.buildApi().getInstantaneousState(matrix, 20.0, 10.0)
        println("Telemetría en (20, 10): ${state.format()}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String> = emptyArray()) {
            App().executeDemo()
        }
    }
}
