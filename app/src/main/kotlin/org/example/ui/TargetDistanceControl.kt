package org.example.ui

import java.util.Locale
import kotlin.math.round
import org.example.rendering.CoordinateTransformer

/**
 * Especificación encapsulada de límites (18.00 m .. 312.00 m) y valor por defecto del objetivo (SRP + Encapsulación).
 */
@ConsistentCopyVisibility
data class TargetDistanceSpec internal constructor(
    private val bounds: List<Double>,
    private val defaultValidator: INumericInputValidator
) : ITargetDistanceSpec {

    constructor(
        minVal: Double = 18.0,
        maxVal: Double = 312.0,
        defaultVal: Double = 264.0,
        defaultValidator: INumericInputValidator = DefaultNumericValidator
    ) : this(listOf(minVal, maxVal, defaultVal), defaultValidator)

    override fun minMeters(): Double {
        return bounds[0]
    }

    override fun maxMeters(): Double {
        return bounds[1]
    }

    override fun defaultMeters(): Double {
        return bounds[2]
    }

    override fun clampMeters(value: Double): Double {
        return round(value.coerceIn(bounds[0], bounds[1]) * 100.0) / 100.0
    }

    override fun resolveCommittedMeters(rawText: String, validator: INumericInputValidator?): Double {
        val chosen = validator ?: defaultValidator
        val parsed = chosen.parseOrNull(rawText) ?: return bounds[2]
        if (!parsed.isFinite()) return bounds[2]
        return clampMeters(parsed)
    }

    override fun formatNumber(meters: Double): String {
        return String.format(Locale.US, "%.2f", bounds[0].coerceAtMost(meters).let { meters })
    }
}

/**
 * Controlador de edición manual de distancia al blanco con clamping [18.00, 312.00] y bloqueo tras disparar (SRP + DIP).
 */
class TargetDistanceController(
    private val spec: ITargetDistanceSpec,
    private val transformer: CoordinateTransformer,
    private val overlayView: ITargetOverlayView,
    private val onTargetCanvasXCommitted: (Double) -> Unit,
    private val validator: INumericInputValidator = DefaultNumericValidator
) : ITargetDistanceController {
    private var syncing: Boolean = false

    override fun bind() {
        overlayView.textPort().installFilter { candidate ->
            validator.isValidPartial(candidate, true)
        }
        overlayView.textPort().onCommitRequested { commitText() }
    }

    override fun commitText() {
        if (syncing || overlayView.isLocked()) return
        val meters = spec.resolveCommittedMeters(overlayView.textPort().currentText(), validator)
        val newCanvasX = transformer.toCanvasX(meters)
        applySynchronized(newCanvasX, meters)
        onTargetCanvasXCommitted(newCanvasX)
    }

    override fun syncFromCanvasX(targetCanvasX: Double) {
        if (syncing) return
        val meters = spec.clampMeters(transformer.toPhysicsX(targetCanvasX))
        applySynchronized(targetCanvasX, meters)
    }

    override fun prepareForLaunch() {
        if (!overlayView.isLocked()) commitText()
        overlayView.setLocked(true)
    }

    override fun unlockForEditing() {
        overlayView.setLocked(false)
    }

    private fun applySynchronized(targetCanvasX: Double, meters: Double) {
        syncing = true
        try {
            overlayView.textPort().updateText(spec.formatNumber(meters))
            overlayView.updateHorizontalPosition(targetCanvasX)
        } finally {
            syncing = false
        }
    }
}
