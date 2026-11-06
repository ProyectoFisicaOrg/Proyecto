package org.example.model

import org.example.IPhysicsParams

/**
 * Contrato de parámetros físicos ambientales y de masa (ISP + DIP).
 */
interface IEnvironmentParameters {
    fun currentGravity(): Double
    fun updateGravity(value: Double)
    fun currentMass(): Double
    fun updateMass(value: Double)
    fun toPhysicsParams(): IPhysicsParams

    var gravity: Double
        get() = currentGravity()
        set(value) = updateGravity(value)

    var mass: Double
        get() = currentMass()
        set(value) = updateMass(value)
}
