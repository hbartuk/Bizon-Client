package com.retrivedmods.wclient.game.module.motion // Адаптируй этот путь к твоей структуре пакетов

import android.util.Log
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module // Используем твой Module
import com.retrivedmods.wclient.game.ModuleCategory // Используем твой ModuleCategory
import org.cloudburstmc.math.vector.Vector3f // Для работы с векторами скорости
import org.cloudburstmc.protocol.bedrock.data.PacketDirection // Для определения направления пакета
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket // Пакет движения игрока
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket // Пакет для установки скорости сущности (мы будем отправлять его клиенту)

// Используем твой конструктор Module с именем и категорией
class GravityControlModule : Module("GravityControl", ModuleCategory.Movement) {

    // Настраиваемые параметры модуля с использованием твоих делегатов
    private var highJumpEnabled by boolValue("Высокий прыжок", true) // Включен ли высокий прыжок
    private var slowFallingEnabled by boolValue("Замедленное падение", true) // Включено ли замедленное падение
    private var jumpVelocityBoost by floatValue("Сила прыжка", 0.8f, 0.1f..5.0f) // Дополнительная скорость по Y при прыжке
    private var slowFallFactor by floatValue("Фактор падения", 0.5f, 0.0f..1.0f) // Множитель для скорости падения (0.0 = нет падения, 1.0 = обычное падение)

    // Внутреннее состояние модуля для отслеживания игрока
    private var lastOnGroundState: Boolean = true // Были ли мы в прошлый раз на земле

    override fun onEnable() {
        super.onEnable() // Вызываем метод базового класса
        Log.d("GravityControlModule", "Модуль управления гравитацией включен.")
    }

    override fun onDisable() {
        super.onDisable() // Вызываем метод базового класса
        Log.d("GravityControlModule", "Модуль управления гравитацией выключен.")
        // Здесь можно отправить пакет, чтобы сбросить скорость игрока, если он в воздухе
        // Однако будь осторожен, чтобы не вызвать лишних проблем с синхронизацией.
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        // Проверяем, что модуль включен
        if (!isEnabled) return

        val bedrockPacket = interceptablePacket.packet
        // Получаем Runtime ID локального игрока. Он нужен для пакета SetEntityMotionPacket.
        // Убедись, что session.localPlayer.entityId всегда доступен и корректен.
        val playerEntityId = session.localPlayer.entityId ?: return // Если ID не найден, выходим

        // Мы интересуемся только пакетами движения, которые идут от клиента к прокси (SERVER_BOUND)
        if (bedrockPacket is MovePlayerPacket && bedrockPacket.direction == PacketDirection.SERVER_BOUND) {
            val currentPos = bedrockPacket.position // Текущая позиция игрока
            val currentOnGround = bedrockPacket.onGround // Находится ли игрок на земле

            // Оцениваем текущую скорость игрока на основе изменения позиции.
            // Это приблизительная оценка. Для более точной скорости тебе нужно
            // чтобы session.localPlayer.motion обновлялся более надежно (например, из входящих SetEntityMotionPacket).
            val lastPos = session.localPlayer.position // Получаем последнюю известную позицию из GameSession
            val playerMotion = if (lastPos != null) {
                Vector3f.from(
                    currentPos.x - lastPos.x,
                    currentPos.y - lastPos.y,
                    currentPos.z - lastPos.z
                )
            } else {
                Vector3f.ZERO // Если lastPos нет, то скорость 0
            }
            // Обновляем позицию локального игрока в GameSession
            session.localPlayer.position = currentPos
            // Здесь же можно обновить и motion в session.localPlayer, если ты его там отслеживаешь
            session.localPlayer.motion = playerMotion


            // --- ЛОГИКА ВЫСОКОГО ПРЫЖКА ---
            // Обнаруживаем прыжок: игрок был на земле (lastOnGroundState), теперь не на земле (!currentOnGround),
            // и движется вверх (playerMotion.y() > 0.01)
            if (highJumpEnabled && lastOnGroundState && !currentOnGround && playerMotion.y() > 0.01) {
                Log.d("GravityControlModule", "Обнаружен прыжок! Применяем высокий прыжок.")
                // Отправляем пакет SetEntityMotionPacket К КЛИЕНТУ, чтобы дать ему мгновенный импульс вверх
                val newJumpVelocity = Vector3f.from(playerMotion.x(), playerMotion.y() + jumpVelocityBoost, playerMotion.z())
                val setMotionPacket = SetEntityMotionPacket().apply {
                    this.runtimeEntityId = playerEntityId
                    this.motion = newJumpVelocity
                }
                session.muCuteRelaySession?.server?.sendPacket(setMotionPacket) // Отправляем клиенту, подключенному к нашему прокси
                // Мы НЕ перехватываем оригинальный MovePlayerPacket клиента здесь.
                // Пусть он идет на сервер. Прокси просто отправляет клиенту дополнительное обновление скорости.
            }

            // --- ЛОГИКА ЗАМЕДЛЕННОГО ПАДЕНИЯ ---
            // Применяем замедленное падение, если оно включено, игрок не на земле и движется вниз
            if (slowFallingEnabled && !currentOnGround && playerMotion.y() < 0) { // Только если скорость по Y отрицательна (падает)
                // Log.d("GravityControlModule", "Обнаружено падение! Применяем замедленное падение.") // Это может спамить в логах, используй для отладки
                val slowFallVelocity = Vector3f.from(
                    playerMotion.x(),
                    playerMotion.y() * slowFallFactor, // Уменьшаем скорость падения
                    playerMotion.z()
                )
                val setMotionPacket = SetEntityMotionPacket().apply {
                    this.runtimeEntityId = playerEntityId
                    this.motion = slowFallVelocity
                }
                session.muCuteRelaySession?.server?.sendPacket(setMotionPacket) // Отправляем клиенту
            }

            // Обновляем состояние "на земле" для следующей итерации
            lastOnGroundState = currentOnGround
        }
    }
}
