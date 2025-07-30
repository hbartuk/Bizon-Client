// File: app/src/main/java/com/retrivedmods/wclient/game/GameSession.kt
package com.retrivedmods.wclient.game

import android.content.Context // Импорт для Context
import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.world.Level
import com.mucheng.mucute.relay.MuCuteRelaySession
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
// Убедитесь, что здесь есть все остальные необходимые импорты

@Suppress("MemberVisibilityCanBePrivate")
class GameSession(
    val muCuteRelaySession: MuCuteRelaySession,
    val context: Context // Добавлен Context в конструктор
) : ComposedPacketHandler {

    val localPlayer = LocalPlayer(this)
    val level = Level(this)

    private val versionName by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        // AppContext.instance.packageManager.getPackageInfo(
        //     AppContext.instance.packageName, 0
        // ).versionName
        // Использование AppContext.instance здесь может быть излишним,
        // если Context уже передан. Можно использовать переданный context.
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
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

        val interceptablePacket = InterceptablePacketImpl(packet)

        for (module in ModuleManager.modules) {
            module.beforePacketBound(interceptablePacket)
            if (interceptablePacket.isIntercepted) {
                return true
            }
        }

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

    // --- НОВЫЕ МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ ЗВУКАМИ (ДОБАВИТЬ В GameSession) ---
    fun stopAllSounds() {
        println("GameSession: Stopping all sounds.")
    }

    fun playSound(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        println("GameSession: Playing sound: $soundName (Volume: $volume, Pitch: $pitch)")
    }

    fun toggleSounds(enable: Boolean) {
        println("GameSession: Toggling sounds to: $enable")
    }

    fun soundList(soundSet: Any) { // Замените 'Any' на ваш фактический тип
        println("GameSession: Setting sound list to: $soundSet")
    }
    // --- КОНЕЦ НОВЫХ МЕТОДОВ ЗВУКА ---
}
