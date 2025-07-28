// File: com.retrivedmods.wclient.game.command.impl.SledCommand.kt
package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.module.visual.TrackingModule // Импортируйте ваш новый модуль

class SledCommand : Command("sled", "sl") { // Команда ".sled" или ".sl"

    override fun exec(args: Array<String>, session: GameSession) {
        val trackingModule = ModuleManager.getModule<TrackingModule>() // Получаем ваш модуль
        if (trackingModule == null) {
            session.displayClientMessage("§c[Слежка] Модуль TrackingModule не найден.")
            return
        }

        if (args.isEmpty()) {
            // Если аргументов нет, то либо выключаем, либо показываем использование
            if (trackingModule.isEnabled) {
                trackingModule.isEnabled = false // Отключаем слежку
            } else {
                session.displayClientMessage("§eИспользование: §7.sled <ник> | .sled off")
            }
            return
        }

        when (args[0].lowercase()) {
            "off" -> {
                trackingModule.isEnabled = false
            }
            else -> {
                val targetName = args[0]
                trackingModule.setTarget(session, targetName) // Устанавливаем цель и включаем модуль
            }
        }
    }
}
