package com.retrivedmods.wclient.overlay

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
            // Compact Premium Container
            Box(
                modifier = Modifier
                    .size(width = 720.dp, height = 480.dp)
                    .rgbBorder()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0A0A),
                                Color(0xFF151515),
                                Color(0xFF0A0A0A)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Compact Header
                    CompactHeader()

                    // Main Content Area
                    MainContentArea()
                }
            }
        }
    }

    @Composable
    private fun CompactHeader() {
        val context = LocalContext.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x35FF0080),
                            Color(0x3500FF80),
                            Color(0x358000FF),
                            Color(0x35FF0080)
                        )
                    ),
                    RoundedCornerShape(15.dp)
                )
                .border(
                    1.5.dp,
                    Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(15.dp)
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x40FFFFFF),
                                    Color(0x20FFFFFF)
                                )
                            ),
                            CircleShape
                        )
                        .border(1.dp, Color.White.copy(0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wclient),
                        contentDescription = "WClient Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                RainbowText("WClient", fontSize = 20f, fontWeight = FontWeight.Bold)
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PremiumIconButton(
                    iconRes = R.drawable.ic_discord,
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/N2Gejr8Fbp")))
                    }
                )
                PremiumIconButton(
                    iconRes = R.drawable.ic_web,
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wclient.neocities.org/")))
                    }
                )
                PremiumIconButton(
                    iconRes = R.drawable.ic_settings,
                    onClick = { selectedModuleCategory = ModuleCategory.Config }
                )
                PremiumIconButton(
                    iconRes = R.drawable.ic_close,
                    onClick = { OverlayManager.dismissOverlayWindow(this@OverlayClickGUI) }
                )
            }
        }
    }

    @Composable
    private fun MainContentArea() {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Category Sidebar
            CompactCategorySidebar()

            // Content Area with Premium Border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x12FFFFFF),
                                Color(0x08FFFFFF),
                                Color(0x12FFFFFF)
                            )
                        ),
                        RoundedCornerShape(15.dp)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(15.dp)
                    )
                    .padding(16.dp)
            ) {
                AnimatedContent(
                    targetState = selectedModuleCategory,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) + slideInHorizontally { it / 4 } togetherWith
                                fadeOut(animationSpec = tween(300)) + slideOutHorizontally { -it / 4 }
                    },
                    label = "CategoryContent"
                ) { category ->
                    if (category == ModuleCategory.Config) {
                        CompactSettingsContent()
                    } else {
                        ModuleContent(category)
                    }
                }
            }
        }
    }

    @Composable
    private fun CompactCategorySidebar() {
        LazyColumn(
            modifier = Modifier
                .width(70.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x25FFFFFF),
                            Color(0x15FFFFFF),
                            Color(0x25FFFFFF)
                        )
                    ),
                    RoundedCornerShape(15.dp)
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.12f),
                    RoundedCornerShape(15.dp)
                )
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(ModuleCategory.entries.size) { index ->
                val category = ModuleCategory.entries[index]
                CategoryIcon(
                    category = category,
                    isSelected = selectedModuleCategory == category,
                    onClick = { selectedModuleCategory = category }
                )
            }
        }
    }

    @Composable
    private fun CategoryIcon(
        category: ModuleCategory,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val animatedScale by animateFloatAsState(
            targetValue = if (isSelected) 1.1f else 1f,
            animationSpec = spring(dampingRatio = 0.6f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                                    Color(0x35FFFFFF),
                                    Color(0x15FFFFFF)
                                )
                            )
                        },
                        CircleShape
                    )
                    .border(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(category.iconResId),
                    contentDescription = category.name,
                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = category.name,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(54.dp)
            )
        }
    }

    @Composable
    private fun PremiumIconButton(
        iconRes: Int,
        onClick: () -> Unit
    ) {
        val transition = rememberInfiniteTransition()
        val shimmer by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing)
            )
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x30FFFFFF),
                            Color(0x15FFFFFF)
                        )
                    ),
                    CircleShape
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.2f + shimmer * 0.1f),
                    CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp)
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
                animation = tween(3000, easing = LinearEasing)
            )
        )

        val colors = List(10) { i ->
            val hue = (i * 36 + phase) % 360
            Color.hsv(hue, 0.85f, 1f)
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
    fun CompactSettingsContent() {
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
                        snackbarHostState.showSnackbar("✅ Config imported successfully")
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("❌ Failed to import config")
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Premium Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x20FF0080),
                                Color(0x2000FF80),
                                Color(0x208000FF)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Configuration",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Manage your WClient configurations",
                        color = Color(0xFFBBBBBB),
                        fontSize = 12.sp
                    )
                }
            }

            // Config Actions Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    PremiumActionCard(
                        title = "Import Config",
                        description = "Load configuration",
                        icon = Icons.Rounded.Upload,
                        onClick = { filePickerLauncher.launch("application/json") }
                    )
                }
                item {
                    PremiumActionCard(
                        title = "Export Config",
                        description = "Save configuration",
                        icon = Icons.Rounded.SaveAlt,
                        onClick = { showFileNameDialog = true }
                    )
                }
                item {
                    PremiumActionCard(
                        title = "Reset Config",
                        description = "Restore defaults",
                        icon = Icons.Rounded.Refresh,
                        onClick = { /* Reset logic */ }
                    )
                }
                item {
                    PremiumActionCard(
                        title = "Backup Config",
                        description = "Create backup",
                        icon = Icons.Rounded.BackupTable,
                        onClick = { /* Backup logic */ }
                    )
                }
            }
        }

        // Snackbar Host
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(snackbarHostState)
        }

        // Export Dialog
        if (showFileNameDialog) {
            AlertDialog(
                onDismissRequest = { showFileNameDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        val filePath = if (ModuleManager.exportConfigToFile(context, configFileName)) {
                            context.getFileStreamPath(configFileName)?.absolutePath ?: "Unknown path"
                        } else null

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                filePath?.let { "✅ Exported to: $it" } ?: "❌ Failed to export config"
                            )
                        }

                        showFileNameDialog = false
                    }) {
                        Text("Export", color = Color(0xFF00FF88))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFileNameDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                title = {
                    Text("Export Config", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    OutlinedTextField(
                        value = configFileName,
                        onValueChange = { configFileName = it },
                        label = { Text("File name", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("e.g., my_config.json", color = Color.White.copy(alpha = 0.5f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF88),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                },
                containerColor = Color(0xFF1A1A1A),
                textContentColor = Color.White
            )
        }
    }

    @Composable
    private fun PremiumActionCard(
        title: String,
        description: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
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
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x40FF0080),
                                        Color(0x4000FF80)
                                    )
                                ),
                                CircleShape
                            )
                            .border(1.dp, Color.White.copy(0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = description,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    // Enhanced RGB Animated Border Modifier
    @Composable
    private fun Modifier.rgbBorder(): Modifier {
        val transition = rememberInfiniteTransition()
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing)
            )
        )

        return this.drawBehind {
            val strokeWidth = 4.dp.toPx()
            val radius = 20.dp.toPx()

            // Create enhanced gradient colors with more vibrant transitions
            val colors = listOf(
                Color.hsv((phase) % 360f, 0.9f, 1f),
                Color.hsv((phase + 45) % 360f, 0.85f, 1f),
                Color.hsv((phase + 90) % 360f, 0.9f, 1f),
                Color.hsv((phase + 135) % 360f, 0.85f, 1f),
                Color.hsv((phase + 180) % 360f, 0.9f, 1f),
                Color.hsv((phase + 225) % 360f, 0.85f, 1f),
                Color.hsv((phase + 270) % 360f, 0.9f, 1f),
                Color.hsv((phase + 315) % 360f, 0.85f, 1f),
                Color.hsv((phase) % 360f, 0.9f, 1f)
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
