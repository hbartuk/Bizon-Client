
package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.session
import com.retrivedmods.wclient.game.entity.Entity
import com.retrivedmods.wclient.game.entity.Player
import com.retrivedmods.wclient.game.entity.LocalPlayer
import com.retrivedmods.wclient.game.entity.EntityUnknown
import com.retrivedmods.wclient.game.data.mob.MobList
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.*

class AutoClickerModule : Module("AutoClicker", ModuleCategory.Combat) {

    private var clicksPerSecond by intValue("КПС", 10, 1..50)
    private var attackRange by floatValue("Дистанция атаки", 4.0f, 1.0f..10.0f)
    private var fovRange by floatValue("FOV угол", 90.0f, 30.0f..180.0f)
    
    // Настройки целей
    private var attackPlayers by boolValue("Атаковать игроков", true)
    private var attackMobs by boolValue("Атаковать мобов", true)
    private var ignoreBots by boolValue("Игнорировать ботов", true)
    private var requireCrosshair by boolValue("Только по прицелу", true)

    private var lastAttackTime: Long = 0L

    override fun onEnabled() {
        session.displayClientMessage("§a[AutoClicker] Включен. КПС: $clicksPerSecond")
    }

    override fun onDisabled() {
        session.displayClientMessage("§c[AutoClicker] Выключен")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        val minDelay = 1000L / clicksPerSecond
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastAttackTime < minDelay) return

        val localPlayer = session.localPlayer
        val localPlayerPos = localPlayer.vec3Position
        val localPlayerEntityId = localPlayer.runtimeEntityId

        if (localPlayerPos == null || localPlayerEntityId == 0L) return

        // Найти подходящую цель
        val target = findBestTarget(localPlayer) ?: return

        // Атаковать цель
        localPlayer.attack(target)
        lastAttackTime = currentTime
    }

    private fun findBestTarget(localPlayer: LocalPlayer): Entity? {
        val localPlayerPos = localPlayer.vec3Position
        val maxRangeSq = attackRange * attackRange

        return session.level.entityMap.values
            .filter { entity ->
                // Исключить себя
                if (entity.runtimeEntityId == localPlayer.runtimeEntityId) return@filter false
                
                // Проверить позицию
                val entityPos = entity.vec3Position ?: return@filter false
                
                // Проверить дистанцию
                val distanceSq = localPlayerPos.distanceSquared(entityPos)
                if (distanceSq > maxRangeSq) return@filter false
                
                // Проверить тип цели
                if (!isValidTarget(entity)) return@filter false
                
                // Проверить направление взгляда (если требуется)
                if (requireCrosshair && !isLookingAtEntity(localPlayer, entity)) return@filter false
                
                true
            }
            .minByOrNull { entity ->
                localPlayerPos.distanceSquared(entity.vec3Position)
            }
    }

    private fun isValidTarget(entity: Entity): Boolean {
        return when (entity) {
            is LocalPlayer -> false
            is Player -> {
                if (!attackPlayers) return false
                if (ignoreBots && isBot(entity)) return false
                true
            }
            is EntityUnknown -> {
                if (!attackMobs) return false
                isMob(entity)
            }
            else -> false
        }
    }

    private fun isBot(player: Player): Boolean {
        if (player is LocalPlayer) return false
        val playerInfo = session.level.playerMap[player.uuid] ?: return true
        return playerInfo.name.isBlank()
    }

    private fun isMob(entity: EntityUnknown): Boolean {
        return entity.identifier in MobList.mobTypes
    }

    private fun isLookingAtEntity(localPlayer: LocalPlayer, entity: Entity): Boolean {
        val playerPos = localPlayer.vec3Position
        val entityPos = entity.vec3Position
        val playerRotation = localPlayer.vec3Rotation

        // Вычислить направление к цели
        val deltaX = entityPos.x - playerPos.x
        val deltaY = entityPos.y - playerPos.y
        val deltaZ = entityPos.z - playerPos.z

        val horizontalDistance = sqrt(deltaX * deltaX + deltaZ * deltaZ)
        
        // Вычислить углы к цели
        val targetYaw = atan2(-deltaX, deltaZ) * 180.0 / PI
        val targetPitch = atan2(-deltaY, horizontalDistance) * 180.0 / PI

        // Нормализовать углы
        val yawDiff = normalizeAngle(targetYaw - playerRotation.y)
        val pitchDiff = abs(targetPitch - playerRotation.x)

        // Проверить, находится ли цель в пределах FOV
        val totalAngleDiff = sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff)
        
        return totalAngleDiff <= fovRange / 2.0
    }

    private fun normalizeAngle(angle: Double): Double {
        var normalizedAngle = angle % 360.0
        if (normalizedAngle > 180.0) {
            normalizedAngle -= 360.0
        } else if (normalizedAngle < -180.0) {
            normalizedAngle += 360.0
        }
        return normalizedAngle
    }
}
