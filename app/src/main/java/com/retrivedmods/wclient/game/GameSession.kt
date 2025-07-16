// File: app/src/main/java/com/retrivedmods/wclient/game/GameSession.kt
package com.retrivedmods.wclient.game

import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.world.Level
import com.mucheng.mucute.relay.MuCuteRelaySession
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
// ... убедитесь, что здесь есть все остальные необходимые импорты

@Suppress("MemberVisibilityCanBePrivate")
class GameSession(val muCuteRelaySession: MuCuteRelaySession) : ComposedPacketHandler {

    val localPlayer = LocalPlayer(this) // `this` передает GameSession в LocalPlayer

    val level = Level(this) // `this` передает GameSession в Level

    // Если ModuleManager - это класс, а не object, вам нужно будет инициализировать его здесь
    // Например: val moduleManager = ModuleManager()
    // Если он у вас уже является 'object ModuleManager', то это свойство не нужно,
    // и вы будете обращаться к нему как 'ModuleManager.modules' или 'ModuleManager.getModule'

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

        // ИСПРАВЛЕНИЕ: Используем InterceptablePacketImpl вместо InterceptablePacket
        val interceptablePacket = InterceptablePacketImpl(packet) // <--- ВОТ ЗДЕСЬ ИСПРАВЛЕНО

        // Используйте ModuleManager.modules напрямую, если ModuleManager - object
        for (module in ModuleManager.modules) {
            module.beforePacketBound(interceptablePacket)
            if (interceptablePacket.isIntercepted) {
                return true
            }
        }

        return false
    }

    override fun afterPacketBound(packet: BedrockPacket) {
        // Используйте ModuleManager.modules напрямую, если ModuleManager - object
        for (module in ModuleManager.modules) {
            module.afterPacketBound(packet)
        }
    }

    override fun onDisconnect(reason: String) {
        localPlayer.onDisconnect()
        level.onDisconnect()

        // Используйте ModuleManager.modules напрямую, если ModuleManager - object
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
        // Здесь должна быть ваша реальная логика остановки всех звуков.
        // Возможно, у вас есть отдельный SoundManager, который GameSession использует.
        // Пример: soundManager.stopAllSounds()
        println("GameSession: Stopping all sounds.")
    }

    fun playSound(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        // Здесь должна быть ваша реальная логика проигрывания конкретного звука.
        // Пример: soundManager.play(soundName, volume, pitch)
        println("GameSession: Playing sound: $soundName (Volume: $volume, Pitch: $pitch)")
    }

    fun toggleSounds(enable: Boolean) {
        // Предполагается, что это метод для включения/выключения всей звуковой системы или категории звуков.
        println("GameSession: Toggling sounds to: $enable")
    }

    // Этот метод предполагает, что у вас есть enum или класс, представляющий SoundSet
    // Замените 'Any' на ваш фактический тип (например, com.retrivedmods.wclient.game.sound.SoundSet)
    fun soundList(soundSet: Any) {
        println("GameSession: Setting sound list to: $soundSet")
    }
    // --- КОНЕЦ НОВЫХ МЕТОДОВ ЗВУКА ---
}
