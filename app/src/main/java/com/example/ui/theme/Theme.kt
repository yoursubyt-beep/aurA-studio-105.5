package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AtelierGold,
    onPrimary = ObsidianNoir,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = AtelierGold,
    secondary = TerracottaRose,
    onSecondary = PureSilkWhite,
    secondaryContainer = ColorTokens.DarkTerracottaContainer,
    onSecondaryContainer = TerracottaSoft,
    tertiary = EmeraldStatus,
    onTertiary = PureSilkWhite,
    background = DarkBackdrop,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = CrimsonError,
    onError = PureSilkWhite
)

private val LightColorScheme = lightColorScheme(
    primary = ObsidianNoir,
    onPrimary = PureSilkWhite,
    primaryContainer = ChampagneTint,
    onPrimaryContainer = ObsidianNoir,
    secondary = AtelierGold,
    onSecondary = ObsidianNoir,
    secondaryContainer = WarmParchment,
    onSecondaryContainer = ObsidianNoir,
    tertiary = TerracottaRose,
    onTertiary = PureSilkWhite,
    tertiaryContainer = TerracottaSoft,
    onTertiaryContainer = TerracottaRose,
    background = IvoryLinen,
    onBackground = ObsidianNoir,
    surface = PureSilkWhite,
    onSurface = ObsidianNoir,
    surfaceVariant = WarmParchment,
    onSurfaceVariant = SlateMuted,
    outline = HairlineSand,
    error = CrimsonError,
    onError = PureSilkWhite
)

private object ColorTokens {
    val DarkTerracottaContainer = androidx.compose.ui.graphics.Color(0xFF47251C)
}

val AuraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AuraStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AuraShapes,
        content = content
    )
}
