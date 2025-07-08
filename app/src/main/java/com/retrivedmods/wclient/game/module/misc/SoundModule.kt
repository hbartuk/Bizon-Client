package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.DisconnectPacket
// Добавьте этот импорт, если его нет
import kotlin.reflect.KProperty0

class SoundModule : Module("Sound", ModuleCategory.Misc) {

    // ... (ваш существующий код) ...

    override fun initialize() {
        super.initialize()
        println("DEBUG: SoundModule.initialize() called. Session is initialized: ${::session.isInitialized}")
    }

    override fun onEnabled() {
        super.onEnabled()
        println("DEBUG: SoundModule.onEnabled() called. Session is initialized: ${::session.isInitialized}")
        if (::session.isInitialized) {
            session.displayClientMessage("§a[SoundModule] Модуль Sound активирован. Сессия доступна.")
        } else {
            session.displayClientMessage("§c[SoundModule] Модуль Sound активирован, но сессия НЕ доступна сразу после onEnabled.")
        }
    }

    override fun onDisabled() {
        super.onDisabled()
        println("DEBUG: SoundModule.onDisabled() called.")
    }

    fun playSound(soundName: String, volume: Float, pitch: Float) {
        println("DEBUG: SoundModule.playSound() called for sound: $soundName")
        println("DEBUG: Inside playSound(), ::session.isInitialized = ${::session.isInitialized}")

        if (!::session.isInitialized) { // Используем ::session.isInitialized напрямую
            session.displayClientMessage("§c[SoundModule] Сессия не инициализирована. Невозможно воспроизвести звук.")
            return
        }
        
        // Теперь мы знаем, что session инициализирована, поэтому можно безопасно использовать ее
        // Проверяем, что session.client не null перед использованием
        if (session.client != null) {
            session.client.playClientSound(soundName, volume, pitch)
            session.displayClientMessage("§a[SoundModule] Воспроизведен звук: $soundName")
        } else {
            session.displayClientMessage("§c[SoundModule] Клиент сессии недоступен для воспроизведения звука.")
            println("DEBUG: session.client is null in playSound().")
        }
    }
}
