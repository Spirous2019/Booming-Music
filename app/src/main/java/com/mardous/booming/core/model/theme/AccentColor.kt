package com.mardous.booming.core.model.theme

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.compose.ui.graphics.Color
import com.mardous.booming.R

enum class AccentColor(
    val id: String,
    @StringRes val titleRes: Int,
    val previewColor: Color,
    @StyleRes val themeOverlayRes: Int,
    val lightPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color
) {
    BLUE(
        id = "blue",
        titleRes = R.string.accent_color_blue,
        previewColor = Color(0xFF3A86F5),
        themeOverlayRes = R.style.ThemeOverlay_Booming_Accent_Blue,
        lightPrimary = Color(0xFF1D5DC6),
        lightPrimaryContainer = Color(0xFFDCE1FF),
        lightOnPrimaryContainer = Color(0xFF04164B),
        darkPrimary = Color(0xFF3A86F5),
        darkPrimaryContainer = Color(0xFF0F253C),
        darkOnPrimaryContainer = Color(0xFFFFFFFF)
    ),
    GREY(
        id = "grey",
        titleRes = R.string.accent_color_grey,
        previewColor = Color(0xFF9E9E9E),
        themeOverlayRes = R.style.ThemeOverlay_Booming_Accent_Grey,
        lightPrimary = Color(0xFF595959),
        lightPrimaryContainer = Color(0xFFE0E0E0),
        lightOnPrimaryContainer = Color(0xFF1A1A1A),
        darkPrimary = Color(0xFFBDBDBD),
        darkPrimaryContainer = Color(0xFF2E2E2E),
        darkOnPrimaryContainer = Color(0xFFF5F5F5)
    ),
    PURPLE(
        id = "purple",
        titleRes = R.string.accent_color_purple,
        previewColor = Color(0xFFA855F7),
        themeOverlayRes = R.style.ThemeOverlay_Booming_Accent_Purple,
        lightPrimary = Color(0xFF7E22CE),
        lightPrimaryContainer = Color(0xFFF3E8FF),
        lightOnPrimaryContainer = Color(0xFF3B0764),
        darkPrimary = Color(0xFFC084FC),
        darkPrimaryContainer = Color(0xFF3B0764),
        darkOnPrimaryContainer = Color(0xFFFAF5FF)
    ),
    GREEN(
        id = "green",
        titleRes = R.string.accent_color_green,
        previewColor = Color(0xFF22C55E),
        themeOverlayRes = R.style.ThemeOverlay_Booming_Accent_Green,
        lightPrimary = Color(0xFF16A34A),
        lightPrimaryContainer = Color(0xFFDCFCE7),
        lightOnPrimaryContainer = Color(0xFF052E16),
        darkPrimary = Color(0xFF4ADE80),
        darkPrimaryContainer = Color(0xFF052E16),
        darkOnPrimaryContainer = Color(0xFFF0FDF4)
    ),
    RED(
        id = "red",
        titleRes = R.string.accent_color_red,
        previewColor = Color(0xFFEF4444),
        themeOverlayRes = R.style.ThemeOverlay_Booming_Accent_Red,
        lightPrimary = Color(0xFFDC2626),
        lightPrimaryContainer = Color(0xFFFEE2E2),
        lightOnPrimaryContainer = Color(0xFF450A0A),
        darkPrimary = Color(0xFFF87171),
        darkPrimaryContainer = Color(0xFF450A0A),
        darkOnPrimaryContainer = Color(0xFFFEF2F2)
    ),
    ORANGE(
        id = "orange",
        titleRes = R.string.accent_color_orange,
        previewColor = Color(0xFFF97316),
        themeOverlayRes = R.style.ThemeOverlay_Booming_Accent_Orange,
        lightPrimary = Color(0xFFEA580C),
        lightPrimaryContainer = Color(0xFFFFEDD5),
        lightOnPrimaryContainer = Color(0xFF431407),
        darkPrimary = Color(0xFFFB923C),
        darkPrimaryContainer = Color(0xFF431407),
        darkOnPrimaryContainer = Color(0xFFFFF7ED)
    ),
    PINK(
        id = "pink",
        titleRes = R.string.accent_color_pink,
        previewColor = Color(0xFFEC4899),
        themeOverlayRes = R.style.ThemeOverlay_Booming_Accent_Pink,
        lightPrimary = Color(0xFFDB2777),
        lightPrimaryContainer = Color(0xFFFCE7F3),
        lightOnPrimaryContainer = Color(0xFF500724),
        darkPrimary = Color(0xFFF472B6),
        darkPrimaryContainer = Color(0xFF500724),
        darkOnPrimaryContainer = Color(0xFFFDF2F8)
    ),
    TEAL(
        id = "teal",
        titleRes = R.string.accent_color_teal,
        previewColor = Color(0xFF06B6D4),
        themeOverlayRes = R.style.ThemeOverlay_Booming_Accent_Teal,
        lightPrimary = Color(0xFF0891B2),
        lightPrimaryContainer = Color(0xFFCFFAFE),
        lightOnPrimaryContainer = Color(0xFF083344),
        darkPrimary = Color(0xFF22D3EE),
        darkPrimaryContainer = Color(0xFF083344),
        darkOnPrimaryContainer = Color(0xFFECFEFF)
    );

    companion object {
        val DEFAULT = BLUE

        fun fromId(id: String?): AccentColor {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
