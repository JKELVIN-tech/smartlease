package com.smartlease.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Design tokens from the SmartLease site (styles.css). */
object Col {
    val background = Color(0xFFF5EEE2)
    val ink = Color(0xFF241C15)
    val card = Color(0xFFFDFAF3)
    val primary = Color(0xFFA94620)
    val secondary = Color(0xFFECE0CE)
    val mutedFg = Color(0xFF847561)
    val border = Color(0xFFE3D8C3)
    val paid = Color(0xFF3D6646)
    val paidSoft = Color(0xFFDFECDD)
    val warn = Color(0xFFA97416)
    val warnSoft = Color(0xFFF4E6CA)
    val accent = primary.copy(alpha = 0.1f)
    val chart1 = primary
    val chart2 = paid
    val chart3 = warn
    val chart4 = mutedFg
    val chart5 = ink
}

private val Scheme = lightColorScheme(
    primary = Col.primary, onPrimary = Col.card,
    secondary = Col.secondary, onSecondary = Col.ink,
    background = Col.background, onBackground = Col.ink,
    surface = Col.card, onSurface = Col.ink,
    surfaceVariant = Col.secondary, onSurfaceVariant = Col.mutedFg,
    outline = Col.border, error = Col.primary
)

@Composable
fun SmartLeaseTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)
