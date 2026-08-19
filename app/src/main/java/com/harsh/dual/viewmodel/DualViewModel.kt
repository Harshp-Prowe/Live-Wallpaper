package com.harsh.dual.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.harsh.dual.data.AppDiscovery
import com.harsh.dual.data.Clone
import com.harsh.dual.data.CloneRepository
import com.harsh.dual.data.InstalledApp
import com.harsh.dual.data.SpaceState
import com.harsh.dual.engine.EngineResult
import com.harsh.dual.engine.WorkProfileEngine
import com.harsh.dual.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val spaceState: SpaceState = SpaceState.NOT_CREATED,
    val busy: Boolean = false,
    val message: String? = null,
)

class DualViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CloneRepository(app)
    private val discovery = AppDiscovery(app)
    private val engine = WorkProfileEngine(app)

    private val _ui = MutableStateFlow(UiState(spaceState = engine.spaceState()))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    val clones: StateFlow<List<Clone>> =
        repo.clones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val themeMode: StateFlow<ThemeMode> =
        repo.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val onboarded: StateFlow<Boolean?> =
        repo.onboarded.map<Boolean, Boolean?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val appLock: StateFlow<Boolean> =
        repo.appLock.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _installed = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installed: StateFlow<List<InstalledApp>> = _installed.asStateFlow()
    private val _loadingApps = MutableStateFlow(false)
    val loadingApps: StateFlow<Boolean> = _loadingApps.asStateFlow()

    fun refreshSpaceState() {
        _ui.value = _ui.value.copy(spaceState = engine.spaceState())
    }

    fun createSpaceIntent() = engine.createSpaceIntent()

    fun loadInstalledApps() {
        if (_loadingApps.value) return
        viewModelScope.launch {
            _loadingApps.value = true
            _installed.value = discovery.loadLaunchableApps()
            _loadingApps.value = false
        }
    }

    fun clone(appInfo: InstalledApp) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, message = null)
            when (val r = engine.cloneApp(appInfo.packageName)) {
                is EngineResult.Success -> {
                    repo.addClone(Clone(appInfo.packageName, appInfo.label, System.currentTimeMillis()))
                    _ui.value = _ui.value.copy(busy = false, message = "${appInfo.label} cloned into your space")
                }
                is EngineResult.Failure ->
                    _ui.value = _ui.value.copy(busy = false, message = r.reason)
            }
        }
    }

    fun removeClone(clone: Clone) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true)
            when (val r = engine.removeClone(clone.packageName)) {
                is EngineResult.Success -> {
                    repo.removeClone(clone.packageName)
                    _ui.value = _ui.value.copy(busy = false, message = "${clone.label} removed")
                }
                is EngineResult.Failure -> _ui.value = _ui.value.copy(busy = false, message = r.reason)
            }
        }
    }

    fun launchClone(clone: Clone) {
        when (val r = engine.launchClone(clone.packageName)) {
            is EngineResult.Failure -> _ui.value = _ui.value.copy(message = r.reason)
            EngineResult.Success -> Unit
        }
    }

    fun removeSpace() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true)
            when (val r = engine.removeSpace()) {
                is EngineResult.Success -> {
                    repo.clearAll()
                    refreshSpaceState()
                    _ui.value = _ui.value.copy(busy = false, message = "Private space removed")
                }
                is EngineResult.Failure -> _ui.value = _ui.value.copy(busy = false, message = r.reason)
            }
        }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setAppLock(enabled: Boolean) = viewModelScope.launch { repo.setAppLock(enabled) }
    fun completeOnboarding() = viewModelScope.launch { repo.setOnboarded() }
    fun consumeMessage() { _ui.value = _ui.value.copy(message = null) }
}
