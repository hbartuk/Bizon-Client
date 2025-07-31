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

        // Анимированные цвета для активного состояния (более живой градиент)
        val activeColorStart = Color(0xFF00FF88) // Ярко-зеленый
        val activeColorEnd = Color(0xFF0088FF)   // Ярко-синий

        // Анимированные цвета для неактивного состояния (более темный, но с небольшим блеском)
        val inactiveColorStart = Color(0xFF202020) // Темно-серый
        val inactiveColorEnd = Color(0xFF101010)   // Еще темнее серый

        val targetBorderColor = if (module.isEnabled) activeColorStart else Color.Transparent
        val borderColor by animateColorAsState(
            targetValue = targetBorderColor,
            animationSpec = tween(durationMillis = 300),
            label = "borderColor"
        )

        val targetTextColor = if (module.isEnabled) Color.White else Color(0xFFBBBBBB) // Белый для активных, светло-серый для неактивных
        val textColor by animateColorAsState(
            targetValue = targetTextColor,
            animationSpec = tween(durationMillis = 300),
            label = "textColor"
        )

        // Градиент для фона кнопки
        val backgroundBrush by animateBrushAsState(
            targetValue = if (module.isEnabled) {
                Brush.horizontalGradient(
                    colors = listOf(activeColorStart.copy(alpha = 0.5f), activeColorEnd.copy(alpha = 0.5f))
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(inactiveColorStart, inactiveColorEnd)
                )
            },
            animationSpec = tween(durationMillis = 300),
            label = "backgroundBrush"
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
                .width(120.dp) // Чуть шире кнопка
                .height(50.dp) // Чуть выше кнопка
                .padding(4.dp) // Меньший внутренний отступ для большей области кнопки
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        _layoutParams.x += dragAmount.x.toInt()
                        _layoutParams.y += dragAmount.y.toInt()
                        windowManager.updateViewLayout(composeView, _layoutParams)
                        updateShortcut()
                    }
                }
                .shadow(
                    elevation = if (module.isEnabled) 12.dp else 6.dp, // Более выраженная тень для активных
                    shape = RoundedCornerShape(16.dp), // Более скругленные углы тени
                    ambientColor = if (module.isEnabled) activeColorEnd.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f), // Цвет тени
                    spotColor = if (module.isEnabled) activeColorEnd.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.5f)
                )
                .background(backgroundBrush, shape = RoundedCornerShape(16.dp)) // Более скругленные углы кнопки
                .border(
                    width = 2.5.dp, // Более толстая рамка
                    brush = if (module.isEnabled) { // Градиентная рамка для активной
                        Brush.horizontalGradient(
                            colors = listOf(
                                activeColorStart,
                                activeColorEnd
                            )
                        )
                    } else {
                        SolidColor(borderColor.copy(alpha = 0.3f)) // Более приглушенная рамка для неактивных
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
                            color = glowColor, // Свечение
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
                    fontSize = 14.sp, // Чуть больше шрифт
                    fontWeight = FontWeight.SemiBold, // Жирный шрифт
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 0.5.sp // Небольшой межбуквенный интервал
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

// Помощник для анимации Brush (Compose 1.6+), если нет - нужен свой.
@Composable
fun animateBrushAsState(
    targetValue: Brush,
    animationSpec: AnimationSpec<Brush> = spring(),
    finishedListener: ((Brush) -> Unit)? = null,
    label: String = "BrushAnimation"
): State<Brush> {
    val currentBrush = remember { mutableStateOf(targetValue) }
    if (currentBrush.value == targetValue) {
        return currentBrush // No animation needed
    }

    val anim = remember(targetValue) {
        Animatable(currentBrush.value, Brush.VectorConverter)
    }

    LaunchedEffect(targetValue) {
        anim.animateTo(targetValue, animationSpec) {
            currentBrush.value = value
        }
        finishedListener?.invoke(targetValue)
    }

    return currentBrush
}

// Вам может понадобиться VectorConverter для Brush, если ваш Compose не поддерживает его "из коробки"
// Если у вас более старая версия Compose, вам может потребоваться реализовать
// Custom AnimationVectorConverter для Brush.
// Если возникнет ошибка "Unresolved reference: VectorConverter",
// раскомментируйте или создайте подобный класс:
/*
object BrushVectorConverter : TwoWayConverter<Brush, AnimationVector4D> {
    override val convertFromVector: (AnimationVector4D) -> Brush = { vector ->
        if (vector.v1 == 0f && vector.v2 == 0f && vector.v3 == 0f && vector.v4 == 0f) {
            SolidColor(Color.Transparent)
        } else {
            // Это упрощенная конвертация, для полноценной обработки разных типов Brush (Linear, Radial, Sweep)
            // понадобится более сложная логика. Для градиента двух цветов можно анимировать цвета.
            // Здесь мы предполагаем, что это в основном SolidColor или двухцветный градиент.
            // Для более сложной анимации Brush, возможно, придется анимировать отдельные Color,
            // а затем создавать Brush из них.
            val color1 = Color(vector.v1)
            val color2 = Color(vector.v2) // Предполагаем, что v2 хранит второй цвет для градиента
            if (color2 != Color.Transparent) {
                LinearGradient(color1, color2, Offset.Zero, Offset.Infinite)
            } else {
                SolidColor(color1)
            }
        }
    }

    override val convertToVector: (Brush) -> AnimationVector4D = { brush ->
        when (brush) {
            is SolidColor -> AnimationVector4D(
                brush.value.red,
                brush.value.green,
                brush.value.blue,
                brush.value.alpha
            )
            is LinearGradient -> {
                // Очень упрощено, нужно больше данных, чтобы полностью представить градиент в 4D векторе
                AnimationVector4D(
                    brush.colors[0].red,
                    brush.colors[0].green,
                    brush.colors[0].blue,
                    brush.colors[0].alpha // Можно использовать alpha для первого цвета
                    // Для второго цвета или других параметров градиента потребуется больше измерений
                )
            }
            else -> AnimationVector4D(0f, 0f, 0f, 0f) // По умолчанию для неподдерживаемых типов
        }
    }
}
*/
