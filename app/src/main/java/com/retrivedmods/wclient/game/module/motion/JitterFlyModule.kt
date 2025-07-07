package com.retrivedmods.wclient.game.module.motion

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.Ability
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket
import kotlin.math.cos
import kotlin.math.sin

class JitterFlyModule : Module("JitterFly", ModuleCategory.Motion) {

    private val horizontalSpeed by floatValue("Horizontal Speed", 3.0f, 0.5f..10.0f)
    private val verticalSpeed by floatValue("Vertical Speed", 2.0f, 0.5f..20.0f)
    private val jitterAmount by floatValue("Jitter Power", 0.05f, 0.01f..0.3f)
    private val motionInterval by intValue("Motion Delay", 50, 10..100)

    private var lastMotionTime = 0L
    private var jitterState = false
    private var flyEnabled = false

    private val flyAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries)
            abilityValues.addAll(Ability.entries)
            walkSpeed = 0.2f
            flySpeed = 1.5f
        })
    }

    private val resetAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries)
            abilityValues.addAll(Ability.entries)
            walkSpeed = 0.1f
            flySpeed = 0f
        })
    }

    private fun updateFlyAbility(enabled: Boolean) {
        if (flyEnabled != enabled) {
            flyAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            resetAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            session.clientBound(if (enabled) flyAbilitiesPacket else resetAbilitiesPacket)
            flyEnabled = enabled
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet is PlayerAuthInputPacket && isEnabled) {
            updateFlyAbility(true)

            val now = System.currentTimeMillis()
            if (now - lastMotionTime < motionInterval) return

            val vertical = when {
                packet.inputData.contains(PlayerAuthInputData.WANT_UP) -> verticalSpeed
                packet.inputData.contains(PlayerAuthInputData.WANT_DOWN) -> -verticalSpeed
                else -> if (jitterState) jitterAmount else -jitterAmount
            }

            val inputX = packet.motion.x
            val inputZ = packet.motion.y

            val yawRadians = Math.toRadians(packet.rotation.y.toDouble()).toFloat()
            val sinYaw = sin(yawRadians)
            val cosYaw = cos(yawRadians)

            val strafe = inputX * horizontalSpeed
            val forward = inputZ * horizontalSpeed

            val motionX = (strafe * cosYaw - forward * sinYaw)
            val motionZ = (forward * cosYaw + strafe * sinYaw)

            val motionPacket = SetEntityMotionPacket().apply {
                runtimeEntityId = session.localPlayer.runtimeEntityId
                motion = Vector3f.from(motionX, vertical, motionZ)
            }

            session.clientBound(motionPacket)

            jitterState = !jitterState
            lastMotionTime = now
        }
    }

    override fun onDisabled() {
        updateFlyAbility(false)
        super.onDisabled()
    }
}
