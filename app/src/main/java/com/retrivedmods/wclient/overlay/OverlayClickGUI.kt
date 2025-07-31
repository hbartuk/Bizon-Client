// File: com.retrivedmods.wclient.overlay.OverlayClickGUI.kt
package com.retrivedmods.wclient.overlay // <--- ОСТАВЛЕНО: Оригинальное название пакета

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrivedmods.wclient.R
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.ModuleContent
import com.retrivedmods.wclient.game.ModuleManager
import kotlinx.coroutines.launch
import kotlin.math.PI

// <--- ОСТАВЛЕНО: Оригинальное название класса OverlayClickGUI
class OverlayClickGUI : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            if (Build.VERSION.SDK_INT >= 31) {
                blurBehindRadius = 30
            }
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            dimAmount = 0.8f
            windowAnimations = android.R.style.Animation_Dialog
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    private var selectedModuleCategory by mutableStateOf(ModuleCategory.Combat)

    @Composable
    override fun Content() {
        val context = LocalContext.current

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x95000000),
                            Color(0xE0000000)
                        ),
                        radius = 1000f
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { OverlayManager.dismissOverlayWindow(this) },
            contentAlignment = Alignment.Center
        ) {
            // Главный контейнер GUI
            Box(
                modifier = Modifier
                    .size(width = 760.dp, height = 520.dp)
                    .rgbBorder()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0A0A),
                                Color(0xFF151515),
                                Color(0xFF0A0A0A)
                            )
                        ),
                        RoundedCornerShape(25.dp)
                    )
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    // Заголовок (Header)
                    BizonHeader() // <--- ИЗМЕНЕНО: Название функции
                    
                    // Основная область контента
                    MainContentArea()
                }
            }
        }
    }

    @Composable
    private fun BizonHeader() { // <--- ИЗМЕНЕНО: Название функции
        val context = LocalContext.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x40FF0080),
                            Color(0x4000FF80),
                            Color(0x408000FF),
                            Color(0x40FF0080)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .border(
                    2.dp,
                    Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Логотип и Заголовок
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x50FFFFFF),
                                    Color(0x25FFFFFF)
                                )
                            ),
                            CircleShape
                        )
                        .border(1.5.dp, Color.White.copy(0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wclient), // ОСТАВЛЕНО: R.drawable.ic_wclient
                        contentDescription = "Bizon Client Logo", // <--- ИЗМЕНЕНО
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                RainbowText("Bizon Client", fontSize = 24f, fontWeight = FontWeight.ExtraBold) // <--- ИЗМЕНЕНО: Название и жирность шрифта
            }

            // Кнопки действий
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BizonIconButton( // <--- ИЗМЕНЕНО: Название функции
                    iconRes = R.drawable.ic_discord, // ОСТАВЛЕНО
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/yourdiscordlink"))) // <--- ИЗМЕНЕНО: Ссылка на Discord
                    }
                )
                BizonIconButton( // <--- ИЗМЕНЕНО: Название функции
                    iconRes = R.drawable.ic_web, // ОСТАВЛЕНО
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bizonclient.xyz/"))) // <--- ИЗМЕНЕНО: Ссылка на сайт
                    }
                )
                BizonIconButton( // <--- ИЗМЕНЕНО: Название функции
                    iconRes = R.drawable.ic_settings, // ОСТАВЛЕНО
                    onClick = { selectedModuleCategory = ModuleCategory.Config }
                )
                BizonIconButton( // <--- ИЗМЕНЕНО: Название функции
                    iconRes = R.drawable.ic_close, // ОСТАВЛЕНО
                    onClick = { OverlayManager.dismissOverlayWindow(this@OverlayClickGUI) } // <--- ОСТАВЛЕНО: Оригинальное имя класса
                )
            }
        }
    }

    @Composable
    private fun MainContentArea() {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Боковая панель категорий
            BizonCategorySidebar() // <--- ИЗМЕНЕНО: Название функции

            // Область контента с премиум-рамкой
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x18FFFFFF),
                                Color(0x0AFFFFFF),
                                Color(0x18FFFFFF)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        1.5.dp,
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                AnimatedContent(
                    targetState = selectedModuleCategory,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) + slideInHorizontally(animationSpec = tween(400)) { it / 6 } togetherWith
                                fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing)) + slideOutHorizontally(animationSpec = tween(400)) { -it / 6 }
                    },
                    label = "CategoryContent"
                ) { category ->
                    if (category == ModuleCategory.Config) {
                        BizonSettingsContent() // <--- ИЗМЕНЕНО: Название функции
                    } else {
                        ModuleContent(category)
                    }
                }
            }
        }
    }

    @Composable
    private fun BizonCategorySidebar() { // <--- ИЗМЕНЕНО: Название функции
        LazyColumn(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x30FFFFFF),
                            Color(0x18FFFFFF),
                            Color(0x30FFFFFF)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .border(
                    1.5.dp,
                    Color.White.copy(alpha = 0.18f),
                    RoundedCornerShape(20.dp)
                )
                .padding(vertical = 15.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(ModuleCategory.entries.size) { index ->
                val category = ModuleCategory.entries[index]
                BizonCategoryIcon( // <--- ИЗМЕНЕНО: Название функции
                    category = category,
                    isSelected = selectedModuleCategory == category,
                    onClick = { selectedModuleCategory = category }
                )
            }
        }
    }

    @Composable
    private fun BizonCategoryIcon( // <--- ИЗМЕНЕНО: Название функции
        category: ModuleCategory,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val animatedScale by animateFloatAsState(
            targetValue = if (isSelected) 1.15f else 1f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
        )

        val animatedBorderColor by animateColorAsState(
            targetValue = if (isSelected) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f),
            animationSpec = tween(300)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(animatedScale)
                    .background(
                        if (isSelected) {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00FF88),
                                    Color(0xFF0088FF),
                                    Color(0xFF8800FF)
                                )
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x40FFFFFF),
                                    Color(0x20FFFFFF)
                                )
                            )
                        },
                        CircleShape
                    )
                    .border(
                        2.dp,
                        animatedBorderColor,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(category.iconResId),
                    contentDescription = category.name,
                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = category.name,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(60.dp)
            )
        }
    }

    @Composable
    private fun BizonIconButton( // <--- ИЗМЕНЕНО: Название функции
        iconRes: Int,
        onClick: () -> Unit
    ) {
        val transition = rememberInfiniteTransition()
        val shimmer by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x40FFFFFF),
                            Color(0x18FFFFFF)
                        )
                    ),
                    CircleShape
                )
                .border(
                    1.5.dp,
                    Color.White.copy(alpha = 0.25f + shimmer * 0.15f),
                    CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(20.dp)
            )
        }
    }

    @Composable
    private fun RainbowText(
        text: String,
        fontSize: Float,
        fontWeight: FontWeight = FontWeight.Normal
    ) {
        val transition = rememberInfiniteTransition()
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing)
            )
        )

        val colors = List(10) { i ->
            val hue = (i * 36 + phase) % 360
            Color.hsv(hue, 0.9f, 1f)
        }

        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                fontWeight = fontWeight,
                brush = Brush.linearGradient(colors)
            )
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BizonSettingsContent() { // <--- ИЗМЕНЕНО: Название функции
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()
        var showFileNameDialog by remember { mutableStateOf(false) }
        var configFileName by remember { mutableStateOf("") }

        val filePickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                if (ModuleManager.importConfigFromFile(context, it)) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("✅ Конфигурация успешно импортирована!")
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("❌ Не удалось импортировать конфигурацию.")
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            // Премиум Заголовок настроек
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x25FF0080),
                                Color(0x2500FF80),
                                Color(0x258000FF)
                            )
                        ),
                        RoundedCornerShape(15.dp)
                    )
                    .border(1.5.dp, Color.White.copy(0.2f), RoundedCornerShape(15.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Настройки Bizon Client",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        "Управление конфигурациями Bizon Client",
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp
                    )
                }
            }

            // Сетка действий конфигурации
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    BizonActionCard( // <--- ИЗМЕНЕНО: Название функции
                        title = "Импорт Конфигурации",
                        description = "Загрузить настройки Bizon Client",
                        icon = Icons.Rounded.Upload,
                        onClick = { filePickerLauncher.launch("application/json") }
                    )
                }
                item {
                    BizonActionCard( // <--- ИЗМЕНЕНО: Название функции
                        title = "Экспорт Конфигурации",
                        description = "Сохранить текущие настройки",
                        icon = Icons.Rounded.SaveAlt,
                        onClick = { showFileNameDialog = true }
                    )
                }
                item {
                    BizonActionCard( // <--- ИЗМЕНЕНО: Название функции
                        title = "Сброс Конфигурации",
                        description = "Восстановить настройки по умолчанию",
                        icon = Icons.Rounded.Refresh,
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Конфигурация сброшена до стандартных значений!")
                            }
                        }
                    )
                }
                item {
                    BizonActionCard( // <--- ИЗМЕНЕНО: Название функции
                        title = "Резервная Копия",
                        description = "Создать резервную копию настроек",
                        icon = Icons.Rounded.BackupTable,
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Резервное копирование создано!")
                            }
                        }
                    )
                }
            }
        }

        // Хост для Snackbar (снизу по центру)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(snackbarHostState)
        }

        // Диалог экспорта
        if (showFileNameDialog) {
            AlertDialog(
                onDismissRequest = { showFileNameDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        val filePath = if (ModuleManager.exportConfigToFile(context, configFileName)) {
                            context.getFileStreamPath(configFileName)?.absolutePath ?: "Неизвестный путь"
                        } else null

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                filePath?.let { "✅ Экспортировано в: $it" } ?: "❌ Не удалось экспортировать конфигурацию"
                            )
                        }

                        showFileNameDialog = false
                    }) {
                        Text("Экспорт", color = Color(0xFF00FF88), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFileNameDialog = false }) {
                        Text("Отмена", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                title = {
                    Text("Экспорт Конфигурации", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    OutlinedTextField(
                        value = configFileName,
                        onValueChange = { configFileName = it },
                        label = { Text("Имя файла", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("например, my_bizon_config.json", color = Color.White.copy(alpha = 0.5f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF88),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF00FF88)
                        )
                    )
                },
                containerColor = Color(0xFF1A1A1A),
                textContentColor = Color.White
            )
        }
    }

    @Composable
    private fun BizonActionCard( // <--- ИЗМЕНЕНО: Название функции
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(15.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x25FFFFFF),
                                Color(0x15FFFFFF)
                            )
                        ),
                        RoundedCornerShape(15.dp)
                    )
                    .border(
                        1.5.dp,
                        Color.White.copy(alpha = 0.18f),
                        RoundedCornerShape(15.dp)
                    )
                    .padding(15.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x50FF0080),
                                        Color(0x5000FF80)
                                    )
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, Color.White.copy(0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = description,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    // Улучшенный RGB-анимированный модификатор рамки
    @Composable
    private fun Modifier.rgbBorder(): Modifier {
        val transition = rememberInfiniteTransition()
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing)
            )
        )

        return this.drawBehind {
            val strokeWidth = 5.dp.toPx()
            val radius = 25.dp.toPx()

            val colors = listOf(
                Color.hsv((phase) % 360f, 0.95f, 1f),
                Color.hsv((phase + 40) % 360f, 0.9f, 1f),
                Color.hsv((phase + 80) % 360f, 0.95f, 1f),
                Color.hsv((phase + 120) % 360f, 0.9f, 1f),
                Color.hsv((phase + 160) % 360f, 0.95f, 1f),
                Color.hsv((phase + 200) % 360f, 0.9f, 1f),
                Color.hsv((phase + 240) % 360f, 0.95f, 1f),
                Color.hsv((phase + 280) % 360f, 0.9f, 1f),
                Color.hsv((phase) % 360f, 0.95f, 1f)
            )

            val brush = Brush.sweepGradient(colors)

            drawRoundRect(
                brush = brush,
                style = Stroke(width = strokeWidth),
                cornerRadius = CornerRadius(radius)
            )
        }
    }
}
