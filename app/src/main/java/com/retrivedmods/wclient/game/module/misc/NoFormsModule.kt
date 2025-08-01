package com.retrivedmods.wclient.game.module.misc

import com.retrivedmods.wclient.game.InterceptablePacket
import com.retrivedmods.wclient.game.Module
import com.retrivedmods.wclient.game.ModuleCategory
import org.cloudburstmc.protocol.bedrock.packet.ModalFormRequestPacket
import org.cloudburstmc.protocol.bedrock.packet.ServerSettingsRequestPacket
import org.cloudburstmc.protocol.bedrock.packet.ServerSettingsResponsePacket

class NoFormsModule : Module("NoForms", ModuleCategory.Misc) {

    private val blockModalForms by boolValue("Block Modal Forms", true)
    private val blockServerSettings by boolValue("Block Server Settings", true)
    private val blockCustomForms by boolValue("Block Custom Forms", true)
    private val blockAllFormTypes by boolValue("Block All Form Types", true)
    
    private val showBlockedMessage by boolValue("Show Blocked Message", true)
    private val logBlockedForms by boolValue("Log Blocked Forms", false)
    
    private var blockedFormsCount = 0

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) return

        val packet = interceptablePacket.packet

        when (packet) {
            // Основной пакет для всех видов форм
            is ModalFormRequestPacket -> {
                if (shouldBlockForm(packet)) {
                    interceptablePacket.intercept() // Блокируем форму
                    blockedFormsCount++
                    
                    if (showBlockedMessage) {
                        session.displayClientMessage("§c[NoForms] Заблокирована форма (ID: ${packet.formId})")
                    }
                    
                    if (logBlockedForms) {
                        println("NoForms: Blocked ModalFormRequestPacket - FormID: ${packet.formId}")
                        println("NoForms: Form Data: ${packet.formData}")
                    }
                }
            }
            
            // Блокируем запросы настроек сервера (если включено)
            is ServerSettingsRequestPacket -> {
                if (blockServerSettings) {
                    interceptablePacket.intercept()
                    blockedFormsCount++
                    
                    if (showBlockedMessage) {
                        session.displayClientMessage("§c[NoForms] Заблокированы настройки сервера")
                    }
                    
                    if (logBlockedForms) {
                        println("NoForms: Blocked ServerSettingsRequestPacket")
                    }
                }
            }
            
            // Также блокируем ответы настроек сервера
            is ServerSettingsResponsePacket -> {
                if (blockServerSettings) {
                    interceptablePacket.intercept()
                    
                    if (logBlockedForms) {
                        println("NoForms: Blocked ServerSettingsResponsePacket")
                    }
                }
            }
        }
    }

    /**
     * Определяет, нужно ли блокировать конкретную форму
     */
    private fun shouldBlockForm(packet: ModalFormRequestPacket): Boolean {
        // Если включена блокировка всех форм
        if (blockAllFormTypes) {
            return true
        }
        
        // Анализируем тип формы по JSON данным
        val formData = packet.formData
        
        try {
            // Простая проверка типа формы по содержимому JSON
            when {
                // Modal форма (с кнопками типа "Да/Нет")
                blockModalForms && (formData.contains("\"type\":\"modal\"") || 
                                   formData.contains("button1") || 
                                   formData.contains("button2")) -> {
                    if (logBlockedForms) {
                        println("NoForms: Detected and blocked modal form")
                    }
                    return true
                }
                
                // Custom форма (с различными элементами ввода)
                blockCustomForms && (formData.contains("\"type\":\"custom\"") || 
                                    formData.contains("\"content\":[") ||
                                    formData.contains("input") || 
                                    formData.contains("slider") || 
                                    formData.contains("dropdown")) -> {
                    if (logBlockedForms) {
                        println("NoForms: Detected and blocked custom form")
                    }
                    return true
                }
                
                // Если это какая-то другая форма, но включена блокировка всех
                else -> false
            }
        } catch (e: Exception) {
            if (logBlockedForms) {
                println("NoForms: Error analyzing form data: ${e.message}")
            }
            // В случае ошибки анализа, блокируем форму для безопасности (если включена общая блокировка)
            return blockAllFormTypes
        }
        
        return false
    }

    override fun onEnable() {
        super.onEnable()
        blockedFormsCount = 0
        session.displayClientMessage("§a[NoForms] Модуль включен - все формы будут скрыты")
        
        if (logBlockedForms) {
            println("NoForms: Module enabled with settings:")
            println("  - Block Modal Forms: $blockModalForms")
            println("  - Block Server Settings: $blockServerSettings") 
            println("  - Block Custom Forms: $blockCustomForms")
            println("  - Block All Form Types: $blockAllFormTypes")
        }
    }

    override fun onDisable() {
        super.onDisable()
        
        if (showBlockedMessage) {
            session.displayClientMessage("§c[NoForms] Модуль отключен (заблокировано форм: $blockedFormsCount)")
        }
        
        if (logBlockedForms) {
            println("NoForms: Module disabled. Total blocked forms: $blockedFormsCount")
        }
        
        blockedFormsCount = 0
    }

    /**
     * Дополнительный метод для ручной очистки статистики
     */
    fun resetStats() {
        blockedFormsCount = 0
        session.displayClientMessage("§a[NoForms] Статистика сброшена")
    }

    /**
     * Получить количество заблокированных форм
     */
    fun getBlockedFormsCount(): Int {
        return blockedFormsCount
    }
}
