// File: app/src/main/java/com/retrivedmods/wclient/game/ModuleManager.kt
package com.retrivedmods.wclient.game

import android.content.Context
import android.net.Uri
import com.retrivedmods.wclient.application.AppContext

// Wildcard imports are fine, but ensure all paths are correct if you're getting Unresolved reference
// If specific modules cause issues, consider changing a wildcard to a direct import for that module.
import com.retrivedmods.wclient.game.module.combat.*
import com.retrivedmods.wclient.game.module.misc.*
import com.retrivedmods.wclient.game.module.motion.*
import com.retrivedmods.wclient.game.module.player.*
import com.retrivedmods.wclient.game.module.visual.*
import com.retrivedmods.wclient.game.module.world.*

// Imports for the command system
import com.retrivedmods.wclient.game.command.Command
import com.retrivedmods.wclient.game.command.impl.SkinStealerCommand
import com.retrivedmods.wclient.game.command.impl.SoundCommand // Ensure this import is correct

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

object ModuleManager {

    private val _modules: MutableList<Module> = ArrayList()
    val modules: List<Module> = _modules

    private val _commands: MutableList<Command> = ArrayList()
    val commands: List<Command> = _commands

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // No changes in init block if it's currently empty or just for JSON setup.

    fun initialize(session: GameSession) {
        _modules.clear() // Clear to avoid duplicates on re-initialization

        // Helper function to simplify module registration and session assignment
        fun <T : Module> addAndInitModule(module: T) {
            module.session = session // Assign the session to the module's lateinit var
            _modules.add(module)
        }

        // --- Registering Modules ---
        addAndInitModule(CommandHandlerModule()) // CommandHandlerModule must be registered first
        addAndInitModule(FlyModule())
        addAndInitModule(GravityControlModule())
        addAndInitModule(ZoomModule())
        addAndInitModule(AutoHvHModule())
        addAndInitModule(AirJumpModule())
        addAndInitModule(NoClipModule())
        addAndInitModule(GlideModule())
        addAndInitModule(JitterFlyModule())
        addAndInitModule(AdvanceCombatAuraModule())
        addAndInitModule(TriggerBotModule())
        addAndInitModule(CrystalauraModule())
        addAndInitModule(TrollerModule())
        addAndInitModule(AutoClickerModule())
        addAndInitModule(DamageTextModule())
        addAndInitModule(WAuraModule())
        addAndInitModule(SpeedModule())
        addAndInitModule(JetPackModule())
        addAndInitModule(BlinkModule())
        addAndInitModule(AdvanceDisablerModule())
        // Removed duplicate BlinkModule
        addAndInitModule(NightVisionModule())
        addAndInitModule(RegenerationModule())
        addAndInitModule(AutoDisconnectModule())
        addAndInitModule(SkinStealerModule())
        addAndInitModule(PlayerJoinNotifierModule())
        addAndInitModule(HitboxModule())
        addAndInitModule(InfiniteAuraModule())
        addAndInitModule(CriticalsModule())
        addAndInitModule(FakeProxyModule())
        addAndInitModule(ReachModule())
        addAndInitModule(SmartAuraModule())
        addAndInitModule(PlayerTPModule())
        addAndInitModule(HighJumpModule())
        addAndInitModule(SpiderModule())
        addAndInitModule(JesusModule())
        addAndInitModule(AntiKnockbackModule())
        addAndInitModule(FastStopModule())
        addAndInitModule(OpFightBotModule())
        addAndInitModule(FakeLagModule())
        addAndInitModule(FastBreakModule())
        addAndInitModule(BhopModule())
        addAndInitModule(SprintModule())
        addAndInitModule(NoHurtCameraModule())
        addAndInitModule(AutoWalkModule())
        addAndInitModule(AntiAFKModule())
        addAndInitModule(DesyncModule()) // <-- Should now be fine
        addAndInitModule(PositionLoggerModule())
        addAndInitModule(SoundModule()) // <-- Should now be fine
        addAndInitModule(MotionFlyModule())
        addAndInitModule(FreeCameraModule()) // <-- Should now be fine
        addAndInitModule(KillauraModule())
        addAndInitModule(AntiCrystalModule())
        addAndInitModule(TimeShiftModule())
        addAndInitModule(WeatherControllerModule())
        addAndInitModule(MotionVarModule())
        addAndInitModule(PlayerTracerModule())
        addAndInitModule(EnemyHunterModule())

        // --- Registering Commands ---
        _commands.clear() // Clear to avoid duplicates on re-initialization
        _commands.add(SkinStealerCommand())
        _commands.add(SoundCommand())
        // Add other commands here
        // _commands.add(HelpCommand()) // Example
        
        loadConfig() // Load config after all modules are initialized with their sessions

        _modules.forEach {
            if (it.isEnabled) {
                it.onEnabled() // Call onEnabled for modules that are enabled by default or config
            }
        }
    }

    fun getCommand(name: String): Command? {
        // Use `equals` for exact match, ignoring case
        return _commands.firstOrNull { it.alias.equals(name, ignoreCase = true) }
        // If 'lowercase()' is still unresolved, this is the safest alternative
    }

    // The rest of the ModuleManager methods (saveConfig, loadConfig, exportConfig, importConfig, etc.)
    // should remain as they were in the previous corrected version.
    // I'm omitting them here for brevity, assuming they are correct from the last iteration.

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
        val modulesConfig = jsonObject["modules"]?.jsonObject
        modulesConfig?.let {
            _modules.forEach { module ->
                (it[module.name] as? JsonObject)?.let { moduleJson ->
                    module.fromJson(moduleJson)
                }
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
            val modulesConfig = jsonObject["modules"]?.jsonObject ?: return

            _modules.forEach { module ->
                modulesConfig[module.name]?.let {
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
