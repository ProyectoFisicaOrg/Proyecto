package org.example.ui

/**
 * Contratos de validación léxica y especificación paramétrica de entradas numéricas (ISP + DIP).
 */
interface INumericInputValidator {
    fun isValidPartial(text: String, allowDecimal: Boolean): Boolean
    fun parseOrNull(text: String): Double?
}

interface INumericParameterSpec {
    fun title(): String
    fun min(): Double
    fun max(): Double
    fun defaultValue(): Double
    fun allowsDecimal(): Boolean
    fun step(): Double
    fun isInRange(value: Double): Boolean
    fun normalize(value: Double): Double
    fun resolveOrDefault(rawText: String, validator: INumericInputValidator? = null): Double
    fun format(value: Double): String
}
