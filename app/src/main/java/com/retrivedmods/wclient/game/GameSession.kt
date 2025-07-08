// File: com.retrivedmods.wclient.game.GameSession.kt

package com.retrivedmods.wclient.game

import com.mucheng.mucute.relay.MuCuteRelaySession
// --- ДОБАВЬТЕ ЭТИ ИМПОРТЫ ИЛИ УБЕДИТЕСЬ В ИХ ПРАВИЛЬНОСТИ ---
import com.mucheng.mucute.relay.listener.PacketListener // УБЕДИТЕСЬ, что это правильный путь
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket // Это базовый класс для всех Bedrock пакетов
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import com.retrivedmods.wclient.game.entity.LocalPlayer // ИМПОРТ ДЛЯ LocalPlayer
// --- КОНЕЦ ИМПОРТОВ ---

class GameSession(
    val muCuteRelaySession: MuCuteRelaySession
) : PacketListener { // Теперь 'PacketListener' должен быть распознан

    var playerX: Double = 0.0
    var playerY: Double = 0.0
    var playerZ: Double = 0.0

    // Инициализируем localPlayer как lateinit, но нужно УБЕДИТЬСЯ, что он будет
    // инициализирован ГДЕ-ТО в GameSession после установки соединения,
    // например, в onPacketIn при получении соответствующего пакета игрока.
    lateinit var localPlayer: LocalPlayer

    fun displayClientMessage(message: String) {
        println("CLIENT MESSAGE: $message")
        val textPacket = TextPacket().apply {
            type = TextPacket.Type.CHAT // Или TextPacket.Type.SYSTEM_MESSAGE
            this.message = message
        }
        muCuteRelaySession.clientBound(textPacket)
    }

    // --- ИСПРАВЛЕНИЯ ЗДЕСЬ: используем BedrockPacket как тип параметра ---
    override fun onPacketIn(packet: BedrockPacket) { // 'Packet' изменено на 'BedrockPacket'
        when (packet) {
            is MovePlayerPacket -> {
                playerX = packet.position.x.toDouble()
                playerY = packet.position.y.toDouble()
                playerZ = packet.position.z.toDouble()
            }
            is RespawnPacket -> {
                playerX = packet.position.x.toDouble()
                playerY = packet.position.y.toDouble()
                playerZ = packet.position.z.toDouble()
            }
            // Здесь же, возможно, нужно инициализировать localPlayer. Например:
            // if (packet is LoginPacket) { // Или какой-то другой пакет, указывающий на готовность игрока
            //     if (!::localPlayer.isInitialized) {
            //         localPlayer = LocalPlayer(this) // Передаем ссылку на текущую GameSession
            //     }
            // }
        }
    }

    override fun onPacketOut(packet: BedrockPacket) { // 'Packet' изменено на 'BedrockPacket'
        // ... логика обработки исходящих пакетов
    }
}
