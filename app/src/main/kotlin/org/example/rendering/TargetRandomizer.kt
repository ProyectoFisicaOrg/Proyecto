package org.example.rendering

import java.util.Random

/**
 * Contrato para el cálculo y asignación aleatoria de la posición del objetivo (SRP + DIP + OCP).
 *
 * Responsabilidad Única (SRP):
 *  - Encapsular la lógica matemática para determinar una coordenada horizontal aleatoria
 *    dentro de los límites válidos del suelo.
 *
 * Principio Abierto/Cerrado (OCP):
 *  - Permite extender o intercambiar estrategias de dispersión (uniforme, gaussiana,
 *    con semillas reproducibles para pruebas, o basadas en zonas) sin modificar los
 *    componentes gráficos ni el panel de control.
 *
 * Inversión de Dependencias (DIP):
 *  - Los componentes de alto nivel dependen de esta abstracción y no de generadores estáticos.
 */
interface ITargetRandomizer {
    /**
     * Calcula una coordenada X aleatoria dentro del rango válido provisto.
     *
     * @param minX Límite inferior permitido en píxeles.
     * @param maxX Límite superior permitido en píxeles.
     * @return Coordenada X calculada dentro del intervalo [minX, maxX].
     */
    fun calculateRandomX(minX: Double, maxX: Double): Double
}

/**
 * Implementación estándar de distribución uniforme para reposicionar el objetivo.
 *
 * @param random Generador de números pseudoaleatorios (inyectable para pruebas deterministas).
 */
class UniformTargetRandomizer(
    private val random: Random = Random()
) : ITargetRandomizer {

    override fun calculateRandomX(minX: Double, maxX: Double): Double {
        require(maxX >= minX) { "El límite superior ($maxX) no puede ser menor que el límite inferior ($minX)" }
        return minX + random.nextDouble() * (maxX - minX)
    }
}
