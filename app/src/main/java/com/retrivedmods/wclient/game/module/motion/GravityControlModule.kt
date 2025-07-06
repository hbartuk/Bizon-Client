package com.retrivedmods.wclient.game.module.motion

import android.util.Log
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData // Возможно, понадобится для более точного отслеживания прыжков

class GravityControlModule : Module("GravityControl", ModuleCategory.Motion) {

    private var highJumpEnabled by boolValue("Высокий прыжок", true)
    private var slowFallingEnabled by boolValue("Замедленное падение", true)
    private var jumpVelocityBoost by floatValue("Сила прыжка", 0.8f, 0.1f..5.0f)
    private var slowFallFactor by floatValue("Фактор падения", 0.5f, 0.0f..1.0f)

    private var lastOnGroundState: Boolean = true
    private var lastMotionUpdateTime: Long = 0L // Добавим для контроля частоты отправки пакетов, если потребуется
    private val motionUpdateInterval: Long = 50 // Миллисекунды между отправкой пакетов движения, можно настроить

    override fun onEnable() {
        Log.d("GravityControlModule", "Модуль управления гравитацией включен.")
        lastOnGroundState = session.localPlayer.isOnGround // Инициализируем при включении
    }

    override fun onDisable() {
        Log.d("GravityControlModule", "Модуль управления гравитацией выключен.")
        // При отключении можно сбросить скорость игрока до нормальной, если это необходимо
        // Например, отправить SetEntityMotionPacket с Vector3f.ZERO, если игрок в воздухе
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val bedrockPacket = interceptablePacket.packet
        val playerEntityId = session.localPlayer.runtimeEntityId
        if (playerEntityId == 0L) return

        val currentTime = System.currentTimeMillis()

        if (bedrockPacket is MovePlayerPacket) {
            val currentPos = bedrockPacket.getPosition()
            val currentOnGround = bedrockPacket.isOnGround()

            // Обновляем позицию и скорость локального игрока в GameSession
            // Важно делать это до вычисления playerMotion, если lastPos берется оттуда
            val lastPos = session.localPlayer.vec3Position
            val playerMotion = if (lastPos != null) {
                Vector3f.from(
                    currentPos.x - lastPos.x,
                    currentPos.y - lastPos.y,
                    currentPos.z - lastPos.z
                )
            } else {
                Vector3f.ZERO
            }

            session.localPlayer.vec3Position = currentPos
            session.localPlayer.vec3Motion = playerMotion

            // --- ЛОГИКА ВЫСОКОГО ПРЫЖКА ---
            // Активируем высокий прыжок, если раньше были на земле, сейчас не на земле
            // и текущая вертикальная скорость положительна (т.е. игрок движется вверх)
            if (highJumpEnabled && lastOnGroundState && !currentOnGround && playerMotion.y > 0.01f) {
                Log.d("GravityControlModule", "Обнаружен прыжок! Применяем высокий прыжок.")
                val newJumpVelocity = Vector3f.from(playerMotion.x, playerMotion.y + jumpVelocityBoost, playerMotion.z)
                val setMotionPacket = SetEntityMotionPacket().apply {
                    this.runtimeEntityId = playerEntityId
                    this.motion = newJumpVelocity
                }
                session.clientBound(setMotionPacket) // Используем clientBound, как в BhopModule
            }

            // --- ЛОГИКА ЗАМЕДЛЕННОГО ПАДЕНИЯ ---
            // Активируем замедленное падение, если игрок не на земле и его вертикальная скорость отрицательна (т.е. падает)
            // Добавляем проверку на lastMotionUpdateTime для предотвращения спама, если необходимо
            if (slowFallingEnabled && !currentOnGround && playerMotion.y < -0.01f && (currentTime - lastMotionUpdateTime >= motionUpdateInterval || lastMotionUpdateTime == 0L)) {
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
                session.clientBound(setMotionPacket) // Используем clientBound
                lastMotionUpdateTime = currentTime
            }

            lastOnGroundState = currentOnGround
        }
        // Если вы хотите обрабатывать PlayerAuthInputPacket для более точного определения прыжка
        // как в BhopModule, это можно добавить здесь:
        /*
        else if (bedrockPacket is PlayerAuthInputPacket) {
            if (highJumpEnabled && packet.inputData.contains(PlayerAuthInputData.JUMP_DOWN)) {
                // Логика высокого прыжка, возможно, с задержкой или условием нахождения на земле
            }
        }
        */
    }
}
