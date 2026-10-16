package org.example.model

/**
 * Objetivo circular desplazable sobre el suelo.
 *
 * Responsabilidad Única (SRP): valor inmutable que representa el objetivo.
 * Encapsula el cálculo de sus bordes y centro a partir del centro y el radio.
 *
 * @param centerX Posición X del centro del objetivo en el suelo (metros)
 * @param radius Radio del objetivo (metros)
 */
data class Target(
    val centerX: Double,
    val radius: Double
) {
    init {
        require(radius >= 0) { "El radio no puede ser negativo" }
    }

    /** Borde izquierdo del objetivo */
    val left: Double get() = centerX - radius

    /** Borde derecho del objetivo */
    val right: Double get() = centerX + radius

    /** Diámetro del objetivo */
    val diameter: Double get() = 2 * radius
}
