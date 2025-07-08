// File: com.retrivedmods.wclient.game.GameSession.kt

package com.retrivedmods.wclient.game

import com.mucheng.mucute.relay.MuCuteRelaySession
// --- УБЕДИТЕСЬ, ЧТО ЭТОТ ИМПОРТ ПРАВИЛЬНЫЙ ---
import com.mucheng.mucute.relay.listener.PacketListener // <-- Проверьте этот путь!
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket // Базовый класс для пакетов
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import com.retrivedmods.wclient.game.entity.LocalPlayer

class GameSession(
    val muCuteRelaySession: MuCuteRelaySession
) : PacketListener { // Если PacketListener из MuCuteRelay, то он должен быть здесь.

    var playerX: Double = 0.0
    var playerY: Double = 0.0
    var playerZ: Double = 0.0

    // Это свойство должно быть инициализировано, например, при первом появлении игрока.
    lateinit var localPlayer: LocalPlayer

    fun displayClientMessage(message: String) {
        println("CLIENT MESSAGE: $message")
        val textPacket = TextPacket().apply {
            type = TextPacket.Type.CHAT
            this.message = message
        }
        muCuteRelaySession.clientBound(textPacket)
    }

    // --- СИГНАТУРЫ МЕТОДОВ onPacketIn/onPacketOut ДОЛЖНЫ СООТВЕТСТВОВАТЬ ИНТЕРФЕЙСУ PacketListener ---
    // Если PacketListener ожидает org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
    override fun onPacketIn(packet: BedrockPacket) {
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
            // Возможно, здесь инициализация localPlayer
            // Например:
            // is LoginPacket -> { // Замените на реальный пакет входа
            //     if (!::localPlayer.isInitialized) {
            //         localPlayer = LocalPlayer(this)
            //     }
            // }
        }
    }

    override fun onPacketOut(packet: BedrockPacket) {
        // Логика для исходящих пакетов
    }
}
