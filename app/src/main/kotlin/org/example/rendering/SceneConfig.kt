package org.example.rendering

/**
 * Configuración geométrica encapsulada y cohesiva del escenario (SRP + Encapsulación).
 */
class SceneConfig(
    canvasWidth: Double = 900.0,
    canvasHeight: Double = 550.0
) : ISceneConfig {
    private val dims: List<Double> = listOf(canvasWidth, canvasHeight, canvasHeight - 80.0, 90.0, 46.0, 12.0)

    override fun width(): Double {
        return dims[0]
    }

    override fun height(): Double {
        return dims[1]
    }

    override fun groundLevelY(): Double {
        return dims[2]
    }

    override fun basePivotX(): Double {
        return dims[3]
    }

    override fun basePivotY(): Double {
        return dims[2]
    }

    override fun barrelLength(): Double {
        return dims[4]
    }

    override fun barrelWidth(): Double {
        return dims[5]
    }

    override fun isWithinCanvas(x: Double, y: Double, margin: Double): Boolean {
        return x in -margin..(dims[0] + margin) && y in -margin..(dims[1] + margin)
    }

    fun clampToCanvasX(x: Double, margin: Double = 0.0): Double {
        return x.coerceIn(margin, dims[0] - margin)
    }

    fun clampToGround(y: Double): Double {
        return y.coerceAtMost(dims[2])
    }

    fun cannonMuzzlePosition(angleDeg: Double): Pair<Double, Double> {
        val angleRad = Math.toRadians(angleDeg)
        val mx = dims[3] + Math.cos(angleRad) * dims[4]
        val my = dims[2] - Math.sin(angleRad) * dims[4]
        return Pair(mx, my)
    }
}
