// File: com.retrivedmods.wclient.game.command.impl.ToggleSoundCommand.kt
package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.ModuleManager 
import com.retrivedmods.wclient.game.module.misc.ToggleSoundModule

class ToggleSoundCommand : Command("togglesound", "ts") { 

    override fun exec(args: Array<String>, session: GameSession) {
        println("DEBUG: ToggleSoundCommand.exec() called. Args: ${args.joinToString(" ")}")

        if (args.isEmpty()) {
            session.displayClientMessage("§e=== ToggleSound Command Help ===")
            session.displayClientMessage("§7.togglesound on/off §8- Включить/выключить звуки")
            session.displayClientMessage("§7.togglesound celestial §8- Режим Celestial")
            session.displayClientMessage("§7.togglesound nursultan §8- Режим Nursultan")
            session.displayClientMessage("§7.togglesound smooth §8- Режим Smooth")
            session.displayClientMessage("§7.togglesound status §8- Показать статус")
            session.displayClientMessage("§7.togglesound test §8- Тест всех режимов")
            session.displayClientMessage("§7.togglesound play §8- Воспроизвести звук")
            session.displayClientMessage("§7.togglesound notify §8- Звук уведомления")
            return
        }

        val toggleSoundModule = ModuleManager.getModule<ToggleSoundModule>()
        if (toggleSoundModule == null) {
            session.displayClientMessage("§c[ToggleSoundCommand] Модуль ToggleSoundModule не найден!")
            println("ERROR: ToggleSoundModule is null в ToggleSoundCommand")
            return
        }

        when (args[0].lowercase()) {
            "on", "enable" -> {
                if (!toggleSoundModule.isEnabled) {
                    toggleSoundModule.isEnabled = true
                    session.displayClientMessage("§a[ToggleSound] Модуль включен!")
                } else {
                    session.displayClientMessage("§e[ToggleSound] Модуль уже включен.")
                }
            }

            "off", "disable" -> {
                if (toggleSoundModule.isEnabled) {
                    toggleSoundModule.isEnabled = false
                    session.displayClientMessage("§c[ToggleSound] Модуль выключен!")
                } else {
                    session.displayClientMessage("§e[ToggleSound] Модуль уже выключен.")
                }
            }

            "celestial" -> {
                if (!toggleSoundModule.isEnabled) {
                    toggleSoundModule.isEnabled = true
                    session.displayClientMessage("§a[ToggleSound] Модуль автоматически включен.")
                }
                toggleSoundModule.toggleCelestial()
                println("DEBUG: Switched to Celestial mode")
            }

            "nursultan" -> {
                if (!toggleSoundModule.isEnabled) {
                    toggleSoundModule.isEnabled = true
                    session.displayClientMessage("§a[ToggleSound] Модуль автоматически включен.")
                }
                toggleSoundModule.toggleNursultan()
                println("DEBUG: Switched to Nursultan mode")
            }

            "smooth" -> {
                if (!toggleSoundModule.isEnabled) {
                    toggleSoundModule.isEnabled = true
                    session.displayClientMessage("§a[ToggleSound] Модуль автоматически включен.")
                }
                toggleSoundModule.toggleSmooth()
                println("DEBUG: Switched to Smooth mode")
            }

            "status", "info" -> {
                session.displayClientMessage("§e=== ToggleSound Status ===")
                session.displayClientMessage("§7Модуль: §${if (toggleSoundModule.isEnabled) "aВключен" else "cВыключен"}")
                session.displayClientMessage("§7Звуки: §${if (toggleSoundModule.isSoundsEnabled()) "aАктивны" else "cНеактивны"}")
                session.displayClientMessage("§7Режим: §b${toggleSoundModule.getCurrentMode()}")
                
                val currentSounds = toggleSoundModule.getCurrentSoundSet()
                if (currentSounds.isNotEmpty()) {
                    session.displayClientMessage("§7Звуки режима:")
                    currentSounds.chunked(3).forEach { chunk ->
                        val line = chunk.joinToString("§7, §b") { "§b$it" }
                        session.displayClientMessage("  §7$line")
                    }
                }
            }

            "test" -> {
                if (!toggleSoundModule.isEnabled) {
                    toggleSoundModule.isEnabled = true
                    session.displayClientMessage("§a[ToggleSound] Модуль автоматически включен для теста.")
                }
                
                session.displayClientMessage("§e[ToggleSound] Запускаю тест всех режимов...")
                println("DEBUG: Starting ToggleSound test")
                toggleSoundModule.testAllModes()
            }

            "play" -> {
                if (!toggleSoundModule.isEnabled) {
                    session.displayClientMessage("§c[ToggleSound] Модуль выключен! Используй: .ts on")
                    return
                }
                
                toggleSoundModule.playToggleSound()
                session.displayClientMessage("§a[ToggleSound] Воспроизвожу звук текущего режима...")
                println("DEBUG: Playing toggle sound")
            }

            "notify", "notification" -> {
                if (!toggleSoundModule.isEnabled) {
                    session.displayClientMessage("§c[ToggleSound] Модуль выключен! Используй: .ts on")
                    return
                }
                
                toggleSoundModule.playNotificationSound()
                session.displayClientMessage("§a[ToggleSound] Звук уведомления отправлен!")
                println("DEBUG: Playing notification sound")
            }

            "debug" -> {
                // Отладочная информация
                session.displayClientMessage("§e=== ToggleSound Debug ===")
                session.displayClientMessage("§7Модуль найден: §atrue")
                session.displayClientMessage("§7Модуль включен: §b${toggleSoundModule.isEnabled}")
                session.displayClientMessage("§7Звуки активны: §b${toggleSoundModule.isSoundsEnabled()}")
                session.displayClientMessage("§7Текущий режим: §b${toggleSoundModule.getCurrentMode()}")
                session.displayClientMessage("§7Игрок: §b${session.localPlayer?.displayName ?: "null"}")
                
                // Тестовый звук
                if (toggleSoundModule.isEnabled) {
                    toggleSoundModule.playToggleSound()
                    session.displayClientMessage("§7Тестовый звук отправлен!")
                }
            }

            else -> {
                session.displayClientMessage("§c[ToggleSound] Неизвестная команда: §7${args[0]}")
                session.displayClientMessage("§7Используй: §e.ts §7для справки")
            }
        }
    }
}
