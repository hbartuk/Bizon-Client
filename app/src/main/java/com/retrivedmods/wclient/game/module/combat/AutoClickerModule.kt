package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.Entity
import com.retrivedmods.wclient.game.entity.Player
import com.retrivedmods.wclient.game.entity.LocalPlayer
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.*

class AutoClickerModule : Module("AutoClicker", ModuleCategory.Combat) {

    private var cpsValue by intValue("CPS", 10, 1..20)
    private var rangeValue by floatValue("Range", 4.0f, 2f..8f)
    private var playersOnly by boolValue("Players Only", true)

    private var lastAttackTime = 0L

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket) {
            val currentTime = System.currentTimeMillis()
            val minAttackDelay = 1000L / cpsValue

            if ((currentTime - lastAttackTime) >= minAttackDelay) {
                val target = findNearestTarget()
                if (target != null) {
                    session.localPlayer.attack(target)
                    lastAttackTime = currentTime
                }
            }
        }
    }

    private fun findNearestTarget(): Entity? {
        val player = session.localPlayer
        val playerPos = player.vec3Position

        return session.level.entityMap.values
            .filter { entity ->
                entity != player && 
                isValidTarget(entity) &&
                entity.vec3Position.distance(playerPos) <= rangeValue
            }
            .minByOrNull { it.vec3Position.distance(playerPos) }
    }

    private fun isValidTarget(entity: Entity): Boolean {
        return when (entity) {
            is LocalPlayer -> false
            is Player -> {
                if (playersOnly) {
                    !isBot(entity)
                } else {
                    false
                }
            }
            else -> !playersOnly
        }
    }

    private fun isBot(player: Player): Boolean {
        if (player is LocalPlayer) return false
        val playerInfo = session.level.playerMap[player.uuid]
        return playerInfo?.name.isNullOrBlank()
    }
}