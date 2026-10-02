package com.konomip.kanatrainer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.konomip.kanatrainer.logic.GlyphStyle

// ---------- 与 Web 版 styles.css 一致的品牌色 ----------

val AccentRed = Color(0xFFD94738)
val AccentRedDark = Color(0xFFB9362B)
val Teal = Color(0xFF157E75)
val TealSoft = Color(0xFFDFF3EF)
val Gold = Color(0xFFD7A63B)
val Wrong = Color(0xFFC8374C)
val Right = Color(0xFF19886B)
val Ink = Color(0xFF20252B)
val Muted = Color(0xFF69727D)
val Line = Color(0xFFE2DED4)
val Paper = Color(0xFFFFFDF8)
val Bg = Color(0xFFF7F5EF)

// 掌握状态配色的暗色变体
private val DarkBg = Color(0xFF15171A)
private val DarkSurface = Color(0xFF1E2126)
private val DarkInk = Color(0xFFE9E6DF)
private val DarkMuted = Color(0xFFA2ABB5)
private val DarkLine = Color(0xFF33383E)
private val DarkAccentRed = Color(0xFFEF7A6C)
private val DarkTeal = Color(0xFF53B5AA)
private val DarkGold = Color(0xFFE1BE67)
private val DarkWrong = Color(0xFFE5798B)
private val DarkRight = Color(0xFF4BAE90)

@Immutable
data class KanaColors(
    val right: Color,
    val wrong: Color,
    val gold: Color,
    val optionCorrectBg: Color,
    val optionWrongBg: Color,
)

private val LightKanaColors = KanaColors(
    right = Right,
    wrong = Wrong,
    gold = Gold,
    optionCorrectBg = Color(0xFFE2F4EC),
    optionWrongBg = Color(0xFFFAE4E8),
)

private val DarkKanaColors = KanaColors(
    right = DarkRight,
    wrong = DarkWrong,
    gold = DarkGold,
    optionCorrectBg = Color(0xFF1F332C),
    optionWrongBg = Color(0xFF3A252B),
)

private fun lightScheme(): ColorScheme = lightColorScheme(
    primary = AccentRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF9DDD9),
    onPrimaryContainer = Color(0xFF7E1F14),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealSoft,
    onSecondaryContainer = Color(0xFF0B5049),
    tertiary = Gold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6E7C8),
    onTertiaryContainer = Color(0xFF6B4E0D),
    background = Bg,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1EDE3),
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
    error = Wrong,
    errorContainer = Color(0xFFFAE4E8),
    onErrorContainer = Color(0xFF8E2233),
)

private fun darkScheme(): ColorScheme = darkColorScheme(
    primary = DarkAccentRed,
    onPrimary = Color(0xFF4A120C),
    primaryContainer = Color(0xFF5C241C),
    onPrimaryContainer = Color(0xFFF9DDD9),
    secondary = DarkTeal,
    onSecondary = Color(0xFF083B36),
    secondaryContainer = Color(0xFF14453F),
    onSecondaryContainer = Color(0xFFC4EAE3),
    tertiary = DarkGold,
    onTertiary = Color(0xFF3F2D05),
    tertiaryContainer = Color(0xFF4E3A10),
    onTertiaryContainer = Color(0xFFF6E7C8),
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF262A2F),
    onSurfaceVariant = DarkMuted,
    outline = DarkLine,
    outlineVariant = DarkLine,
    error = DarkWrong,
    errorContainer = Color(0xFF3A252B),
    onErrorContainer = Color(0xFFF6D5DB),
)

val LocalKanaColors = staticCompositionLocalOf { LightKanaColors }

/** 假名字形：印刷体用默认无衬线，手写体用衬线近似楷书风格。 */
val LocalKanaFontFamily = staticCompositionLocalOf { FontFamily.Default }

@Composable
fun KanaTrainerTheme(
    glyphStyle: GlyphStyle = GlyphStyle.PRINT,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkScheme() else lightScheme()
    val kanaColors = if (darkTheme) DarkKanaColors else LightKanaColors
    val kanaFont = if (glyphStyle == GlyphStyle.HAND) FontFamily.Serif else FontFamily.Default

    CompositionLocalProvider(
        LocalKanaColors provides kanaColors,
        LocalKanaFontFamily provides kanaFont,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
