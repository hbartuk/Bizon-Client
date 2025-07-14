package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
// --- ИМПОРТЫ ДЛЯ BlockEntityDataPacket ---
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket // <-- ИСПОЛЬЗУЕМ BlockEntityDataPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.nbt.NbtMap // Убедитесь, что эта зависимость добавлена в build.gradle
// --- КОНЕЦ ИМПОРТОВ ---
import kotlin.math.ceil
import kotlin.math.sqrt
import java.util.concurrent.ConcurrentHashMap

class ChestFinderModule : Module("Поиск сундуков", ModuleCategory.Misc) {

    private var playerPosition: Vector3f = Vector3f.ZERO
    private val discoveredChests = ConcurrentHashMap.newKeySet<Vector3f>()
    private var scanRadius by intValue("Радиус сканирования", 128, 16..500)
    private var notifyInChat by boolValue("Оповещать в чат", true)
    private var resetOnDisable by boolValue("Сброс при отключении", true)

    override fun onEnabled() {
        super.onEnabled()
        session?.displayClientMessage("§8[§6ПоискСундуков§8] §aМодуль активирован. Сканирую область.")
        discoveredChests.clear()
    }

    override fun onDisabled() {
        super.onDisabled()
        session?.displayClientMessage("§8[§6ПоискСундуков§8] §cМодуль деактивирован.")
        if (resetOnDisable) {
            discoveredChests.clear()
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || session?.localPlayer == null) {
            return
        }

        val packet = interceptablePacket.packet

        if (packet is PlayerAuthInputPacket) {
            playerPosition = packet.position
        }

        // Используем BlockEntityDataPacket вместо BlockActorDataPacket
        if (packet is BlockEntityDataPacket) {
            handleBlockEntityDataPacket(packet)
        }
    }

    // Обработчик для BlockEntityDataPacket
    private fun handleBlockEntityDataPacket(packet: BlockEntityDataPacket) {
        // Данные блочной сущности находятся в packet.data
        val blockEntityData: NbtMap? = packet.data // Используем packet.data
        val blockEntityId = blockEntityData?.getString("id")

        val isChest = when (blockEntityId) {
            "Chest", "TrappedChest", "EnderChest" -> true
            else -> false
        }

        if (isChest) {
            // Позиция блока находится в packet.blockPosition
            val chestPosition = Vector3f.from(
                packet.blockPosition.x.toFloat(), // Используем packet.blockPosition
                packet.blockPosition.y.toFloat(),
                packet.blockPosition.z.toFloat()
            )

            val distance = calculateDistance(playerPosition, chestPosition)

            if (distance <= scanRadius.toFloat()) {
                if (discoveredChests.add(chestPosition)) {
                    if (notifyInChat) {
                        val roundedDistance = ceil(distance).toInt()
                        val roundedCoords = chestPosition.roundUpCoordinates()
                        session?.displayClientMessage("§8[§6Сундук§8] §aОбнаружен сундук на координатах: §f$roundedCoords §aДистанция: §c$roundedDistance")
                    }
                }
            }
        }
    }

    private fun calculateDistance(from: Vector3f, to: Vector3f): Float {
        val dx = from.x - to.x
        val dy = from.y - to.y
        val dz = from.z - to.z
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    private fun Vector3f.roundUpCoordinates(): String {
        val roundedX = ceil(this.x).toInt()
        val roundedY = ceil(this.y).toInt()
        val roundedZ = ceil(this.z).toInt()
        return "$roundedX, $roundedY, $roundedZ"
    }
}
