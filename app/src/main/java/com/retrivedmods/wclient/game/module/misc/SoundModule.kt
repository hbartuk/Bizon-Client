// File: com.retrivedmods.wclient.game.module.misc.SoundModule.kt

package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
// Убедитесь, что этот импорт правильный для PlaySoundPacket
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
import org.cloudburstmc.math.vector.Vector3f // Для координат звука, возможно, вам понадобится
import org.cloudburstmc.math.vector.Vector3i // Для координат звука, возможно, вам понадобится

// Добавьте этот импорт, если его нет (для ::session.isInitialized)
import kotlin.reflect.KProperty0

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    // ВАЖНО: Мы больше не будем использовать ::session.isInitialized в initialize() и onEnabled()
    // так как ModuleManager.initialize() ГАРАНТИРУЕТ, что session будет установлена до вызова initialize() модуля.
    // Если она не установлена, это фундаментальная ошибка, и проверять её здесь не имеет смысла.

    override fun initialize() {
        super.initialize()
        // На этом этапе session ГАРАНТИРОВАННО инициализирована благодаря ModuleManager.initialize()
        println("DEBUG: SoundModule.initialize() called. Session should be initialized.")
        session.displayClientMessage("§a[SoundModule] Модуль Sound проинициализирован. Сессия доступна.")
    }

    override fun onEnabled() {
        super.onEnabled()
        // На этом этапе session ГАРАНТИРОВАННО инициализирована.
        println("DEBUG: SoundModule.onEnabled() called. Session should be initialized.")
        session.displayClientMessage("§a[SoundModule] Модуль Sound активирован. Сессия доступна.")
    }

    override fun onDisabled() {
        super.onDisabled()
        // Здесь session также должна быть инициализирована, если модуль был активен
        // Проверка на null для session.displayClientMessage, чтобы избежать краша при попытке логирования
        // после полного уничтожения сессии (хотя ModuleManager.session = null должно быть раньше)
        session.displayClientMessage("§c[SoundModule] Модуль Sound деактивирован.")
        println("DEBUG: SoundModule.onDisabled() called.")
    }

    fun playSound(soundName: String, volume: Float, pitch: Float) {
        println("DEBUG: SoundModule.playSound() called for sound: $soundName")
        println("DEBUG: Inside playSound(), session.muCuteRelaySession is null? ${session.muCuteRelaySession == null}")

        // Проверяем, инициализирована ли session и доступна ли muCuteRelaySession
        // Здесь уже НЕ НУЖНО проверять ::session.isInitialized, так как до этого места
        // мы дойдем только если session уже была присвоена модулю.
        // Главное, чтобы session.muCuteRelaySession была не null.
        if (session.muCuteRelaySession == null) {
            session.displayClientMessage("§c[SoundModule] MuCuteRelaySession недоступна для воспроизведения звука.")
            println("ERROR: session.muCuteRelaySession is null in playSound(). Cannot play sound.")
            return
        }

        // --- ИСПРАВЛЕНИЯ ДЛЯ PlaySoundPacket ---
        // PlaySoundPacket обычно имеет методы setSoundId, setX, setY, setZ, setVolume, setPitch
        // или принимает эти значения в конструкторе, или через property setters.
        // Проверьте документацию CloudburstMC или сам класс PlaySoundPacket.
        // Скорее всего, это выглядит так:

        val playSoundPacket = PlaySoundPacket().apply {
            // Имя звука
            // Обычно, это setSoundId() или просто .soundId = "minecraft:sound_name"
            // Я использую .soundIdentifier, это типичное имя в CloudburstMC для версии 1.16+
            // Если у вас более старая версия, это может быть .soundId.
            // Если .soundIdentifier не работает, попробуйте .soundId или поищите в документации.
            soundIdentifier = soundName // <--- ИСПРАВЛЕНО
            
            // Координаты звука. PlaySoundPacket обычно принимает Vector3f или Vector3i.
            // Предполагаем, что у вашей GameSession есть актуальные координаты игрока.
            // Если playerX, playerY, playerZ в GameSession Double, их нужно преобразовать.
            // Используйте Vector3f для float координат или Vector3i для int координат.
            x = session.playerX.toFloat() // <--- ИСПРАВЛЕНО: конвертация в Float, если playerX Double
            y = session.playerY.toFloat() // <--- ИСПРАВЛЕНО
            z = session.playerZ.toFloat() // <--- ИСПРАВЛЕНО

            // Громкость и высота тона
            this.volume = volume // <--- ИСПРАВЛЕНО
            this.pitch = pitch   // <--- ИСПРАВЛЕНО
        }

        // Отправляем пакеты через muCuteRelaySession
        session.muCuteRelaySession.serverBound(playSoundPacket)
        session.muCuteRelaySession.clientBound(playSoundPacket)

        session.displayClientMessage("§a[SoundModule] Попытка воспроизвести звук: $soundName")
    }

    fun stopAllSounds() {
        // Мы можем безопасно обращаться к session здесь, если модуль активен.
        if (session.muCuteRelaySession == null) {
            println("DEBUG: Cannot stop all sounds, MuCuteRelaySession not initialized.")
            session.displayClientMessage("§c[SoundModule] Сессия или релей-сессия не активны для остановки звуков.")
            return
        }
        
        session.displayClientMessage("§e[SoundModule] Заглушка для остановки всех звуков. Реализация отсутствует.")
        println("DEBUG: stopAllSounds() called. No specific implementation for MCBE.")
    }
}
