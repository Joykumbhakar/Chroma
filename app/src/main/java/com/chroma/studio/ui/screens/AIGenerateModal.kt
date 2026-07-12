package com.chroma.studio.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chroma.studio.ai.AIGenerator
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.ui.theme.LocalChromaColors
import com.google.firebase.auth.FirebaseAuth
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AIGenerateModal(
    onDismiss: () -> Unit,
    onApplyLayers: (List<GradientLayer>) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalChromaColors.current
    val scope = rememberCoroutineScope()
    
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: "guest"
    
    val prefs = remember { context.getSharedPreferences("ai_prefs_$uid", Context.MODE_PRIVATE) }
    
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    var generationsToday by remember {
        mutableStateOf(
            if (prefs.getString("last_gen_date", "") == today) {
                prefs.getInt("gen_count", 0)
            } else {
                0
            }
        )
    }
    
    val maxGenerations = 3

    var prompt by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            try {
                selectedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load image"
            }
        }
    }

    Dialog(onDismissRequest = { if (!isGenerating) onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.glassBg)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "AI Gradient Generator",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textMain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$generationsToday/$maxGenerations used",
                        color = if (generationsToday >= maxGenerations) Color.Red else colors.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Describe the gradient (e.g. 'Cyberpunk neon city' or 'Soft pastel sunset')") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textMain,
                        unfocusedTextColor = colors.textMain,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.glassBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.glassBgHover, contentColor = colors.textMain),
                        enabled = !isGenerating
                    ) {
                        Icon(Lucide.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (selectedBitmap != null) "Change Image" else "Upload Image")
                    }

                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected Reference",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                        )
                        IconButton(onClick = { 
                            selectedBitmap = null
                            selectedImageUri = null
                        }) {
                            Icon(Lucide.X, contentDescription = "Remove Image", tint = colors.textMuted)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (generationsToday >= maxGenerations) {
                            errorMessage = "You have reached your daily limit of $maxGenerations generations."
                            return@Button
                        }
                        
                        isGenerating = true
                        errorMessage = null
                        
                        scope.launch {
                            try {
                                var base64Image: String? = null
                                selectedBitmap?.let { bmp ->
                                    // scale down for API
                                    val scaled = Bitmap.createScaledBitmap(bmp, 512, 512, true)
                                    val baos = ByteArrayOutputStream()
                                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                    base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                                }
                                
                                val generatedLayers = AIGenerator.generateGradientLayers(prompt, base64Image)
                                
                                // Update limits
                                val newCount = generationsToday + 1
                                prefs.edit()
                                    .putString("last_gen_date", today)
                                    .putInt("gen_count", newCount)
                                    .apply()
                                generationsToday = newCount
                                
                                onApplyLayers(generatedLayers)
                                onDismiss()
                            } catch (e: Exception) {
                                errorMessage = "Generation failed: ${e.message}"
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.bg),
                    enabled = !isGenerating && prompt.isNotBlank() && generationsToday < maxGenerations
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.bg)
                    } else {
                        Icon(Lucide.Sparkles, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Generate AI Gradient", fontWeight = FontWeight.Bold)
                    }
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), enabled = !isGenerating) {
                    Text("Cancel", color = colors.textMuted)
                }
            }
        }
    }
}
