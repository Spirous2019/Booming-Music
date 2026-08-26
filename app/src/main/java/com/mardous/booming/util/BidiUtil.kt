package com.mardous.booming.util

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection

object BidiUtil {

    /**
     * Determines whether the first strong directional character in the text is RTL (e.g. Arabic, Hebrew, Persian, Urdu).
     * Neutral characters (whitespace, punctuation, numbers, symbols, brackets) are skipped.
     */
    fun isRtl(text: CharSequence?): Boolean {
        if (text.isNullOrBlank()) return false
        var i = 0
        val length = text.length
        while (i < length) {
            val codePoint = java.lang.Character.codePointAt(text, i)
            when (java.lang.Character.getDirectionality(codePoint)) {
                java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return true

                java.lang.Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return false
            }
            i += java.lang.Character.charCount(codePoint)
        }
        return false
    }

    /**
     * Returns the appropriate Compose TextDirection based on the text's first strong directional character.
     */
    fun getTextDirection(text: CharSequence?): TextDirection {
        return if (isRtl(text)) TextDirection.Rtl else TextDirection.Ltr
    }

    /**
     * Returns the natural alignment (Start for LTR, End for RTL) or Center if isCenterHorizontally is true.
     */
    fun getNaturalAlignment(text: CharSequence?, isCenterHorizontally: Boolean): TextAlign {
        if (isCenterHorizontally) return TextAlign.Center
        return if (isRtl(text)) TextAlign.End else TextAlign.Start
    }
}
