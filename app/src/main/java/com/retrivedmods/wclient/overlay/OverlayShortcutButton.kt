package com.retrivedmods.wclient.overlay

import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.util.translatedSelf
import kotlin.math.min

class OverlayShortcutButton(
    private val module: Module
) : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            windowAnimations = android.R.style.Animation_Toast
            x = module.shortcutX
            y = module.shortcutY
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val width = context.resources.displayMetrics.widthPixels
        val height = context.resources.displayMetrics.heightPixels
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Цвета для активного состояния
        val activeColorStart = Color(0xFF00FF88) // Ярко-зеленый
        val activeColorEnd = Color(0xFF0088FF)   // Ярко-синий

        // Цвета для неактивного состояния
        val inactiveColorStart = Color(0xFF202020) // Темно-серый
        val inactiveColorEnd = Color(0xFF101010)   // Еще темнее серый

        // Анимированные цвета фона (для создания градиента)
        val animatedBgColor1 by animateColorAsState(
            targetValue = if (module.isEnabled) activeColorStart.copy(alpha = 0.5f) else inactiveColorStart,
            animationSpec = tween(durationMillis = 300),
            label = "animatedBgColor1"
        )
        val animatedBgColor2 by animateColorAsState(
            targetValue = if (module.isEnabled) activeColorEnd.copy(alpha = 0.5f) else inactiveColorEnd,
            animationSpec = tween(durationMillis = 300),
            label = "animatedBgColor2"
        )

        // Анимированный цвет рамки (для активного состояния будет градиентная)
        val targetBorderColor = if (module.isEnabled) activeColorStart else Color.Transparent
        val borderColor by animateColorAsState(
            targetValue = targetBorderColor,
            animationSpec = tween(durationMillis = 300),
            label = "borderColor"
        )

        // Анимированный цвет текста
        val targetTextColor = if (module.isEnabled) Color.White else Color(0xFFBBBBBB)
        val textColor by animateColorAsState(
            targetValue = targetTextColor,
            animationSpec = tween(durationMillis = 300),
            label = "textColor"
        )

        // Анимация свечения для активной кнопки
        val infiniteTransition = rememberInfiniteTransition(label = "shimmerTransition")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "glowAlpha"
        )
        val glowColor = if (module.isEnabled) activeColorStart.copy(alpha = glowAlpha) else Color.Transparent

        LaunchedEffect(isLandscape) {
            _layoutParams.x = min(width, _layoutParams.x)
            _layoutParams.y = min(height, _layoutParams.y)
            windowManager.updateViewLayout(composeView, _layoutParams)
            updateShortcut()
        }

        Box(
            modifier = Modifier
                .width(120.dp)
                .height(50.dp)
                .padding(4.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        _layoutParams.x += dragAmount.x.toInt()
                        _layoutParams.y += dragAmount.y.toInt()
                        windowManager.updateViewLayout(composeView, _layoutParams)
                        updateShortcut()
                    }
                }
                .shadow(
                    elevation = if (module.isEnabled) 12.dp else 6.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = if (module.isEnabled) activeColorEnd.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f),
                    spotColor = if (module.isEnabled) activeColorEnd.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.5f)
                )
                .background(
                    brush = if (module.isEnabled) {
                        Brush.horizontalGradient(
                            colors = listOf(animatedBgColor1, animatedBgColor2)
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(animatedBgColor1, animatedBgColor2)
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 2.5.dp,
                    brush = if (module.isEnabled) {
                        Brush.horizontalGradient(
                            colors = listOf(activeColorStart, activeColorEnd)
                        )
                    } else {
                        SolidColor(borderColor.copy(alpha = 0.3f))
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { module.isEnabled = !module.isEnabled }
        ) {
            // Слой для свечения активной кнопки
            if (module.isEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = glowColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = module.name.translatedSelf,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }

    private fun updateShortcut() {
        module.shortcutX = _layoutParams.x
        module.shortcutY = _layoutParams.y
    }
}
