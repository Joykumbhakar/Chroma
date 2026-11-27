@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.BuildConfig
import com.chroma.studio.data.AppUpdateRelease
import com.composables.icons.lucide.ArrowDownToLine
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Rocket
import com.composables.icons.lucide.ShieldCheck
import androidx.compose.material3.Icon

@Composable
fun AppUpdateModalSheet(
    release: AppUpdateRelease,
    isForceRequired: Boolean,
    downloadProgress: Float?,
    downloadStatusText: String?,
    onDownloadAndInstall: () -> Unit,
    onOpenDownloadPage: () -> Unit,
    onDismissLater: () -> Unit,
    isDarkMode: Boolean = isSystemInDarkTheme()
) {
    val surfaceBg = if (isDarkMode) Color(0xFF141519) else Color(0xFFFDFDFE)
    val cardBg = if (isDarkMode) Color(0xFF1E2027) else Color(0xFFF2F4F8)
    val primaryText = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
    val accentBlue = Color(0xFF0A84FF)
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (isForceRequired && target == SheetValue.Hidden) false else true
        }
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (!isForceRequired) onDismissLater()
        },
        sheetState = sheetState,
        containerColor = surfaceBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Rocket,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isForceRequired) "REQUIRED UPDATE" else "NEW VERSION AVAILABLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = if (isForceRequired) Color(0xFFFF453A) else accentBlue
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = release.title.ifBlank { "Oscellion v${release.latestVersion}" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Text(
                        text = "Installed v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) → v${release.latestVersion} (${release.latestVersionCode})",
                        fontSize = 12.5.sp,
                        color = secondaryText
                    )
                }
            }

            if (release.summary.isNotBlank()) {
                Text(
                    text = release.summary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = primaryText.copy(alpha = 0.88f)
                )
            }

            if (release.highlights.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "What's New",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryText
                    )
                    release.highlights.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(accentBlue.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    tint = accentBlue,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Text(
                                text = item,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = primaryText
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Lucide.ShieldCheck,
                        contentDescription = null,
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (release.sha256.isNotBlank()) {
                            "Signed APK • ${release.sha256.take(12)}…"
                        } else {
                            "Signed Oscellion APK"
                        },
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (release.sizeLabel.isNotBlank()) {
                    Text(
                        text = release.sizeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryText
                    )
                }
            }

            if (downloadProgress != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = accentBlue,
                        trackColor = cardBg
                    )
                    Text(
                        text = downloadStatusText ?: "Downloading update…",
                        fontSize = 12.sp,
                        color = secondaryText
                    )
                }
            }

            Button(
                onClick = onDownloadAndInstall,
                enabled = downloadProgress == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentBlue,
                    contentColor = Color.White,
                    disabledContainerColor = accentBlue.copy(alpha = 0.45f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Icon(
                    imageVector = Lucide.ArrowDownToLine,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (downloadProgress != null) {
                        "Downloading Update…"
                    } else {
                        "Download & Install v${release.latestVersion}"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isForceRequired) Arrangement.Center else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenDownloadPage() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ExternalLink,
                        contentDescription = null,
                        tint = secondaryText,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Download in browser",
                        fontSize = 12.5.sp,
                        color = secondaryText
                    )
                }

                if (!isForceRequired) {
                    TextButton(onClick = onDismissLater) {
                        Text(
                            text = "Remind Me Later",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryText
                        )
                    }
                }
            }
        }
    }
}
