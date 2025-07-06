package com.retrivedmods.wclient.game.module.motion

import android.util.Log
import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
// If PlayerAuthInputData is not used, you can remove this import.
// import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData

class GravityControlModule : Module("GravityControl", ModuleCategory.Motion) {

    private var highJumpEnabled by boolValue("Высокий прыжок", true)
    private var slowFallingEnabled by boolValue("Замедленное падение", true)
    private var jumpVelocityBoost by floatValue("Сила прыжка", 0.8f, 0.1f..5.0f)
    private var slowFallFactor by floatValue("Фактор падения", 0.5f, 0.0f..1.0f)

    private var lastOnGroundState: Boolean = true
    private var lastMotionUpdateTime: Long = 0L
    private val motionUpdateInterval: Long = 50

    // FIX 1 & 3: Remove 'override' if Module class does not define open onEnable/onDisable.
    // If Module *does* define them as 'open', then keep 'override'.
    // Assuming for now they are not overridable methods from 'Module' based on the error.
    // If they are meant to be event listeners, they don't need 'override'.
    // If Module *should* have these, you'd need to modify the Module class.
    fun onEnable() {
        Log.d("GravityControlModule", "Модуль управления гравитацией включен.")
        // FIX 2: Correctly access isOnGround status.
        // Assuming MovePlayerPacket.isOnGround() is the reliable source.
        // If session.localPlayer has another way to get ground status, use that.
        // For now, we'll rely on the packet's info for the initial state.
        // We'll primarily use the MovePlayerPacket's isOnGround for continuous updates.
        // A direct 'session.localPlayer.isOnGround' might not exist or be updated synchronously.
        lastOnGroundState = false // Initialize to false, it will be updated by the first MovePlayerPacket
    }

    fun onDisable() {
        Log.d("GravityControlModule", "Модуль управления гравитацией выключен.")
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val bedrockPacket = interceptablePacket.packet
        val playerEntityId = session.localPlayer.runtimeEntityId
        if (playerEntityId == 0L) return

        val currentTime = System.currentTimeMillis()

        if (bedrockPacket is MovePlayerPacket) {
            val currentPos = bedrockPacket.getPosition()
            val currentOnGround = bedrockPacket.isOnGround() // This is the source for 'on ground' status

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
            if (highJumpEnabled && lastOnGroundState && !currentOnGround && playerMotion.y > 0.01f) {
                Log.d("GravityControlModule", "Обнаружен прыжок! Применяем высокий прыжок.")
                val newJumpVelocity = Vector3f.from(playerMotion.x, playerMotion.y + jumpVelocityBoost, playerMotion.z)
                val setMotionPacket = SetEntityMotionPacket().apply {
                    this.runtimeEntityId = playerEntityId
                    this.motion = newJumpVelocity
                }
                session.clientBound(setMotionPacket)
            }

            // --- ЛОГИКА ЗАМЕДЛЕННОГО ПАДЕНИЯ ---
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
                session.clientBound(setMotionPacket)
                lastMotionUpdateTime = currentTime
            }

            lastOnGroundState = currentOnGround // Update for the next tick
        }
    }
}
