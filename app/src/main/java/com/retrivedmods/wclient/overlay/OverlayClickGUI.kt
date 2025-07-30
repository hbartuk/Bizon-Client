package com.retrivedmods.wclient.overlay

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retrivedmods.wclient.game.ModuleCategory
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.overlay.window.OverlayWindow
import kotlinx.coroutines.delay

class OverlayClickGUI : OverlayWindow() {

    private val _layoutParams by lazy {
        super.layoutParams.apply {
            flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            if (Build.VERSION.SDK_INT >= 31) {
                blurBehindRadius = 25
            }
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            dimAmount = 0.9f
            windowAnimations = android.R.style.Animation_Dialog
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    override val layoutParams: WindowManager.LayoutParams
        get() = _layoutParams

    private var selectedModuleCategory by mutableStateOf(ModuleCategory.Combat)
    private var searchQuery by mutableStateOf("")

    @Composable
    override fun Content() {
        val context = LocalContext.current

        // Анимация появления
        var isVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100)
            isVisible = true
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        ) {
            ModernMenuContent()
        }
    }

    @Composable
    private fun ModernMenuContent() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E),
                            Color(0xFF0F0F23)
                        ),
                        radius = 1200f
                    )
                )
        ) {
            // Главная панель
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .shadow(
                        elevation = 25.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E3F).copy(alpha = 0.95f)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Заголовок
                    HeaderSection()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Категории (теперь сверху)
                    CategorySection()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Строка поиска
                    SearchSection()

                    Spacer(modifier = Modifier.height(20.dp))

                    // Модули
                    ModulesSection()
                }
            }

            // Кнопка закрытия
            IconButton(
                onClick = { hide() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(30.dp)
                    .size(50.dp)
                    .background(
                        Color(0xFFFF4757).copy(alpha = 0.8f),
                        CircleShape
                    )
            ) {
                Text(
                    "×",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    private fun HeaderSection() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WClient",
                color = Color(0xFF00D9FF),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Configuration Panel",
                color = Color(0xFFAAB2BD),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun CategorySection() {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(ModuleCategory.values()) { category ->
                CategoryChip(
                    category = category,
                    isSelected = selectedModuleCategory == category,
                    onClick = { selectedModuleCategory = category }
                )
            }
        }
    }

    @Composable
    private fun CategoryChip(
        category: ModuleCategory,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val animatedColor by animateColorAsState(
            targetValue = if (isSelected) Color(0xFF00D9FF) else Color(0xFF3A3A5C),
            animationSpec = tween(200)
        )

        val animatedTextColor by animateColorAsState(
            targetValue = if (isSelected) Color.White else Color(0xFFAAB2BD),
            animationSpec = tween(200)
        )

        Surface(
            modifier = Modifier
                .clickable { onClick() }
                .padding(vertical = 4.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(25.dp),
            color = animatedColor
        ) {
            Text(
                text = category.displayName,
                color = animatedTextColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }

    @Composable
    private fun SearchSection() {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск модулей...", color = Color(0xFFAAB2BD)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00D9FF),
                unfocusedBorderColor = Color(0xFF3A3A5C),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFAAB2BD),
                cursorColor = Color(0xFF00D9FF)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        )
    }

    @Composable
    private fun ModulesSection() {
        val filteredModules = ModuleManager.modules
            .filter { 
                it.category == selectedModuleCategory &&
                (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true))
            }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(filteredModules) { module ->
                ModuleCard(module = module)
            }
        }
    }

    @Composable
    private fun ModuleCard(module: com.retrivedmods.wclient.game.Module) {
        val isEnabled = module.isEnabled

        val animatedCardColor by animateColorAsState(
            targetValue = if (isEnabled) Color(0xFF2E5266) else Color(0xFF2A2A4A),
            animationSpec = tween(200)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { module.toggle() },
            colors = CardDefaults.cardColors(containerColor = animatedCardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = module.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (module.description.isNotBlank()) {
                        Text(
                            text = module.description,
                            color = Color(0xFFAAB2BD),
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { module.toggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00D9FF),
                        uncheckedThumbColor = Color(0xFFAAB2BD),
                        uncheckedTrackColor = Color(0xFF3A3A5C)
                    )
                )
            }
        }
    }
}