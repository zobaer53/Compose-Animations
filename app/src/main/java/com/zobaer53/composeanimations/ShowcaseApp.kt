package com.zobaer53.composeanimations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBackground = Color(0xFFF2F2F4)
private val Muted = Color(0xFF8A8A8E)

/** The demo list; tapping a demo opens it full screen and Back returns to the list. */
@Composable
fun ShowcaseApp() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color.Black)) {
        // An index rather than the Demo itself, so the open demo survives rotation and process death.
        var openDemo by rememberSaveable { mutableStateOf<Int?>(null) }
        val demo = openDemo?.let(Demos::getOrNull)
        if (demo != null) {
            BackHandler { openDemo = null }
            demo.content()
        } else {
            DemoList(onOpen = { openDemo = it })
        }
    }
}

@Composable
private fun DemoList(onOpen: (Int) -> Unit) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .systemBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 8.dp)) {
                Text("Compose Animations", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Canvas animation practice in Jetpack Compose", fontSize = 14.sp, color = Muted)
            }
        }
        itemsIndexed(Demos) { index, demo ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable { onOpen(index) }
                    .padding(20.dp),
            ) {
                Text(demo.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(demo.description, fontSize = 14.sp, color = Muted)
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun ShowcaseAppPreview() {
    ShowcaseApp()
}
