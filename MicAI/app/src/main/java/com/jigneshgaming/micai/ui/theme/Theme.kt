package com.jigneshgaming.micai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** JARVIS palette: deep space black with electric-blue accents. */
val JarvisBackground = Color(0xFF05070F)
val JarvisSurface = Color(0xFF0B1220)
val JarvisSurfaceVariant = Color(0xFF111A2E)
val JarvisPrimary = Color(0xFF00E5FF)
val JarvisSecondary = Color(0xFF2979FF)
val JarvisOnSurface = Color(0xFFE0F7FF)
val JarvisOnSurfaceVariant = Color(0xFF8FB8D8)
val JarvisError = Color(0xFFFF5252)
val JarvisSuccess = Color(0xFF69F0AE)

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisPrimary,
    secondary = JarvisSecondary,
    background = JarvisBackground,
    surface = JarvisSurface,
    surfaceVariant = JarvisSurfaceVariant,
    onPrimary = Color(0xFF001318),
    onBackground = JarvisOnSurface,
    onSurface = JarvisOnSurface,
    onSurfaceVariant = JarvisOnSurfaceVariant,
    error = JarvisError
)

@Composable
fun MicAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        content = content
    )
}

val JarvisCardShape = RoundedCornerShape(16.dp)
val JarvisPillShape = RoundedCornerShape(50)
