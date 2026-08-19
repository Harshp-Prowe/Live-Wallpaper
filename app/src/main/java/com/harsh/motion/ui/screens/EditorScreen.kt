package com.harsh.motion.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.harsh.motion.data.EffectType
import com.harsh.motion.data.ParticleStyle
import com.harsh.motion.engine.EffectPreviewView
import com.harsh.motion.viewmodel.EditorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorState,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onToggleEffect: (EffectType) -> Unit,
    onParticleStyle: (ParticleStyle) -> Unit,
    onIntensity: (Float) -> Unit,
    onName: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.75f),
                ) {
                    if (state.photoUri == null) {
                        androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            OutlinedButton(onClick = onPickPhoto) {
                                Icon(Icons.Rounded.Photo, contentDescription = null)
                                Text("  Choose a photo")
                            }
                        }
                    } else {
                        LivePreview(state, Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)))
                    }
                }
            }

            if (state.photoUri != null) {
                item {
                    OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth()) {
                        Text("Change photo")
                    }
                }

                item { Text("Animation & interaction", style = MaterialTheme.typography.titleMedium) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EffectType.values().toList()) { effect ->
                            FilterChip(
                                selected = effect in state.effects,
                                onClick = { onToggleEffect(effect) },
                                label = { Text(effect.label) },
                            )
                        }
                    }
                }
                item {
                    Text(
                        state.effects.joinToString("\n") { "${it.label}: ${it.description}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (EffectType.PARTICLES in state.effects) {
                    item { Text("Particle style", style = MaterialTheme.typography.titleMedium) }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ParticleStyle.values().toList()) { style ->
                                FilterChip(
                                    selected = state.particleStyle == style,
                                    onClick = { onParticleStyle(style) },
                                    label = { Text(style.label) },
                                )
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Motion intensity", style = MaterialTheme.typography.titleMedium)
                        Slider(value = state.intensity, onValueChange = onIntensity, valueRange = 0.1f..1f)
                    }
                }

                item {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onName,
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                        Text("Save & set as wallpaper")
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePreview(state: EditorState, modifier: Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { EffectPreviewView(context, buildConfig(state)) },
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
)
