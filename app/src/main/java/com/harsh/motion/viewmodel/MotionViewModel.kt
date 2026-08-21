package com.harsh.motion.viewmodel

import android.app.Application
import android.app.WallpaperManager
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.harsh.motion.data.EffectType
import com.harsh.motion.data.ParticleStyle
import com.harsh.motion.data.WallpaperConfig
import com.harsh.motion.data.WallpaperRepository
import com.harsh.motion.data.WallpaperTemplate
import com.harsh.motion.engine.PhotoStore
import com.harsh.motion.ui.theme.ThemeMode
import com.harsh.motion.wallpaper.MotionWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class EditorState(
    // Non-null when editing an already-saved wallpaper: saving then updates that
    // entry in place instead of minting a new id, which previously left a
    // duplicate behind on every edit.
    val editingId: String? = null,
    val photoUri: String? = null,
    // A single effect (esp. tilt-only) reads as "nothing is happening" until the
    // phone is physically tilted. Defaulting to a combo that's alive on its own
    // (floating drift + particles + light) plus the two interactive ones gives
    // a lively result out of the box, matching what the templates already do.
    val effects: Set<EffectType> = setOf(
        EffectType.FLOATING,
        EffectType.PARTICLES,
        EffectType.DYNAMIC_LIGHT,
        EffectType.TILT_PARALLAX,
        EffectType.TOUCH_REACTIVE,
        EffectType.CINEMATIC_ZOOM,
        EffectType.AURORA_GLOW,
    ),
    val particleStyle: ParticleStyle = ParticleStyle.SPARKLE,
    val intensity: Float = 0.75f,
    val name: String = "",
    val photoScale: Float = 1f,
    val photoOffsetX: Float = 0f,
    val photoOffsetY: Float = 0f,
)

class MotionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WallpaperRepository(app)

    val savedConfigs: StateFlow<List<WallpaperConfig>> =
        repo.configs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val themeMode: StateFlow<ThemeMode> =
        repo.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        // Safe to run at startup: the editor is empty, so every file that no
        // saved wallpaper references really is abandoned.
        pruneUnreferencedPhotos()
    }

    /** Drops private photo copies no saved wallpaper points at any more. See
     *  [PhotoStore.pruneUnreferenced] for why leaving them behind broke
     *  wallpaper changing device-wide. */
    private fun pruneUnreferencedPhotos() = viewModelScope.launch {
        val app = getApplication<Application>()
        val referenced = repo.configs.first().map { it.photoUri }
        withContext(Dispatchers.IO) {
            PhotoStore.pruneUnreferenced(app, referenced)
        }
    }

    fun startFromTemplate(template: WallpaperTemplate) {
        _editor.value = EditorState(
            effects = template.effects,
            particleStyle = template.particleStyle,
            name = template.name,
        )
    }

    fun startBlank() {
        _editor.value = EditorState()
    }

    fun editExisting(config: WallpaperConfig) {
        _editor.value = EditorState(
            editingId = config.id,
            photoUri = config.photoUri,
            effects = config.effects,
            particleStyle = config.particleStyle,
            intensity = config.intensity,
            name = config.name,
            photoScale = config.scale,
            photoOffsetX = config.offsetX,
            photoOffsetY = config.offsetY,
        )
    }

    fun setPhoto(uri: Uri) {
        // Photo Picker URIs are only reliably readable right at pick time, so
        // copy the photo into our own private storage immediately — every
        // later read (preview, wallpaper service, app restart) then reads a
        // plain file we own, with no URI-permission edge cases.
        viewModelScope.launch {
            val app = getApplication<Application>()
            val saved = runCatching { withContext(Dispatchers.IO) { PhotoStore.copyToPrivateStorage(app, uri) } }
            saved.onSuccess { localUri ->
                // Reset any prior crop — it belonged to a different photo.
                _editor.value = _editor.value.copy(
                    photoUri = localUri.toString(),
                    photoScale = 1f,
                    photoOffsetX = 0f,
                    photoOffsetY = 0f,
                )
            }.onFailure {
                _message.value = "Couldn't use that photo: ${it.message}"
            }
        }
    }

    fun setPhotoTransform(scale: Float, offsetX: Float, offsetY: Float) {
        _editor.value = _editor.value.copy(photoScale = scale, photoOffsetX = offsetX, photoOffsetY = offsetY)
    }

    fun toggleEffect(effect: EffectType) {
        val current = _editor.value.effects
        _editor.value = _editor.value.copy(
            effects = if (effect in current) current - effect else current + effect,
        )
    }

    fun setParticleStyle(style: ParticleStyle) {
        _editor.value = _editor.value.copy(particleStyle = style)
    }

    fun setIntensity(value: Float) {
        _editor.value = _editor.value.copy(intensity = value)
    }

    fun setName(name: String) {
        _editor.value = _editor.value.copy(name = name)
    }

    fun currentConfigOrNull(): WallpaperConfig? {
        val state = _editor.value
        val photo = state.photoUri ?: return null
        if (state.effects.isEmpty()) return null
        return WallpaperConfig(
            id = state.editingId ?: UUID.randomUUID().toString(),
            name = state.name.ifBlank { "My Wallpaper" },
            photoUri = photo,
            effects = state.effects,
            particleStyle = state.particleStyle,
            intensity = state.intensity,
            scale = state.photoScale,
            offsetX = state.photoOffsetX,
            offsetY = state.photoOffsetY,
        )
    }

    /** Saves the current editor state and marks it active. When this app's
     *  wallpaper isn't applied yet the caller launches [buildSetWallpaperIntent]
     *  to show Android's own confirmation screen; when it already is, the
     *  running engine picks the change up on its own. */
    suspend fun saveAndActivate(): WallpaperConfig? {
        val config = currentConfigOrNull() ?: run {
            _message.value = "Choose a photo and at least one effect first."
            return null
        }
        repo.save(config)
        repo.setActive(config.id)
        // Keep editing the same entry, so a second save updates it rather than
        // piling up another copy.
        _editor.value = _editor.value.copy(editingId = config.id)
        // Clears the copies left by photos picked and then replaced during this
        // editing session; the one just saved is referenced, so it survives.
        pruneUnreferencedPhotos()
        if (isThisWallpaperActive()) {
            _message.value = "Wallpaper updated — your changes are already live."
        }
        return config
    }

    /** True when this app's live wallpaper is the one currently applied. If so,
     *  saving is enough — the running engine picks the change up on its own, and
     *  re-launching Android's picker for an already-active wallpaper is a no-op
     *  (or an outright dead end) on several older OEM builds. */
    fun isThisWallpaperActive(): Boolean {
        val app = getApplication<Application>()
        val info = WallpaperManager.getInstance(app).wallpaperInfo ?: return false
        return info.packageName == app.packageName &&
            info.serviceName == MotionWallpaperService::class.java.name
    }

    fun activate(config: WallpaperConfig) = viewModelScope.launch { repo.setActive(config.id) }

    fun delete(config: WallpaperConfig) {
        viewModelScope.launch {
            repo.delete(config.id)
            // Reads the list back rather than assuming, so a photo another
            // saved wallpaper still uses is never deleted out from under it.
            pruneUnreferencedPhotos()
        }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun consumeMessage() { _message.value = null }

    fun buildSetWallpaperIntent(): Intent {
        val app = getApplication<Application>()
        return Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                android.content.ComponentName(app, MotionWallpaperService::class.java),
            )
        }
    }
}
