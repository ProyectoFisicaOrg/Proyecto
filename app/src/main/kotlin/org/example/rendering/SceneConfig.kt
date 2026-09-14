package org.example.rendering

/**
 * Configuración geométrica del escenario (SRP).
 *
 * Define, en un único lugar, las dimensiones del canvas y las posiciones
 * del cañón y del suelo. Centralizar estos valores evita duplicación y
 * permite ajustar toda la escena modificando solo esta clase.
 *
 * @property canvasWidth Ancho del canvas en píxeles.
 * @property canvasHeight Alto del canvas en píxeles.
 */
class SceneConfig(
    val canvasWidth: Double = 900.0,
    val canvasHeight: Double = 550.0,
) {
    /** Coordenada Y de la línea del suelo (80 px por encima del borde inferior). */
    val groundY: Double = canvasHeight - 80.0

    /** Coordenada X del pivote del cañón. */
    val cannonBaseX: Double = 90.0

    /** Coordenada Y del pivote del cañón (coincide con el suelo). */
    val cannonBaseY: Double = groundY

    /** Longitud del tubo del cañón en píxeles. */
    val cannonLength: Double = 46.0

    /** Grosor (ancho) del tubo del cañón en píxeles. */
    val cannonWidth: Double = 12.0
}
