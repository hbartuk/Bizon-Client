package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.* // Импортируем все сущности из твоего пакета
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class AutoClickerModule : Module("AutoClicker", ModuleCategory.Combat) {

    private var clicksPerSecond by intValue("КПС", 10, 1..50000000)
    private var attackRange by floatValue("Дистанция атаки", 4.0f, 1.0f..1000.0f)

    private var lastAttackTime: Long = 0L

    fun onEnable() {
        session.displayClientMessage("§a[WClient] Авто-кликер включен. КПС: ${clicksPerSecond}")
    }

    fun onDisable() {
        session.displayClientMessage("§c[WClient] Авто-кликер выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) {
            return
        }

        val minDelay = 1000L / clicksPerSecond
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastAttackTime < minDelay) {
            return
        }

        val localPlayerPos = session.localPlayer.vec3Position
        val localPlayerEntityId = session.localPlayer.runtimeEntityId

        if (localPlayerPos == null || localPlayerEntityId == 0L) {
            return
        }

        var targetEntity: Entity? = null
        val maxAttackRangeSq = attackRange * attackRange

        // Ищем ближайшую сущность. Теперь без фильтрации по типу игрока/моба,
        // бьём всех, кто в радиусе, кроме себя.
        for (entity in session.level.entityMap.values) {
            // Исключаем самого себя и сущности без позиции
            if (entity.runtimeEntityId == localPlayerEntityId || entity.vec3Position == null) {
                continue
            }

            val distanceSq = localPlayerPos.distanceSquared(entity.vec3Position)
            if (distanceSq < maxAttackRangeSq) {
                targetEntity = entity
                break // Нашли ближайшего, этого достаточно
            }
        }

        if (targetEntity != null) {
            session.localPlayer.attack(targetEntity!!)
            lastAttackTime = currentTime
        }
    }
}
