package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
// --- ДОБАВЛЕННЫЕ ИМПОРТЫ ---
import org.cloudburstmc.protocol.bedrock.packet.BlockActorDataPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
// Если вы используете NBTMap, убедитесь, что NbtMap импортирован, если он не из того же пакета:
import org.cloudburstmc.nbt.NbtMap // Убедитесь, что эта зависимость добавлена в build.gradle
// --- КОНЕЦ ДОБАВЛЕННЫХ ИМПОРТОВ ---

import kotlin.math.ceil
import kotlin.math.sqrt

import java.util.concurrent.ConcurrentHashMap

class ChestFinderModule : Module("Поиск сундуков", ModuleCategory.Misc) {

    private var playerPosition: Vector3f = Vector3f.ZERO

    private val discoveredChests = ConcurrentHashMap.newKeySet<Vector3f>()

    // Убран .value из intValue/boolValue в объявлении
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

        if (packet is BlockActorDataPacket) {
            handleBlockActorDataPacket(packet)
        }
    }

    private fun handleBlockActorDataPacket(packet: BlockActorDataPacket) {
        // Убедимся, что packet.nbt имеет тип NbtMap, иначе это может вызвать ошибку.
        // Если nbt может быть null, используйте safe call ?.
        val blockActorData: NbtMap? = packet.nbt // Явно указываем тип NbtMap?
        val blockActorId = blockActorData?.getString("id")

        val isChest = when (blockActorId) {
            "Chest", "TrappedChest", "EnderChest" -> true
            else -> false
        }

        if (isChest) {
            // blockPosition - это BlockPos, его координаты нужно получить как Int, затем преобразовать в Float для Vector3f
            val chestPosition = Vector3f.from(
                packet.blockPosition.x.toFloat(),
                packet.blockPosition.y.toFloat(),
                packet.blockPosition.z.toFloat()
            )

            // Убран .value из scanRadius.value
            val distance = calculateDistance(playerPosition, chestPosition)

            if (distance <= scanRadius.toFloat()) { // Здесь тоже убран .value
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
