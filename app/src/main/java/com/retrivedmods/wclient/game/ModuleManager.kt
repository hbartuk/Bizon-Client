package com.retrivedmods.wclient.game

import android.content.Context
import android.net.Uri
import com.retrivedmods.wclient.application.AppContext

// Импорты модулей (из вашего рабочего файла)
import com.retrivedmods.wclient.game.module.combat.AdvanceCombatAuraModule
import com.retrivedmods.wclient.game.module.combat.WAuraModule
import com.retrivedmods.wclient.game.module.combat.AntiCrystalModule
import com.retrivedmods.wclient.game.module.combat.HitboxModule
import com.retrivedmods.wclient.game.module.combat.TrollerModule
import com.retrivedmods.wclient.game.module.combat.AutoClickerModule
import com.retrivedmods.wclient.game.module.combat.InfiniteAuraModule
import com.retrivedmods.wclient.game.module.combat.AntiKnockbackModule
import com.retrivedmods.wclient.game.module.combat.AutoHvHModule
import com.retrivedmods.wclient.game.module.combat.TriggerBotModule
import com.retrivedmods.wclient.game.module.combat.CriticalsModule
import com.retrivedmods.wclient.game.module.combat.CrystalauraModule
import com.retrivedmods.wclient.game.module.combat.EnemyHunterModule
import com.retrivedmods.wclient.game.module.combat.KillauraModule
import com.retrivedmods.wclient.game.module.combat.ReachModule
import com.retrivedmods.wclient.game.module.combat.SmartAuraModule
import com.retrivedmods.wclient.game.module.misc.AdvanceDisablerModule
import com.retrivedmods.wclient.game.module.misc.AutoDisconnectModule
import com.retrivedmods.wclient.game.module.misc.SkinStealerModule
import com.retrivedmods.wclient.game.module.player.DesyncModule
import com.retrivedmods.wclient.game.module.motion.NoClipModule
import com.retrivedmods.wclient.game.module.misc.PlayerTracerModule
import com.retrivedmods.wclient.game.module.misc.PositionLoggerModule
import com.retrivedmods.wclient.game.module.world.TimeShiftModule
import com.retrivedmods.wclient.game.module.player.BlinkModule
import com.retrivedmods.wclient.game.module.player.RegenerationModule
import com.retrivedmods.wclient.game.module.world.WeatherControllerModule
import com.retrivedmods.wclient.game.module.motion.AirJumpModule
import com.retrivedmods.wclient.game.module.motion.AntiAFKModule
import com.retrivedmods.wclient.game.module.motion.AutoWalkModule
import com.retrivedmods.wclient.game.module.motion.BhopModule
import com.retrivedmods.wclient.game.module.motion.FastStopModule
import com.retrivedmods.wclient.game.module.motion.FlyModule
import com.retrivedmods.wclient.game.module.motion.GravityControlModule
import com.retrivedmods.wclient.game.module.motion.GlideModule
import com.retrivedmods.wclient.game.module.motion.HighJumpModule
import com.retrivedmods.wclient.game.module.motion.JetPackModule
import com.retrivedmods.wclient.game.module.motion.JitterFlyModule
import com.retrivedmods.wclient.game.module.motion.MotionFlyModule
import com.retrivedmods.wclient.game.module.motion.MotionVarModule
import com.retrivedmods.wclient.game.module.motion.OpFightBotModule
import com.retrivedmods.wclient.game.module.motion.SpeedModule
import com.retrivedmods.wclient.game.module.motion.SprintModule
import com.retrivedmods.wclient.game.module.player.FreeCameraModule
import com.retrivedmods.wclient.game.module.visual.NoHurtCameraModule
import com.retrivedmods.wclient.game.module.visual.ZoomModule
import com.retrivedmods.wclient.game.module.visual.DamageTextModule
import com.retrivedmods.wclient.game.module.motion.PlayerTPModule
import com.retrivedmods.wclient.game.module.motion.SpiderModule
import com.retrivedmods.wclient.game.module.player.FastBreakModule
import com.retrivedmods.wclient.game.module.player.JesusModule
import com.retrivedmods.wclient.game.module.visual.NightVisionModule
import com.retrivedmods.wclient.game.module.visual.FakeProxyModule
import com.retrivedmods.wclient.game.module.visual.PlayerJoinNotifierModule
import com.retrivedmods.wclient.game.module.world.FakeLagModule

// Импорт CommandHandlerModule (был в вашем рабочем файле)
import com.retrivedmods.wclient.game.module.misc.CommandHandlerModule

// --- ДОБАВЛЕНЫ ИМПОРТЫ ДЛЯ КОМАНД ---
import com.retrivedmods.wclient.game.command.Command // Базовый класс команды
import com.retrivedmods.wclient.game.command.impl.SkinStealerCommand // Команда SkinStealerCommand
import com.retrivedmods.wclient.game.command.impl.SoundCommand // Команда SoundCommand
// ------------------------------------

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
// --- ДОБАВЛЕНЫ ИМПОРТЫ ДЛЯ SERIALIZATION (если их не было в рабочем файле) ---
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
// -----------------------------------------------------------------------------
import java.io.File

