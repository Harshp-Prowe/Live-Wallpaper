package com.harsh.motion.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.harsh.motion.R
import com.harsh.motion.data.WallpaperConfig
import com.harsh.motion.data.WallpaperTemplate
import com.harsh.motion.engine.BitmapLoader
import com.harsh.motion.ui.theme.AuroraBloom
import com.harsh.motion.ui.theme.AuroraButton
import com.harsh.motion.ui.theme.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    saved: List<WallpaperConfig>,
    templates: List<WallpaperTemplate>,
    onOpenSettings: () -> Unit,
    onNewBlank: () -> Unit,
    onUseTemplate: (WallpaperTemplate) -> Unit,
    onOpenSaved: (WallpaperConfig) -> Unit,
    onDeleteSaved: (WallpaperConfig) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AuroraButton(
                text = "New wallpaper",
                onClick = onNewBlank,
                icon = Icons.Rounded.Add,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Masthead(onOpenSettings) }

            item { Eyebrow("TEMPLATES", "${templates.size} looks") }
            items(templates.chunked(2)) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    row.forEach { template ->
                        TemplateCard(template, Modifier.weight(1f), onUseTemplate)
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            item {
                Eyebrow(
                    "YOUR WALLPAPERS",
                    if (saved.isEmpty()) null else "${saved.size} saved",
                    topPadding = 26.dp,
                )
            }
            if (saved.isEmpty()) {
                item { EmptyState() }
            } else {
                items(saved, key = { it.id }) { config ->
                    SavedCard(config, onOpenSaved, onDeleteSaved)
                }
            }
        }
    }
}

/**
 * The masthead is the app's thesis: the logo monogram sitting in its own light.
 * The bloom behind it is the same three-colour gradient the wallpaper engine
 * emits, drifting at the same unhurried pace — the app shows you what it does
 * before you tap anything.
 */
@Composable
private fun Masthead(onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(216.dp)) {
        AuroraBloom(Modifier.fillMaxSize(), intensity = 0.55f)
        // Fade the bloom out into the page so the header has no hard edge.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.72f to MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                    1f to MaterialTheme.colorScheme.background,
                ),
            ),
        )
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
            Text(
                "HarshFlow",
                style = MaterialTheme.typography.displaySmall.merge(
                    TextStyle(brush = Brand.auroraBrush()),
                ),
            )
            Text(
                "LIVE WALLPAPER STUDIO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Monospace section marker. Wide tracking and small caps, like equipment labelling. */
@Composable
private fun Eyebrow(label: String, trailing: String?, topPadding: androidx.compose.ui.unit.Dp = 8.dp) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = topPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateCard(template: WallpaperTemplate, modifier: Modifier, onUse: (WallpaperTemplate) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        onClick = { onUse(template) },
        shape = shape,
        color = Color.Transparent,
        modifier = modifier.aspectRatio(0.74f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(template.previewGradient.first), Color(template.previewGradient.second)),
                    ),
                )
                // A hairline of light along the edge is what makes a flat
                // gradient read as a lit panel rather than a coloured rectangle.
                .border(1.dp, Color.White.copy(alpha = 0.14f), shape),
        ) {
            // Off-centre highlight, so each card looks lit from a real direction.
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        radius = 340f,
                    ),
                ),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.62f),
                    ),
                ),
            )
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    "${template.effects.size} EFFECTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    template.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedCard(
    config: WallpaperConfig,
    onOpen: (WallpaperConfig) -> Unit,
    onDelete: (WallpaperConfig) -> Unit,
) {
    Surface(
        onClick = { onOpen(config) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PhotoThumb(
                config.photoUri,
                Modifier.size(56.dp, 72.dp).clip(RoundedCornerShape(14.dp)),
            )
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    config.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    config.effects.joinToString("  ·  ") { it.label },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { onDelete(config) }) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete ${config.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Small thumbnail of the wallpaper's own photo. Saved wallpapers used to be
 * name-only rows, which made a list of them impossible to tell apart at a
 * glance. Decoded off the main thread and deliberately tiny.
 */
@Composable
private fun PhotoThumb(photoUri: String, modifier: Modifier) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, photoUri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                BitmapLoader.decodeScaled(context, Uri.parse(photoUri), THUMB_PX).asImageBitmap()
            }.getOrNull()
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private const val THUMB_PX = 220

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Nothing here yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pick a photo and it starts moving. Start from a template above, or build one from scratch.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
