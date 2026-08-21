package com.harsh.motion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.harsh.motion.data.EffectType
import com.harsh.motion.data.ParticleStyle
import com.harsh.motion.engine.EffectPreviewView
import com.harsh.motion.ui.theme.AuroraButton
import com.harsh.motion.ui.theme.Brand
import com.harsh.motion.viewmodel.EditorState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    state: EditorState,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onToggleEffect: (EffectType) -> Unit,
    onParticleStyle: (ParticleStyle) -> Unit,
    onIntensity: (Float) -> Unit,
    onName: (String) -> Unit,
    onReposition: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Editor", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { PreviewStage(state, onPickPhoto) }

            if (state.photoUri != null) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GhostButton("Reposition", Icons.Rounded.CropFree, onReposition, Modifier.weight(1f))
                        GhostButton("Change photo", Icons.Rounded.SwapHoriz, onPickPhoto, Modifier.weight(1f))
                    }
                }

                item {
                    Panel("MOTION", "Tap to turn each one on or off") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EffectType.values().forEach { effect ->
                                EffectChip(
                                    label = effect.label,
                                    selected = effect in state.effects,
                                    onClick = { onToggleEffect(effect) },
                                )
                            }
                        }
                    }
                }

                if (EffectType.PARTICLES in state.effects) {
                    item {
                        Panel("PARTICLE STYLE", null) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ParticleStyle.values().forEach { style ->
                                    EffectChip(
                                        label = style.label,
                                        selected = state.particleStyle == style,
                                        onClick = { onParticleStyle(style) },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Panel("INTENSITY", null, trailing = "${(state.intensity * 100).toInt()}%") {
                        Slider(
                            value = state.intensity,
                            onValueChange = onIntensity,
                            valueRange = 0.1f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Brand.Violet,
                                activeTrackColor = Brand.Violet,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                    }
                }

                item {
                    Panel("NAME", null) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = onName,
                            placeholder = { Text("My wallpaper") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    AuroraButton(
                        text = "Set as wallpaper",
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * The preview is the whole point of the screen, so it gets the tall 9:16 stage
 * and a lit edge rather than sitting in a flat grey box like a form field.
 */
@Composable
private fun PreviewStage(state: EditorState, onPickPhoto: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.62f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
    ) {
        if (state.photoUri == null) {
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Start with a photo",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Anything from your gallery. It stays on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                AuroraButton(text = "Choose photo", onClick = onPickPhoto, icon = Icons.Rounded.Photo)
            }
        } else {
            LivePreview(state, Modifier.fillMaxSize())
        }
    }
}

/** A titled glass panel. The mono eyebrow is the structural device throughout. */
@Composable
private fun Panel(
    eyebrow: String,
    hint: String?,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    eyebrow,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                if (trailing != null) {
                    Text(
                        trailing,
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand.Violet,
                    )
                }
            }
            if (hint != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

/**
 * Selection is shown by a lit border and a check, not by a filled pill. With up
 * to nine of these on screen, filled chips turn the panel into a block of
 * colour and it stops being obvious which ones are actually on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EffectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Brand.Violet else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Brand.Violet,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GhostButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LivePreview(state: EditorState, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context -> EffectPreviewView(context, buildConfig(state)) },
        update = { it.updateConfig(buildConfig(state)) },
    )
}

private fun buildConfig(state: EditorState) = com.harsh.motion.data.WallpaperConfig(
    id = "preview",
    name = state.name,
    photoUri = state.photoUri ?: "",
    effects = state.effects,
    particleStyle = state.particleStyle,
    intensity = state.intensity,
    scale = state.photoScale,
    offsetX = state.photoOffsetX,
    offsetY = state.photoOffsetY,
)
