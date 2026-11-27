package com.chroma.studio.ui.storystudio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.ToastManager
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private val InteractiveStoryJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

data class StoryPublishSlide(
    val uri: Uri,
    val interactiveFormatJson: String = "",
    val musicTrack: com.chroma.studio.data.AttachedMusicSelection? = null
)

@Serializable
data class PublishedStoryWidget(
    val id: String,
    val widgetType: String,
    val xNorm: Float = 0f,
    val yNorm: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val question: String = "",
    val optA: String = "",
    val optB: String = "",
    val quizOpts: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val prompt: String = "",
    val emoji: String = "🔥",
    val sliderValue: Int = 72,
    val title: String = "",
    val days: String = "00",
    val hours: String = "00",
    val mins: String = "00",
    val handle: String = "",
    val linkLabel: String = "",
    val place: String = "",
    val timeText: String = "",
    val musicTitle: String = "",
    val musicArtist: String = "",
    val musicFreq: Double = 261.63
)

@Serializable
data class PublishedStoryWidgetsEnvelope(
    val stageWdp: Float = 360f,
    val stageHdp: Float = 640f,
    val studioWidgets: List<PublishedStoryWidget> = emptyList()
)

fun StorySlide.toPublishedWidgetsJson(stageWdp: Float, stageHdp: Float): String {
    val widgets = items.filterIsInstance<WidgetStoryItem>()
    if (widgets.isEmpty()) return ""
    val safeW = stageWdp.coerceAtLeast(180f)
    val safeH = stageHdp.coerceAtLeast(320f)
    val payload = PublishedStoryWidgetsEnvelope(
        stageWdp = safeW,
        stageHdp = safeH,
        studioWidgets = widgets.map { w ->
            PublishedStoryWidget(
                id = w.id,
                widgetType = w.widgetType.name,
                xNorm = w.xDp / safeW,
                yNorm = w.yDp / safeH,
                scale = w.scale,
                rotation = w.rotation,
                question = w.question,
                optA = w.optA,
                optB = w.optB,
                quizOpts = w.quizOpts,
                correctIndex = w.correctIndex,
                prompt = w.prompt,
                emoji = w.emoji,
                sliderValue = w.sliderValue,
                title = w.title,
                days = w.days,
                hours = w.hours,
                mins = w.mins,
                handle = w.handle,
                linkLabel = w.linkLabel,
                place = w.place,
                timeText = w.timeText,
                musicTitle = w.musicTitle,
                musicArtist = w.musicArtist,
                musicFreq = w.musicFreq
            )
        }
    )
    return InteractiveStoryJson.encodeToString(payload)
}

fun decodePublishedStoryWidgetsEnvelope(rawDecompressedFormat: String): PublishedStoryWidgetsEnvelope? {
    if (rawDecompressedFormat.isBlank() || !rawDecompressedFormat.contains("studioWidgets")) {
        return null
    }
    return try {
        val env = InteractiveStoryJson.decodeFromString<PublishedStoryWidgetsEnvelope>(rawDecompressedFormat)
        if (env.studioWidgets.isEmpty()) null else env
    } catch (_: Exception) {
        null
    }
}

internal object StoryWidgetResponseStore {
    private const val PREF = "osc_story_widget_responses"

    private fun key(userId: String, storyId: String, widgetId: String, suffix: String): String =
        "${userId.ifBlank { "anon" }}_${storyId}_${widgetId}_$suffix"

    fun getInt(context: Context, userId: String, storyId: String, widgetId: String, suffix: String, default: Int = -1): Int =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(key(userId, storyId, widgetId, suffix), default)

