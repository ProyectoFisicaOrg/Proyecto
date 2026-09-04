package org.example.physics

import org.example.Position

/**
 * Abstracción del motor de física (DIP).
 *
 * Define el contrato que deben cumplir todos los motores de física.
 * Los módulos de alto nivel (la animación) dependen de esta abstracción
 * y no de una implementación concreta, de modo que se puede cambiar de
 * motor sin tocar el resto del sistema (OCP / LSP).
 */
interface PhysicsEngine {

    /**
     * Devuelve la posición del proyectil en el instante [time].
     *
     * @param time Tiempo transcurrido en segundos desde el disparo.
     * @return Posición en coordenadas del modelo físico (metros).
     */
    fun getState(time: Double): Position
}
