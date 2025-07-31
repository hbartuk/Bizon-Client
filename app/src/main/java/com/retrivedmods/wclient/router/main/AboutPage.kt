package com.retrivedmods.wclient.router.main

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retrivedmods.wclient.util.LocalSnackbarHostState
import com.retrivedmods.wclient.util.SnackbarHostStateScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AboutPageContent() {
    SnackbarHostStateScope {
        val snackbarHostState = LocalSnackbarHostState.current
        val context = LocalContext.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("информация об bizon client") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                    )
                )
            },
            bottomBar = {
                SnackbarHost(snackbarHostState, modifier = Modifier.animateContentSize())
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoCard(title = "bizon client") {
                    Text("версия : 0.1 alpha private", style = MaterialTheme.typography.bodyMedium)
                    Text("создатель : Hbart/hbort", style = MaterialTheme.typography.bodyMedium)
                }

                InfoCard(title = "Большое спасибо") {
                    Text("• MuCuteClient")
                    Text("• Protohax")
                    Text("• Project Lumina")
                    Text("• WClient")
                    Text("• Другу Nikita218000")
                }

                InfoCard(title = "Социальные сети") {
                    ClickableText("GitHub", "https://github.com/hbartuk")
                    ClickableText("telegram", "@hbortuk")
                    ClickableText("vk", "@hbortik")
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ClickableText(label: String, url: String) {
    val context = LocalContext.current
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .padding(vertical = 2.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
    )
}
