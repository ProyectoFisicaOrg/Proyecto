package org.example.physics

import org.example.Position

/**
 * Implementación concreta del motor de física (SRP).
 *
 * Este es un motor "mock" que simula un movimiento lineal simple:
 * x = t * 20, y = t * 10. Se usa para probar la animación y el
 * renderizado sin necesidad de física real. Siguiendo el principio de
 * inversión de dependencias (DIP), se crea por constructor en el punto
 * de entrada y se pasa como [PhysicsEngine].
 */
class MockPhysicsEngine : PhysicsEngine {

    /**
     * Devuelve la posición del proyectil en el instante [time].
     *
     * @param time Tiempo transcurrido en segundos.
     * @return Posición calculada con movimiento lineal simple.
     */
    override fun getState(time: Double): Position {
        return Position(time * 20.0, time * 10.0)
    }
}
