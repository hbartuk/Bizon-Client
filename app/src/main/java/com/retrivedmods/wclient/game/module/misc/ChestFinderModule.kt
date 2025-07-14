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

// Для работы с корутинами
import kotlinx.coroutines.* // Убедитесь, что эта зависимость добавлена: org.jetbrains.kotlinx:kotlinx-coroutines-core

class ChestFinderModule : Module("Поиск сундуков", ModuleCategory.Misc) {

    private var playerPosition: Vector3f = Vector3f.ZERO

    // Храним позицию сундука и время последнего отправленного уведомления для КАЖДОГО сундука
    private val discoveredChests = ConcurrentHashMap<Vector3f, Long>()

    // Настраиваемые опции для модуля
    private var scanRadius by intValue("Радиус сканирования", 128, 16..500)
    private var notifyInChat by boolValue("Оповещать в чат", true)
    private var resetOnDisable by boolValue("Сброс при отключении", true)

    // --- ОПЦИИ ДЛЯ ПОВТОРНОЙ ОТПРАВКИ ЧЕРЕЗ КОРУТИНУ ---
    private var resendEnabled by boolValue("Повторная отправка", false)
    private var resendIntervalSeconds by intValue("Интервал повтора (сек)", 5, 1..60)
    // --- КОНЕЦ ОПЦИЙ ---

    private var resendJob: Job? = null // Для управления фоновой корутиной

    override fun onEnabled() {
        super.onEnabled()
        session?.displayClientMessage("§8[§6ПоискСундуков§8] §aМодуль активирован. Сканирую область.")
        discoveredChests.clear() // Очищаем список при включении

        // Запускаем фоновую задачу для повторной отправки, если опция включена
        if (resendEnabled) {
            startResendJob()
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        session?.displayClientMessage("§8[§6ПоискСундуков§8] §cМодуль деактивирован.")
        resendJob?.cancel() // Отменяем фоновую задачу при отключении модуля
        resendJob = null
        if (resetOnDisable) {
            discoveredChests.clear()
        }
    }

    // --- ФОНОВАЯ ЗАДАЧА ДЛЯ ПОВТОРНОЙ ОТПРАВКИ И УДАЛЕНИЯ ВЫШЕДШИХ СУНДУКОВ ---
    @OptIn(DelicateCoroutinesApi::class) // Используем GlobalScope для простоты примера
    private fun startResendJob() {
        resendJob?.cancel() // Отменяем любую существующую задачу перед запуском новой
        resendJob = GlobalScope.launch(Dispatchers.Default) {
            while (isActive) { // Пока корутина активна
                val currentTime = System.currentTimeMillis()
                val intervalMs = resendIntervalSeconds * 1000L

                // Создаем копию ключей, чтобы избежать ConcurrentModificationException
                // при изменении map во время итерации
                val chestsToProcess = discoveredChests.keys.toList()
                val chestsToRemove = mutableListOf<Vector3f>()

                for (chestPos in chestsToProcess) {
                    val lastSent = discoveredChests[chestPos] ?: continue // Время последнего уведомления для этого сундука

                    val distance = calculateDistance(playerPosition, chestPos)

                    // Если сундук вышел за радиус сканирования, помечаем его для удаления
                    if (distance > scanRadius.toFloat()) {
                        chestsToRemove.add(chestPos)
                        continue // Переходим к следующему сундуку
                    }

                    // Если повторная отправка включена И прошло достаточно времени с последнего уведомления этого сундука
                    if (resendEnabled && currentTime - lastSent >= intervalMs) {
                        if (notifyInChat) {
                            val roundedDistance = ceil(distance).toInt()
                            val roundedCoords = chestPos.roundUpCoordinates()
                            session?.displayClientMessage("§8[§6Сундук§8] §aПовторно обнаружен сундук на координатах: §f$roundedCoords §aДистанция: §c$roundedDistance")
                        }
                        discoveredChests[chestPos] = currentTime // Обновляем время последнего уведомления для этого сундука
                    }
                }
                // Удаляем сундуки, вышедшие за радиус
                chestsToRemove.forEach { discoveredChests.remove(it) }

                delay(1000L) // Проверяем состояние каждую секунду (это не интервал повтора, а частота проверки)
            }
        }
    }
    // --- КОНЕЦ ФОНОВОЙ ЗАДАЧИ ---

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

            // Если сундук находится в радиусе сканирования
            if (distance <= scanRadius.toFloat()) {
                // Если этот сундук обнаружен впервые (его нет в списке discoveredChests)
                if (!discoveredChests.containsKey(chestPosition)) {
                    if (notifyInChat) {
                        val roundedDistance = ceil(distance).toInt()
                        val roundedCoords = chestPosition.roundUpCoordinates()
                        session?.displayClientMessage("§8[§6Сундук§8] §aОбнаружен сундук на координатах: §f$roundedCoords §aДистанция: §c$roundedDistance")
                    }
                    // Добавляем сундук в список с текущим временем (это время первого обнаружения/уведомления)
                    discoveredChests[chestPosition] = currentTime
                } else {
                    // Если сундук уже есть в списке, просто обновляем его lastSentTime.
                    // Это нужно, чтобы фоновая корутина не отправляла сообщение сразу,
                    // если сервер сам прислал пакет об этом сундуке.
                    // Иначе, если resendIntervalSeconds очень большой, а packet приходит часто,
                    // мы могли бы его пропустить.
                    discoveredChests[chestPosition] = currentTime
                }
            } else {
                // Если сундук, о котором пришел пакет, находится за пределами радиуса,
                // удаляем его из списка (если он там был).
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
