package org.example.rendering

/**
 * Implementación estándar de transformación de coordenadas ortogonales (SRP + DIP + Encapsulación).
 */
class DefaultCoordinateTransformer(
    private val cfg: ISceneConfig,
    private val pixelsPerMeter: Double = 2.5
) : CoordinateTransformer {

    override fun toCanvasX(physicsX: Double): Double {
        return cfg.basePivotX() + physicsX * pixelsPerMeter
    }

    override fun toCanvasY(physicsY: Double): Double {
        return cfg.basePivotY() - physicsY * pixelsPerMeter
    }

    override fun toPhysicsX(canvasX: Double): Double {
        return (canvasX - cfg.basePivotX()) / pixelsPerMeter
    }

    override fun toPhysicsY(canvasY: Double): Double {
        return (cfg.basePivotY() - canvasY) / pixelsPerMeter
    }

    override fun toCanvasDistance(physicsMeters: Double): Double {
        return physicsMeters * pixelsPerMeter
    }

    override fun toPhysicsDistance(canvasPixels: Double): Double {
        return canvasPixels / pixelsPerMeter
    }
}