object ModuleManager {

    private val _modules: MutableList<Module> = ArrayList()
    val modules: List<Module> = _modules

    // --- ДОБАВЛЕНО: Список для команд ---
    private val _commands: MutableList<Command> = ArrayList()
    val commands: List<Command> = _commands
    // ------------------------------------

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        // --- Регистрация модулей (из вашего рабочего файла) ---
        with(_modules) {
            // CommandHandlerModule должен быть зарегистрирован, чтобы обрабатывать команды
            add(CommandHandlerModule())

            // Все остальные модули
            add(FlyModule())
            add(GravityControlModule())
            add(ZoomModule())
            add(AutoHvHModule())
            add(AirJumpModule())
            add(NoClipModule())
            add(GlideModule())
            add(JitterFlyModule())
            add(AdvanceCombatAuraModule())
            add(TriggerBotModule())
            add(CrystalauraModule())
            add(TrollerModule())
            add(AutoClickerModule())
            add(DamageTextModule())
            add(WAuraModule())
            add(SpeedModule())
            add(JetPackModule())
            add(BlinkModule())
            add(AdvanceDisablerModule())
            add(BlinkModule())
            add(NightVisionModule())
            add(RegenerationModule())
            add(AutoDisconnectModule())
            add(SkinStealerModule())
            add(PlayerJoinNotifierModule())
            add(HitboxModule())
            add(InfiniteAuraModule())
            add(CriticalsModule())
            add(FakeProxyModule())
            add(ReachModule())
            add(SmartAuraModule())
            add(PlayerTPModule())
            add(HighJumpModule())
            add(SpiderModule())
            add(JesusModule())
            add(AntiKnockbackModule())
            add(FastStopModule())
            add(OpFightBotModule())
            add(FakeLagModule())
            add(FastBreakModule())
            add(BhopModule())
            add(SprintModule())
            add(NoHurtCameraModule())
            add(AutoWalkModule())
            add(AntiAFKModule())
            add(DesyncModule())
            add(PositionLoggerModule())
            // add(SoundModule()) // SoundModule регистрируется в initialize, а не здесь
            add(MotionFlyModule())
            add(FreeCameraModule())
            add(KillauraModule())
            add(AntiCrystalModule())
            add(TimeShiftModule())
            add(WeatherControllerModule())
            add(MotionVarModule())
            add(PlayerTracerModule())
            add(EnemyHunterModule())
        }

        // --- ДОБАВЛЕНО: Регистрация команд ---
        with(_commands) {
            add(SkinStealerCommand())
            add(SoundCommand()) // Добавляем SoundCommand
            // Добавляй другие команды здесь, если они есть
        }
    }

    // --- ДОБАВЛЕНО: Функция getCommand ---
    fun getCommand(name: String): Command? {
        // Ищем команду по её алиасу (имени), игнорируя регистр
        return _commands.firstOrNull { it.alias.contains(name.lowercase()) }
    }
    // ------------------------------------

    // Остальной код, связанный с сохранением/загрузкой конфигурации, остаётся таким же, как в вашем рабочем коммите
    // и использует kotlinx.serialization.
    fun saveConfig() {
        val configsDir = AppContext.instance.filesDir.resolve("configs")
        configsDir.mkdirs()

        val config = configsDir.resolve("UserConfig.json")
        val jsonObject = buildJsonObject {
            put("modules", buildJsonObject {
                _modules.forEach {
                    if (it.private) {
                        return@forEach
                    }
                    put(it.name, it.toJson())
                }
            })
        }

        config.writeText(json.encodeToString(jsonObject))
    }

    fun loadConfig() {
        val configsDir = AppContext.instance.filesDir.resolve("configs")
        configsDir.mkdirs()

        val config = configsDir.resolve("UserConfig.json")
        if (!config.exists()) {
            return
        }

        val jsonString = config.readText()
        if (jsonString.isEmpty()) {
            return
        }

        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val modules = jsonObject["modules"]!!.jsonObject
        _modules.forEach { module ->
            (modules[module.name] as? JsonObject)?.let {
                module.fromJson(it)
            }
        }
    }

    fun exportConfig(): String {
        val jsonObject = buildJsonObject {
            put("modules", buildJsonObject {
                _modules.forEach {
                    if (it.private) {
                        return@forEach
                    }
                    put(it.name, it.toJson())
                }
            })
        }
        return json.encodeToString(jsonObject)
    }

    fun importConfig(configStr: String) {
        try {
            val jsonObject = json.parseToJsonElement(configStr).jsonObject
            val modules = jsonObject["modules"]?.jsonObject ?: return

            _modules.forEach { module ->
                modules[module.name]?.let {
                    if (it is JsonObject) {
                        module.fromJson(it)
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid config format")
        }
    }

    fun exportConfigToFile(context: Context, fileName: String): Boolean {
        return try {
            val configsDir = context.getExternalFilesDir("configs")
            configsDir?.mkdirs()

            val configFile = File(configsDir, "$fileName.json")
            configFile.writeText(exportConfig())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importConfigFromFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val configStr = input.bufferedReader().readText()
                importConfig(configStr)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
