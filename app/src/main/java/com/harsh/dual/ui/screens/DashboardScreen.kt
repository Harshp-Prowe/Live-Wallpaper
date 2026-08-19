package com.harsh.dual.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harsh.dual.data.Clone
import com.harsh.dual.data.SpaceState
import com.harsh.dual.ui.components.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    spaceState: SpaceState,
    clones: List<Clone>,
    iconFor: (String) -> android.graphics.drawable.Drawable?,
    onCreateSpace: () -> Unit,
    onAddApp: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloneClick: (Clone) -> Unit,
    onCloneLongClick: (Clone) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dual by Harsh", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Your private second space",
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
            if (spaceState == SpaceState.READY) {
                ExtendedFloatingActionButton(
                    onClick = onAddApp,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add App") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (spaceState) {
                SpaceState.UNAVAILABLE -> InfoState(
                    title = "Second space not supported",
                    body = "This device does not allow creating a managed space (some manufacturers disable it). Cloning cannot run here without root, which this app never does.",
                )
                SpaceState.NOT_CREATED -> CreateSpacePrompt(onCreateSpace)
                SpaceState.READY ->
                    if (clones.isEmpty()) {
                        InfoState(
                            title = "No clones yet",
                            body = "Tap “Add App” to clone an installed app into your private space.",
                        )
                    } else {
                        CloneGrid(clones, iconFor, onCloneClick, onCloneLongClick)
                    }
            }
        }
    }
}

@Composable
private fun CreateSpacePrompt(onCreateSpace: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            "Create your private space",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            "One tap sets up an isolated space on this phone. Your personal apps, settings and data are never changed, and you can remove the space anytime.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Button(onClick = onCreateSpace, modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
            Text("Create Space")
        }
    }
}

@Composable
private fun CloneGrid(
    clones: List<Clone>,
    iconFor: (String) -> android.graphics.drawable.Drawable?,
    onClick: (Clone) -> Unit,
    onLongClick: (Clone) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(96.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(clones, key = { it.packageName }) { clone ->
            CloneTile(clone, iconFor(clone.packageName), onClick, onLongClick)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CloneTile(
    clone: Clone,
    icon: android.graphics.drawable.Drawable?,
    onClick: (Clone) -> Unit,
    onLongClick: (Clone) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick(clone) },
                    onLongClick = { onLongClick(clone) },
                )
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(icon, size = 52.dp)
            Text(
                clone.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InfoState(title: String, body: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
