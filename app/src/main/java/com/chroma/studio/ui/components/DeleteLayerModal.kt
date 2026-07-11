package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chroma.studio.ui.theme.LocalChromaColors

/**
 * #delete-modal .ios-modal-card — centered confirmation card with Cancel / Delete actions.
 */
@Composable
fun DeleteLayerModal(layerName: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val colors = LocalChromaColors.current

    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.bg)
                .padding(20.dp)
        ) {
            Text("Delete Layer", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
            Text(
                "Are you sure you want to delete \u201c$layerName\u201d? This can't be undone from here, but Undo in the toolbar will restore it.",
                color = colors.textMuted,
                fontSize = 13.sp
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Cancel",
                    color = colors.textMuted,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onCancel() }
                )
                Text(
                    "Delete",
                    color = colors.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onConfirm() }
                )
            }
        }
    }
}
