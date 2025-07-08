// File: app/src/main/java/com/retrivedmods/wclient/game/module/misc/SoundModule.kt
package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket
// Добавьте этот импорт, если его нет (для ::session.isInitialized)
import kotlin.reflect.KProperty0

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    // ... (ваш существующий код) ...

    override fun initialize() {
        super.initialize()
        // Теперь здесь безопасно использовать session, так как initialize() вызывается после присвоения session
        println("DEBUG: SoundModule.initialize() called. Session is initialized: ${::session.isInitialized}")
        if (::session.isInitialized) {
            session.displayClientMessage("§a[SoundModule] Модуль Sound проинициализирован. Сессия доступна.")
        }
    }

    override fun onEnabled() {
        super.onEnabled()
        // Здесь session тоже должна быть инициализирована, если ModuleManager.initialize() отработал
        println("DEBUG: SoundModule.onEnabled() called. Session is initialized: ${::session.isInitialized}")
        if (::session.isInitialized) {
            session.displayClientMessage("§a[SoundModule] Модуль Sound активирован. Сессия доступна.")
        } else {
            // Если вы видите это, значит, сессия НЕ инициализирована при активации модуля,
            // что указывает на проблему с порядком инициализации в ModuleManager.
            // Но предыдущие изменения должны были это исправить.
            // Если сообщение появляется, это сигнал для дальнейшей отладки в Services.kt
            println("ERROR: Session not initialized when SoundModule.onEnabled() is called. This indicates an issue.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        println("DEBUG: SoundModule.onDisabled() called.")
        if (::session.isInitialized) { // Проверка на случай, если модуль отключается до инициализации сессии
            session.displayClientMessage("§c[SoundModule] Модуль Sound деактивирован.")
        }
    }

    fun playSound(soundName: String, volume: Float, pitch: Float) {
        println("DEBUG: SoundModule.playSound() called for sound: $soundName")
        println("DEBUG: Inside playSound(), ::session.isInitialized = ${::session.isInitialized}")

        if (!::session.isInitialized) {
            // Этот displayClientMessage нужно вызывать только если session инициализирована,
            // иначе это само приведет к UninitializedPropertyAccessException.
            // Поскольку мы здесь, это означает, что session НЕ инициализирована.
            // Логировать здесь можно, но displayClientMessage вызовет ошибку.
            println("ERROR: Attempted to play sound, but session is NOT initialized.")
            // Если вы хотите показать сообщение пользователю, убедитесь, что сессия доступна.
            // Иначе, просто возвращаемся.
            return
        }

        // Если мы дошли до сюда, session гарантированно инициализирована.
        // Теперь займемся 'client'.
        // У GameSession, вероятно, нет свойства 'client'.
        // Вместо этого у нее есть 'relaySession' (MuCuteRelaySession),
        // у которой есть методы для отправки пакетов.
        // Мы будем использовать relaySession для отправки PlaySoundPacket.

        // Предполагается, что GameSession имеет свойство, дающее доступ к MuCuteRelaySession,
        // которое вы ранее назвали 'relaySession' или 'muCuteRelaySession'.
        // Например: class GameSession(val muCuteRelaySession: MuCuteRelaySession)
        // Если это не так, вам нужно будет добавить это свойство в GameSession.
        if (session.muCuteRelaySession == null) { // Используем session.muCuteRelaySession
            session.displayClientMessage("§c[SoundModule] MuCuteRelaySession недоступна для воспроизведения звука.")
            println("DEBUG: session.muCuteRelaySession is null in playSound().")
            return
        }

        val playSoundPacket = PlaySoundPacket().apply {
            this.soundId = soundName // Имя звука
            this.x = session.playerX.toInt() // Предполагается, что у session есть playerX/Y/Z
            this.y = session.playerY.toInt()
            this.z = session.playerZ.toInt()
            this.volume = volume // Громкость
            this.pitch = pitch   // Высота тона
        }

        // Отправляем пакеты через muCuteRelaySession
        session.muCuteRelaySession.serverBound(playSoundPacket)
        session.muCuteRelaySession.clientBound(playSoundPacket)

        session.displayClientMessage("§a[SoundModule] Попытка воспроизвести звук: $soundName")
    }

    // *** НОВЫЙ МЕТОД ДЛЯ ОСТАНОВКИ ЗВУКОВ ***
    // Этот метод был упомянут в SoundCommand.kt, но его не было в SoundModule.
    // Реализация может быть пустой или отправлять специфический пакет,
    // если у вашего релея есть такая функциональность.
    fun stopAllSounds() {
        if (!::session.isInitialized || session.muCuteRelaySession == null) {
            println("DEBUG: Cannot stop all sounds, session or relay session not initialized.")
            if (::session.isInitialized) { // Проверяем, чтобы не вызвать ошибку
                session.displayClientMessage("§c[SoundModule] Сессия или релей-сессия не активны для остановки звуков.")
            }
            return
        }
        // В Minecraft Bedrock нет прямого пакета для "остановки всех звуков".
        // Обычно для остановки звуков на клиенте нужно либо перезапустить клиент,
        // либо отправлять пакеты, которые "перебивают" другие звуки.
        // Если ваш `MuCuteRelay` имеет внутренний механизм для остановки звуков на стороне клиента,
        // вызовите его здесь. Иначе, это просто заглушка.
        // Например, если MuCuteRelaySession имеет метод типа stopClientSounds():
        // session.muCuteRelaySession.stopClientSounds()
        session.displayClientMessage("§e[SoundModule] Заглушка для остановки всех звуков. Реализация отсутствует.")
        println("DEBUG: stopAllSounds() called. No specific implementation for MCBE.")
    }
}
