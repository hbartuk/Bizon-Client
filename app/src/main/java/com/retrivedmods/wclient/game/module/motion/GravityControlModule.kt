package com.retrivedmods.wclient.game.module.motion

import android.util.Log
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory // Убедитесь, что этот импорт присутствует
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PacketDirection // Этот импорт все еще нужен для других мест, если ты используешь PacketDirection
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class GravityControlModule : Module("GravityControl", ModuleCategory.Movement) { // Ошибка "Movement" здесь

    private var highJumpEnabled by boolValue("Высокий прыжок", true)
    private var slowFallingEnabled by boolValue("Замедленное падение", true)
    private var jumpVelocityBoost by floatValue("Сила прыжка", 0.8f, 0.1f..5.0f)
    private var slowFallFactor by floatValue("Фактор падения", 0.5f, 0.0f..1.0f)

    private var lastOnGroundState: Boolean = true

    fun onEnable() {
        Log.d("GravityControlModule", "Модуль управления гравитацией включен.")
    }

    fun onDisable() {
        Log.d("GravityControlModule", "Модуль управления гравитацией выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val bedrockPacket = interceptablePacket.packet
        val playerEntityId = session.localPlayer.runtimeEntityId
        if (playerEntityId == 0L) return

        // ИЗМЕНЕНО: Удалена проверка getDirection(), так как MovePlayerPacket не имеет такого метода.
        if (bedrockPacket is MovePlayerPacket) {
            val currentPos = bedrockPacket.getPosition() // Используем getPosition()
            val currentOnGround = bedrockPacket.isOnGround() // Используем isOnGround()

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
            // Обновляем позицию и скорость локального игрока в GameSession
            session.localPlayer.vec3Position = currentPos
            session.localPlayer.vec3Motion = playerMotion

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
