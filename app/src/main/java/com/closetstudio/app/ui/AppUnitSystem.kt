package com.closetstudio.app.ui

import androidx.compose.runtime.compositionLocalOf
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Supported measurement unit systems in Closet Studio 3D.
 * All internal geometry and persistence remain strictly in millimeters (`mm`) for precision,
 * while [AppUnitSystem] governs display, formatting, input parsing, and step increments.
 */
enum class AppUnitSystem(
    val code: String,
    val symbol: String,
) {
    MM("mm", "mm"),
    CM("cm", "cm"),
    M("m", "m"),
    INCH("in", "in"),
    FEET_INCHES("ft_in", "ft");

    fun displayName(lang: AppLanguage): String = when (this) {
        MM -> if (lang == AppLanguage.ES) "Milímetros (mm)" else "Millimeters (mm)"
        CM -> if (lang == AppLanguage.ES) "Centímetros (cm)" else "Centimeters (cm)"
        M -> if (lang == AppLanguage.ES) "Metros (m)" else "Meters (m)"
        INCH -> if (lang == AppLanguage.ES) "Pulgadas (in / \")" else "Inches (in / \")"
        FEET_INCHES -> if (lang == AppLanguage.ES) "Pies y Pulgadas (ft / in)" else "Feet & Inches (ft / in)"
    }

    fun subtitle(lang: AppLanguage): String = when (this) {
        MM -> if (lang == AppLanguage.ES) "Estándar milimétrico industrial" else "Industrial metric standard"
        CM -> if (lang == AppLanguage.ES) "Métrico decimal (1 cm = 10 mm)" else "Decimal metric (1 cm = 10 mm)"
        M -> if (lang == AppLanguage.ES) "Métrico arquitectónico (1 m = 1000 mm)" else "Architectural metric (1 m = 1000 mm)"
        INCH -> if (lang == AppLanguage.ES) "Imperial EE.UU. (1 in = 25.4 mm)" else "US Imperial (1 in = 25.4 mm)"
        FEET_INCHES -> if (lang == AppLanguage.ES) "Arquitectura EE.UU. (1 ft = 12 in)" else "US Architectural (1 ft = 12 in)"
    }

    /**
     * Formats a millimeter dimension into a clean localized text with unit symbol.
     * e.g. "3600 mm", "360 cm", "3.60 m", "141.7 in", "11' 9.7\""
     */
    fun format(mm: Float): String = when (this) {
        MM -> "${mm.roundToInt()} mm"
        CM -> {
            val cm = mm / 10f
            if (abs(cm - cm.roundToInt()) < 0.05f) "${cm.roundToInt()} cm"
            else String.format(Locale.US, "%.1f cm", cm)
        }
        M -> String.format(Locale.US, "%.2f m", mm / 1000f)
        INCH -> {
            val inches = mm / 25.4f
            String.format(Locale.US, "%.1f in", inches)
        }
        FEET_INCHES -> {
            val totalInches = mm / 25.4f
            val feet = (totalInches / 12f).toInt()
            val remInches = totalInches - feet * 12f
            String.format(Locale.US, "%d' %.1f\"", feet, remInches)
        }
    }

    /**
     * Formats a compact representation for floating 3D anchors and chips.
     * e.g. "3600mm", "360cm", "3.6m", "141.7\"", "11' 10\""
     */
    fun formatCompact(mm: Float): String = when (this) {
        MM -> "${mm.roundToInt()}mm"
        CM -> "${(mm / 10f).roundToInt()}cm"
        M -> String.format(Locale.US, "%.2fm", mm / 1000f)
        INCH -> String.format(Locale.US, "%.1f\"", mm / 25.4f)
        FEET_INCHES -> {
            val totalInches = (mm / 25.4f).roundToInt()
            val feet = totalInches / 12
            val remInches = totalInches % 12
            "$feet' $remInches\""
        }
    }

    /**
     * Formats the numeric value to populate text inputs (without unit suffix).
     */
    fun formatInput(mm: Float): String = when (this) {
        MM -> "${mm.roundToInt()}"
        CM -> {
            val cm = mm / 10f
            if (abs(cm - cm.roundToInt()) < 0.05f) "${cm.roundToInt()}"
            else String.format(Locale.US, "%.1f", cm)
        }
        M -> String.format(Locale.US, "%.2f", mm / 1000f)
        INCH -> String.format(Locale.US, "%.1f", mm / 25.4f)
        FEET_INCHES -> {
            val totalInches = mm / 25.4f
            val feet = (totalInches / 12f).toInt()
            val remInches = totalInches - feet * 12f
            String.format(Locale.US, "%d' %.1f\"", feet, remInches)
        }
    }

    /**
     * Parses a string typed by the user in this unit system back into millimeters.
     */
    fun parseInput(rawText: String): Float? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return null

        // Check for feet/inches syntax like 11' 9.7" or 11' 9 1/2" or 141"
        if (trimmed.contains("'") || trimmed.contains("\"")) {
            var feet = 0f
            var inches = 0f
            if (trimmed.contains("'")) {
                val parts = trimmed.split("'", limit = 2)
                feet = parts[0].trim().replace(',', '.').toFloatOrNull() ?: 0f
                val remainder = parts.getOrNull(1)?.replace("\"", "")?.trim() ?: ""
                if (remainder.isNotEmpty()) {
                    inches = parseFractional(remainder) ?: 0f
                }
            } else {
                val rem = trimmed.replace("\"", "").trim()
                inches = parseFractional(rem) ?: 0f
            }
            return (feet * 304.8f + inches * 25.4f).coerceAtLeast(0f)
        }

        val parsedVal = parseFractional(trimmed) ?: return null

        return when (this) {
            MM -> parsedVal
            CM -> parsedVal * 10f
            M -> parsedVal * 1000f
            INCH -> parsedVal * 25.4f
            FEET_INCHES -> parsedVal * 304.8f // Decimals without symbols interpreted as feet
        }.coerceAtLeast(0f)
    }

    /**
     * Returns the 4 quick step adjustment buttons (label to delta in millimeters).
     */
    fun stepValues(): List<Pair<String, Float>> = when (this) {
        MM -> listOf("-100" to -100f, "-25" to -25f, "+25" to 25f, "+100" to 100f)
        CM -> listOf("-10" to -100f, "-1" to -10f, "+1" to 10f, "+10" to 100f)
        M -> listOf("-0.1" to -100f, "-0.01" to -10f, "+0.01" to 10f, "+0.1" to 100f)
        INCH -> listOf("-6\"" to -152.4f, "-1\"" to -25.4f, "+1\"" to 25.4f, "+6\"" to 152.4f)
        FEET_INCHES -> listOf("-1'" to -304.8f, "-1\"" to -25.4f, "+1\"" to 25.4f, "+1'" to 304.8f)
    }

    /**
     * Recommended slider step in mm for snapping sliders.
     */
    fun sliderStepMm(): Float = when (this) {
        MM -> 25f
        CM -> 10f
        M -> 10f
        INCH -> 25.4f
        FEET_INCHES -> 25.4f
    }

    companion object {
        fun fromCode(code: String): AppUnitSystem =
            entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: MM
    }
}

val LocalAppUnitSystem = compositionLocalOf { AppUnitSystem.MM }

private fun parseFractional(str: String): Float? {
    val clean = str.trim().replace(',', '.')
    if (clean.isEmpty()) return null
    if (clean.contains("/")) {
        val parts = clean.split(" ")
        if (parts.size == 2) {
            val whole = parts[0].toFloatOrNull() ?: 0f
            val fracParts = parts[1].split("/")
            if (fracParts.size == 2) {
                val num = fracParts[0].toFloatOrNull() ?: 0f
                val den = fracParts[1].toFloatOrNull() ?: 1f
                return if (den != 0f) whole + (num / den) else null
            }
        } else if (parts.size == 1) {
            val fracParts = parts[0].split("/")
            if (fracParts.size == 2) {
                val num = fracParts[0].toFloatOrNull() ?: 0f
                val den = fracParts[1].toFloatOrNull() ?: 1f
                return if (den != 0f) num / den else null
            }
        }
    }
    return clean.toFloatOrNull()
}
