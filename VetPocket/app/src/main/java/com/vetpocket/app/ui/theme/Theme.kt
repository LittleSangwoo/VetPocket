package com.vetpocket.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Фирменная палитра: глубокий бирюзовый + тёплый коралловый на кремовом фоне.
val Teal700 = Color(0xFF0F766E)
val Teal900 = Color(0xFF0B3D3A)
val Teal300 = Color(0xFF5EEAD4)
val Coral = Color(0xFFFF6B4A)
val Cream = Color(0xFFFBF7F2)
val Ink = Color(0xFF1F2A2E)

// Вердикты анализатора корма
val VerdictGreen = Color(0xFF2E9E5B)
val VerdictYellow = Color(0xFFE8A317)
val VerdictRed = Color(0xFFE5484D)

// SOS
val SosRedLight = Color(0xFFFF5A5F)
val SosRedDark = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = Teal700,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDF3EC),
    onPrimaryContainer = Color(0xFF062F2B),
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE2D8),
    onSecondaryContainer = Color(0xFF4A1706),
    tertiary = Color(0xFFF2A516),
    onTertiary = Color(0xFF3A2600),
    tertiaryContainer = Color(0xFFFFEBC2),
    onTertiaryContainer = Color(0xFF3A2600),
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2EBE2),
    onSurfaceVariant = Color(0xFF5B6669),
    surfaceTint = Teal700,
    outline = Color(0xFFC9BFB3),
    outlineVariant = Color(0xFFE6DDD2),
    error = Color(0xFFD92D20),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0DC),
    onErrorContainer = Color(0xFF410002),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFDFA),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF6F0E8),
    surfaceContainerHighest = Color(0xFFF2EBE2),
)

private val DarkColors = darkColorScheme(
    primary = Teal300,
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF0B4F4A),
    onPrimaryContainer = Color(0xFFCDF3EC),
    secondary = Color(0xFFFF9B7E),
    onSecondary = Color(0xFF4A1706),
    secondaryContainer = Color(0xFF6B2A15),
    onSecondaryContainer = Color(0xFFFFE2D8),
    tertiary = Color(0xFFFFC857),
    onTertiary = Color(0xFF3A2600),
    tertiaryContainer = Color(0xFF5A3F00),
    onTertiaryContainer = Color(0xFFFFEBC2),
    background = Color(0xFF0F1B1C),
    onBackground = Color(0xFFE6EEEE),
    surface = Color(0xFF162526),
    onSurface = Color(0xFFE6EEEE),
    surfaceVariant = Color(0xFF213536),
    onSurfaceVariant = Color(0xFFB4C4C5),
    surfaceTint = Teal300,
    outline = Color(0xFF5B7274),
    outlineVariant = Color(0xFF2B4143),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0C1617),
    surfaceContainerLow = Color(0xFF132123),
    surfaceContainer = Color(0xFF162526),
    surfaceContainerHigh = Color(0xFF1C2E30),
    surfaceContainerHighest = Color(0xFF213536),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun VetPocketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Динамические цвета Material You намеренно не используются — у приложения собственная узнаваемая палитра.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
