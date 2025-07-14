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

class ChestFinderModule : Module("Поиск сундуков", ModuleCategory.Misc) {

    private var playerPosition: Vector3f = Vector3f.ZERO

    // Используем Set для отслеживания УЖЕ обнаруженных сундуков (для сообщений "Обнаружен" / "Повторно обнаружен")
    // Но управляется она теперь в handleBlockEntityDataPacket, без фоновой корутины.
    private val discoveredChests = ConcurrentHashMap.newKeySet<Vector3f>()

    // Настраиваемые опции для модуля
    private var scanRadius by intValue("Радиус сканирования", 128, 16..500)
    private var notifyInChat by boolValue("Оповещать в чат", true)
    private var resetOnDisable by boolValue("Сброс при отключении", true)

    // --- ГЛОБАЛЬНЫЙ ТАЙМЕР КАК В PositionLoggerModule ---
    private var chatMessageCooldownMs by intValue("Задержка сообщения в чате (мс)", 1000, 100..5000)
    private var lastChatMessageTime: Long = 0L // Время последнего отправленного сообщения
    // --- КОНЕЦ ГЛОБАЛЬНОГО ТАЙМЕРА ---

    // Удаляем все, что связано с фоновой корутиной (resendEnabled, resendIntervalSeconds, resendJob, startResendJob)

    override fun onEnabled() {
        super.onEnabled()
        session?.displayClientMessage("§8[§6ПоискСундуков§8] §aМодуль активирован. Сканирую область.")
        discoveredChests.clear() // Очищаем список при включении
        lastChatMessageTime = 0L // Сбрасываем таймер при включении
    }

    override fun onDisabled() {
        super.onDisabled()
        session?.displayClientMessage("§8[§6ПоискСундуков§8] §cМодуль деактивирован.")
        if (resetOnDisable) {
            discoveredChests.clear()
        }
        // Здесь нет корутины для отмены
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
            val currentTime = System.currentTimeMillis()

            // --- ЛОГИКА ГЛОБАЛЬНОГО ТАЙМЕРА ---
            // Отправляем сообщение только если прошло достаточно времени с момента последнего сообщения
            // И сундук находится в радиусе сканирования
            if (distance <= scanRadius.toFloat()) {
                // Если кулдаун прошёл, и оповещения в чат включены
                if (currentTime - lastChatMessageTime >= chatMessageCooldownMs) {
                    if (notifyInChat) {
                        val roundedDistance = ceil(distance).toInt()
                        val roundedCoords = chestPosition.roundUpCoordinates()

                        // Определяем префикс сообщения: "Обнаружен" (если это первое обнаружение)
                        // или "Повторно обнаружен" (если сундук уже был в списке).
                        // Метод add() возвращает true, если элемент успешно добавлен (то есть его не было).
                        val messagePrefix = if (discoveredChests.add(chestPosition)) {
                            "Обнаружен"
                        } else {
                            "Повторно обнаружен"
                        }

                        session?.displayClientMessage("§8[§6Сундук§8] §a$messagePrefix сундук на координатах: §f$roundedCoords §aДистанция: §c$roundedDistance")
                    }
                    lastChatMessageTime = currentTime // Обновляем время последнего отправленного сообщения
                }
            } else {
                // Если сундук находится вне радиуса сканирования, удаляем его из списка "обнаруженных".
                // Это важно, чтобы при повторном входе в радиус он снова считался "Обнаружен",
                // а не "Повторно обнаружен" сразу.
                discoveredChests.remove(chestPosition)
            }
            // --- КОНЕЦ ЛОГИКИ ГЛОБАЛЬНОГО ТАЙМЕРА ---
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
