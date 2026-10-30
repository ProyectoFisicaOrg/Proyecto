package org.example.ui

import java.util.Locale
import kotlin.math.round
import org.example.rendering.CoordinateTransformer

/**
 * Especificación encapsulada de límites (18.00 m .. 312.00 m) y valor por defecto del objetivo (SRP + Encapsulación).
 */
class TargetDistanceSpec(
    private val minVal: Double = 18.0,
    private val maxVal: Double = 312.0,
    private val defaultVal: Double = 264.0
) {
    fun minMeters(): Double = minVal

    fun maxMeters(): Double = maxVal

    fun defaultMeters(): Double = defaultVal

    fun clampMeters(value: Double): Double =
        round(value.coerceIn(minVal, maxVal) * 100.0) / 100.0

    fun resolveCommittedMeters(rawText: String, validator: INumericInputValidator = NumericInputValidator()): Double {
        val parsed = validator.parseOrNull(rawText) ?: return defaultVal
        if (!parsed.isFinite()) return defaultVal
        return clampMeters(parsed)
    }

    fun formatNumber(meters: Double): String =
        String.format(Locale.US, "%.2f", meters)
}

/**
 * Puerto abstracto del componente visual flotante de distancia bajo el blanco (DIP + ISP).
 */
interface ITargetOverlayView {
    fun textPort(): ITextInputPort
    fun updateHorizontalPosition(targetCanvasX: Double)
    fun setLocked(locked: Boolean)
    fun isLocked(): Boolean
}

/**
 * Contrato del controlador de edición manual y sincronización bidireccional de la distancia al blanco (DIP + ISP).
 */
interface ITargetDistanceController {
    fun bind()
    fun syncFromCanvasX(targetCanvasX: Double)
    fun prepareForLaunch()
    fun unlockForEditing()
}

/**
 * Controlador de edición manual de distancia al blanco con clamping [18.00, 312.00] y bloqueo tras disparar (SRP + DIP).
 */
class TargetDistanceController(
    private val spec: TargetDistanceSpec,
    private val transformer: CoordinateTransformer,
    private val overlayView: ITargetOverlayView,
    private val onTargetCanvasXCommitted: (Double) -> Unit,
    private val validator: INumericInputValidator = NumericInputValidator()
) : ITargetDistanceController {
    private var syncing: Boolean = false

    override fun bind() {
        overlayView.textPort().installFilter { candidate ->
            validator.isValidPartial(candidate, allowDecimal = true)
        }
        overlayView.textPort().onCommitRequested { commitText() }
    }

    fun commitText() {
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
