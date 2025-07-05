package com.retrivedmods.wclient.game.module.motion // ПРОВЕРЬ: это путь, который был в ошибках. Если у тебя папка называется 'movement', измени на 'movement'.

import android.util.Log
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module // Твой базовый класс Module
import com.retrivedmods.wclient.game.ModuleCategory // Твой ModuleCategory
import org.cloudburstmc.math.vector.Vector3f // Импорт для Vector3f
import org.cloudburstmc.protocol.bedrock.data.PacketDirection // ДОБАВЬ ЭТОТ ИМПОРТ
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

// Предполагается, что ModuleCategory.Movement - это правильное значение из твоего enum ModuleCategory
class GravityControlModule : Module("GravityControl", ModuleCategory.Movement) {

    // Настраиваемые параметры модуля с использованием твоих делегатов
    private var highJumpEnabled by boolValue("Высокий прыжок", true)
    private var slowFallingEnabled by boolValue("Замедленное падение", true)
    private var jumpVelocityBoost by floatValue("Сила прыжка", 0.8f, 0.1f..5.0f)
    private var slowFallFactor by floatValue("Фактор падения", 0.5f, 0.0f..1.0f)

    // Внутреннее состояние модуля для отслеживания игрока
    private var lastOnGroundState: Boolean = true

    // Убраны 'super.' вызовы и 'override' если твой базовый Module не имеет этих методов или они не 'open'.
    // Если твой базовый Module *действительно* имеет open fun onEnable() и open fun onDisable(),
    // то оставь 'override'.
    override fun onEnable() {
        Log.d("GravityControlModule", "Модуль управления гравитацией включен.")
    }

    override fun onDisable() {
        Log.d("GravityControlModule", "Модуль управления гравитацией выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return // Проверяем, включен ли модуль

        val bedrockPacket = interceptablePacket.packet
        // Используем session.localPlayer.runtimeEntityId, как в твоем AutoHvHModule
        val playerEntityId = session.localPlayer.runtimeEntityId
        if (playerEntityId == 0L) return // Базовая проверка на валидность ID

        // Исправленный доступ к свойствам MovePlayerPacket и PacketDirection
        if (bedrockPacket is MovePlayerPacket && bedrockPacket.direction == PacketDirection.SERVER_BOUND) {
            val currentPos = bedrockPacket.position // .position - это публичное свойство
            val currentOnGround = bedrockPacket.isOnGround() // ИСПОЛЬЗУЕМ МЕТОД .isOnGround() как указано в ошибке (поле private)

            val lastPos = session.localPlayer.position // Предполагается, что LocalPlayer.position - это публичное свойство
            val playerMotion = if (lastPos != null) {
                // ДОСТУП К КОМПОНЕНТАМ Vector3f КАК К СВОЙСТВАМ (.x, .y, .z), а не как к функциям (.x(), .y(), .z())
                Vector3f.from(
                    currentPos.x - lastPos.x,
                    currentPos.y - lastPos.y,
                    currentPos.z - lastPos.z
                )
            } else {
                Vector3f.ZERO
            }
            // Обновляем позицию и скорость локального игрока в GameSession
            session.localPlayer.position = currentPos
            session.localPlayer.motion = playerMotion // Предполагается, что LocalPlayer.motion существует

            // --- ЛОГИКА ВЫСОКОГО ПРЫЖКА ---
            if (highJumpEnabled && lastOnGroundState && !currentOnGround && playerMotion.y > 0.01f) { // Доступ к y как к свойству
                Log.d("GravityControlModule", "Обнаружен прыжок! Применяем высокий прыжок.")
                val newJumpVelocity = Vector3f.from(playerMotion.x, playerMotion.y + jumpVelocityBoost, playerMotion.z) // Доступ к x, y, z как к свойствам
                val setMotionPacket = SetEntityMotionPacket().apply {
                    this.runtimeEntityId = playerEntityId
                    this.motion = newJumpVelocity
                }
                session.muCuteRelaySession?.server?.sendPacket(setMotionPacket)
            }

            // --- ЛОГИКА ЗАМЕДЛЕННОГО ПАДЕНИЯ ---
            if (slowFallingEnabled && !currentOnGround && playerMotion.y < 0f) { // Доступ к y как к свойству
                Log.d("GravityControlModule", "Обнаружено падение! Применяем замедленное падение.")
                val slowFallVelocity = Vector3f.from(
                    playerMotion.x,
                    playerMotion.y * slowFallFactor,
                    playerMotion.z
                )
                val setMotionPacket = SetEntityMotionPacket().apply {
                    this.runtimeEntityId = playerEntityId
                    this.motion = slowFallVelocity
                }
                session.muCuteRelaySession?.server?.sendPacket(setMotionPacket)
            }

            lastOnGroundState = currentOnGround
        }
    }
}
