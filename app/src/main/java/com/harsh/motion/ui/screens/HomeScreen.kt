package com.harsh.motion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harsh.motion.data.WallpaperConfig
import com.harsh.motion.data.WallpaperTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    saved: List<WallpaperConfig>,
    templates: List<WallpaperTemplate>,
    onOpenSettings: () -> Unit,
    onNewBlank: () -> Unit,
    onUseTemplate: (WallpaperTemplate) -> Unit,
    onOpenSaved: (WallpaperConfig) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Motion by Harsh", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Turn any photo into a living wallpaper",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewBlank,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Templates", style = MaterialTheme.typography.titleMedium)
            }
            items(templates.chunked(2)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { template ->
                        TemplateCard(template, Modifier.weight(1f), onUseTemplate)
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            item {
                Text(
                    "Your wallpapers",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (saved.isEmpty()) {
                item {
                    Text(
                        "Nothing saved yet. Start from a template or tap New.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(saved, key = { it.id }) { config ->
                    SavedRow(config, onOpenSaved)
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(template: WallpaperTemplate, modifier: Modifier, onUse: (WallpaperTemplate) -> Unit) {
    Card(
        onClick = { onUse(template) },
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.aspectRatio(0.85f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(template.previewGradient.first), Color(template.previewGradient.second)),
                    ),
                ),
        ) {
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    template.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    template.description,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedRow(config: WallpaperConfig, onOpen: (WallpaperConfig) -> Unit) {
    Card(onClick = { onOpen(config) }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.padding(16.dp)) {
            Column {
                Text(config.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    config.effects.joinToString(" · ") { it.label },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
