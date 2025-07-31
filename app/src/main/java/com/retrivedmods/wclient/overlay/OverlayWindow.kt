package com.retrivedmods.wclient.overlay

import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.input.InputManager
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

// Внутренний класс для управления жизненным циклом Composable внутри оверлея
// Этот класс должен находиться внутри OverlayManager или быть отдельным файлом,
// но не внутри OverlayWindow, так как он используется OverlayManager'ом.
// Для удобства я оставлю его здесь, но лучше его вынести.
class OverlayLifecycleOwner : androidx.lifecycle.LifecycleOwner {
    private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
    private val job = Job()
    val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override val lifecycle: androidx.lifecycle.Lifecycle
        get() = lifecycleRegistry

    fun performRestore(savedState: android.os.Bundle?) {
        lifecycleRegistry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
    }

    fun handleLifecycleEvent(event: androidx.lifecycle.Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
        if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
            job.cancel()
        }
    }
}

@Suppress("MemberVisibilityCanBePrivate")
abstract class OverlayWindow {

    open val layoutParams by lazy {
        WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT // По умолчанию WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT // По умолчанию WRAP_CONTENT
            gravity = Gravity.START or Gravity.TOP
            x = 0
            y = 0

            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            // Ключевые изменения флагов для пропуска касаний и правильного поведения оверлея:
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or // Важно: касания вне окна проходят
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS // Позволяет выйти за пределы экрана
                    // FLAG_SCALED удален, так как редко нужен и может вызывать проблемы

            format = PixelFormat.TRANSLUCENT // Для прозрачности

            windowAnimations = android.R.style.Animation_Toast // Стандартные анимации

            // dimAmount и blurBehindRadius по умолчанию 0.0f и 0
            // OverlayClickGUI будет их переопределять.
            dimAmount = 0.0f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alpha = (OverlayManager.currentContext!!.getSystemService(Service.INPUT_SERVICE) as? InputManager)?.maximumObscuringOpacityForTouch
                    ?: 0.9f
                blurBehindRadius = 0 // По умолчанию нет размытия
            } else {
                alpha = 0.9f
            }

            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    // Использование ленивой инициализации для composeView
    open val composeView by lazy {
        ComposeView(OverlayManager.currentContext!!)
    }

    val windowManager: WindowManager
        get() = OverlayManager.currentContext!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val lifecycleOwner = OverlayLifecycleOwner()

    val viewModelStore = ViewModelStore()

    val composeScope: CoroutineScope

    val recomposer: Recomposer

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
