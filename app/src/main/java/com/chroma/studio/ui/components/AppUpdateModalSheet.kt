@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.BuildConfig
import com.chroma.studio.data.AppUpdateCheckerLogic
import com.chroma.studio.data.AppUpdateInstallStage
import com.chroma.studio.data.AppUpdateRelease
import com.composables.icons.lucide.ArrowDownToLine
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PackageCheck
import com.composables.icons.lucide.Rocket
import com.composables.icons.lucide.ShieldAlert
import com.composables.icons.lucide.ShieldCheck

@Composable
fun AppUpdateModalSheet(
    release: AppUpdateRelease,
    stage: AppUpdateInstallStage,
    isForceRequired: Boolean,
    onDownloadClick: () -> Unit,
    onCancelDownload: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onConfirmInstallClick: (String) -> Unit,
    onDismissLater: () -> Unit,
    isDarkMode: Boolean = isSystemInDarkTheme()
) {
    val surfaceBg = if (isDarkMode) Color(0xFF141519) else Color(0xFFFDFDFE)
    val cardBg = if (isDarkMode) Color(0xFF1E2027) else Color(0xFFF2F4F8)
    val primaryText = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
    val accentBlue = Color(0xFF0A84FF)
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)

    // Stage: Unknown Sources permission required dialog
    if (stage is AppUpdateInstallStage.PermissionRequired) {
        Dialog(
            onDismissRequest = { if (!isForceRequired) onDismissLater() },
            properties = DialogProperties(dismissOnClickOutside = !isForceRequired)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(surfaceBg)
                    .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9F0A).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.ShieldAlert,
                        contentDescription = null,
                        tint = Color(0xFFFF9F0A),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Allow App Install Permission",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Oscellion v${release.latestVersion} has finished downloading inside the app. Enable \"Allow from this source\" so Oscellion can install updates directly without opening an external browser or file manager.",
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = secondaryText,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onRequestPermissionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Allow Permission",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!isForceRequired) {
                    TextButton(onClick = onDismissLater) {
                        Text(
                            text = "Install Later",
                            fontSize = 13.sp,
                            color = secondaryText
                        )
                    }
                }
            }
        }
        return
    }

    // Stage: Pop-up confirmation with "Install" button (or live PackageInstaller commit progress)
    if (stage is AppUpdateInstallStage.ReadyToInstallPopup || stage is AppUpdateInstallStage.Installing) {
        val apkPath = (stage as? AppUpdateInstallStage.ReadyToInstallPopup)?.apkPath.orEmpty()
        val isInstalling = stage is AppUpdateInstallStage.Installing

        Dialog(
            onDismissRequest = { if (!isForceRequired && !isInstalling) onDismissLater() },
            properties = DialogProperties(
                dismissOnBackPress = !isForceRequired && !isInstalling,
                dismissOnClickOutside = !isForceRequired && !isInstalling
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(surfaceBg)
                    .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF30D158), Color(0xFF0A84FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.PackageCheck,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (isInstalling) {
                        "Updating Oscellion…"
                    } else {
                        "Install Oscellion v${release.latestVersion}?"
                    },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isInstalling) {
                        (stage as AppUpdateInstallStage.Installing).statusText
                    } else {
                        "Verified package (${release.sizeLabel.ifBlank { "APK" }}) is ready. Tap Install below to update directly without leaving Oscellion."
                    },
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = secondaryText,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.ShieldCheck,
                            contentDescription = null,
                            tint = Color(0xFF30D158),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} → v${release.latestVersion}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText
                        )
                    }
                    if (release.sizeLabel.isNotBlank()) {
                        Text(
                            text = release.sizeLabel,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = secondaryText
                        )
                    }
                }

                if (isInstalling) {
                    CircularProgressIndicator(
                        color = accentBlue,
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .size(30.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Button(
                        onClick = { onConfirmInstallClick(apkPath) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Install",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isForceRequired) {
                        TextButton(onClick = onDismissLater) {
                            Text(
                                text = "Download Later",
                                fontSize = 13.sp,
                                color = secondaryText
                            )
                        }
                    }
                }
            }
        }
        return
    }

    // Bottom Sheet Drawer for New Version Available + Live In-App Download Progress
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (isForceRequired && target == SheetValue.Hidden) false else true
        }
    )

    val downloadingState = stage as? AppUpdateInstallStage.Downloading
    val errorState = stage as? AppUpdateInstallStage.Error

    ModalBottomSheet(
        onDismissRequest = {
            if (!isForceRequired && downloadingState == null) onDismissLater()
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
                    Text(
                        text = if (isForceRequired) "REQUIRED UPDATE" else "NEW VERSION AVAILABLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = if (isForceRequired) Color(0xFFFF453A) else accentBlue
                    )
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
                            "Signed In-App APK • ${release.sha256.take(12)}…"
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

            if (errorState != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFF453A).copy(alpha = 0.12f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ShieldAlert,
                        contentDescription = null,
                        tint = Color(0xFFFF453A),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = errorState.message,
                        fontSize = 12.5.sp,
                        color = Color(0xFFFF453A)
                    )
                }
            }

            if (downloadingState != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { downloadingState.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = accentBlue,
                        trackColor = cardBg
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val downloadedLabel = AppUpdateCheckerLogic.formatBytesMb(downloadingState.bytesDownloaded)
                        val totalLabel = if (downloadingState.totalBytes > 0L) {
                            AppUpdateCheckerLogic.formatBytesMb(downloadingState.totalBytes)
                        } else {
                            release.sizeLabel
                        }
                        Text(
                            text = if (totalLabel.isNotBlank()) "$downloadedLabel / $totalLabel" else downloadedLabel,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = secondaryText
                        )
                        Text(
                            text = "${(downloadingState.progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText
                        )
                    }
                }

                if (!isForceRequired) {
                    TextButton(
                        onClick = onCancelDownload,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Cancel Download",
                            fontSize = 13.5.sp,
                            color = secondaryText
                        )
                    }
                }
            } else {
                val retryApkPath = errorState?.downloadedApkPath
                Button(
                    onClick = {
                        if (!retryApkPath.isNullOrBlank()) {
                            onConfirmInstallClick(retryApkPath)
                        } else {
                            onDownloadClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (!retryApkPath.isNullOrBlank()) Lucide.PackageCheck else Lucide.ArrowDownToLine,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            !retryApkPath.isNullOrBlank() -> "Install v${release.latestVersion}"
                            errorState != null -> "Retry Download v${release.latestVersion}"
                            else -> "Download v${release.latestVersion}"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!isForceRequired) {
                    TextButton(
                        onClick = onDismissLater,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Download Later",
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
