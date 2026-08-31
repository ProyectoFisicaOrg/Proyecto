package org.example

/**
 * Data class inmutable que representa una posición en el espacio 2D.
 *
 * Sirve para transportar el resultado del motor de física (coordenadas
 * del modelo) de forma segura e inmutable. Al ser un `data class` de
 * Kotlin, obtiene automáticamente equals/hashCode/toString.
 *
 * @property x Coordenada horizontal.
 * @property y Coordenada vertical.
 */
data class Position(
    val x: Double,
    val y: Double,
)
