package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.BlockActorDataPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import kotlin.math.ceil
import kotlin.math.sqrt

// Используем Set для хранения уже обнаруженных сундуков, чтобы не спамить в чат
import java.util.concurrent.ConcurrentHashMap

class ChestFinderModule : Module("Поиск сундуков", ModuleCategory.Misc) {

    // Текущая позиция игрока
    private var playerPosition: Vector3f = Vector3f.ZERO

    // Список уже обнаруженных сундуков, чтобы избежать повторных сообщений
    // ConcurrentHashMap используется для потокобезопасности, если обработка пакетов происходит в разных потоках
    private val discoveredChests = ConcurrentHashMap.newKeySet<Vector3f>()

    // Настраиваемые опции для модуля
    private var scanRadius by intValue("Радиус сканирования", 128, 16..500) // Радиус в блоках
    private var notifyInChat by boolValue("Оповещать в чат", true)
    private var resetOnDisable by boolValue("Сброс при отключении", true)

    override fun onEnabled() {
        super.onEnabled()
        session?.displayClientMessage("§8[§6ПоискСундуков§8] §aМодуль активирован. Сканирую область.")
        // Очищаем список обнаруженных сундуков при включении модуля,
        // чтобы найти все сундуки в текущей прогрузке
        discoveredChests.clear()
    }

    override fun onDisabled() {
        super.onDisabled()
        session?.displayClientMessage("§8[§6ПоискСундуков§8] §cМодуль деактивирован.")
        if (resetOnDisable) {
            discoveredChests.clear() // Очищаем список при отключении
        }
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
        if (packet is BlockActorDataPacket) {
            handleBlockActorDataPacket(packet)
        }
    }

    private fun handleBlockActorDataPacket(packet: BlockActorDataPacket) {
        val blockActorData = packet.nbt
        val blockActorId = blockActorData?.getString("id")

        // Проверяем, является ли сущность сундуком или его вариантом
        val isChest = when (blockActorId) {
            "Chest", "TrappedChest", "EnderChest" -> true
            // Добавьте сюда другие ID, если у вас есть моды или другие типы сундуков
            else -> false
        }

        if (isChest) {
            val chestPosition = Vector3f.from(
                packet.blockPosition.x.toFloat(),
                packet.blockPosition.y.toFloat(),
                packet.blockPosition.z.toFloat()
            )

            // Проверяем, находится ли сундук в радиусе сканирования
            val distance = calculateDistance(playerPosition, chestPosition)

            if (distance <= scanRadius.value.toFloat()) {
                // Добавляем сундук в список, если его ещё нет, и оповещаем
                if (discoveredChests.add(chestPosition)) { // add() возвращает true, если элемент был добавлен (т.е. его не было)
                    if (notifyInChat) {
                        val roundedDistance = ceil(distance).toInt()
                        val roundedCoords = chestPosition.roundUpCoordinates()
                        session?.displayClientMessage("§8[§6Сундук§8] §aОбнаружен сундук на координатах: §f$roundedCoords §aДистанция: §c$roundedDistance")
                    }
                }
            }
        }
    }

    // Вспомогательная функция для расчета дистанции (взята из PlayerTracerModule)
    private fun calculateDistance(from: Vector3f, to: Vector3f): Float {
        val dx = from.x - to.x
        val dy = from.y - to.y
        val dz = from.z - to.z
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    // Вспомогательная функция для округления координат (взята из PlayerTracerModule)
    private fun Vector3f.roundUpCoordinates(): String {
        val roundedX = ceil(this.x).toInt()
        val roundedY = ceil(this.y).toInt()
        val roundedZ = ceil(this.z).toInt()
        return "$roundedX, $roundedY, $roundedZ"
    }
}
