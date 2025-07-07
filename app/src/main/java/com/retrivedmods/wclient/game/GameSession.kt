// File: app/src/main/java/com/retrivedmods/wclient/game/GameSession.kt
package com.retrivedmods.wclient.game

import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.world.Level
import com.mucheng.mucute.relay.MuCuteRelaySession
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import com.retrivedmods.wclient.game.module.Module // <-- НОВЫЙ ИМПОРТ: Убедитесь, что это правильный путь к вашему базовому классу Module

@Suppress("MemberVisibilityCanBePrivate")
class GameSession(val muCuteRelaySession: MuCuteRelaySession) : ComposedPacketHandler {

    val localPlayer = LocalPlayer(this)

    val level = Level(this)

    private val versionName by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContext.instance.packageManager.getPackageInfo(
            AppContext.instance.packageName, 0
        ).versionName
    }

    fun clientBound(packet: BedrockPacket) {
        muCuteRelaySession.clientBound(packet)
    }

    fun serverBound(packet: BedrockPacket) {
        muCuteRelaySession.serverBound(packet)
    }

    override fun beforePacketBound(packet: BedrockPacket): Boolean {
        localPlayer.onPacketBound(packet)
        level.onPacketBound(packet)

        val interceptablePacket = InterceptablePacket(packet)

        // --- Обработка пакетов модулями (этот блок остаётся) ---
        for (module in ModuleManager.modules) {
            // module.session = this // Устанавливаем сессию для каждого модуля. Если модуль получает сессию через конструктор, эта строка не нужна.
            module.beforePacketBound(interceptablePacket)
            if (interceptablePacket.isIntercepted) {
                return true
            }
        }

        // --- БЛОК ОБРАБОТКИ КОМАНД, КОТОРЫЙ БЫЛ ЗДЕСЬ, УДАЛЁН! ---
        // Теперь он находится в CommandHandlerModule.kt

        return false
    }

    override fun afterPacketBound(packet: BedrockPacket) {
        for (module in ModuleManager.modules) {
            module.afterPacketBound(packet)
        }
    }

    override fun onDisconnect(reason: String) {
        localPlayer.onDisconnect()
        level.onDisconnect()

        for (module in ModuleManager.modules) {
            module.onDisconnect(reason)
        }
    }

    fun displayClientMessage(message: String, type: TextPacket.Type = TextPacket.Type.RAW) {
        val textPacket = TextPacket()
        textPacket.type = type
        textPacket.isNeedsTranslation = false
        textPacket.sourceName = ""
        textPacket.message = message
        textPacket.xuid = ""
        textPacket.platformChatId = ""
        textPacket.filteredMessage = ""
        clientBound(textPacket)
    }

    ---

    ## Добавленный метод `getModule`

    Этот метод позволяет получить экземпляр любого зарегистрированного модуля, если он является подтипом `Module`.

    ```kotlin
    fun <T : Module> getModule(moduleClass: Class<T>): T? {
        // Мы итерируемся по списку модулей из ModuleManager.
        // Предполагается, что ModuleManager.modules является доступным списком ваших активных модулей.
        for (module in ModuleManager.modules) {
            if (moduleClass.isInstance(module)) {
                return moduleClass.cast(module) // Возвращаем найденный модуль, приведенный к нужному типу.
            }
        }
        return null // Модуль не найден.
    }
    ```
}
