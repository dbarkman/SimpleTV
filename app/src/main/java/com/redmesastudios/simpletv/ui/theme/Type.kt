package com.redmesastudios.simpletv.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.redmesastudios.simpletv.R

// Manrope is a variable font; each weight pins the wght axis.
@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

// Sizes are the design's 1920x1080 px values halved (the stick runs at 320 dpi).
object SimpleTVType {
    val greeting = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = (-0.25).sp, color = Ink)
    val clock = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 30.sp, letterSpacing = (-0.25).sp, color = Ink)
    val date = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = InkSoft)
    val tile = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 18.sp)
    val hint = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = InkSoft)
    val okBadge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 7.5.sp, color = Ink)

    // Folders
    val serviceName = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
    val button = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    val heroTitle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp, color = Ink)
    val heroMeta = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkSoft)
    val heroBody = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, lineHeight = 19.5.sp, color = Ink)
    val rowHeading = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
    val cardTitle = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Ink)
    val cardNote = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, color = InkSoft)
    val rank = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 66.sp, lineHeight = 56.sp, letterSpacing = (-3).sp, color = RankInk)
}
