package com.retrivedmods.wclient.game.module.combat

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.entity.Entity
import com.retrivedmods.wclient.game.entity.Player
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import kotlin.math.*

class AutoClickerModule : Module("autoclicker", ModuleCategory.Combat) {

    private var lastClickTime = 0L
    private val clickDelay = 100L // 100ms между кликами

    private fun isLookingAtEntity(entity: Entity): Boolean {
        val player = session.localPlayer
        val playerPos = player.position
        val playerRotation = player.rotation

        // Вычисляем направление взгляда игрока
        val yaw = Math.toRadians(playerRotation.y.toDouble())
        val pitch = Math.toRadians(playerRotation.x.toDouble())

        val lookDirection = Vector3f.from(
            -sin(yaw) * cos(pitch),
            -sin(pitch),
            cos(yaw) * cos(pitch)
        )

        // Вектор от игрока к сущности
        val entityPos = entity.position
        val toEntity = entityPos.sub(playerPos).normalize()

        // Вычисляем угол между направлением взгляда и направлением к сущности
        val dotProduct = lookDirection.dot(toEntity)
        val angle = Math.toDegrees(acos(dotProduct.toDouble()))

        // Если угол меньше 30 градусов, считаем что игрок смотрит на сущность
        return angle < 30.0
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        if (packet is PlayerAuthInputPacket) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastClickTime < clickDelay) {
                return
            }

            // Ищем ближайшую сущность, на которую смотрит игрок
            var targetEntity: Entity? = null
            var closestDistance = Double.MAX_VALUE

            // Проверяем игроков
            session.players.values.forEach { player ->
                if (player != session.localPlayer) {
                    val distance = session.localPlayer.position.distance(player.position).toDouble()
                    if (distance < 6.0 && distance < closestDistance && isLookingAtEntity(player)) {
                        targetEntity = player
                        closestDistance = distance
                    }
                }
            }

            // Проверяем других сущностей
            session.entities.values.forEach { entity ->
                if (entity != session.localPlayer) {
                    val distance = session.localPlayer.position.distance(entity.position).toDouble()
                    if (distance < 6.0 && distance < closestDistance && isLookingAtEntity(entity)) {
                        targetEntity = entity
                        closestDistance = distance
                    }
                }
            }

            // Если нашли цель, атакуем её
            targetEntity?.let { target ->
                attackEntity(target)
                lastClickTime = currentTime
            }
        }
    }

    private fun attackEntity(entity: Entity) {
        val transaction = ItemUseTransaction().apply {
            actionType = 1 // Attack action
            runtimeEntityId = entity.runtimeEntityId
            position = entity.position
        }

        val transactionPacket = InventoryTransactionPacket().apply {
            transactionType = InventoryTransactionType.ITEM_USE
            this.transaction = transaction
        }

        session.serverBound(transactionPacket)

        // Отправляем сообщение о атаке
        val entityName = if (entity is Player) {
            entity.name
        } else {
            entity.identifier ?: "Unknown Entity"
        }

        session.displayClientMessage("§c[AutoClicker] Атакую: §f$entityName")
    }
}