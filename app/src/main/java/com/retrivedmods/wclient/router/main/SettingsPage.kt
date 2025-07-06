package com.retrivedmods.wclient.router.main

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.retrivedmods.wclient.R
import com.retrivedmods.wclient.game.ModuleManager
import com.retrivedmods.wclient.util.LocalSnackbarHostState
import com.retrivedmods.wclient.util.SnackbarHostStateScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageContent() {
    SnackbarHostStateScope {
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current
        val coroutineScope = rememberCoroutineScope()
        var showFileNameDialog by remember { mutableStateOf(false) }
        var configFileName by remember { mutableStateOf("") }

        val filePickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                if (ModuleManager.importConfigFromFile(context, it)) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("✅ Конфигурация загружена")
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("❌ ошибка при загрузке конфигурации")
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.settings),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                SnackbarHost(snackbarHostState)
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Конфигурация",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Сначало включите Bizon client, после экспортируйте/импортируйте конфигурацию.",
                    style = MaterialTheme.typography.titleMedium
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    elevation = CardDefaults.elevatedCardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Управляйте своими конфигурациями",
                            style = MaterialTheme.typography.titleMedium
                        )

                        ElevatedButton(
                            onClick = { filePickerLauncher.launch("application/json") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Rounded.Upload, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Импортировать конфиг")
                        }

                        ElevatedButton(
                            onClick = { showFileNameDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Экспортируйте конфиг")
                        }
                    }
                }
            }
        }

        if (showFileNameDialog) {
            AlertDialog(
                onDismissRequest = { showFileNameDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        // Try to export config and retrieve the file path
                        val filePath = if (ModuleManager.exportConfigToFile(context, configFileName)) {
                            val file = context.getFileStreamPath(configFileName)
                            file?.absolutePath ?: "Unknown path"
                        } else {
                            null
                        }

                        // Show the result in the snackbar
                        if (filePath != null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("✅ Конфиг успешно экспортирован в: $filePath")
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("❌ неудачная экспортированная попытка конфига")
                            }
                        }

                        // Close the dialog
                        showFileNameDialog = false
                    }) {
                        Text("Export", style = MaterialTheme.typography.labelLarge)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFileNameDialog = false }) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                },
                title = {
                    Text("Export Config", style = MaterialTheme.typography.titleLarge)
                },
                text = {
                    OutlinedTextField(
                        value = configFileName,
                        onValueChange = { configFileName = it },
                        label = { Text("File name") },
                        placeholder = { Text("e.g., my_config.json") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large
                    )
                },
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        }
    }
}
