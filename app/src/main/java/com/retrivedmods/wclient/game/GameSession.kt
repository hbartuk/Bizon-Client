// File: com.retrivedmods.wclient.game.GameSession.kt (ПРИМЕР)

package com.retrivedmods.wclient.game

import com.mucheng.mucute.relay.MuCuteRelaySession
import com.mucheng.mucute.relay.listener.PacketListener
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket // Пример импорта для обновления координат
import org.cloudburstmc.protocol.bedrock.packet.RespawnPacket // Пример для обновления координат

class GameSession(
    val muCuteRelaySession: MuCuteRelaySession // Это свойство теперь обязательно!
    // ... другие свойства, если есть
) : PacketListener { // Если GameSession является PacketListener

    // *** НУЖНО ДОБАВИТЬ ЭТИ СВОЙСТВА И ОБНОВЛЯТЬ ИХ ***
    var playerX: Double = 0.0
    var playerY: Double = 0.0
    var playerZ: Double = 0.0

    // ... ваш существующий код GameSession ...

    // Пример метода для отображения сообщения клиенту
    fun displayClientMessage(message: String) {
        println("CLIENT MESSAGE: $message")
        // Если вы отправляете это через сетевой пакет, то:
        // val textPacket = TextPacket().apply {
        //     type = TextPacket.Type.CHAT
        //     message = message
        // }
        // muCuteRelaySession.clientBound(textPacket)
    }

    // Пример, как можно обновлять координаты игрока, если GameSession слушает пакеты
    override fun onPacketIn(packet: Packet) {
        when (packet) {
            is MovePlayerPacket -> {
                playerX = packet.position.x.toDouble()
                playerY = packet.position.y.toDouble()
                playerZ = packet.position.z.toDouble()
                // println("DEBUG: Player position updated to ($playerX, $playerY, $playerZ)")
            }
            is RespawnPacket -> {
                playerX = packet.position.x.toDouble()
                playerY = packet.position.y.toDouble()
                playerZ = packet.position.z.toDouble()
                // println("DEBUG: Player position updated to ($playerX, $playerY, $playerZ) after respawn")
            }
            // ... другие пакеты, которые могут обновлять положение игрока
        }
        // ... остальная логика обработки входящих пакетов
    }

    override fun onPacketOut(packet: Packet) {
        // ... логика обработки исходящих пакетов
    }
}
