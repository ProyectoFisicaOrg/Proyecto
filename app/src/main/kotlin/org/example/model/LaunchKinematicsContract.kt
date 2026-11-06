package org.example.model

import org.example.IPhysicsParams

/**
 * Contrato de parámetros cinemáticos iniciales del lanzamiento (ISP + DIP).
 */
interface ILaunchKinematics {
    fun currentAngle(): Double
    fun updateAngle(value: Double)
    fun currentVelocity(): Double
    fun updateVelocity(value: Double)

    var angle: Double
        get() = currentAngle()
        set(value) = updateAngle(value)

    var velocity: Double
        get() = currentVelocity()
        set(value) = updateVelocity(value)
}
