package com.chroma.studio.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import com.chroma.studio.R
import com.chroma.studio.ui.components.doubleEdgeGlassBorder
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw

/**
 * DuplicateAccountDialog
 *
 * Triggered in Flow B when tapping "Create a new account" but the selected Google email
 * is already registered in the database.
 *
 * Actions:
 * - "Go to Login" -> navigates to AccountConfirmationScreen
 * - "Use a different Google account" -> signs out/revokes and re-prompts Google picker
 * - "Cancel" -> returns to Welcome screen
 */
@Composable
fun DuplicateAccountDialog(
    email: String,
    onGoToLogin: () -> Unit,
    onUseDifferentAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Samsung One UI Dialog with Iconic Double Edge Glass Border
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF10141E))
                .doubleEdgeGlassBorder(cornerRadius = 28.dp)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning badge icon with double edge glass border
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x22F59E0B))
                        .doubleEdgeGlassBorder(cornerRadius = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Alert",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Account already exists",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    letterSpacing = (-0.4).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "An account with this Google email already exists. Please log in instead or choose another account.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                if (email.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF1E2433))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_google_logo),
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = email,
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Primary Action: "Go to Login"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0066FF))
                        .clickable(role = Role.Button, onClick = onGoToLogin)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Go to Login",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Lucide.ArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Secondary Action: "Use a different Google account"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2536))
                        .clickable(role = Role.Button, onClick = onUseDifferentAccount)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.RefreshCw,
                        contentDescription = null,
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Use a different Google account",
                        color = Color(0xFF93C5FD),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Cancel Link
                Text(
                    text = "Cancel",
                    color = Color(0xFF64748B),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
