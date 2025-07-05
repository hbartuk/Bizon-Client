package com.retrivedmods.wclient.game.module.motion // ПРОВЕРЬ: это путь из твоих ошибок. Если папка 'movement', измени на 'movement'.

import android.util.Log
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module // Твой базовый класс Module
import com.retrivedmods.wclient.game.ModuleCategory // Твой ModuleCategory
import org.cloudburstmc.math.vector.Vector3f // Импорт для Vector3f
import org.cloudburstmc.protocol.bedrock.data.PacketDirection // ДОБАВЬ ЭТОТ ИМПОРТ (был unresolved reference)
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

// Предполагается, что ModuleCategory.Movement - это правильное значение из твоего enum ModuleCategory
// Если ошибка "Unresolved reference 'Movement'" повторяется, проверь точное имя элемента в твоем enum ModuleCategory.
class GravityControlModule : Module("GravityControl", ModuleCategory.Movement) {

    private var highJumpEnabled by boolValue("Высокий прыжок", true)
    private var slowFallingEnabled by boolValue("Замедленное падение", true)
    private var jumpVelocityBoost by floatValue("Сила прыжка", 0.8f, 0.1f..5.0f)
    private var slowFallFactor by floatValue("Фактор падения", 0.5f, 0.0f..1.0f)

    private var lastOnGroundState: Boolean = true

    // ИСПРАВЛЕНИЕ: Ошибки 'onEnable'/'onDisable' overrides nothing.
    // Убираем 'override', если в твоем базовом классе Module нет open fun onEnable()/onDisable().
    // Если они есть и помечены как 'open', тогда 'override' оставить.
    fun onEnable() { // Убрал 'override'
        Log.d("GravityControlModule", "Модуль управления гравитацией включен.")
    }

    fun onDisable() { // Убрал 'override'
        Log.d("GravityControlModule", "Модуль управления гравитацией выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val bedrockPacket = interceptablePacket.packet
        // Используем session.localPlayer.runtimeEntityId, как в твоем AutoHvHModule
        val playerEntityId = session.localPlayer.runtimeEntityId
        if (playerEntityId == 0L) return

        // ИСПРАВЛЕНИЕ: Доступ к свойствам пакетов CloudburstMC Protocol Bedrock через ГЕТТЕРЫ.
        // Ошибки "Unresolved reference 'direction'", "Unresolved reference 'position'", "Cannot access 'field onGround: Boolean': it is private"
        // говорят, что нужны геттеры, даже если в других пакетах direct access работает.
        if (bedrockPacket is MovePlayerPacket && bedrockPacket.getDirection() == PacketDirection.SERVER_BOUND) { // ИСПОЛЬЗУЕМ .getDirection()
            val currentPos = bedrockPacket.getPosition() // ИСПОЛЬЗУЕМ .getPosition()
            val currentOnGround = bedrockPacket.isOnGround() // ИСПОЛЬЗУЕМ .isOnGround()

            // ИСПРАВЛЕНИЕ: Доступ к полям LocalPlayer. Используем .vec3Position и .vec3Motion
            // на основе примера AutoHvHModule и PlayerTracerModule.
            val lastPos = session.localPlayer.vec3Position // ИСПОЛЬЗУЕМ .vec3Position
            val playerMotion = if (lastPos != null) {
                // Доступ к компонентам Vector3f КАК К СВОЙСТВАМ (.x, .y, .z)
                Vector3f.from(
                    currentPos.x - lastPos.x,
                    currentPos.y - lastPos.y,
                    currentPos.z - lastPos.z
                )
            } else {
                Vector3f.ZERO
            }
            // Обновляем позицию и скорость локального игрока в GameSession
            session.localPlayer.vec3Position = currentPos // ИСПОЛЬЗУЕМ .vec3Position
            session.localPlayer.vec3Motion = playerMotion // ИСПОЛЬЗУЕМ .vec3Motion (предполагаем, что такое поле есть)

            // --- ЛОГИКА ВЫСОКОГО ПРЫЖКА ---
            if (highJumpEnabled && lastOnGroundState && !currentOnGround && playerMotion.y > 0.01f) {
                Log.d("GravityControlModule", "Обнаружен прыжок! Применяем высокий прыжок.")
                val newJumpVelocity = Vector3f.from(playerMotion.x, playerMotion.y + jumpVelocityBoost, playerMotion.z)
                val setMotionPacket = SetEntityMotionPacket().apply {
                    this.runtimeEntityId = playerEntityId
                    this.motion = newJumpVelocity
                }
                session.muCuteRelaySession?.server?.sendPacket(setMotionPacket)
            }

            // --- ЛОГИКА ЗАМЕДЛЕННОГО ПАДЕНИЯ ---
            if (slowFallingEnabled && !currentOnGround && playerMotion.y < 0f) {
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
