package org.example.scrubbing

import javafx.scene.canvas.Canvas

/**
 * Contrato de instalador de eventos de mouse sobre el canvas para scrubbing (ISP + DIP).
 */
interface ICanvasMouseInstaller {
    fun bind(canvas: Canvas)
}
