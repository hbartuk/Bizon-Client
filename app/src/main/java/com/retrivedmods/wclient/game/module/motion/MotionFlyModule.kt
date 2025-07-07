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

class MotionFlyModule : Module("motion_fly", ModuleCategory.Motion) {

    private val horizontalSpeed = floatValue("horizontalSpeed", 3.5f, 0.5f..10.0f)
    private val verticalSpeed = floatValue("verticalSpeed", 1.5f, 0.5f..100.0f)
    private val glideSpeed = floatValue("glideSpeed", 0.1f, -0.01f..1.0f)
    private val bypassMode = boolValue("lifeboatBypass", true)
    private val motionInterval = floatValue("delay", 50.0f, 10.0f..100.0f)

    private var lastMotionTime = 0L
    private var jitterState = false
    private var canFly = false

    private val flyAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(arrayOf(
                Ability.BUILD, Ability.MINE, Ability.DOORS_AND_SWITCHES,
                Ability.OPEN_CONTAINERS, Ability.ATTACK_PLAYERS, Ability.ATTACK_MOBS,
                Ability.MAY_FLY, Ability.FLY_SPEED, Ability.WALK_SPEED, Ability.OPERATOR_COMMANDS
            ))
            walkSpeed = 0.2f
            flySpeed = 1.5f
        })
    }

    private val resetAbilitiesPacket = UpdateAbilitiesPacket().apply {
        playerPermission = PlayerPermission.OPERATOR
        commandPermission = CommandPermission.OWNER
        abilityLayers.add(AbilityLayer().apply {
            layerType = AbilityLayer.Type.BASE
            abilitiesSet.addAll(Ability.entries.toTypedArray())
            abilityValues.addAll(arrayOf(
                Ability.BUILD, Ability.MINE, Ability.DOORS_AND_SWITCHES,
                Ability.OPEN_CONTAINERS, Ability.ATTACK_PLAYERS, Ability.ATTACK_MOBS,
                Ability.OPERATOR_COMMANDS
            ))
            walkSpeed = 0.1f
            flySpeed = 0f
        })
    }

    private fun handleFlyAbilities(isEnabled: Boolean) {
        if (canFly != isEnabled) {
            flyAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            resetAbilitiesPacket.uniqueEntityId = session.localPlayer.uniqueEntityId
            session.clientBound(if (isEnabled) flyAbilitiesPacket else resetAbilitiesPacket)
            canFly = isEnabled
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet

        if (packet is PlayerAuthInputPacket) {
            handleFlyAbilities(isEnabled)

            if (isEnabled && System.currentTimeMillis() - lastMotionTime >= motionInterval.value) {

                val vertical = when {
                    packet.inputData.contains(PlayerAuthInputData.WANT_UP) -> verticalSpeed.value
                    packet.inputData.contains(PlayerAuthInputData.WANT_DOWN) -> -verticalSpeed.value
                    bypassMode.value -> -glideSpeed.value.coerceAtLeast(-0.1f)
                    else -> glideSpeed.value
                }


                val inputX = packet.motion.x
                val inputZ = packet.motion.y


                val yaw = Math.toRadians(packet.rotation.y.toDouble()).toFloat()
                val sinYaw = sin(yaw)
                val cosYaw = cos(yaw)


                val strafe = inputX * horizontalSpeed.value
                val forward = inputZ * horizontalSpeed.value

                val motionX = (strafe * cosYaw - forward * sinYaw)
                val motionZ = (forward * cosYaw + strafe * sinYaw)

                val motionPacket = SetEntityMotionPacket().apply {
                    runtimeEntityId = session.localPlayer.runtimeEntityId
                    motion = Vector3f.from(
                        motionX,
                        vertical + if (jitterState) 0.05f else -0.05f,
                        motionZ
                    )
                }

                session.clientBound(motionPacket)
                jitterState = !jitterState
                lastMotionTime = System.currentTimeMillis()
            }
        }
    }
}
