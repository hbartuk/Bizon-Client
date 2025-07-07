package com.retrivedmods.wclient.game.entity

import android.util.Log
import com.retrivedmods.wclient.game.GameSession
import com.retrivedmods.wclient.game.inventory.AbstractInventory
import com.retrivedmods.wclient.game.inventory.ContainerInventory
import com.retrivedmods.wclient.game.inventory.PlayerInventory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.AuthoritativeMovementMode
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket
import java.util.UUID

@Suppress("MemberVisibilityCanBePrivate")
class LocalPlayer(val session: GameSession) : Player(0L, 0L, UUID.randomUUID(), "") {

    override var runtimeEntityId: Long = 0L
        private set

    override var uniqueEntityId: Long = 0L
        private set

    override var uuid: UUID = UUID.randomUUID()
        private set

    var blockBreakServerAuthoritative = false
        private set

    var movementServerAuthoritative = true
        private set

    var inventoriesServerAuthoritative = false
        private set

    var soundServerAuthoritative = false
        private set

    override val inventory = PlayerInventory(this)

    var openContainer: AbstractInventory? = null
        private set

    override var health: Float = 100f

    // Возвращаем 'override' для vec3Position
    override var vec3Position: Vector3f = Vector3f.ZERO // Инициализация начальной позицией

    // ВОЗВРАЩЕНО: 'override' для vec3Rotation
    override var vec3Rotation: Vector3f = Vector3f.ZERO // Инициализация начальной ротацией
    var vec3Motion: Vector3f = Vector3f.ZERO // Инициализация нулевой скоростью


    override fun onPacketBound(packet: BedrockPacket) {
        super.onPacketBound(packet)
        if (packet is StartGamePacket) {
            runtimeEntityId = packet.runtimeEntityId
            uniqueEntityId = packet.uniqueEntityId

            session.displayClientMessage("§a[WClient] Обнаружен мой runtimeEntityId: §b${this.runtimeEntityId}")
            session.displayClientMessage("§a[WClient] Обнаружен мой uniqueEntityId (long): §b${this.uniqueEntityId}")

            movementServerAuthoritative =
                packet.authoritativeMovementMode != AuthoritativeMovementMode.CLIENT
            packet.authoritativeMovementMode = AuthoritativeMovementMode.SERVER
            inventoriesServerAuthoritative = packet.isInventoriesServerAuthoritative
            blockBreakServerAuthoritative = packet.isServerAuthoritativeBlockBreaking
            soundServerAuthoritative = packet.networkPermissions.isServerAuthSounds

            this.vec3Motion = Vector3f.ZERO // Убедимся, что motion обнулен при старте

            reset()
        }
        if (packet is PlayerListPacket) {
            for (entry in packet.entries) {
                if (entry.entityId == this.uniqueEntityId) {
                    this.uuid = entry.uuid
                    session.displayClientMessage("§a[WClient] Обнаружен мой UUID из PlayerListPacket: §b${this.uuid}")
                    session.displayClientMessage("§a[WClient] Мой никнейм (из PlayerListPacket): §b${entry.name}")
                    break
                }
            }
        }

        if (packet is PlayerAuthInputPacket) {
            this.vec3Position = packet.position
            this.vec3Rotation = packet.rotation // Обновляем ротацию игрока из пакета
            tickExists = packet.tick
        }
        if (packet is ContainerOpenPacket) {
            openContainer = if (packet.id.toInt() == 0) {
                return
            } else {
                ContainerInventory(packet.id.toInt(), packet.type)
            }
        }
        if (packet is ContainerClosePacket && packet.id.toInt() == openContainer?.containerId) {
            openContainer = null
        }

        inventory.onPacketBound(packet)
        openContainer?.also {
            if (it is ContainerInventory) {
                it.onPacketBound(packet)
            }
        }
    }

    fun swing() {
        val animatePacket = AnimatePacket().apply {
            action = AnimatePacket.Action.SWING_ARM
            runtimeEntityId = this@LocalPlayer.runtimeEntityId
        }

        session.serverBound(animatePacket)
        session.clientBound(animatePacket)

        val levelSoundEventPacket = LevelSoundEventPacket().apply {
            sound = SoundEvent.ATTACK_NODAMAGE
            position = vec3Position
            extraData = -1
            identifier = "minecraft:player"
            isBabySound = false
            isRelativeVolumeDisabled = false
        }

        session.serverBound(levelSoundEventPacket)
        session.clientBound(levelSoundEventPacket)
    }

    fun attack(entity: Entity) {
        swing()

        Log.e(
            "Inventory", """
            hotbarSlot: ${inventory.heldItemSlot}
            hand: ${inventory.hand}
        """.trimIndent()
        )

        val inventoryTransactionPacket = InventoryTransactionPacket().apply {
            transactionType = InventoryTransactionType.ITEM_USE_ON_ENTITY
            actionType = 1
            runtimeEntityId = entity.runtimeEntityId
            hotbarSlot = inventory.heldItemSlot
            itemInHand = inventory.hand
            playerPosition = vec3Position

            // КЛЮЧЕВОЕ ИЗМЕНЕНИЕ ЗДЕСЬ: РАСЧЁТ clickPosition
            clickPosition = entity.vec3Position.add(0f, 0.9f, 0f)
        }

        session.serverBound(inventoryTransactionPacket)
    }

    override fun onDisconnect() {
        super.onDisconnect()
        reset()
    }
}
