package com.harsh.motion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.harsh.motion.data.Templates
import com.harsh.motion.ui.screens.EditorScreen
import com.harsh.motion.ui.screens.HomeScreen
import com.harsh.motion.ui.screens.PhotoAdjustScreen
import com.harsh.motion.ui.screens.SettingsScreen
import com.harsh.motion.ui.theme.MotionTheme
import com.harsh.motion.viewmodel.MotionViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MotionRoot() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotionRoot() {
    val vm: MotionViewModel = viewModel()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()

    MotionTheme(themeMode = themeMode) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            val saved by vm.savedConfigs.collectAsStateWithLifecycle()
            val editor by vm.editor.collectAsStateWithLifecycle()
            val message by vm.message.collectAsStateWithLifecycle()
            val snackbar = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            val photoPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia(),
            ) { uri ->
                uri?.let {
                    vm.setPhoto(it)
                    navController.navigate("adjust")
                }
            }

            val setWallpaperLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { navController.popBackStack("home", inclusive = false) }

            LaunchedEffect(message) {
                message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
            }

            Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                Box(Modifier.padding(padding)) {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                saved = saved,
                                templates = Templates.all,
                                onOpenSettings = { navController.navigate("settings") },
                                onNewBlank = { vm.startBlank(); navController.navigate("editor") },
                                onUseTemplate = { vm.startFromTemplate(it); navController.navigate("editor") },
                                onOpenSaved = { vm.editExisting(it); navController.navigate("editor") },
                                onDeleteSaved = { vm.delete(it) },
                            )
                        }
                        composable("editor") {
                            EditorScreen(
                                state = editor,
                                onBack = { navController.popBackStack() },
                                onPickPhoto = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                onToggleEffect = { vm.toggleEffect(it) },
                                onParticleStyle = { vm.setParticleStyle(it) },
                                onIntensity = { vm.setIntensity(it) },
                                onName = { vm.setName(it) },
                                onReposition = { navController.navigate("adjust") },
                                onSave = {
                                    scope.launch {
                                        if (vm.saveAndActivate() != null) {
                                            setWallpaperLauncher.launch(vm.buildSetWallpaperIntent())
                                        }
                                    }
                                },
                            )
                        }
                        composable("adjust") {
                            PhotoAdjustScreen(
                                photoUri = editor.photoUri,
                                initialScale = editor.photoScale,
                                initialOffsetX = editor.photoOffsetX,
                                initialOffsetY = editor.photoOffsetY,
                                onBack = { navController.popBackStack() },
                                onConfirm = { s, ox, oy ->
                                    vm.setPhotoTransform(s, ox, oy)
                                    navController.popBackStack()
                                },
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                themeMode = themeMode,
                                onBack = { navController.popBackStack() },
                                onThemeChange = { vm.setTheme(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}
