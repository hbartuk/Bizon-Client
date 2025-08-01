// File: com.retrivedmods.wclient.game.module.misc.LagModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket
import org.cloudburstmc.math.vector.Vector3f

class LagModule : Module("LagMachine", ModuleCategory.Misc) {

    private var isCollecting = false
    private val collectedPackets = mutableListOf<InterceptablePacket>()

    private var actionsPerSecond: Int = 0
    private var collectionTime: Long = 0

    private var startTime: Long = 0

    fun startCollecting(actionsPerSecond: Int, collectionTime: Long) {
        this.actionsPerSecond = actionsPerSecond
        this.collectionTime = collectionTime * 1000
        this.isCollecting = true
        this.startTime = System.currentTimeMillis()
        collectedPackets.clear()
        session.displayClientMessage("§a[LagMachine] Начало сбора пакетов...")
    }

    fun stopCollecting() {
        if (!isCollecting) return

        isCollecting = false
        sendCollectedPackets()
        session.displayClientMessage("§a[LagMachine] Отправка пакетов остановлена вручную.")
    }

    private fun sendCollectedPackets() {
        session.displayClientMessage("§a[LagMachine] Отправка §e${collectedPackets.size}§a пакетов...")
        for (packet in collectedPackets) {
            session.serverBound(packet.packet)
        }
        collectedPackets.clear()
        session.displayClientMessage("§a[LagMachine] Все пакеты отправлены.")
    }

    override fun onTick() {
        if (!isCollecting) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - startTime >= collectionTime) {
            isCollecting = false
            sendCollectedPackets()
            return
        }

        val localPlayer = session.localPlayer ?: return
        val playerPosition = localPlayer.vec3Position // ИСПРАВЛЕНО: Используем vec3Position
        
        // Генерируем пакеты броска зелья
        for (i in 0 until actionsPerSecond) {
            // Пакет начала использования предмета
            val startUsePacket = PlayerActionPacket().apply {
                runtimeEntityId = localPlayer.runtimeEntityId
                action = PlayerActionType.START_BREAK 
                blockPosition = playerPosition.toIntFloor()
                face = -1
            }
            collectedPackets.add(InterceptablePacket(startUsePacket))

            // Пакет анимации
            val animatePacket = AnimatePacket().apply {
                runtimeEntityId = localPlayer.runtimeEntityId
                action = AnimatePacket.Action.SWING_ARM
            }
            collectedPackets.add(InterceptablePacket(animatePacket))
            
            // Пакет завершения использования предмета
            val stopUsePacket = PlayerActionPacket().apply {
                runtimeEntityId = localPlayer.runtimeEntityId
                action = PlayerActionType.STOP_BREAK
                blockPosition = playerPosition.toIntFloor()
                face = -1
            }
            collectedPackets.add(InterceptablePacket(stopUsePacket))
        }
    }

    override fun onDisabled() {
        stopCollecting()
    }
}
