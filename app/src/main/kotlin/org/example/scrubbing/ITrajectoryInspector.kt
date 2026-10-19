package org.example.scrubbing

/**
 * Contrato de inspección de trayectoria para la interacción de Dev 2 (DIP + ISP).
 *
 * Principio de Inversión de Dependencias (DIP):
 *  - Los componentes de UI e Interacción (ScrubbingController, ResultsHUD, TooltipRenderer)
 *    dependen estrictamente de esta interfaz y NO del motor de física ni de las matrices internas de Dev 1.
 *
 * Principio de Segregación de Interfaces (ISP):
 *  - Expone únicamente los métodos necesarios para:
 *    1. Consultar el punto cinemático más cercano a una posición física (en metros).
 *    2. Consultar el punto cinemático más cercano a la posición del cursor del mouse (en píxeles de pantalla).
 *    3. Obtener el resumen consolidado del vuelo ([FlightSummary]).
 */
interface ITrajectoryInspector {

    /**
     * Obtiene el punto de trayectoria más cercano a la coordenada consultada en el plano físico (metros).
     *
     * @param physicsX Coordenada horizontal en metros.
     * @param physicsY Coordenada vertical en metros.
     * @return [TrajectoryPoint] exacto sobre la curva parabólica.
     */
    fun findNearestPoint(physicsX: Double, physicsY: Double): TrajectoryPoint

    /**
     * Obtiene el punto de trayectoria más cercano a la posición del cursor del mouse en el canvas (píxeles).
     * Realiza internamente la transformación matemática al espacio físico.
     *
     * @param cursorCanvasX Coordenada horizontal del cursor en píxeles.
     * @param cursorCanvasY Coordenada vertical del cursor en píxeles.
     * @return [TrajectoryPoint] correspondiente para enganchar el proyectil.
     */
    fun findNearestPointFromCursor(cursorCanvasX: Double, cursorCanvasY: Double): TrajectoryPoint

    /**
     * Obtiene el punto de la trayectoria más cercano a una coordenada horizontal X (metros).
     *
     * @param physicsX Coordenada horizontal en metros.
     * @return [TrajectoryPoint] correspondiente.
     */
    fun findNearestPointByX(physicsX: Double): TrajectoryPoint = findNearestPoint(physicsX, 0.0)

    /**
     * Obtiene el resumen del vuelo con el tiempo total de caída y la distancia recorrida.
     *
     * @return [FlightSummary] consolidado.
     */
    fun getFlightSummary(): FlightSummary
}
