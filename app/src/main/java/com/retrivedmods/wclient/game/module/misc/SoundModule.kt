// File: com.retrivedmods.wclient.game.module.misc.SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.GameSession // Убедитесь, что GameSession импортирован

import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.math.vector.Vector3f // Для координат игрока

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    // override fun initialize() вызывается после того, как 'session' будет присвоена (в ModuleManager.registerModules)
    override fun initialize() {
        super.initialize()
        // Проверка this::session.isInitialized подтвердит, что 'session' была присвоена.
        if (this::session.isInitialized) {
            session.displayClientMessage("§a[SoundModule] Модуль Sound проинициализирован. Сессия доступна.")
        } else {
            println("ERROR: Session is NOT initialized in SoundModule.initialize(). Critical setup error!")
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        if (this::session.isInitialized) {
            session.displayClientMessage("§a[SoundModule] Модуль Sound активирован. Сессия доступна.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        if (this::session.isInitialized) { // Проверка, чтобы не вызывать, если сессия уже уничтожена
            session.displayClientMessage("§c[SoundModule] Модуль Sound деактивирован.")
        }
    }

    fun playSound(soundName: String, volume: Float, pitch: Float) {
        println("DEBUG: SoundModule.playSound() called for sound: $soundName (Volume: $volume, Pitch: $pitch)")

        // Проверяем, инициализирована ли 'session' и доступна ли 'muCuteRelaySession'
        // 'this::session.isInitialized' безопасна для 'lateinit' переменных.
        if (!this::session.isInitialized || session.muCuteRelaySession == null) {
            println("ERROR: Session or session.muCuteRelaySession is not available in playSound(). Cannot play sound: $soundName")
            // Если 'session' сама не инициализирована, мы не можем использовать 'session.displayClientMessage'
            if (this::session.isInitialized) {
                session.displayClientMessage("§c[SoundModule] Сессия или MuCuteRelaySession недоступна для воспроизведения звука.")
            }
            return
        }

        // Получаем текущую позицию игрока из GameSession.localPlayer.
        // Предполагается, что 'session.localPlayer.position' возвращает Vector3f.
        val playerPos: Vector3f = session.localPlayer.position 

        val playSoundPacket = PlaySoundPacket().apply {
            // Установка имени звука. 'soundIdentifier' - стандартное название свойства в CloudburstMC.
            // Если компилятор все еще жалуется на 'soundIdentifier', проверьте реальные свойства
            // класса PlaySoundPacket в вашей библиотеке (возможно, это 'soundId' или 'name').
            this.soundIdentifier = soundName 
            
            // Установка координат звука. Предполагается, что PlaySoundPacket имеет свойства x, y, z типа Float.
            // Если эти поля все еще не разрешены, PlaySoundPacket, возможно, ожидает Vector3f
            // через свойство 'position' или метод 'setPosition(Vector3f)'.
            this.x = playerPos.x 
            this.y = playerPos.y
            this.z = playerPos.z

            this.volume = volume
            this.pitch = pitch
        }

        // Отправляем пакет на клиент, чтобы воспроизвести звук.
        session.clientBound(playSoundPacket)

        // Сообщение в чат без указания координат
        session.displayClientMessage("§a[SoundModule] Попытка воспроизвести звук: §b$soundName")
        println("DEBUG: PlaySoundPacket sent for sound: $soundName with volume $volume and pitch $pitch.")
    }

    fun stopAllSounds() {
        // Заглушка: В протоколе Bedrock Edition нет прямого пакета для "остановки всех звуков".
        // Если вам нужна эта функциональность, потребуется более сложная реализация,
        // возможно, отслеживающая активные звуки и отправляющая индивидуальные пакеты остановки.
        if (!this::session.isInitialized || session.muCuteRelaySession == null) {
            println("DEBUG: Cannot stop all sounds, session or MuCuteRelaySession not initialized.")
            if (this::session.isInitialized) {
                session.displayClientMessage("§c[SoundModule] Сессия или релей-сессия не активны для остановки звуков.")
            }
            return
        }
        
        session.displayClientMessage("§e[SoundModule] Заглушка: Функция 'stopAllSounds' не имеет прямой реализации в MCBE. Необходима пользовательская логика.")
        println("DEBUG: stopAllSounds() called. No direct MCBE packet. Custom implementation needed.")
    }
}
