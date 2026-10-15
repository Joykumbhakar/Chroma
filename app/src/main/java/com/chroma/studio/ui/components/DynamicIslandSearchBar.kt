package com.chroma.studio.ui.components
import com.chroma.studio.ui.components.GlassTextButton
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SearchBarState {
    HIDDEN,
    DROPPED,
    EXPANDED
}

@Composable
fun DynamicIslandSearchBar(
    isVisible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    hazeState: HazeState // Pass the hazeState from the parent screen for blurring
) {
    var internalState by remember { mutableStateOf(SearchBarState.HIDDEN) }
    var showContent by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Animation sequences
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Opening Sequence
            internalState = SearchBarState.DROPPED
            delay(300) // wait for drop
            internalState = SearchBarState.EXPANDED
            delay(300) // wait for expansion
            showContent = true
            delay(100)
            focusRequester.requestFocus()
        } else {
            // Closing Sequence
            showContent = false
            focusManager.clearFocus()
            delay(200) // wait for content fade out
            internalState = SearchBarState.DROPPED
            delay(300) // wait for shrink
            internalState = SearchBarState.HIDDEN
        }
    }

    // Background Overlay
    AnimatedVisibility(
        visible = isVisible || internalState != SearchBarState.HIDDEN,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        )
    }

    // Animations for the Island
    val transition = updateTransition(targetState = internalState, label = "IslandTransition")
    
    val yOffset by transition.animateDp(
        transitionSpec = {
            if (targetState == SearchBarState.DROPPED && initialState == SearchBarState.HIDDEN) {
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            } else if (targetState == SearchBarState.HIDDEN) {
                tween(300, easing = FastOutSlowInEasing)
            } else {
                tween(0)
            }
        },
        label = "yOffset"
    ) { state ->
        when (state) {
            SearchBarState.HIDDEN -> (-100).dp
            SearchBarState.DROPPED -> 24.dp
            SearchBarState.EXPANDED -> 24.dp
        }
    }

    val islandWidth by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "islandWidth"
    ) { state ->
        when (state) {
            SearchBarState.HIDDEN, SearchBarState.DROPPED -> 0f // 0f indicates fixed width
            SearchBarState.EXPANDED -> 1f // 1f indicates fill max width
        }
    }
    
    val islandSize by transition.animateDp(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "islandSize"
    ) { state ->
        when (state) {
            SearchBarState.HIDDEN, SearchBarState.DROPPED -> 44.dp
            SearchBarState.EXPANDED -> 52.dp // height when expanded
        }
    }

    val cornerRadius by transition.animateDp(
        transitionSpec = {
            tween(300)
        },
        label = "cornerRadius"
    ) { state ->
        when (state) {
            SearchBarState.HIDDEN, SearchBarState.DROPPED -> 22.dp
            SearchBarState.EXPANDED -> 20.dp
        }
    }

    val islandAlpha by transition.animateFloat(
        transitionSpec = { tween(200) },
        label = "islandAlpha"
    ) { state ->
        if (state == SearchBarState.HIDDEN) 0f else 1f
    }

    if (islandAlpha > 0f) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val glossyBorder = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.15f)
                )
            )

            // The Island Container
            Box(
                modifier = Modifier
                    .offset(y = yOffset)
                    .then(
                        if (islandWidth == 1f) {
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        } else {
                            Modifier.width(44.dp)
                        }
                    )
                    .height(islandSize)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(cornerRadius),
                        spotColor = Color.Black.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(cornerRadius))
                    .hazeChild(state = hazeState, shape = RoundedCornerShape(cornerRadius))
                    .background(Color(0x992D2D2D))
                    .border(
                        width = 1.dp,
                        brush = glossyBorder,
                        shape = RoundedCornerShape(cornerRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner highlight overlay for 3D pill feel
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
                
                // Content
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.Search,
                            contentDescription = "Search",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search Chats/Favorites",
                                    color = Color.Gray,
                                    fontSize = 15.sp
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(
                                    color = Color.White,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = SolidColor(Color.White),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                            )
                        }
                        
                        GlassTextButton(
                            onClick = onClose,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFF0A84FF),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
