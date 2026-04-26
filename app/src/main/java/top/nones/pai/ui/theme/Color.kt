package top.nones.pai.ui.theme

import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// --- Color Palette (M3 Inspired) ---
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF), // Purple 80
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFFE1D8FF),
    onPrimaryContainer = Color(0xFF381E72),
    secondary = Color(0xFFB4B7FF), // Purple Grey 80
    onSecondary = Color(0xFF342F5D),
    secondaryContainer = Color(0xFFE6E1FF),
    onSecondaryContainer = Color(0xFF342F5D),
    tertiary = Color(0xFFF50057), // Pink 80
    onTertiary = Color(0xFFFFC8D9),
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF49002B),
    background = Color(0xFF1C1B1F), // Dark BG
    onBackground = Color.White,
    surface = Color(0xFF1C1B1F), // Dark Surface
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3C3B41),
    onSurfaceVariant = Color(0xFFE4E1E7),
    outline = Color(0xFF938C9F),
    scrim = Color(0xFF000000),
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4), // Purple 40
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1D8FF),
    onPrimaryContainer = Color(0xFF381E72),
    secondary = Color(0xFF625b71), // Purple Grey 40
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E1FF),
    onSecondaryContainer = Color(0xFF342F5D),
    tertiary = Color(0xFFFF98C8), // Pink 40
    onTertiary = Color(0xFF49002B),
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF49002B),
    background = Color(0xFFFBF8F1), // Light BG
    onBackground = Color.Black,
    surface = Color(0xFFFBF8F1), // Light Surface
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF4A444A),
    outline = Color(0xFF79747E),
    scrim = Color(0xFF000000),
)

