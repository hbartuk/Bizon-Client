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

    override var uuid: UUID = UUID.randomUUID() // Это инициализация по умолчанию, будет перезаписана
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

    override fun onPacketBound(packet: BedrockPacket) {
        super.onPacketBound(packet)
        if (packet is StartGamePacket) {
            runtimeEntityId = packet.runtimeEntityId
            uniqueEntityId = packet.uniqueEntityId // Это long ID сущности

            session.displayClientMessage("§a[WClient] Обнаружен мой runtimeEntityId: §b${this.runtimeEntityId}")
            session.displayClientMessage("§a[WClient] Обнаружен мой uniqueEntityId (long): §b${this.uniqueEntityId}")

            movementServerAuthoritative =
                packet.authoritativeMovementMode != AuthoritativeMovementMode.CLIENT
            packet.authoritativeMovementMode = AuthoritativeMovementMode.SERVER
            inventoriesServerAuthoritative = packet.isInventoriesServerAuthoritative
            blockBreakServerAuthoritative = packet.isServerAuthoritativeBlockBreaking
            soundServerAuthoritative = packet.networkPermissions.isServerAuthSounds

            reset()
        }
        // --- ОБРАБОТКА PlayerListPacket ДЛЯ ПОЛУЧЕНИЯ UUID И НИКНЕЙМА ---
        if (packet is PlayerListPacket) {
            for (entry in packet.entries) {
                // Ищем свою собственную запись в списке игроков по entityId
                // Используем entry.entityId вместо entry.uniqueEntityId
                if (entry.entityId == this.uniqueEntityId) { // Сравниваем long ID из StartGamePacket с entityId из PlayerListPacket
                    this.uuid = entry.uuid // Вот наш java.util.UUID!
                    session.displayClientMessage("§a[WClient] Обнаружен мой UUID из PlayerListPacket: §b${this.uuid}")
                    session.displayClientMessage("§a[WClient] Мой никнейм (из PlayerListPacket): §b${entry.name}") // Используем entry.name вместо entry.username
                    break // Нашли свою запись, можно выйти из цикла
                }
            }
        }
        // --- КОНЕЦ ОБРАБОТКИ PlayerListPacket ---

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
