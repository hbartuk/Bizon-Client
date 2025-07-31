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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradient
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
                .background(backgroundBrush, shape = RoundedCornerShape(16.dp))
                .border(
                    width = 2.5.dp,
                    brush = if (module.isEnabled) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                activeColorStart,
                                activeColorEnd
                            )
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

/**
 * Пользовательский TwoWayConverter для анимации Brush.
 * Этот конвертер упрощен и работает для SolidColor и LinearGradient с двумя цветами.
 * Для более сложных градиентов (например, с более чем двумя цветами, RadialGradient, SweepGradient)
 * потребуется более сложная логика конвертации.
 */
val BrushTwoWayConverter = TwoWayConverter<Brush, AnimationVector4D>(
    convertFromVector = { vector ->
        // Предполагаем, что v1 и v2 - это компоненты первого цвета, v3 и v4 - второго.
        // Или v1-v4 представляют собой параметры для создания SolidColor или LinearGradient.
        // Для упрощения, анимируем цвета градиента, если это LinearGradient.
        // Если вектор полностью нулевой или первый цвет прозрачный, возвращаем SolidColor(Transparent)
        if (vector.v1 == 0f && vector.v2 == 0f && vector.v3 == 0f && vector.v4 == 0f) {
            SolidColor(Color.Transparent)
        } else {
            val color1 = Color(vector.v1, vector.v2, vector.v3, vector.v4)
            // Мы не можем легко представить второй цвет в одном AnimationVector4D,
            // поэтому для градиента будем просто анимировать его как SolidColor
            // и создавать LinearGradient из двух SolidColor в Composable.
            // ИЛИ, если мы всегда знаем, что это двухцветный градиент,
            // мы можем использовать AnimationVector8D, но Compose не имеет встроенного.
            // Для этой задачи, где мы анимируем между двумя известными Brush,
            // достаточно просто анимировать компоненты цвета.

            // Важно: Эта реализация не идеально преобразует *любой* Brush из вектора.
            // Она работает, потому что мы анимируем между SolidColor и LinearGradient
            // с фиксированным количеством цветов, и можем "распаковать" их.
            // Здесь мы будем полагаться на то, что animateBrushAsState
            // правильно подает нам значения, которые можно интерпретировать как цвета.
            SolidColor(color1) // Вернем просто цвет, т.к. сложно анимировать Brush напрямую.
                               // Фактически, animateBrushAsState будет анимировать
                               // отдельные цвета и Offset для градиента, если это нужно.
        }
    },
    convertToVector = { brush ->
        when (brush) {
            is SolidColor -> AnimationVector4D(
                brush.value.red,
                brush.value.green,
                brush.value.blue,
                brush.value.alpha
            )
            is LinearGradient -> {
                // Очень упрощено. Для полноценной конвертации LinearGradient требуется больше параметров
                // (colors, start/end offset, tilemode). AnimationVector4D не хватает.
                // Для нашей текущей задачи (анимация между двумя конкретными Brush),
                // мы будем анимировать цвета, которые строят эти Brush.
                val color1 = brush.colors.getOrElse(0) { Color.Transparent }
                // Если градиент имеет только один цвет или его нет, можно сделать SolidColor
                // Для двухцветного градиента мы должны были бы кодировать оба цвета в векторе.
                AnimationVector4D(
                    color1.red,
                    color1.green,
                    color1.blue,
                    color1.alpha
                )
            }
            else -> AnimationVector4D(0f, 0f, 0f, 0f) // Для неподдерживаемых типов Brush
        }
    }
)

/**
 * Анимирует Brush между начальным и конечным значениями.
 * Использует пользовательский BrushTwoWayConverter.
 */
@Composable
fun animateBrushAsState(
    targetValue: Brush,
    animationSpec: AnimationSpec<Brush> = spring(),
    finishedListener: ((Brush) -> Unit)? = null,
    label: String = "BrushAnimation"
): State<Brush> {
    // В отличие от стандартного animate*AsState, который использует State<T>,
    // здесь мы хотим анимировать между двумя "известными" Brush.
    // Вместо прямого анимирования Brush, мы будем анимировать их компоненты (цвета),
    // а затем создавать Brush из этих анимированных компонентов.

    // Для текущей задачи (анимация между двумя конкретными градиентами:
    // SolidColor -> LinearGradient; LinearGradient -> SolidColor)
    // мы можем анимировать их цвета и затем создавать Brush.

    // Извлекаем цвета из текущего и целевого Brush.
    // Это упрощение, которое хорошо работает для градиентов из 1-2 цветов.
    val initialColors = (currentCompositeKeyHash.toString().hashCode() % 1000).let { hash ->
        // Простая эвристика для получения начального состояния, если оно не SolidColor.
        // Можно передавать начальный Brush явно.
        if (targetValue is SolidColor) listOf(targetValue.value)
        else if (targetValue is LinearGradient) targetValue.colors
        else listOf(Color.Transparent)
    }

    val targetColors = if (targetValue is SolidColor) listOf(targetValue.value)
                       else if (targetValue is LinearGradient) targetValue.colors
                       else listOf(Color.Transparent)

    val animatedColor1 by animateColorAsState(targetColors.getOrElse(0) { Color.Transparent }, animationSpec = tween(300))
    val animatedColor2 by animateColorAsState(targetColors.getOrElse(1) { Color.Transparent }, animationSpec = tween(300))


    // Собираем Brush из анимированных цветов.
    val resultBrush = remember(animatedColor1, animatedColor2) {
        if (targetValue is SolidColor) {
            SolidColor(animatedColor1)
        } else if (targetValue is LinearGradient && targetColors.size >= 2) {
            // Сохраняем направление градиента исходного targetValue
            LinearGradient(
                colors = listOf(animatedColor1, animatedColor2),
                start = targetValue.start,
                end = targetValue.end,
                tileMode = targetValue.tileMode
            )
        } else {
            SolidColor(animatedColor1) // Fallback
        }
    }

    // Возвращаем результат как State
    return rememberUpdatedState(resultBrush)
}
