package org.example

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

fun main() {
    val params = PhysicsParams(
        angleDeg = 45.0,
        initialVelocity = 25.0,
        gravity = 9.8,
        mass = 2.0
    )

    val data = SimulationRunner().run(params)

    val json = Json { prettyPrint = true }
    println(json.encodeToString<TrajectoryData>(data))
}