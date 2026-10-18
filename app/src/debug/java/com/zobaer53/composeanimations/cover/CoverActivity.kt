package com.zobaer53.composeanimations.cover

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** Pixels per dp in the PNG, so the cover always comes out at 1280 x 640 whatever the device. */
private const val CoverScale = 2f
/** Long enough for every spring and morph to settle. */
private const val SettleMillis = 1_800L
/** From firing the lightning to its second, brightest flicker. */
private const val BoltPeakMillis = 110L

/**
 * Debug only. Draws [Cover] at a fixed pixel size, saves it to `files/cover.png` and finishes.
 * `scripts/record-demos.sh` starts it and pulls the file with `run-as`.
 */
class CoverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val layer = rememberGraphicsLayer()
            var strikes by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                delay(SettleMillis)
                strikes = 1
                delay(BoltPeakMillis)
                val bitmap = layer.toImageBitmap().asAndroidBitmap()
                withContext(Dispatchers.IO) {
                    File(filesDir, "cover.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                Log.i("Cover", "Saved ${bitmap.width}x${bitmap.height} cover")
                finish()
            }
            // The cover is wider than the phone; it's recorded into the layer whole, even though
            // the screen only shows the middle of it.
            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalDensity provides Density(CoverScale, fontScale = 1f)) {
                    Cover(
                        strikes = strikes,
                        modifier = Modifier
                            .requiredSize(CoverWidth, CoverHeight)
                            .drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            },
                    )
                }
            }
        }
    }
}
