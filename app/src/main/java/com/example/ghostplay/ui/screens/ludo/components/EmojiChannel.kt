package com.example.ghostplay.ui.screens.ludo.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmojiChannel(
    onEmojiSelected: (String) -> Unit,
    activeEmojis: List<Pair<String, String>>, // Name to Emoji
    modifier: Modifier = Modifier
) {
    val emojis = listOf("🔥", "😂", "🤔", "😮", "👿", "👍", "👻", "⚡")

    Column(modifier = modifier.fillMaxWidth()) {
        // Display Area
        Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            activeEmojis.forEachIndexed { index, pair ->
                var visible by remember { mutableStateOf(true) }
                LaunchedEffect(pair) {
                    kotlinx.coroutines.delay(3000)
                    visible = false
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pair.first, color = Color.White, fontSize = 10.sp, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(pair.second, fontSize = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selector Area
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
        }
    }
}
