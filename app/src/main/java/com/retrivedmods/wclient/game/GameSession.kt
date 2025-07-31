package com.retrivedmods.wclient.game

import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.world.Level
import com.mucheng.mucute.relay.MuCuteRelaySession // Используем этот импорт
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import android.util.Log // Используем Log для отладки

@Suppress("MemberVisibilityCanBePrivate")
class GameSession(
    // Здесь должен быть ваш класс прокси-сессии
    val muCuteRelaySession: MuCuteRelaySession,
    val context: Context
) : ComposedPacketHandler { // Убрал IProxyPacketListener, так как вы его не реализовывали полностью

    val localPlayer = LocalPlayer(this)
    val level = Level(this)

    private val versionName by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    /**
     * Отправляет пакет от прокси к клиенту.
     */
    fun clientBound(packet: BedrockPacket) {
        muCuteRelaySession.clientBound(packet)
    }

    /**
     * Отправляет пакет от прокси к серверу.
     */
    fun serverBound(packet: BedrockPacket) {
        muCuteRelaySession.serverBound(packet)
    }

    /**
     * Обрабатывает пакеты перед их отправкой.
     * Возвращает `true`, если пакет был перехвачен и не должен идти дальше.
     */
    override fun beforePacketBound(packet: BedrockPacket): Boolean {
        // Уведомляем другие части системы о пакете
        localPlayer.onPacketBound(packet)
        level.onPacketBound(packet)

        val interceptablePacket = InterceptablePacket(packet)

        // Передаем пакет всем модулям для обработки
        for (module in ModuleManager.modules) {
            module.beforePacketBound(interceptablePacket)
            if (interceptablePacket.isIntercepted) {
                // Если какой-либо модуль перехватил пакет, не передаем его дальше
                Log.d("GameSession", "Packet ${packet.javaClass.simpleName} intercepted by a module.")
                return true
            }
        }

        // Если ни один модуль не перехватил пакет, продолжаем обработку.
        // false означает, что пакет не был перехвачен.
        return false
    }

    /**
     * Обрабатывает пакеты после их отправки.
     */
    override fun afterPacketBound(packet: BedrockPacket) {
        // Уведомляем модули о пакете
        for (module in ModuleManager.modules) {
            module.afterPacketBound(packet)
        }
    }

    /**
     * Обрабатывает отключение от сервера.
     */
    override fun onDisconnect(reason: String) {
        localPlayer.onDisconnect()
        level.onDisconnect()

        // Уведомляем модули об отключении
        for (module in ModuleManager.modules) {
            module.onDisconnect(reason)
        }
        Log.i("GameSession", "Отключено. Причина: $reason")
    }

    /**
     * Отправляет сообщение во внутриигровой чат клиента.
     */
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
}