    fun putInt(context: Context, userId: String, storyId: String, widgetId: String, suffix: String, value: Int) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt(key(userId, storyId, widgetId, suffix), value)
            .apply()
    }

    fun getString(context: Context, userId: String, storyId: String, widgetId: String, suffix: String): String? =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(key(userId, storyId, widgetId, suffix), null)

    fun putString(context: Context, userId: String, storyId: String, widgetId: String, suffix: String, value: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(key(userId, storyId, widgetId, suffix), value)
            .apply()
    }

    /** Aggregates saved integer option indices across all local voter entries for a widget. */
    fun aggregateChoicesForWidget(context: Context, storyId: String, widgetId: String, suffix: String): Map<Int, Int> {
        if (storyId.isBlank() || widgetId.isBlank()) return emptyMap()
        val token = "_${storyId}_${widgetId}_$suffix"
        val counts = mutableMapOf<Int, Int>()
        val allEntries = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).all
        for ((k, v) in allEntries) {
            if (k.endsWith(token) && v is Int && v >= 0) {
                counts[v] = (counts[v] ?: 0) + 1
            }
        }
        return counts
    }
}

@Composable
fun StoryInteractiveWidgetsOverlay(
    storyId: String,
    storyAuthorId: String,
    currentUserId: String,
    decompressedTextFormat: String,
    onPauseChanged: (Boolean) -> Unit,
    onWidgetReply: (interactionSummary: String) -> Unit,
    onMentionClick: (handle: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val envelope = remember(decompressedTextFormat) {
        decodePublishedStoryWidgetsEnvelope(decompressedTextFormat)
    } ?: return

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var activeQuestionWidget by remember(storyId) { mutableStateOf<PublishedStoryWidget?>(null) }
    var questionDraft by remember(storyId) { mutableStateOf("") }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewerWdp = maxWidth.value.coerceAtLeast(180f)
        val viewerHdp = maxHeight.value.coerceAtLeast(320f)
        val cropScale = max(viewerWdp / 9f, viewerHdp / 16f)
        val renderedStageWdp = 9f * cropScale
        val renderedStageHdp = 16f * cropScale
        val baseWidthScale = (viewerWdp / envelope.stageWdp.coerceAtLeast(180f)).coerceIn(0.84f, 1.24f)

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            envelope.studioWidgets.forEach { w ->
                val rawXdp = w.xNorm * renderedStageWdp
                val rawYdp = w.yNorm * renderedStageHdp
                val maxHalfX = (viewerWdp * 0.5f - 58f).coerceAtLeast(36f)
                val maxHalfY = (viewerHdp * 0.5f - 82f).coerceAtLeast(70f)
                val xPx = with(density) { rawXdp.coerceIn(-maxHalfX, maxHalfX).dp.toPx() }
                val yPx = with(density) { rawYdp.coerceIn(-maxHalfY, maxHalfY).dp.toPx() }
                val finalScale = (w.scale * baseWidthScale).coerceIn(0.45f, 2.8f)

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = xPx
                            translationY = yPx
                            scaleX = finalScale
                            scaleY = finalScale
                            rotationZ = w.rotation
                        }
                        // Absorb taps on interactive widgets so StoryViewer doesn't advance slides
                        .pointerInput(w.id) {
                            detectTapGestures(
                                onPress = {
                                    onPauseChanged(true)
                                    tryAwaitRelease()
                                    onPauseChanged(false)
                                }
                            )
                        }
                ) {
                    InteractivePublishedWidgetCard(
                        widget = w,
                        storyId = storyId,
                        currentUserId = currentUserId,
                        context = context,
                        onPauseChanged = onPauseChanged,
                        onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onOpenQuestionDialog = { target ->
                            questionDraft = ""
                            activeQuestionWidget = target
                            onPauseChanged(true)
                        },
                        onPlayMusic = { freq ->
                            scope.launch(Dispatchers.IO) {
                                playStoryWidgetChord(freq)
                            }
                        },
                        onMentionClick = onMentionClick,
                        onWidgetReply = onWidgetReply
                    )
                }
            }
        }

        // Inline Question Reply Overlay (pauses timer cleanly)
        AnimatedVisibility(
            visible = activeQuestionWidget != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            val target = activeQuestionWidget
            if (target != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.62f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            activeQuestionWidget = null
                            onPauseChanged(false)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF1C1C1E))
                            .border(0.75.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = target.prompt.ifBlank { "Ask me anything" },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = questionDraft,
                            onValueChange = { questionDraft = it },
                            placeholder = { Text("Write your answer...", color = Color.White.copy(alpha = 0.5f)) },
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF0A84FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                                cursorColor = Color(0xFF0A84FF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        activeQuestionWidget = null
                                        onPauseChanged(false)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            val canSend = questionDraft.trim().isNotEmpty()
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (canSend) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.16f))
                                    .clickable(enabled = canSend) {
                                        val answer = questionDraft.trim()
                                        StoryWidgetResponseStore.putString(
                                            context, currentUserId, storyId, target.id, "answer", answer
                                        )
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (storyAuthorId.isNotBlank() && storyAuthorId != currentUserId) {
                                            onWidgetReply("Q \"${target.prompt}\": $answer")
                                        }
                                        ToastManager.showToast("Response sent!")
                                        activeQuestionWidget = null
                                        onPauseChanged(false)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Lucide.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("Send", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractivePublishedWidgetCard(
    widget: PublishedStoryWidget,
    storyId: String,
    currentUserId: String,
    context: Context,
    onPauseChanged: (Boolean) -> Unit,
    onHaptic: () -> Unit,
    onOpenQuestionDialog: (PublishedStoryWidget) -> Unit,
    onPlayMusic: (Double) -> Unit,
    onMentionClick: (String) -> Unit,
    onWidgetReply: (String) -> Unit
) {
    when (widget.widgetType) {
        "POLL" -> {
            var selectedChoice by remember(storyId, widget.id) {
                mutableIntStateOf(StoryWidgetResponseStore.getInt(context, currentUserId, storyId, widget.id, "poll", -1))
            }
            val pctA by animateFloatAsState(
                targetValue = when (selectedChoice) {
                    0 -> 0.72f
                    1 -> 0.28f
                    else -> 0.50f
                },
                animationSpec = tween(320),
                label = "pollA"
            )
            val pctB = 1f - pctA

            Column(
                modifier = Modifier
                    .width(224.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xF5FFFFFF))
                    .padding(11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(widget.question, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(0 to widget.optA, 1 to widget.optB).forEach { (idx, label) ->
                        val picked = selectedChoice == idx
                        val pctInt = ((if (idx == 0) pctA else pctB) * 100).roundToInt()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (picked) Color(0x18007AFF) else Color(0xFFF2F2F7))
                                .clickable {
                                    selectedChoice = idx
                                    StoryWidgetResponseStore.putInt(context, currentUserId, storyId, widget.id, "poll", idx)
                                    onHaptic()
                                    onWidgetReply("Voted \"$label\" on poll \"${widget.question}\"")
                                    ToastManager.showToast("Voted: $label")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedChoice != -1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(if (idx == 0) pctA else pctB)
                                        .align(Alignment.CenterStart)
                                        .background(if (picked) Color(0x2E007AFF) else Color(0x14000000))
                                )
                            }
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (selectedChoice != -1) "$label $pctInt%" else label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (picked || (selectedChoice == -1 && idx == 0)) Color(0xFF007AFF) else Color(0xFF1C1C1E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        "SLIDER" -> {
            var sliderVal by remember(storyId, widget.id) {
                val saved = StoryWidgetResponseStore.getInt(context, currentUserId, storyId, widget.id, "slider", -1)
                mutableIntStateOf(if (saved in 0..100) saved else widget.sliderValue.coerceIn(5, 95))
            }
            var hasVoted by remember(storyId, widget.id) {
                mutableStateOf(StoryWidgetResponseStore.getInt(context, currentUserId, storyId, widget.id, "slider", -1) >= 0)
            }
            var isSliding by remember { mutableStateOf(false) }
            var trackWidthPx by remember { mutableIntStateOf(1) }

            Column(
                modifier = Modifier
                    .width(224.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xF5FFFFFF))
                    .padding(11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = widget.prompt,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    if (isSliding || hasVoted) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${sliderVal}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF2D55)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(horizontal = 6.dp)
                        .onSizeChanged { trackWidthPx = it.width.coerceAtLeast(1) }
                        .pointerInput(widget.id, storyId) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                down.consume()
                                isSliding = true
                                onPauseChanged(true)

                                fun updateFromX(x: Float) {
                                    val frac = (x / trackWidthPx.toFloat()).coerceIn(0.03f, 0.97f)
                                    sliderVal = (frac * 100f).roundToInt().coerceIn(0, 100)
                                }
                                updateFromX(down.position.x)

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }
                                    change.consume()
                                    updateFromX(change.position.x)
                                }

                                isSliding = false
                                hasVoted = true
                                onPauseChanged(false)
                                StoryWidgetResponseStore.putInt(context, currentUserId, storyId, widget.id, "slider", sliderVal)
                                onHaptic()
                                onWidgetReply("Slid ${widget.emoji} $sliderVal% on \"${widget.prompt}\"")
                                ToastManager.showToast("${widget.emoji} Voted $sliderVal%")
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    val frac = (sliderVal / 100f).coerceIn(0.04f, 0.96f)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE5E5EA)))
                    Box(
                        Modifier
                            .fillMaxWidth(frac)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF9500), Color(0xFFFF2D55))))
                    )
                    Text(
                        text = widget.emoji,
                        fontSize = if (isSliding) 27.sp else 22.sp,
                        modifier = Modifier.offset {
                            val x = (maxWidth.toPx() * frac - 13.dp.toPx()).roundToInt()
                            IntOffset(x, 0)
                        }
                    )
                }
            }
        }

        "QUIZ" -> {
            var chosenIdx by remember(storyId, widget.id) {
                mutableIntStateOf(StoryWidgetResponseStore.getInt(context, currentUserId, storyId, widget.id, "quiz", -1))
            }
            val letters = listOf("A", "B", "C", "D")
            Column(
                modifier = Modifier
                    .width(224.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xF5FFFFFF))
                    .padding(11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(widget.question, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                    widget.quizOpts.forEachIndexed { idx, opt ->
                        val answered = chosenIdx >= 0
                        val isCorrect = idx == widget.correctIndex
                        val isPickedWrong = answered && idx == chosenIdx && !isCorrect
                        val bgColor = when {
                            answered && isCorrect -> Color(0xFF34C759)
                            isPickedWrong -> Color(0xFFFF3B30)
                            else -> Color(0xFFF2F2F7)
                        }
                        val fgColor = if ((answered && isCorrect) || isPickedWrong) Color.White else Color(0xFF111111)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable {
                                    chosenIdx = idx
                                    StoryWidgetResponseStore.putInt(context, currentUserId, storyId, widget.id, "quiz", idx)
                                    onHaptic()
                                    if (isCorrect) {
                                        ToastManager.showToast("Correct! 🎉")
                                    } else {
                                        val rightOpt = widget.quizOpts.getOrElse(widget.correctIndex) { "" }
                                        ToastManager.showToast("Answer: $rightOpt")
                                    }
                                    onWidgetReply("Answered \"$opt\" on quiz \"${widget.question}\"")
                                }
                                .padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if ((answered && isCorrect) || isPickedWrong) Color.White.copy(alpha = 0.25f) else Color(0xFFE5E5EA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(letters.getOrElse(idx) { "?" }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fgColor)
                            }
                            Text(opt, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fgColor, modifier = Modifier.weight(1f))
                            if (answered && isCorrect) {
                                Icon(Lucide.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        "QUESTION" -> {
            var submittedAnswer by remember(storyId, widget.id) {
                mutableStateOf(StoryWidgetResponseStore.getString(context, currentUserId, storyId, widget.id, "answer"))
            }
            val latest = StoryWidgetResponseStore.getString(context, currentUserId, storyId, widget.id, "answer")
            if (latest != submittedAnswer) submittedAnswer = latest

            Column(
                modifier = Modifier
                    .width(224.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xF5FFFFFF))
                    .clickable { onOpenQuestionDialog(widget) }
                    .padding(11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(widget.prompt.ifBlank { "Ask me anything" }, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (submittedAnswer != null) Color(0x1A34C759) else Color(0xFFF2F2F7))
                        .padding(9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = submittedAnswer ?: "Tap to reply...",
                        fontSize = 12.sp,
                        fontWeight = if (submittedAnswer != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (submittedAnswer != null) Color(0xFF1D8348) else Color(0xFF8E8E93),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        "COUNTDOWN" -> {
            var reminded by remember(storyId, widget.id) {
                mutableStateOf(StoryWidgetResponseStore.getInt(context, currentUserId, storyId, widget.id, "remind", 0) == 1)
            }
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE81C1C1E))
                    .clickable {
                        reminded = !reminded
                        StoryWidgetResponseStore.putInt(context, currentUserId, storyId, widget.id, "remind", if (reminded) 1 else 0)
                        onHaptic()
                        ToastManager.showToast(if (reminded) "Countdown reminder set ✓" else "Reminder removed")
                    }
                    .padding(11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(widget.title, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9F0A), letterSpacing = 0.8.sp)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(widget.days to "DAYS", widget.hours to "HRS", widget.mins to "MIN").forEach { (v, lbl) ->
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(v, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(lbl, fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (reminded) Lucide.Check else Lucide.Bell,
                        contentDescription = null,
                        tint = if (reminded) Color(0xFF30D158) else Color(0xFFFF9F0A),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (reminded) "Reminder On" else "Remind Me",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (reminded) Color(0xFF30D158) else Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        "MENTION" -> {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xF0FFFFFF))
                    .clickable {
                        onHaptic()
                        onMentionClick(widget.handle)
                    }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(widget.handle, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
            }
        }

        "LINK" -> {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xF0FFFFFF))
                    .clickable {
                        onHaptic()
                        val raw = widget.linkLabel.trim()
                        val url = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {
                            ToastManager.showToast("Link: $url")
                        }
                    }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Lucide.ExternalLink, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(13.dp))
                Text(widget.linkLabel, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF007AFF))
            }
        }

        "LOCATION" -> {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE01C1C1E))
                    .clickable {
                        onHaptic()
                        try {
                            val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(widget.place)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, geoUri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {
                            ToastManager.showToast("Location: ${widget.place}")
                        }
                    }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Lucide.MapPin, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(13.dp))
                Text(widget.place, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        "TIME" -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xB8000000))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(widget.timeText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.4.sp)
            }
        }

        "MUSIC" -> {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xE01C1C1E))
                    .clickable {
                        onHaptic()
                        onPlayMusic(widget.musicFreq)
                        ToastManager.showToast("Playing ${widget.musicTitle} · ${widget.musicArtist}")
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFFF2D55), Color(0xFFFF9500)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Music, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
                Column {
                    Text(widget.musicTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(widget.musicArtist, fontSize = 10.sp, color = Color.White.copy(alpha = 0.68f))
                }
            }
        }
    }
}

private fun playStoryWidgetChord(rootHz: Double) {
    try {
        val sampleRate = 22050
        val durationSec = 1.4
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        val ratios = listOf(1.0, 1.25, 1.5)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = max(0.0, 1.0 - (t / durationSec)) * minOf(1.0, t / 0.04)
            var sample = 0.0
            for (r in ratios) {
                sample += sin(2.0 * PI * (rootHz * r) * t)
            }
            buffer[i] = ((sample / ratios.size) * env * 0.25 * Short.MAX_VALUE).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep((durationSec * 1000).toLong())
        track.stop()
        track.release()
    } catch (_: Throwable) {
    }
}
