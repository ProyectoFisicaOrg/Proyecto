package org.example.model

/**
 * Contenedor del estado mutable de la simulación (SRP).
 *
 * Agrupa en un único objeto todos los valores que cambian durante la
 * ejecución: los parámetros de los controles y la posición del
 * proyectil. Varias clases comparten esta misma instancia para leer y
 * escribir estado sin acoplarse directamente entre sí.
 *
 * (Por simplicidad de la demo, las propiedades son públicas y mutables;
 * en un sistema mayor se usarían getters/setters o una cola de eventos.)
 */
class SimulationState {

    /** Ángulo del cañón en grados (rango 0-90). Cambia con el slider. */
    var angle: Double = 45.0

    /** Velocidad inicial del disparo en m/s. Cambia con el slider. */
    var velocity: Double = 50.0

    /** Gravedad aplicada en m/s². Cambia con el slider. */
    var gravity: Double = 9.8

    /** Masa del proyectil en kg. Cambia con el slider. */
    var mass: Double = 10.0

    /** Indica si el proyectil está actualmente en movimiento. */
    var isAnimating: Boolean = false

    /** Marca de tiempo (en nanosegundos) del inicio de la animación. */
    var animationStartTime: Long = 0

    /** Coordenada horizontal actual del proyectil en el canvas. */
    var projectileX: Double = 90.0

    /** Coordenada vertical actual del proyectil en el canvas. */
    var projectileY: Double = 470.0
}
