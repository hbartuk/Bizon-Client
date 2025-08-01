package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.module.misc.ChatIgnoreModule

class ChatIgnoreCommand : Command("chignore", "чиг", "chat") {

    override fun exec(args: Array<String>, session: GameSession) {
        val module = ModuleManager.getModule<ChatIgnoreModule>()
        if (module == null) {
            session.displayClientMessage("§c[ChatIgnoreCommand] Модуль ChatIgnore не найден!")
            return
        }

        when (args.getOrNull(0)?.lowercase()) {
            "add" -> {
                if (args.size < 2) {
                    session.displayClientMessage("§c[ChatIgnore] Используйте: .chignore add <текст>")
                    return
                }
                val itemToAdd = args.drop(1).joinToString(" ")
                module.addIgnoreItem(itemToAdd)
            }

            "remove" -> {
                if (args.size < 2) {
                    session.displayClientMessage("§c[ChatIgnore] Используйте: .chignore remove <текст>")
                    return
                }
                val itemToRemove = args.drop(1).joinToString(" ")
                module.removeIgnoreItem(itemToRemove)
            }
            
            "list" -> {
                val ignoredItems = module.getIgnoreList()
                if (ignoredItems.isEmpty()) {
                    session.displayClientMessage("§e[ChatIgnore] Список игнорируемых слов пуст.")
                } else {
                    session.displayClientMessage("§e[ChatIgnore] Список игнорируемых слов:")
                    ignoredItems.forEachIndexed { index, item ->
                        session.displayClientMessage("§7[${index + 1}] §b\"$item\"")
                    }
                }
            }

            "on" -> {
                module.isEnabled = true
                session.displayClientMessage("§a[ChatIgnore] Модуль включен.")
            }

            "off" -> {
                module.isEnabled = false
                session.displayClientMessage("§c[ChatIgnore] Модуль отключен.")
            }
            
            else -> {
                session.displayClientMessage("§e[ChatIgnore] Команды: add, remove, list, on, off")
            }
        }
    }
}
