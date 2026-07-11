package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chroma.studio.ui.theme.LocalChromaColors

@Composable
fun DeleteLayerModal(
    dontAskAgain: Boolean,
    onDontAskAgainChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalChromaColors.current

    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.bg)
                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Delete Layer",
                    color = colors.textMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    buildAnnotatedString {
                        append("Are you sure you want to delete this layer?\n")
                        append("You can always undo with ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Ctrl+Z")
                        }
                        append(".")
                    },
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDontAskAgainChange(!dontAskAgain) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (dontAskAgain) colors.primary else Color.Transparent)
                            .border(1.dp, if (dontAskAgain) colors.primary else colors.textMuted.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dontAskAgain) {
                            Icon(Lucide.Check, null, tint = colors.onPrimary, modifier = Modifier.size(10.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Don't ask me again", color = colors.textMuted, fontSize = 11.sp)
                }
            }
            
            Spacer(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.textMuted.copy(alpha = 0.25f)))

            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Cancel",
                        color = Color(0xFF007AFF), // iOS blue
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                
                Spacer(modifier = Modifier.width(0.5.dp).height(44.dp).background(colors.textMuted.copy(alpha = 0.25f)))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Delete",
                        color = Color(0xFFFF3B30), // iOS red
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
