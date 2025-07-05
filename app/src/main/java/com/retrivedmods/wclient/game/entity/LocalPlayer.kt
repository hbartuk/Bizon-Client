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
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket
import java.util.UUID

@Suppress("MemberVisibilityCanBePrivate")
class LocalPlayer(val session: GameSession) : Player(0L, 0L, UUID.randomUUID(), "") {

    override var runtimeEntityId: Long = 0L
        private set

    override var uniqueEntityId: Long = 0L
        private set

    // Это поле будет перезаписано UUID из StartGamePacket
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

    // Overridden health property
    override var health: Float = 100f // Default health is 100

    override fun onPacketBound(packet: BedrockPacket) {
        super.onPacketBound(packet)
        if (packet is StartGamePacket) {
            runtimeEntityId = packet.runtimeEntityId
            uniqueEntityId = packet.uniqueEntityId

            // --- ИСПРАВЛЕНИЕ: ПРАВИЛЬНОЕ ПРИСВОЕНИЕ UUID И ЛОГИРОВАНИЕ ---
            this.uuid = packet.playerUuid // <-- ИСПОЛЬЗУЕМ packet.playerUuid (тип UUID)
            session.displayClientMessage("§a[WClient] Обнаружен мой UUID из StartGamePacket: §b${this.uuid}")
            // Строка с 'packet.username' удалена, т.к. StartGamePacket не содержит этого поля
            // --- КОНЕЦ ИСПРАВЛЕНИЯ ---

            movementServerAuthoritative =
                packet.authoritativeMovementMode != AuthoritativeMovementMode.CLIENT
            // Это поле часто меняется только для отладки, но оставлю как было:
            packet.authoritativeMovementMode = AuthoritativeMovementMode.SERVER
            inventoriesServerAuthoritative = packet.isInventoriesServerAuthoritative
            blockBreakServerAuthoritative = packet.isServerAuthoritativeBlockBreaking
            soundServerAuthoritative = packet.networkPermissions.isServerAuthSounds

            reset()
        }
        if (packet is PlayerAuthInputPacket) {
            move(packet.position)
            rotate(packet.rotation)
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
        val animatePacket = AnimatePacket()
        animatePacket.action = AnimatePacket.Action.SWING_ARM
        animatePacket.runtimeEntityId = runtimeEntityId

        session.serverBound(animatePacket)
        session.clientBound(animatePacket)

        val levelSoundEventPacket = LevelSoundEventPacket()
        levelSoundEventPacket.sound = SoundEvent.ATTACK_NODAMAGE
        levelSoundEventPacket.position = vec3Position
        levelSoundEventPacket.extraData = -1
        levelSoundEventPacket.identifier = "minecraft:player"
        levelSoundEventPacket.isBabySound = false
        levelSoundEventPacket.isRelativeVolumeDisabled = false

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

        val inventoryTransactionPacket = InventoryTransactionPacket()
        inventoryTransactionPacket.transactionType = InventoryTransactionType.ITEM_USE_ON_ENTITY
        inventoryTransactionPacket.actionType = 1
        inventoryTransactionPacket.runtimeEntityId = entity.runtimeEntityId
        inventoryTransactionPacket.hotbarSlot = inventory.heldItemSlot
        inventoryTransactionPacket.itemInHand = inventory.hand
        inventoryTransactionPacket.playerPosition = vec3Position
        inventoryTransactionPacket.clickPosition = Vector3f.ZERO

        session.serverBound(inventoryTransactionPacket)
    }

    override fun onDisconnect() {
        super.onDisconnect()
        reset()
    }
}
