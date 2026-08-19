package com.harsh.motion.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.harsh.motion.engine.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Dedicated photo positioning step, shown right after picking a photo (and
 * reachable again from the editor). Kept separate from the animated effects
 * preview so positioning a photo — pinch to zoom, drag to pan, or use the
 * +/- buttons — isn't muddled by ripples/particles running at the same time.
 *
 * Draws with a raw [Canvas] using the exact same translate/scale math as
 * [com.harsh.motion.engine.EffectRenderer], so what's framed here is exactly
 * what the live wallpaper will show — no Compose auto-fit surprises.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoAdjustScreen(
    photoUri: String?,
    initialScale: Float,
    initialOffsetX: Float,
    initialOffsetY: Float,
    onBack: () -> Unit,
    onConfirm: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Position your photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (photoUri == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            val context = LocalContext.current
            var bitmap by remember(photoUri) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(photoUri) {
                bitmap = runCatching {
                    withContext(Dispatchers.IO) { BitmapLoader.decodeScaled(context, Uri.parse(photoUri), 1600) }
                }.getOrNull()
            }

            val bmp = bitmap
            if (bmp == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            var scale by remember(photoUri) { mutableFloatStateOf(initialScale) }
            var offsetPx by remember(photoUri) { mutableStateOf(Offset(0f, 0f)) }
            var boxSizePx by remember { mutableStateOf(Offset(1f, 1f)) }
            var restored by remember(photoUri) { mutableStateOf(false) }

            Text(
                "Pinch or use +/- to zoom. Drag to reposition — useful for wide photos where you want a specific left/right area on screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .pointerInput(bmp) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                                    val (maxX, maxY) = maxPan(boxSizePx.x, boxSizePx.y, bmp, newScale)
                                    scale = newScale
                                    offsetPx = Offset(
                                        (offsetPx.x + pan.x).coerceIn(-maxX, maxX),
                                        (offsetPx.y + pan.y).coerceIn(-maxY, maxY),
                                    )
                                }
                            },
                    ) {
                        boxSizePx = Offset(size.width, size.height)
                        if (!restored) {
                            val (maxX, maxY) = maxPan(size.width, size.height, bmp, scale)
                            offsetPx = Offset(
                                initialOffsetX.coerceIn(-1f, 1f) * maxX,
                                initialOffsetY.coerceIn(-1f, 1f) * maxY,
                            )
                            restored = true
                        }

                        val fitScale = min(size.width / bmp.width, size.height / bmp.height) * scale
                        val image = bmp.asImageBitmap()
                        translate(left = size.width / 2f + offsetPx.x, top = size.height / 2f + offsetPx.y) {
                            scale(fitScale, fitScale, pivot = Offset.Zero) {
                                translate(left = -bmp.width / 2f, top = -bmp.height / 2f) {
                                    drawImage(image)
                                }
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedIconButton(onClick = {
                    val newScale = (scale / 1.25f).coerceIn(1f, 4f)
                    val (maxX, maxY) = maxPan(boxSizePx.x, boxSizePx.y, bmp, newScale)
                    scale = newScale
                    offsetPx = Offset(offsetPx.x.coerceIn(-maxX, maxX), offsetPx.y.coerceIn(-maxY, maxY))
                }) { Icon(Icons.Rounded.Remove, contentDescription = "Zoom out") }

                OutlinedIconButton(onClick = {
                    val newScale = (scale * 1.25f).coerceIn(1f, 4f)
                    val (maxX, maxY) = maxPan(boxSizePx.x, boxSizePx.y, bmp, newScale)
                    scale = newScale
                    offsetPx = Offset(offsetPx.x.coerceIn(-maxX, maxX), offsetPx.y.coerceIn(-maxY, maxY))
                }) { Icon(Icons.Rounded.Add, contentDescription = "Zoom in") }

                OutlinedIconButton(onClick = { scale = 1f; offsetPx = Offset(0f, 0f) }) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset")
                }

                Button(
                    onClick = {
                        val (maxX, maxY) = maxPan(boxSizePx.x, boxSizePx.y, bmp, scale)
                        val fx = if (maxX > 0f) (offsetPx.x / maxX).coerceIn(-1f, 1f) else 0f
                        val fy = if (maxY > 0f) (offsetPx.y / maxY).coerceIn(-1f, 1f) else 0f
                        onConfirm(scale, fx, fy)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Use this position") }
            }
        }
    }
}

/** Same "how far can this photo pan at this zoom" math as
 *  [com.harsh.motion.engine.EffectRenderer.maxPan], in Compose pixel terms. */
private fun maxPan(boxW: Float, boxH: Float, bmp: Bitmap, scale: Float): Pair<Float, Float> {
    if (boxW <= 0f || boxH <= 0f) return 0f to 0f
    val fitScale = min(boxW / bmp.width, boxH / bmp.height) * scale
    val maxX = max(0f, (bmp.width * fitScale - boxW) / 2f)
    val maxY = max(0f, (bmp.height * fitScale - boxH) / 2f)
    return maxX to maxY
}
