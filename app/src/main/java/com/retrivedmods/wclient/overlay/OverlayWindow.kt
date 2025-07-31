package com.retrivedmods.wclient.overlay

import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.input.InputManager
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CoroutineScope

@Suppress("MemberVisibilityCanBePrivate")
abstract class OverlayWindow {

    open val layoutParams by lazy {
        LayoutParams().apply {
            width = LayoutParams.WRAP_CONTENT
            height = LayoutParams.WRAP_CONTENT
            gravity = Gravity.START or Gravity.TOP // Позволяет располагать окно в любой точке экрана
            x = 0 // Начальная позиция по X
            y = 0 // Начальная позиция по Y

            // Основной тип окна для оверлея
            type = LayoutParams.TYPE_APPLICATION_OVERLAY

            // Флаги для "премиального" поведения:
            // FLAG_NOT_FOCUSABLE: Окно не получает фокус, клики проходят сквозь него, если не обработаны явно.
            // FLAG_LAYOUT_IN_SCREEN: Позволяет окну занимать всю область экрана, включая системные бары.
            // FLAG_NOT_TOUCH_MODAL: Позволяет событиям касания "проходить" сквозь незанятые области окна.
            // FLAG_WATCH_OUTSIDE_TOUCH: Если нужно отслеживать касания вне окна (например, для закрытия).
            // FLAG_SCALED: Окно масштабируется, что может быть полезно для разных разрешений.
            flags = LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    LayoutParams.FLAG_NOT_TOUCH_MODAL or // Добавлено для лучшего взаимодействия
                    LayoutParams.FLAG_SCALED // Добавлено для масштабирования

            // Формат пикселей: TRANSLUCENT для поддержки полупрозрачности
            format = PixelFormat.TRANSLUCENT

            // Анимации для появления/исчезновения окна
            windowAnimations = android.R.style.Animation_Toast // Легкая анимация появления/исчезновения
            // Или можно использовать более выразительные: android.R.style.Animation_Dialog

            // Затемнение фона (для эффекта диалога или затемнения)
            // Увеличим затемнение для более выраженного эффекта
            dimAmount = 0.6f // От 0.0 (без затемнения) до 1.0 (полностью черный фон)

            // Размытие фона (только для Android S и выше)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Если поддерживается, используем максимальную непрозрачность для касаний
                alpha = (OverlayManager.currentContext!!.getSystemService(Service.INPUT_SERVICE) as? InputManager)?.maximumObscuringOpacityForTouch
                    ?: 0.9f // Чуть менее прозрачный по умолчанию
                blurBehindRadius = 40 // Увеличим радиус размытия для более сильного эффекта
            } else {
                // Для более старых версий, где нет размытия, можно настроить только alpha
                alpha = 0.9f // Общая непрозрачность окна
            }

            // Обработка вырезов экрана (notch)
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    // Использование ленивой инициализации для composeView
    // Убедимся, что OverlayManager.currentContext!! не null при доступе.
    open val composeView by lazy {
        ComposeView(OverlayManager.currentContext!!)
    }

    // WindowManager для управления окном
    val windowManager: WindowManager
        get() = OverlayManager.currentContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Жизненный цикл и ViewModelStore
    val lifecycleOwner = OverlayLifecycleOwner()

    val viewModelStore = ViewModelStore()

    // CoroutineScope и Recomposer для Compose
    val composeScope: CoroutineScope

    val recomposer: Recomposer

    // Флаг для первой инициализации
    var firstRun = true

    init {
        lifecycleOwner.performRestore(null)

        val coroutineContext = AndroidUiDispatcher.CurrentThread
        composeScope = CoroutineScope(coroutineContext)
        recomposer = Recomposer(coroutineContext)
    }

    @Composable
    abstract fun Content()

}
