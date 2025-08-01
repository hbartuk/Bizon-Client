// File: com.retrivedmods.wclient.game.command.impl.LagCommand.kt
package com.retrivedmods.wclient.game.command.impl

import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.module.misc.LagModule // ИСПРАВЛЕНО: Путь к модулю теперь 'misc'

class LagCommand : Command("lag", ".") {

    override fun exec(args: Array<String>, session: GameSession) {
        val lagModule = ModuleManager.getModule<LagModule>()

        if (lagModule == null) {
            session.displayClientMessage("§c[LagCommand] Модуль LagModule не найден. Убедитесь, что он включен и зарегистрирован.")
            return
        }

        if (args.isEmpty()) {
            session.displayClientMessage("§cИспользование: §7.lag <кол-во_действий/с> <время_сбора_с>")
            session.displayClientMessage("§eИли: §7.lag stop§e для остановки.")
            return
        }

        when (args[0].lowercase()) {
            "stop" -> {
                lagModule.stopCollecting()
                session.displayClientMessage("§a[LagCommand] Отправка пакетов остановлена.")
            }
            else -> {
                try {
                    val actionsPerSecond = args[0].toInt()
                    val collectionTime = args[1].toLong()

                    if (actionsPerSecond <= 0 || collectionTime <= 0) {
                        session.displayClientMessage("§c[LagCommand] Значения должны быть больше 0.")
                        return
                    }

                    lagModule.startCollecting(actionsPerSecond, collectionTime)
                    session.displayClientMessage("§a[LagCommand] Сбор пакетов начат. Отправка произойдет через §e$collectionTime§a секунд.")
                } catch (e: NumberFormatException) {
                    session.displayClientMessage("§c[LagCommand] Неверный формат чисел. Используйте целые числа.")
                } catch (e: ArrayIndexOutOfBoundsException) {
                    session.displayClientMessage("§c[LagCommand] Недостаточно аргументов. Используйте: .lag <кол-во> <время>")
                }
            }
        }
    }
}
