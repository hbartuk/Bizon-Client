package com.retrivedmods.wclient.game

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.retrivedmods.wclient.R

enum class ModuleCategory(
    @DrawableRes val iconResId: Int,
    @StringRes val labelResId: Int
) {

    Combat(
        iconResId = R.drawable.swords_24px,
        labelResId = R.string.combat
    ),
    Motion(
        iconResId = R.drawable.sprint_24px,
        labelResId = R.string.motion
    ),
    Visual(
        iconResId = R.drawable.view_in_ar_24px,
        labelResId = R.string.visual
    ),
    Player(
        iconResId = R.drawable.baseline_emoji_people_24,
        labelResId = R.string.player
    ),
    World(
        iconResId = R.drawable.baseline_cloudy_snowing_24,
        labelResId = R.string.world
    ),
    Misc(
        iconResId = R.drawable.toc_24px,
        labelResId = R.string.misc
    ),
    Config(
        iconResId = R.drawable.manufacturing_24px,
        labelResId = R.string.config
    )

}
