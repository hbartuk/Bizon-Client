
package com.retrivedmods.wclient.overlay

import android.content.Context
import android.graphics.Point
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
            ModernMenuContent(
                context = context,
                selectedCategory = selectedModuleCategory,
                onCategorySelected = { selectedModuleCategory = it },
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                onDismiss = {
                    OverlayManager.dismissOverlayWindow(this@OverlayClickGUI)
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ModernMenuContent(
        context: Context,
        selectedCategory: ModuleCategory,
        onCategorySelected: (ModuleCategory) -> Unit,
        searchQuery: String,
        onSearchQueryChanged: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        val gradientBackground = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F0F23),
                Color(0xFF1A1A2E),
                Color(0xFF16213E)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .clickable { onDismiss() }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f)
                    .align(Alignment.Center)
                    .shadow(20.dp, shape = MaterialTheme.shapes.large)
                    .clickable(enabled = false) { },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2E).copy(alpha = 0.95f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Заголовок
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WClient Menu",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red.copy(alpha = 0.2f)
                            )
                        ) {
                            Text("✕", color = Color.Red, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Поиск
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        label = { Text("Поиск модулей...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Cyan,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Категории
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ModuleCategory.values()) { category ->
                            CategoryChip(
                                category = category,
                                isSelected = category == selectedCategory,
                                onClick = { onCategorySelected(category) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Модули
                    val filteredModules = ModuleManager.modules
                        .filter { module ->
                            module.category == selectedCategory &&
                                    (searchQuery.isEmpty() || 
                                     module.name.contains(searchQuery, ignoreCase = true))
                        }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredModules) { module ->
                            ModuleCard(module = module)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CategoryChip(
        category: ModuleCategory,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        val backgroundColor by animateColorAsState(
            targetValue = if (isSelected) Color.Cyan else Color.Gray.copy(alpha = 0.3f),
            label = "chipBackground"
        )
        
        val textColor by animateColorAsState(
            targetValue = if (isSelected) Color.Black else Color.White,
            label = "chipText"
        )

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = category.name,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }

    @Composable
    private fun ModuleCard(module: com.retrivedmods.wclient.game.Module) {
        val borderColor by animateColorAsState(
            targetValue = if (module.isEnabled) Color.Green else Color.Transparent,
            label = "moduleBorder"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor, MaterialTheme.shapes.medium)
                .clickable { module.toggle() },
            colors = CardDefaults.cardColors(
                containerColor = if (module.isEnabled) 
                    Color.Green.copy(alpha = 0.1f) 
                else 
                    Color.Gray.copy(alpha = 0.1f)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = module.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (module.isEnabled) Color.Green else Color.White
                    )
                    Text(
                        text = "Категория: ${module.category.name}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Switch(
                    checked = module.isEnabled,
                    onCheckedChange = { module.toggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Green,
                        checkedTrackColor = Color.Green.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}
