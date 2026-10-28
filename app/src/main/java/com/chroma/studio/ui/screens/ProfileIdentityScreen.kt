package com.chroma.studio.ui.screens


import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.chroma.studio.data.SupabaseClient
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileIdentityScreen(
    profile: UserProfile,
    works: List<DiscoverPost> = emptyList(),
    onSave: suspend (UserProfile) -> Result<Unit>,
    onBack: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()


    // Form states
    var displayName by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var avatarUrl by remember { mutableStateOf(profile.avatarUrl) }
    var bio by remember { mutableStateOf(profile.bio) }
    var role by remember { mutableStateOf(profile.role.ifBlank { "Developer" }) }
    var dob by remember { mutableStateOf(profile.dateOfBirthAsLong ?: profile.dob) }
    var gender by remember { mutableStateOf(profile.gender.ifBlank { "Prefer not to say" }) }
    
    var contentTags by remember { mutableStateOf(profile.contentTags.toList()) }
    var dataVisibility by remember { mutableStateOf(profile.dataVisibility.toMutableMap()) }
    var isPrivateAccount by remember { mutableStateOf(profile.isPrivateAccount) }


    var officeLocation by remember { mutableStateOf(profile.officeLocation.ifBlank { "Remotely" }) }
    var homeLocation by remember { mutableStateOf(profile.homeLocation) }
    var countryLocation by remember { mutableStateOf(profile.countryLocation.ifBlank { "United States" }) }


    var github by remember { mutableStateOf(profile.socialLinks["github"] ?: "") }
    var instagram by remember { mutableStateOf(profile.socialLinks["instagram"] ?: "") }
    var website by remember { mutableStateOf(profile.socialLinks["website"] ?: "") }


    var selectedBannerWorkId by remember { mutableStateOf(profile.bannerWorkId) }
    var bannerUrl by remember { mutableStateOf(profile.bannerUrl.takeIf { it.isNotBlank() } ?: "preset:wave_6") }


    // Navigation and Upload states
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isSaving by remember { mutableStateOf(false) }


    var selectedBannerUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }


    // Generic Modal Sheet State
    var activePickerSheet by remember { mutableStateOf<PickerSheetConfig?>(null) }


    val isUsernameValid = remember(username) {
        val clean = username.trim().lowercase()
        clean.length in 3..30 && clean.matches(Regex("^[a-z0-9._]+$"))
    }


    val isDisplayNameValid = remember(displayName) {
        displayName.trim().isNotBlank() && displayName.trim().length <= 50
    }


    // Save Execution with Verification
    fun handleSave() {
        if (isSaving) return


        val cleanName = displayName.trim()
        val cleanUser = username.trim().lowercase()


        if (cleanName.isBlank()) {
            Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }


        if (cleanUser.length < 3 || cleanUser.length > 30) {
            Toast.makeText(context, "Username must be between 3 and 30 characters", Toast.LENGTH_SHORT).show()
            return
        }


        if (!cleanUser.matches(Regex("^[a-z0-9._]+$"))) {
            Toast.makeText(context, "Username can only contain lowercase letters, numbers, periods, and underscores", Toast.LENGTH_SHORT).show()
            return
        }


        if (isUploadingAvatar || isUploadingBanner) {
            Toast.makeText(context, "Please wait for media upload to finish", Toast.LENGTH_SHORT).show()
            return
        }


        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)


        coroutineScope.launch {
            isSaving = true
            val newSocialLinks = mutableMapOf<String, String>()
            if (github.isNotBlank()) newSocialLinks["github"] = github.trim()
            if (instagram.isNotBlank()) newSocialLinks["instagram"] = instagram.trim()
            if (website.isNotBlank()) newSocialLinks["website"] = website.trim()


            val updated = profile.copy(
                displayName = cleanName,
                username = cleanUser,
                avatarUrl = avatarUrl.trim(),
                bannerUrl = bannerUrl.trim(),
                bio = bio.trim(),
                role = role.trim(),
                dob = dob,
                dateOfBirth = dob,
                gender = gender,
                contentTags = contentTags,
                dataVisibility = dataVisibility,
                socialLinks = newSocialLinks,
                bannerWorkId = selectedBannerWorkId,
                location = homeLocation.trim().ifBlank { countryLocation.trim() },
                officeLocation = officeLocation.trim(),
                homeLocation = homeLocation.trim(),
                countryLocation = countryLocation.trim(),
                isPrivateAccount = isPrivateAccount
            )


            val result = onSave(updated)
            isSaving = false


            if (result.isSuccess) {
                com.chroma.studio.data.SavedAccountManager.saveFromProfile(
                    context,
                    updated,
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email
                )
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                onBack()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile changes"
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }


    // Banner and Avatar Upload Launchers
    val bannerGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedBannerUri = uri
            coroutineScope.launch {
                isUploadingBanner = true
                val bytes = com.chroma.studio.utils.ImageUtils.compressBannerForSupabase(context, uri)
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (bytes != null && uid != null) {
                    val url = uploadBannerToSupabase(uid, bytes)
                    if (url != null) {
                        bannerUrl = url
                        Toast.makeText(context, "Banner uploaded", Toast.LENGTH_SHORT).show()
                    }
                }
                isUploadingBanner = false
            }
        }
    }


    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedAvatarUri = uri
            coroutineScope.launch {
                isUploadingAvatar = true
                val bytes = com.chroma.studio.utils.compressImageToUnder50KB(context, uri)
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (bytes != null && uid != null) {
                    val url = uploadAvatarToSupabase(uid, bytes)
                    if (url != null) {
                        avatarUrl = url
                        Toast.makeText(context, "Avatar uploaded", Toast.LENGTH_SHORT).show()
                    }
                }
                isUploadingAvatar = false
            }
        }
    }


    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            selectedAvatarUri = cameraImageUri
            coroutineScope.launch {
                isUploadingAvatar = true
                val bytes = com.chroma.studio.utils.compressImageToUnder50KB(context, cameraImageUri!!)
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (bytes != null && uid != null) {
                    val url = uploadAvatarToSupabase(uid, bytes)
                    if (url != null) {
                        avatarUrl = url
                        Toast.makeText(context, "Avatar captured and saved", Toast.LENGTH_SHORT).show()
                    }
                }
                isUploadingAvatar = false
            }
        }
    }


    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            try {
                val tmpFile = File(context.cacheDir, "avatar_profile_tmp.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tmpFile)
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission required to capture photos", Toast.LENGTH_SHORT).show()
        }
    }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
                val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true


                if (isGpsEnabled || isNetworkEnabled) {
                    val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
                    @Suppress("MissingPermission")
                    val location = locationManager?.getLastKnownLocation(provider)
                    if (location != null) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                val address = addresses.firstOrNull()
                                if (address != null) {
                                    countryLocation = address.countryName ?: countryLocation
                                    if (homeLocation.isBlank()) {
                                        homeLocation = address.locality ?: address.subAdminArea ?: ""
                                    }
                                    Toast.makeText(context, "Location updated from GPS", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val address = addresses?.firstOrNull()
                            if (address != null) {
                                countryLocation = address.countryName ?: countryLocation
                                if (homeLocation.isBlank()) {
                                    homeLocation = address.locality ?: address.subAdminArea ?: ""
                                }
                                Toast.makeText(context, "Location updated from GPS", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Unable to get current location coordinate", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    val datePickerDialog = remember {
        val cal = Calendar.getInstance()
        if (dob != null && dob != 0L) {
            cal.timeInMillis = dob!!
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, dayOfMonth)
                dob = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }


    val pageBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)


    Scaffold(
        containerColor = pageBg,
        topBar = {
            // Authentic iOS Top Navigation Bar with Frosted Glass
            Surface(
                color = if (isDark) Color(0xFF161618).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.88f),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel / Back Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onBack() }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.ChevronLeft,
                                contentDescription = "Cancel",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "Cancel",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF007AFF)
                            )
                        }


                        // Title & Subtitle Center
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Edit Profile",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMain
                            )
                            Text(
                                text = "Identity & Settings",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF8E8E93)
                            )
                        }


                        // Top Save Quick Action
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isSaving && isDisplayNameValid && isUsernameValid) {
                                    handleSave()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF007AFF)
                                )
                            } else {
                                Text(
                                    text = "Done",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDisplayNameValid && isUsernameValid) Color(0xFF007AFF) else Color(0xFF8E8E93).copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Floating Frosted Save Changes Bar
            Surface(
                color = if (isDark) Color(0xFF161618).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    ChromaPillButton(
                        onClick = { handleSave() },
                        variant = ChromaPillVariant.Blue,
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Saving Profile...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Save Changes",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. SIGNATURE HERO IDENTITY SHOWCASE CARD ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
                    .border(
                        0.5.dp,
                        if (isDark) Color.White.copy(alpha = 0.10f) else Color(0x18000000),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Cover Banner with specular vignette
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(138.dp)
                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                    ) {
                        val bannerRes = remember(bannerUrl) {
                            if (bannerUrl.isNotBlank() && bannerUrl.startsWith("preset:")) {
                                getBannerPresetDrawableRes(bannerUrl)
                            } else null
                        }


                        if (selectedBannerUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedBannerUri),
                                contentDescription = "Cover Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (bannerRes != null) {
                            Image(
                                painter = painterResource(id = bannerRes),
                                contentDescription = "Cover Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (bannerUrl.isNotBlank()) {
                            AsyncImage(
                                model = bannerUrl,
                                contentDescription = "Cover Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }


                        // Specular vignette overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                        )


                        // Banner upload indicator
                        if (isUploadingBanner) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Text("Updating Banner...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }


                        // "Edit Cover" glass pill button in top right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                                .clickable {
                                    selectedTabIndex = 1 // Switch to Media tab
                                }
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Lucide.Image, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Text("Edit Cover", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }


                    // User Identity Details Area (Clean surface below the banner - zero overlap on cover art!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Spacer accommodating the overlapping floating avatar
                        Spacer(Modifier.width(92.dp))

                        // User Identity Text Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = displayName.ifBlank { "Your Name" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = if (isDark) Color.White else Color(0xFF000000),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (profile.verified) {
                                    VerifiedBadge(
                                        size = 17.dp,
                                        tier = VerifiedBadgeTier.BLUE,
                                        animated = true
                                    )
                                }
                            }

                            Text(
                                text = "@${username.ifBlank { "handle" }}",
                                fontSize = 13.5.sp,
                                color = Color(0xFF8E8E93),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(3.dp))

                            // Role Chip (Apple pill button)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                                    .clickable {
                                        activePickerSheet = PickerSheetConfig(
                                            title = "Select Professional Role",
                                            options = listOf("Developer", "Designer", "Creator", "Photographer", "Video Editor", "Manager", "Student", "Artist", "Other"),
                                            selected = role,
                                            onSelect = { role = it }
                                        )
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.Briefcase, null, tint = Color(0xFF007AFF), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = role.ifBlank { "Developer" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF007AFF)
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Overlapping Avatar: perfectly anchored at banner bottom boundary (95dp from top)
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 95.dp)
                        .size(86.dp)
                        .clickable { showAvatarDialog = true }
                ) {
                    val presetRes = remember(avatarUrl) { getAvatarPresetDrawableRes(avatarUrl) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.28f))
                            .background(if (isDark) Color(0xFF1C1C1E) else Color.White, CircleShape)
                            .padding(3.5.dp)
                            .clip(CircleShape)
                    ) {
                        if (selectedAvatarUri != null) {
                            AsyncImage(selectedAvatarUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else if (presetRes != null) {
                            Image(painterResource(id = presetRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else if (avatarUrl.isNotBlank()) {
                            AsyncImage(avatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Image(painterResource(id = getDefaultAvatarDrawableRes(gender)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }

                        if (isUploadingAvatar) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            }
                        }
                    }

                    // Anchored Camera Action Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .shadow(4.dp, CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF007AFF), Color(0xFF0051D5)),
                                    start = Offset.Zero,
                                    end = Offset.Infinite
                                ),
                                CircleShape
                            )
                            .border(2.dp, if (isDark) Color(0xFF1C1C1E) else Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Camera,
                            contentDescription = "Change Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }


            // ── 2. APPLE SEGMENTED PILL TAB SWITCHER ─────────────────────────
            val tabs = listOf(
                "Profile" to Lucide.User,
                "Media" to Lucide.Image,
                "Interests" to Lucide.Tag,
                "Privacy" to Lucide.Lock
            )


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA))
                    .padding(3.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, (title, icon) ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .shadow(
                                    elevation = if (isSelected) 2.dp else 0.dp,
                                    shape = CircleShape,
                                    spotColor = Color.Black.copy(alpha = 0.15f)
                                )
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        if (isDark) Color(0xFF2C2C2E) else Color.White
                                    } else Color.Transparent
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTabIndex = index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF007AFF) else Color(0xFF8E8E93),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) colors.textMain else Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
            }


            // ── 3. DYNAMIC CONTENT PANELS ────────────────────────────────────
            when (selectedTabIndex) {
                0 -> {
                    // TAB 0: PROFILE (PUBLIC IDENTITY, PERSONAL INFO, LOCATION, SOCIALS)
                    
                    // A. Public Identity
                    AppleSettingsGroup(header = "Public Identity") {
                        AppleSettingsTextFieldRow(
                            icon = Lucide.User,
                            label = "Display Name",
                            value = displayName,
                            onValueChange = { displayName = it },
                            placeholder = "Enter your display name",
                            maxLength = 50,
                            isError = displayName.trim().isBlank(),
                            errorMessage = if (displayName.trim().isBlank()) "Name cannot be empty" else null
                        )


                        AppleSettingsTextFieldRow(
                            icon = Lucide.AtSign,
                            label = "Username Handle",
                            value = username,
                            onValueChange = { username = it.trim().lowercase() },
                            placeholder = "handle",
                            prefix = "@",
                            maxLength = 30,
                            isError = !isUsernameValid,
                            errorMessage = if (!isUsernameValid) "Use 3-30 letters, numbers, periods, or underscores" else null,
                            trailingContent = {
                                if (isUsernameValid) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = "Valid",
                                        tint = Color(0xFF34C759),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )


                        AppleSettingsTextFieldRow(
                            icon = Lucide.AlignLeft,
                            label = "Bio / Status",
                            value = bio,
                            onValueChange = { bio = it },
                            placeholder = "Tell the world what you create and explore...",
                            maxLength = 150,
                            singleLine = false,
                            maxLines = 4
                        )


                        AppleSettingNavRow(
                            icon = Lucide.Briefcase,
                            title = "Professional Role",
                            valueText = role,
                            onClick = {
                                activePickerSheet = PickerSheetConfig(
                                    title = "Select Professional Role",
                                    options = listOf("Developer", "Designer", "Creator", "Photographer", "Video Editor", "Manager", "Student", "Artist", "Other"),
                                    selected = role,
                                    onSelect = { role = it }
                                )
                            }
                        )
                    }


                    // B. Personal Information
                    AppleSettingsGroup(header = "Personal Information") {
                        val formattedDob = remember(dob) {
                            if (dob != null && dob != 0L) {
                                android.text.format.DateFormat.format("dd MMM yyyy", Date(dob!!)).toString()
                            } else {
                                "Set Date"
                            }
                        }


                        AppleSettingNavRow(
                            icon = Lucide.Calendar,
                            title = "Date of Birth",
                            valueText = formattedDob,
                            onClick = { datePickerDialog.show() }
                        )

                        AppleSettingDivider()


                        AppleSettingNavRow(
                            icon = Lucide.Users,
                            title = "Gender",
                            valueText = gender,
                            onClick = {
                                activePickerSheet = PickerSheetConfig(
                                    title = "Select Gender",
                                    options = listOf("Male", "Female", "Non-Binary", "Prefer not to say"),
                                    selected = gender,
                                    onSelect = { gender = it }
                                )
                            }
                        )
                    }


                    // C. Location & Workplace
                    AppleSettingsGroup(
                        header = "Location & Workplace",
                        footer = "Auto-detect retrieves your current city and country using high-accuracy GPS geocoding."
                    ) {
                        AppleSettingNavRow(
                            icon = Lucide.Building,
                            title = "Work Setup",
                            valueText = officeLocation,
                            onClick = {
                                activePickerSheet = PickerSheetConfig(
                                    title = "Select Work Setup",
                                    options = listOf("Remotely", "Hybrid", "On-site", "Studio"),
                                    selected = officeLocation,
                                    onSelect = { officeLocation = it }
                                )
                            }
                        )

                        AppleSettingDivider()


                        AppleSettingsTextFieldRow(
                            icon = Lucide.House,
                            label = "City / Region",
                            value = homeLocation,
                            onValueChange = { homeLocation = it },
                            placeholder = "e.g. San Francisco, California"
                        )


                        AppleSettingNavRow(
                            icon = Lucide.Globe,
                            title = "Country",
                            valueText = countryLocation,
                            onClick = {
                                activePickerSheet = PickerSheetConfig(
                                    title = "Select Country",
                                    options = listOf("United States", "India", "United Kingdom", "Canada", "Australia", "Germany", "France", "Japan", "Singapore", "Brazil", "Other"),
                                    selected = countryLocation,
                                    onSelect = { countryLocation = it }
                                )
                            }
                        )

                        AppleSettingDivider()


                        // 1-Tap Auto Detect Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Lucide.Navigation, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                                Text("Auto-detect Current Location", fontSize = 15.sp, color = Color(0xFF007AFF), fontWeight = FontWeight.Medium)
                            }
                            Icon(Lucide.ChevronRight, null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(15.dp))
                        }
                    }


                    // D. Social Handles & Links
                    AppleSettingsGroup(header = "Online Profiles & Socials") {
                        AppleSettingsTextFieldRow(
                            icon = Lucide.Github,
                            label = "GitHub",
                            value = github,
                            onValueChange = { github = it },
                            placeholder = "username",
                            prefix = "github.com/"
                        )


                        AppleSettingsTextFieldRow(
                            icon = Lucide.Instagram,
                            label = "Instagram",
                            value = instagram,
                            onValueChange = { instagram = it },
                            placeholder = "username",
                            prefix = "@"
                        )


                        AppleSettingsTextFieldRow(
                            icon = Lucide.Link,
                            label = "Portfolio / Website",
                            value = website,
                            onValueChange = { website = it },
                            placeholder = "https://yourportfolio.com"
                        )
                    }
                }


                1 -> {
                    // TAB 1: MEDIA (COVER BANNERS & 3D AVATARS)


                    // A. Cover Banner Management
                    AppleSettingsGroup(
                        header = "Cover Banner Presets",
                        footer = "Select from handcrafted dynamic wave graphics or upload a personalized cover image."
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Banner Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ChromaPillButton(
                                    onClick = { bannerGalleryLauncher.launch("image/*") },
                                    variant = ChromaPillVariant.Blue,
                                    height = 38.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Lucide.Upload, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Upload Custom", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                }


                                ChromaPillButton(
                                    onClick = {
                                        bannerUrl = ""
                                        selectedBannerUri = null
                                        Toast.makeText(context, "Cover banner removed", Toast.LENGTH_SHORT).show()
                                    },
                                    variant = ChromaPillVariant.Secondary,
                                    height = 38.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Lucide.Trash2, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Remove Banner", color = Color(0xFFFF3B30), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }


                            Text(
                                text = "VECTOR WAVE PRESETS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E8E93),
                                letterSpacing = 0.5.sp
                            )


                            val wavePresets = remember { AllBannerPresets.filter { it.id.startsWith("preset:wave_") } }
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(wavePresets) { preset ->
                                    val isSelected = bannerUrl == preset.id
                                    Box(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.5.dp,
                                                color = if (isSelected) Color(0xFF007AFF) else if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                bannerUrl = preset.id
                                                selectedBannerUri = null
                                            }
                                    ) {
                                        Image(
                                            painter = painterResource(id = preset.drawableRes),
                                            contentDescription = preset.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(18.dp)
                                                    .background(Color(0xFF007AFF), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Lucide.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                    // B. Profile Avatar Catalog
                    AppleSettingsGroup(
                        header = "Profile Avatar Catalog",
                        footer = "Choose a 3D vector character preset or take a quick photo with your camera."
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Avatar Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ChromaPillButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    variant = ChromaPillVariant.Blue,
                                    height = 38.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Lucide.Image, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("From Photos", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                }


                                ChromaPillButton(
                                    onClick = {
                                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            val tmpFile = File(context.cacheDir, "avatar_profile_tmp.jpg")
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tmpFile)
                                            cameraImageUri = uri
                                            cameraLauncher.launch(uri)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    variant = ChromaPillVariant.Secondary,
                                    height = 38.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Lucide.Camera, null, tint = colors.textMain, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Take Photo", color = colors.textMain, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }


                            Text(
                                text = "3D CHARACTER AVATARS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E8E93),
                                letterSpacing = 0.5.sp
                            )


                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(AllAvatarPresets) { preset ->
                                    val isSelected = avatarUrl == preset.id
                                    Box(
                                        modifier = Modifier
                                            .size(66.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 0.5.dp,
                                                color = if (isSelected) Color(0xFF007AFF) else if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                avatarUrl = preset.id
                                                selectedAvatarUri = null
                                            }
                                    ) {
                                        Image(
                                            painter = painterResource(id = preset.drawableRes),
                                            contentDescription = preset.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                2 -> {
                    // TAB 2: INTERESTS & CONTENT TAGS
                    AppleSettingsGroup(
                        header = "Content Categories & Tags",
                        footer = "Selected tags highlight your work in community feeds and help other creators connect with you."
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val availableCategories = listOf(
                                "Creativity & Art", "UI/UX Design", "Coding & Tech", "Vlogging",
                                "Photography", "Music & Audio", "Video Editing", "Writing",
                                "Gaming", "Health & Fitness", "Lifestyle", "Finance",
                                "Education", "3D Design", "AI & ML"
                            )


                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableCategories.forEach { tag ->
                                    val isSelected = contentTags.contains(tag)
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color(0xFF007AFF)
                                                else if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                                            )
                                            .border(
                                                width = 0.5.dp,
                                                color = if (isSelected) Color(0xFF007AFF) else if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                contentTags = if (isSelected) {
                                                    contentTags - tag
                                                } else {
                                                    contentTags + tag
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Lucide.Check else Lucide.Plus,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else Color(0xFF8E8E93),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = tag,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else colors.textMain
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                3 -> {
                    // TAB 3: PRIVACY & DATA VISIBILITY
                    AppleSettingsGroup(
                        header = "Account Privacy",
                        footer = "When your account is private, only followers you approve can view your full profile and creations."
                    ) {
                        AppleSettingToggleRow(
                            icon = Lucide.Lock,
                            title = "Private Account",
                            subtitle = "Require approval for new followers",
                            checked = isPrivateAccount,
                            onCheckedChange = { isPrivateAccount = it }
                        )
                    }


                    AppleSettingsGroup(
                        header = "Data Visibility Controls",
                        footer = "Choose whether other community members can see your personal information on your profile card."
                    ) {
                        val visibilityFields = listOf(
                            "dob" to ("Date of Birth" to Lucide.Calendar),
                            "gender" to ("Gender" to Lucide.Users),
                            "location" to ("Location" to Lucide.MapPin)
                        )


                        visibilityFields.forEachIndexed { idx, (key, pair) ->
                            val (title, icon) = pair
                            val currentVis = dataVisibility[key] ?: "public"


                            AppleSettingNavRow(
                                icon = icon,
                                title = title,
                                valueText = currentVis.replaceFirstChar { it.uppercase() },
                                onClick = {
                                    activePickerSheet = PickerSheetConfig(
                                        title = "$title Visibility",
                                        options = listOf("Public", "Followers Only", "Private"),
                                        selected = currentVis.replaceFirstChar { it.uppercase() },
                                        onSelect = { selectedText ->
                                            val mapped = selectedText.lowercase().replace(" ", "_").replace("followers_only", "followers")
                                            dataVisibility = dataVisibility.toMutableMap().apply { put(key, mapped) }
                                        }
                                    )
                                },
                            )

                            if (idx < visibilityFields.size - 1) {
                                AppleSettingDivider()
                            }
                        }
                    }
                }
            }
        }
    }


    // ── MODAL PICKER SHEET ───────────────────────────────────────────────────
    if (activePickerSheet != null) {
        val sheetConfig = activePickerSheet!!
        ModalBottomSheet(
            onDismissRequest = { activePickerSheet = null },
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = sheetConfig.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )


                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(sheetConfig.options) { option ->
                        val isSelected = sheetConfig.selected.equals(option, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sheetConfig.onSelect(option)
                                    activePickerSheet = null
                                }
                                .padding(vertical = 13.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF007AFF) else colors.textMain
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp)
                    }
                }
            }
        }
    }


    // ── AVATAR PICKER SHEET ──────────────────────────────────────────────────
    if (showAvatarDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarDialog = false },
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Profile Avatar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Text(
                    text = "Take a new photo or select from 3D vector characters",
                    fontSize = 12.5.sp,
                    color = Color(0xFF8E8E93)
                )


                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChromaPillButton(
                        onClick = {
                            galleryLauncher.launch("image/*")
                            showAvatarDialog = false
                        },
                        variant = ChromaPillVariant.Blue,
                        height = 42.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Lucide.Image, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }


                    ChromaPillButton(
                        onClick = {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                val tmpFile = File(context.cacheDir, "avatar_profile_tmp.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tmpFile)
                                cameraImageUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                            showAvatarDialog = false
                        },
                        variant = ChromaPillVariant.Secondary,
                        height = 42.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Lucide.Camera, null, tint = colors.textMain, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }


                Text(
                    text = "VECTOR CHARACTER PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E8E93),
                    letterSpacing = 0.5.sp
                )


                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(AllAvatarPresets) { preset ->
                        val isSelected = avatarUrl == preset.id
                        Box(
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.5.dp,
                                    color = if (isSelected) Color(0xFF007AFF) else if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    avatarUrl = preset.id
                                    selectedAvatarUri = null
                                    showAvatarDialog = false
                                }
                        ) {
                            Image(
                                painter = painterResource(id = preset.drawableRes),
                                contentDescription = preset.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}


data class PickerSheetConfig(
    val title: String,
    val options: List<String>,
    val selected: String,
    val onSelect: (String) -> Unit
)


/**
 * Authentic Apple Inset Settings Text Field Row with Inline Input
 */
@Composable
fun AppleSettingsTextFieldRow(
    icon: ImageVector? = null,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    prefix: String? = null,
    maxLength: Int? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    isError: Boolean = false,
    errorMessage: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    showDivider: Boolean = true
) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (singleLine) 10.dp else 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.padding(top = if (singleLine) 0.dp else 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }


            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isError) Color(0xFFFF3B30) else if (isDark) Color(0xFF8E8E93) else Color(0xFF6D6D72)
                    )
                    if (maxLength != null) {
                        Text(
                            text = "${value.length}/$maxLength",
                            fontSize = 11.sp,
                            color = if (value.length > maxLength) Color(0xFFFF3B30) else Color(0xFF8E8E93),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }


                Spacer(Modifier.height(3.dp))


                if (!singleLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.5f) else Color(0xFFF2F2F7))
                            .border(0.5.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = { input ->
                                if (maxLength == null || input.length <= maxLength) {
                                    onValueChange(input)
                                }
                            },
                            textStyle = TextStyle(
                                fontSize = 14.5.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isDark) Color.White else Color(0xFF000000)
                            ),
                            singleLine = false,
                            maxLines = maxLines,
                            cursorBrush = SolidColor(Color(0xFF007AFF)),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (value.isEmpty() && placeholder.isNotEmpty()) {
                                    Text(
                                        text = placeholder,
                                        fontSize = 14.5.sp,
                                        lineHeight = 20.sp,
                                        color = Color(0xFF8E8E93).copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (prefix != null) {
                            Text(
                                text = prefix,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF8E8E93),
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = { input ->
                                if (maxLength == null || input.length <= maxLength) {
                                    onValueChange(input)
                                }
                            },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isDark) Color.White else Color(0xFF000000)
                            ),
                            singleLine = true,
                            maxLines = 1,
                            cursorBrush = SolidColor(Color(0xFF007AFF)),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (value.isEmpty() && placeholder.isNotEmpty()) {
                                    Text(
                                        text = placeholder,
                                        fontSize = 15.sp,
                                        color = Color(0xFF8E8E93).copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (trailingContent != null) {
                            trailingContent()
                        }
                    }
                }


                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 11.sp,
                        color = Color(0xFFFF3B30)
                    )
                }
            }
        }
    }
    if (showDivider) {
        AppleSettingDivider(paddingStart = if (icon != null) 47.dp else 16.dp)
    }
}


suspend fun uploadBannerToSupabase(uid: String, imageBytes: ByteArray): String? {
    return try {
        val compressedBytes = com.chroma.studio.utils.ImageUtils.compressAvatarBytesForSupabase(imageBytes) ?: imageBytes
        val bucket = SupabaseClient.client.storage["banner"]
        val path = "$uid/banner.jpg"
        bucket.upload(path, compressedBytes, upsert = true)
        bucket.publicUrl(path)
    } catch (e: Exception) {
        android.util.Log.e("BannerUpload", "Upload failed", e)
        null
    }
}
