package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.math.vector.Vector3f // Для координат звука
// org.cloudburstmc.math.vector.Vector3i // Если вам нужны целочисленные координаты

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    // ВАЖНО: Мы больше не будем использовать ::session.isInitialized в initialize() и onEnabled()
    // так как ModuleManager.initialize() (или ваш механизм регистрации модулей)
    // ГАРАНТИРУЕТ, что session будет установлена до вызова initialize() модуля.
    // Если она не установлена, это фундаментальная ошибка, и проверять её здесь не имеет смысла.

    override fun initialize() {
        super.initialize()
        // На этом этапе session ГАРАНТИРОВАННО инициализирована благодаря ModuleManager.initialize()
        println("DEBUG: SoundModule.initialize() called. Session should be initialized: ${this::session.isInitialized}.")
        // Проверка на инициализацию session перед использованием displayClientMessage
        if (this::session.isInitialized) {
            session.displayClientMessage("§a[SoundModule] Модуль Sound проинициализирован. Сессия доступна.")
        } else {
            println("ERROR: Session is not initialized in SoundModule.initialize()! This should not happen.")
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        // На этом этапе session ГАРАНТИРОВАННО инициализирована.
        println("DEBUG: SoundModule.onEnabled() called. Session should be initialized: ${this::session.isInitialized}.")
        if (this::session.isInitialized) {
            session.displayClientMessage("§a[SoundModule] Модуль Sound активирован. Сессия доступна.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        // Здесь session также должна быть инициализирована, если модуль был активен
        println("DEBUG: SoundModule.onDisabled() called.")
        if (this::session.isInitialized) { // Проверка на null для session.displayClientMessage
            session.displayClientMessage("§c[SoundModule] Модуль Sound деактивирован.")
        }
    }

    // Если SoundModule обрабатывает пакеты, раскомментируйте и реализуйте:
    // override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
    //     // Ваш код обработки пакетов для SoundModule
    // }
    //
    // override fun afterPacketBound(packet: BedrockPacket) {
    //     // Ваш код после обработки пакетов для SoundModule
    // }
    //
    // override fun onDisconnect(reason: String) {
    //     // Ваш код при отключении
    // }

    fun playSound(soundName: String, volume: Float, pitch: Float) {
        println("DEBUG: SoundModule.playSound() called for sound: $soundName (Volume: $volume, Pitch: $pitch)")

        // Важная проверка: убедитесь, что session инициализирована и muCuteRelaySession доступна.
        // Если session не инициализирована, значит, что-то пошло не так на более ранних этапах.
        if (!this::session.isInitialized || session.muCuteRelaySession == null) {
            session.displayClientMessage("§c[SoundModule] Сессия или MuCuteRelaySession недоступна для воспроизведения звука.")
            println("ERROR: session or session.muCuteRelaySession is not available in playSound(). Cannot play sound.")
            return
        }

        val playSoundPacket = PlaySoundPacket().apply {
            // Установка имени звука. 'soundIdentifier' - это правильное поле для CloudburstMC
            soundIdentifier = soundName
            
            // Получение текущих координат игрока из GameSession.localPlayer
            // GameSession.localPlayer.position - это Vector3f, поэтому преобразования в Float не нужны,
            // если PlaySoundPacket принимает float напрямую.
            // Если PlaySoundPacket принимает int, используйте .toInt()
            x = session.localPlayer.position.x
            y = session.localPlayer.position.y
            z = session.localPlayer.position.z

            // Установка громкости и питча
            this.volume = volume
            this.pitch = pitch
        }

        // Отправляем пакет на клиент (чтобы игрок услышал звук)
        session.clientBound(playSoundPacket)
        // Возможно, вам также нужно отправить его на сервер, если это часть геймплея.
        // session.serverBound(playSoundPacket)

        session.displayClientMessage("§a[SoundModule] Попытка воспроизвести звук: §b$soundName §7на координатах §e(${x.toInt()}, ${y.toInt()}, ${z.toInt()})")
        println("DEBUG: PlaySoundPacket sent for sound: $soundName at (${x}, ${y}, ${z}) with volume $volume and pitch $pitch.")
    }

    fun stopAllSounds() {
        // Заглушка: В Bedrock Edition нет прямого пакета "stop all sounds".
        // Обычно это достигается путем отправки множества пакетов StopSoundEvent (если у вас есть список активных звуков)
        // или изменением громкости игрока, или просто прекращением отправки PlaySoundPacket'ов.
        // Если вы хотите *остановить* воспроизведение звуков на клиенте, вам нужно будет реализовать более сложную логику,
        // возможно, отслеживая все активные звуки и отправляя пакеты для их остановки, если CloudburstMC поддерживает это.

        if (!this::session.isInitialized || session.muCuteRelaySession == null) {
            println("DEBUG: Cannot stop all sounds, session or MuCuteRelaySession not initialized.")
            session.displayClientMessage("§c[SoundModule] Сессия или релей-сессия не активны для остановки звуков.")
            return
        }
        
        session.displayClientMessage("§e[SoundModule] Заглушка: Функция 'stopAllSounds' не имеет прямой реализации в MCBE. Необходима пользовательская логика.")
        println("DEBUG: stopAllSounds() called. No direct MCBE packet. Custom implementation needed.")
    }
}
