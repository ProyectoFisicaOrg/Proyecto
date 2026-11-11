package org.example

import org.example.collision.ICollisionTarget
import org.example.model.Target
import org.example.telemetry.DefaultTelemetryProvider

/**
 * Demostración de consola desacoplada por constructores e interfaces (SRP + DIP).
 */
class App(
    factory: ITrajectorySimulationFactory = DefaultTrajectorySimulationFactory(),
    paramsFactory: (Double, Double, Double, Double) -> IPhysicsParams = ::PhysicsParams,
    targetFactory: (Double, Double) -> ICollisionTarget = ::Target
) : ISimulationCliDemo {
    private val deps: Triple<ITrajectorySimulationFactory, (Double, Double, Double, Double) -> IPhysicsParams, (Double, Double) -> ICollisionTarget> =
        Triple(factory, paramsFactory, targetFactory)

    override fun executeDemo() {
        val params = deps.second(45.0, 25.0, 9.8, 2.0)
        val matrix = deps.first.buildGenerator().generate(params, 120)
        println(matrix)
        val target = deps.third(50.0, 2.0)
        val collision = deps.first.buildCollisionDetector().checkCollision(matrix, target)
        println("Colisión con objetivo en x=${target.centerX}: ${collision.status}")
        println(collision.message)
        val state = DefaultTelemetryProvider().buildApi().getInstantaneousState(matrix, 20.0, 10.0)
        println("Telemetría en (20, 10): ${state.format()}")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String> = emptyArray()) {
            App().executeDemo()
        }
    }
}
