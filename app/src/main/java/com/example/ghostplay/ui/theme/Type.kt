package com.example.ghostplay.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Sora and Geist font families (assuming default fallbacks if not added to res/font)
val Sora = FontFamily.Default
val Geist = FontFamily.Default

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.W800,
        fontSize = 48.sp,
        lineHeight = 52.8.sp,
        letterSpacing = (-0.02).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 43.2.sp,
        letterSpacing = (-0.01).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Sora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 31.2.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.8.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.6.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 16.8.sp,
        letterSpacing = 0.05.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Geist,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 12.sp
    )
)
