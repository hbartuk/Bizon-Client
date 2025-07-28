package com.retrivedmods.wclient.game

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.retrivedmods.wclient.R

enum class ModuleCategory(
    @DrawableRes val iconResId: Int,
    @StringRes val labelResId: Int
) {

    Для пвп(
        iconResId = R.drawable.swords_24px,
        labelResId = R.string.combat
    ),
    Передвижение(
        iconResId = R.drawable.sprint_24px,
        labelResId = R.string.motion
    ),
    Визуалка(
        iconResId = R.drawable.view_in_ar_24px,
        labelResId = R.string.visual
    ),
    Игрок(
        iconResId = R.drawable.baseline_emoji_people_24,
        labelResId = R.string.player
    ),
    Мир(
        iconResId = R.drawable.baseline_cloudy_snowing_24,
        labelResId = R.string.world
    ),
    Разное(
        iconResId = R.drawable.toc_24px,
        labelResId = R.string.misc
    ),
    Конфиг(
        iconResId = R.drawable.manufacturing_24px,
        labelResId = R.string.config
    )

}
