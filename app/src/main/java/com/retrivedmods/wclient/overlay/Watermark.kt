package com.retrivedmods.wclient.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI

@Composable
fun Watermark() {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        )
    )

    val rainbowColors = List(7) { i ->
        val hue = ((i * 360f / 7) + (phase * 180f / PI).toFloat()) % 360
        Color.hsv(hue, 1f, 1f)
    }

    val gradientBrush = Brush.horizontalGradient(rainbowColors)

    val richText: AnnotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)) {
            append("Alifa-test 48f82j37b")
        }
        withStyle(
            style = SpanStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                baselineShift = BaselineShift.Superscript
            )
        ) {
            append(" v0.1 alifa")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x00000000)) // semi-transparent dark background
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = richText,
                style = androidx.compose.ui.text.TextStyle(
                    brush = gradientBrush,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
