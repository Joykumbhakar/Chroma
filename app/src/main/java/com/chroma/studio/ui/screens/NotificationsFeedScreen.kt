package com.chroma.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.ui.components.OscellionFluidBackground
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ChevronLeft

import com.chroma.studio.ui.components.ChromaScreenHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsFeedScreen(
    onBack: () -> Unit
) {
    val colors = LocalOscellionColors.current
    
    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "Activity",
                showBackButton = true,
                onBack = onBack
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Text(
                        text = "Today",
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                items(3) { index ->
                    NotificationItem(
                        username = if (index == 0) "thejoykumbhakar" else "user_$index",
                        action = if (index % 2 == 0) "liked your post." else "started following you.",
                        time = "${index * 2 + 1}h",
                        avatarUrl = null
                    )
                }
                
                item {
                    Text(
                        text = "This Week",
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                    )
                }
                
                items(5) { index ->
                    NotificationItem(
                        username = "artist_$index",
                        action = "remixed your artwork.",
                        time = "${index + 2}d",
                        avatarUrl = null
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    username: String,
    action: String,
    time: String,
    avatarUrl: String?
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.glassBg)
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = username,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                fontSize = 14.sp
            )
            Text(
                text = action,
                color = colors.textMuted,
                fontSize = 14.sp
            )
        }
        
        Text(
            text = time,
            color = colors.textMuted,
            fontSize = 12.sp
        )
    }
}
