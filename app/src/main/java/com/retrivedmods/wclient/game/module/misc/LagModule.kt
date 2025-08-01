package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket
import org.cloudburstmc.protocol.bedrock.data.entity.AnimateAction

class LagModule : Module("LagMachine", ModuleCategory.Combat) {

    private var isCollecting = false
    private val collectedPackets = mutableListOf<InterceptablePacket>()

    private var potionsPerSecond: Int = 0
    private var collectionTime: Long = 0

    private var startTime: Long = 0

    fun startCollecting(potionsPerSecond: Int, collectionTime: Long) {
        this.potionsPerSecond = potionsPerSecond
        this.collectionTime = collectionTime * 1000 // Переводим в миллисекунды
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
        
        // Генерируем пакеты броска зелья
        for (i in 0 until potionsPerSecond) {
            // Пакет начала использования предмета
            val useItemPacket = PlayerActionPacket().apply {
                runtimeEntityId = localPlayer.runtimeEntityId
                action = PlayerActionType.START_BREAK // Или START_ITEM_USE
                blockPosition = localPlayer.position.toIntFloor() // Нужно указать позицию
                face = 2 // Произвольное значение
            }
            collectedPackets.add(InterceptablePacket(useItemPacket))

            // Пакет анимации
            val animatePacket = AnimatePacket().apply {
                runtimeEntityId = localPlayer.runtimeEntityId
                action = AnimateAction.SWING_ARM
            }
            collectedPackets.add(InterceptablePacket(animatePacket))
            
            // Пакет завершения использования предмета
            val stopUsePacket = PlayerActionPacket().apply {
                runtimeEntityId = localPlayer.runtimeEntityId
                action = PlayerActionType.STOP_BREAK // Или STOP_ITEM_USE
                blockPosition = localPlayer.position.toIntFloor()
                face = 2
            }
            collectedPackets.add(InterceptablePacket(stopUsePacket))
        }
    }

    override fun onDisabled() {
        stopCollecting()
    }
}
