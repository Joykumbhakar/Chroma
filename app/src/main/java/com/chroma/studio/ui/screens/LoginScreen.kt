package com.chroma.studio.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.Zap
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.R

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Using DEFAULT_HOME_BG_JSON for consistent main app theme
    val bgLayers = remember(DEFAULT_HOME_BG_JSON) {
        WorkRepository(context).deserializeLayers(DEFAULT_HOME_BG_JSON)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            scope.launch {
                try {
                    val account = task.await()
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).await()
                    onLoginSuccess()
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Login failed"
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
            errorMessage = "Google Sign-In canceled"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF001133)) // Fallback deep blue
    ) {
        // Full screen Animated Gradient Background
        CanvasPreview(
            layers = bgLayers, 
            shape = "full", 
            borderColor = Color.Transparent, 
            modifier = Modifier.fillMaxSize()
        )
        // Add a slight blue overlay to enforce the blue/cyan aesthetic
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF003366).copy(alpha = 0.4f)))
        
        // Top Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.ChevronLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { /* Handle Help */ }
                ) {
                    Text("Help", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }

            // Text content
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .offset(y = (-30).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val titleAnnotated = buildAnnotatedString {
                    append("Unlock\n")
                    withStyle(style = SpanStyle(color = Color(0xFF00FFFF))) {
                        append("AI ")
                    }
                    append("Generation")
                }
                Text(
                    text = titleAnnotated,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Sign in to link your account and track your\ndaily AI generation limits across all devices.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Section
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        errorMessage!!, 
                        color = Color(0xFFFF5252), 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 16.dp)
                    )
                }
    
                // Google Button (Glassmorphism white style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        if (!isLoading) {
                            isLoading = true
                            errorMessage = null
                            try {
                                val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                                if (resId == 0) {
                                    errorMessage = "Google Services not configured properly."
                                    isLoading = false
                                } else {
                                    val defaultWebClientId = context.getString(resId)
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(defaultWebClientId)
                                        .requestEmail()
                                        .build()
                                    val client = GoogleSignIn.getClient(context, gso)
                                    launcher.launch(client.signInIntent)
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to start sign in"
                                isLoading = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF003366), strokeWidth = 2.dp)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Continue with Google", color = Color(0xFF002244), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Trust Signals
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrustSignal(Lucide.ShieldCheck, "Secure\n& Private")
                TrustSignal(Lucide.Cloud, "Sync Across\nDevices")
                TrustSignal(Lucide.Zap, "AI Limits\nTracking")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Terms Text
            val annotatedString = buildAnnotatedString {
                append("By continuing, you agree to Chroma Studio's\n")
                withStyle(style = SpanStyle(color = Color(0xFF00FFFF), fontWeight = FontWeight.Medium)) {
                    append("Terms of Service")
                }
                append(" and ")
                withStyle(style = SpanStyle(color = Color(0xFF00FFFF), fontWeight = FontWeight.Medium)) {
                    append("Privacy Policy")
                }
            }
            Text(
                text = annotatedString,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            )

            // Bottom Navigation Bar Padding
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
fun TrustSignal(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text, 
            color = Color.White, 
            fontSize = 11.sp, 
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}
