package com.retrivedmods.wclient.overlay

import androidx.compose.foundation.clickable
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

        // Изменены цвета для Bizon Client
        val enabledColor = Color(0xFF00BFFF) // Небесно-голубой для включенного состояния
        val disabledColor = Color(0xFFFFFFFF).copy(alpha = 0.6f) // Немного приглушенный белый для выключенного состояния

        val borderColor by animateColorAsState(
            targetValue = if (module.isEnabled) enabledColor else Color.Transparent, // Прозрачный для выключенного, синий для включенного
            label = "borderColor"
        )

        val backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A1A1A), // Верхний цвет фона кнопки, чуть темнее
                Color(0xFF0D0D0D)  // Нижний цвет фона кнопки, очень темный
            )
        )

        val textColor by animateColorAsState(
            targetValue = if (module.isEnabled) enabledColor else disabledColor, // Текст синий для включенного, приглушенный белый для выключенного
            label = "textColor"
        )

        LaunchedEffect(isLandscape) {
            _layoutParams.x = min(width, _layoutParams.x)
            _layoutParams.y = min(height, _layoutParams.y)
            windowManager.updateViewLayout(composeView, _layoutParams)
            updateShortcut()
        }

        Box(
            modifier = Modifier
                .width(110.dp)
                .height(44.dp)
                .padding(6.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        _layoutParams.x += dragAmount.x.toInt()
                        _layoutParams.y += dragAmount.y.toInt()
                        windowManager.updateViewLayout(composeView, _layoutParams)
                        updateShortcut()
                    }
                }
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .background(backgroundGradient, shape = RoundedCornerShape(14.dp))
                .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                .clickable { module.isEnabled = !module.isEnabled }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = module.name.translatedSelf, // Текст модуля (он уже переведен, если используется `translatedSelf`)
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }

    private fun updateShortcut() {
        module.shortcutX = _layoutParams.x
        module.shortcutY = _layoutParams.y
    }
}
