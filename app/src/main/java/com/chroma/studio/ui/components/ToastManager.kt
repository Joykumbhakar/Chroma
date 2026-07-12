package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalChromaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ToastManager {
    data class ToastData(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null,
        val durationMs: Long = 3000L
    )

    private val _toasts = MutableSharedFlow<ToastData>(extraBufferCapacity = 1)
    val toasts = _toasts.asSharedFlow()

    fun showToast(message: String, actionLabel: String? = null, durationMs: Long = 3000L, onAction: (() -> Unit)? = null) {
        _toasts.tryEmit(ToastData(message, actionLabel, onAction, durationMs))
    }
}

@Composable
fun ChromaToastHost() {
    val colors = LocalChromaColors.current
    var currentToast by remember { mutableStateOf<ToastManager.ToastData?>(null) }
    
    LaunchedEffect(Unit) {
        ToastManager.toasts.collect { toast ->
            currentToast = toast
            delay(toast.durationMs)
            if (currentToast == toast) {
                currentToast = null
            }
        }
    }

    AnimatedVisibility(
        visible = currentToast != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(), // From top
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp).statusBarsPadding(), 
            contentAlignment = Alignment.TopCenter
        ) {
            currentToast?.let { toast ->
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.glassBg)
                        .glossyBorder(RoundedCornerShape(50), colors)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = toast.message,
                        color = colors.textMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (toast.actionLabel != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = toast.actionLabel,
                            color = colors.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                toast.onAction?.invoke()
                                currentToast = null
                            }
                        )
                    }
                }
            }
        }
    }
}
