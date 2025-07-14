package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.BlockEntityDataPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.nbt.NbtMap
import kotlin.math.ceil
import kotlin.math.sqrt
import java.util.concurrent.ConcurrentHashMap // Для потокобезопасного хранения

// Убираем все импорты, связанные с корутинами (kotlinx.coroutines.*)

class ChestFinderModule : Module("Поиск сундуков", ModuleCategory.Misc) {

    private var playerPosition: Vector3f = Vector3f.ZERO

    // Используем Set для отслеживания УЖЕ обнаруженных сундуков (чтобы не отправлять сообщения повторно)
    private val discoveredChests = ConcurrentHashMap.newKeySet<Vector3f>()

    // Настраиваемые опции для модуля
    private var scanRadius by intValue("Радиус сканирования", 128, 16..500)
    private var notifyInChat by boolValue("Оповещать в чат", true)
    private var resetOnDisable by boolValue("Сброс при отключении", true)

    // Удалены все параметры и переменные, связанные с повторной отправкой (resendEnabled, resendIntervalSeconds, chatMessageCooldownMs, lastChatMessageTime)
    // Удалены все переменные и функции, связанные с корутинами (resendJob, startResendJob)

    override fun onEnabled() {
        super.onEnabled()
        session?.displayClientMessage("§8[§6ПоискСундуков§8] §aМодуль активирован. Сканирую область.")
        discoveredChests.clear() // Очищаем список при включении
    }

    override fun onDisabled() {
        super.onDisabled()
        session?.displayClientMessage("§8[§6ПоискСундуков§8] §cМодуль деактивирован.")
        if (resetOnDisable) {
            discoveredChests.clear()
        }
        // Здесь нет корутины для отмены, и глобальных таймеров тоже нет
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled || session?.localPlayer == null) {
            return
        }

        val packet = interceptablePacket.packet

        // Обновляем позицию игрока
        if (packet is PlayerAuthInputPacket) {
            playerPosition = packet.position
        }

        // Отслеживаем пакеты данных блочных сущностей
        if (packet is BlockEntityDataPacket) {
            handleBlockEntityDataPacket(packet)
        }
    }

    private fun handleBlockEntityDataPacket(packet: BlockEntityDataPacket) {
        val blockEntityData: NbtMap? = packet.data
        val blockEntityId = blockEntityData?.getString("id")

        val isChest = when (blockEntityId) {
            "Chest", "TrappedChest", "EnderChest" -> true
            else -> false
        }

        if (isChest) {
            val chestPosition = Vector3f.from(
                packet.blockPosition.x.toFloat(),
                packet.blockPosition.y.toFloat(),
                packet.blockPosition.z.toFloat()
            )

            val distance = calculateDistance(playerPosition, chestPosition)

            // Если сундук находится в радиусе сканирования
            if (distance <= scanRadius.toFloat()) {
                // Отправляем сообщение только если сундук обнаружен впервые в текущей сессии "в радиусе".
                // Метод add() возвращает true, если элемент успешно добавлен (т.е. его не было в Set).
                if (notifyInChat && discoveredChests.add(chestPosition)) {
                    val roundedDistance = ceil(distance).toInt()
                    val roundedCoords = chestPosition.roundUpCoordinates()
                    session?.displayClientMessage("§8[§6Сундук§8] §aОбнаружен сундук на координатах: §f$roundedCoords §aДистанция: §c$roundedDistance")
                }
                // Если сундук уже есть в discoveredChests (т.е. add() вернул false), то сообщение не отправляется.
                // Это и есть "нет повторения".
            } else {
                // Если сундук вышел из радиуса сканирования, удаляем его из списка "обнаруженных".
                // Это позволяет снова обнаружить его как "новый", если он вернется в радиус.
                discoveredChests.remove(chestPosition)
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
