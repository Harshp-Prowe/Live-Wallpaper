package com.harsh.dual

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.harsh.dual.data.Clone
import com.harsh.dual.ui.screens.AddAppScreen
import com.harsh.dual.ui.screens.DashboardScreen
import com.harsh.dual.ui.screens.OnboardingScreen
import com.harsh.dual.ui.screens.SettingsScreen
import com.harsh.dual.ui.theme.DualTheme
import com.harsh.dual.viewmodel.DualViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DualRoot() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DualRoot() {
    val vm: DualViewModel = viewModel()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val onboarded by vm.onboarded.collectAsStateWithLifecycle()

    DualTheme(themeMode = themeMode) {
        val onboardedValue = onboarded
        if (onboardedValue == null) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
            return@DualTheme
        }

        val navController = rememberNavController()
        val context = LocalContext.current
        val pm = context.packageManager
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }

        val ui by vm.ui.collectAsStateWithLifecycle()
        val clones by vm.clones.collectAsStateWithLifecycle()
        val installed by vm.installed.collectAsStateWithLifecycle()
        val loadingApps by vm.loadingApps.collectAsStateWithLifecycle()
        val appLock by vm.appLock.collectAsStateWithLifecycle()

        var sheetClone by remember { mutableStateOf<Clone?>(null) }

        // Surface engine messages as snackbars.
        LaunchedEffect(ui.message) {
            ui.message?.let {
                snackbar.showSnackbar(it)
                vm.consumeMessage()
            }
        }

        val provisionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { vm.refreshSpaceState() }

        val iconFor: (String) -> Drawable? = { pkg ->
            runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
        }

        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Box(Modifier.padding(padding)) {
                NavHost(
                    navController = navController,
                    startDestination = if (onboardedValue) "dashboard" else "onboarding",
                ) {
                    composable("onboarding") {
                        OnboardingScreen(onContinue = {
                            vm.completeOnboarding()
                            navController.navigate("dashboard") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        })
                    }
                    composable("dashboard") {
                        LaunchedEffect(Unit) { vm.refreshSpaceState() }
                        DashboardScreen(
                            spaceState = ui.spaceState,
                            clones = clones,
                            iconFor = iconFor,
                            onCreateSpace = {
                                vm.createSpaceIntent()?.let { provisionLauncher.launch(it) }
                                    ?: run { vm.refreshSpaceState() }
                            },
                            onAddApp = {
                                vm.loadInstalledApps()
                                navController.navigate("addapp")
                            },
                            onOpenSettings = { navController.navigate("settings") },
                            onCloneClick = { vm.launchClone(it) },
                            onCloneLongClick = { sheetClone = it },
                        )
                    }
                    composable("addapp") {
                        AddAppScreen(
                            apps = installed,
                            loading = loadingApps,
                            clonedPackages = clones.map { it.packageName }.toSet(),
                            onBack = { navController.popBackStack() },
                            onAdd = { vm.clone(it) },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            themeMode = themeMode,
                            appLock = appLock,
                            spaceState = ui.spaceState,
                            onBack = { navController.popBackStack() },
                            onThemeChange = { vm.setTheme(it) },
                            onAppLockChange = { vm.setAppLock(it) },
                            onRemoveSpace = {
                                vm.removeSpace()
                                navController.popBackStack()
                            },
                        )
                    }
                }
            }
        }

        sheetClone?.let { clone ->
            ModalBottomSheet(onDismissRequest = { sheetClone = null }) {
                Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Text(
                        clone.label,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                    ListItem(
                        headlineContent = { Text("Launch") },
                        leadingContent = { Icon(Icons.Rounded.PlayArrow, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.launchClone(clone); sheetClone = null },
                    )
                    ListItem(
                        headlineContent = { Text("Remove clone") },
                        leadingContent = { Icon(Icons.Rounded.DeleteOutline, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.removeClone(clone); sheetClone = null },
                    )
                }
            }
        }
    }
}
