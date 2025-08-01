package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket
import org.cloudburstmc.protocol.bedrock.data.SoundEvent
import org.cloudburstmc.math.vector.Vector3f
import kotlin.random.Random
import org.cloudburstmc.protocol.bedrock.packet.LevelSoundEvent2Packet

import org.cloudburstmc.protocol.bedrock.data.SoundEvent.BLOCK_PLACE
import org.cloudburstmc.protocol.bedrock.data.SoundEvent.ENTITY_HURT

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    private val workingSounds = listOf(
        "random.pop", "note.pling", "random.click", "random.orb", "mob.endermen.portal",
        "random.anvil_land", "random.break", "tile.piston.out", "mob.ghast.scream", "random.explode",
        "random.bow", "mob.zombie.say", "mob.skeleton.say", "random.levelup", "mob.enderdragon.growl"
    )

    override fun onEnabled() {
        super.onEnabled()
        session?.displayClientMessage("§a[SoundModule] Модуль активирован.")
        playSound("random.pop", 1.0f, 1.0f)
    }

    override fun onDisabled() {
        super.onDisabled()
        session?.displayClientMessage("§c[SoundModule] Модуль деактивирован.")
    }

    fun playSound(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        val currentSession = session ?: return
        val player = currentSession.localPlayer
        if (player == null) {
            currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен.")
            return
        }

        val playerPos = player.vec3Position ?: Vector3f.ZERO
        val playSoundPacket = PlaySoundPacket().apply {
            setSound(soundName)
            setPosition(playerPos)
            setVolume(volume.coerceIn(0.0f, 10.0f))
            setPitch(pitch.coerceIn(0.1f, 2.0f))
        }

        currentSession.serverBound(playSoundPacket)
        currentSession.displayClientMessage("§a[Sound] §b$soundName §7отправлен §8(V:$volume P:$pitch)")
    }

    fun playLevelSound(
        soundEvent: SoundEvent,
        identifier: String = "",
        extraData: Int = -1
    ) {
        val currentSession = session ?: return
        val player = currentSession.localPlayer
        if (player == null) {
            currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен для LevelSound.")
            return
        }

        val playerPos = player.vec3Position ?: Vector3f.ZERO
        val packet = LevelSoundEvent2Packet().apply {
            sound = soundEvent
            position = playerPos
            this.extraData = extraData
            this.identifier = identifier
        }

        currentSession.serverBound(packet)
        currentSession.displayClientMessage("§a[LevelSound] §b$soundEvent §7отправлен на сервер")
    }

    fun listAvailableSounds(): List<String> = workingSounds

    fun testSounds() {
        val currentSession = session ?: return
        currentSession.displayClientMessage("§e[SoundModule] §7Начинаю тест звуков...")

        Thread {
            workingSounds.take(5).forEachIndexed { index, sound ->
                try {
                    Thread.sleep(1500L)
                    playSound(sound, 1.0f, 1.0f)
                    session?.displayClientMessage("§7[${index + 1}/5] Тест: §b$sound")
                } catch (e: Exception) {
                    println("ERROR в testSounds: ${e.message}")
                }
            }
            session?.displayClientMessage("§a[SoundModule] §7Тест завершен!")
        }.start()
    }

    fun playRandomSound(volume: Float = 1.0f, pitch: Float = 1.0f) {
        val randomSound = workingSounds.random()
        playSound(randomSound, volume, pitch)
        session?.displayClientMessage("§e[Random] §b$randomSound")
    }

    fun playAttackSound() {
        val currentSession = session ?: return
        val player = currentSession.localPlayer
        if (player == null) {
            currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен для звука атаки.")
            return
        }
        
        val packet = LevelSoundEvent2Packet().apply {
            sound = SoundEvent.ATTACK_NODAMAGE
            position = player.vec3Position ?: Vector3f.ZERO
            extraData = -1
            identifier = "minecraft:player"
        }

        currentSession.serverBound(packet)
        currentSession.displayClientMessage("§a[AttackSound] Звук атаки отправлен!")
    }

    fun playLevelSoundAdvanced(
        soundEvent: SoundEvent,
        identifier: String = "minecraft:player",
        extraData: Int = -1,
        babySound: Boolean = false,
        relativeVolumeDisabled: Boolean = false
    ) {
        val currentSession = session ?: return
        val player = currentSession.localPlayer
        if (player == null) {
            currentSession.displayClientMessage("§c[SoundModule] Игрок недоступен.")
            return
        }

        val packet = LevelSoundEvent2Packet().apply {
            sound = soundEvent
            position = player.vec3Position ?: Vector3f.ZERO
            this.extraData = extraData
            this.identifier = identifier
        }

        currentSession.serverBound(packet)
        currentSession.displayClientMessage("§a[AdvancedLevel] §b$soundEvent §7отправлен")
    }

    fun playSoundSafe(soundName: String, volume: Float = 1.0f, pitch: Float = 1.0f) {
        val currentSession = session ?: return
        if (workingSounds.contains(soundName)) {
            playSound(soundName, volume, pitch)
        } else {
            currentSession.displayClientMessage("§c[Sound] Неизвестный звук: §7$soundName")
            currentSession.displayClientMessage("§7Используй: §e.sound list §7для просмотра доступных")
        }
    }

    fun stopAllSounds() {
        val currentSession = session ?: return
        val playerPos = currentSession.localPlayer?.vec3Position ?: Vector3f.ZERO
        
        val silentPacket = PlaySoundPacket().apply {
            setSound("random.click")
            setPosition(playerPos)
            setVolume(0.0f)
            setPitch(1.0f)
        }
        currentSession.serverBound(silentPacket)
        currentSession.displayClientMessage("§e[SoundModule] Попытка заглушить звуки (ограниченная поддержка)")
    }

    fun testLevelSounds() {
        val testEvents = listOf(
            SoundEvent.ATTACK_NODAMAGE, BLOCK_PLACE, SoundEvent.ITEM_USE_ON,
            SoundEvent.STEP, SoundEvent.HIT, ENTITY_HURT
        )

        val currentSession = session ?: return
        currentSession.displayClientMessage("§e[SoundModule] Тест LevelSound событий...")

        Thread {
            testEvents.forEachIndexed { index, event ->
                try {
                    Thread.sleep(2000L)
                    playLevelSoundAdvanced(event)
                    session?.displayClientMessage("§7[${index + 1}/${testEvents.size}] §b$event")
                } catch (e: Exception) {
                    println("ERROR в testLevelSounds: ${e.message}")
                }
            }
            session?.displayClientMessage("§a[SoundModule] Тест LevelSound завершен!")
        }.start()
    }
}
