package org.example

import org.example.model.Target

// PhysicsParams se toma de org.example (Physics.kt)

fun main() {
    // Parámetros de entrada del usuario
    val params = PhysicsParams(
        angleDeg = 45.0,
        initialVelocity = 25.0,
        gravity = 9.8,
        mass = 2.0
    )

    // Generar matriz de trayectoria (Issue 2.1) a través de la fábrica
    val matrix = TrajectorySimulationFactory.generateMatrix(params)
    println(matrix)

    // Detectar colisión con un objetivo circular en el suelo
    val target = Target(centerX = 50.0, radius = 2.0)
    val collisionDetector = TrajectorySimulationFactory.createCollisionDetector()
    val collision = collisionDetector.checkCollision(matrix, target)
    println("Colisión con objetivo en x=${target.centerX}: ${collision.status}")
    println(collision.message)
}
