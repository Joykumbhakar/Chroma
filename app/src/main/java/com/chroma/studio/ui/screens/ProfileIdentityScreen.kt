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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.chroma.studio.R
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
    works: List<DiscoverPost>,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var displayName by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var avatarUrl by remember { mutableStateOf(profile.avatarUrl) }
    var bio by remember { mutableStateOf(profile.bio) }
    var role by remember { mutableStateOf(profile.role) }
    var dob by remember { mutableStateOf(profile.dateOfBirthAsLong ?: profile.dob) }
    var gender by remember { mutableStateOf(profile.gender) }
    
    var contentTags by remember { mutableStateOf(profile.contentTags.toList()) }
    var dataVisibility by remember { mutableStateOf(profile.dataVisibility.toMutableMap()) }

    var officeLocation by remember { mutableStateOf(profile.officeLocation ?: "") }
    var homeLocation by remember { mutableStateOf(profile.homeLocation ?: "") }
    var countryLocation by remember { mutableStateOf(profile.countryLocation ?: "") }

    var github by remember { mutableStateOf(profile.socialLinks["github"] ?: "") }
    var instagram by remember { mutableStateOf(profile.socialLinks["instagram"] ?: "") }
    var website by remember { mutableStateOf(profile.socialLinks["website"] ?: "") }

    var selectedBannerWorkId by remember { mutableStateOf(profile.bannerWorkId) }
    var bannerUrl by remember { mutableStateOf(profile.bannerUrl?.takeIf { it.isNotBlank() } ?: "preset:wave_6") }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Upload states
    var selectedBannerUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    fun handleSave() {
        val newSocialLinks = mutableMapOf<String, String>()
        if (github.isNotBlank()) newSocialLinks["github"] = github.trim()
        if (instagram.isNotBlank()) newSocialLinks["instagram"] = instagram.trim()
        if (website.isNotBlank()) newSocialLinks["website"] = website.trim()
        
        val updated = profile.copy(
            displayName = displayName.trim(),
            username = username.trim(),
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
            officeLocation = officeLocation.trim(),
            homeLocation = homeLocation.trim(),
            countryLocation = countryLocation.trim()
        )
        com.chroma.studio.data.SavedAccountManager.saveFromProfile(context, updated, com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email)
        onSave(updated)
        onBack()
    }

    val bannerGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedBannerUri = uri
            coroutineScope.launch {
                isUploadingBanner = true
                val bytes = com.chroma.studio.utils.ImageUtils.compressBannerForSupabase(context, uri)
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (bytes != null && uid != null) {
                    val url = uploadBannerToSupabase(uid, bytes)
                    if (url != null) bannerUrl = url
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
                    if (url != null) avatarUrl = url
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
                    if (url != null) avatarUrl = url
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
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
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
                                    countryLocation = address.countryName ?: ""
                                    if (homeLocation.isBlank()) homeLocation = address.locality ?: address.subAdminArea ?: ""
                                }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val address = addresses?.firstOrNull()
                            if (address != null) {
                                countryLocation = address.countryName ?: ""
                                if (homeLocation.isBlank()) homeLocation = address.locality ?: address.subAdminArea ?: ""
                            }
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            dob = cal.timeInMillis
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    var editSheetState by remember { mutableStateOf<EditSheetData?>(null) }
    
    // Bottom sheet for editing field values
    if (editSheetState != null) {
        val sheetData = editSheetState!!
        ModalBottomSheet(
            onDismissRequest = { editSheetState = null },
            containerColor = colors.bg,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 36.dp)) {
                Text("Edit ${sheetData.title}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textMain)
                Spacer(Modifier.height(16.dp))
                
                if (sheetData.options != null) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        items(sheetData.options!!) { option ->
                            val isSelected = sheetData.currentValue == option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        sheetData.onSave(option)
                                        editSheetState = null
                                    }
                                    .padding(vertical = 14.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(option, color = if (isSelected) Color(0xFF007AFF) else colors.textMain, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (isSelected) {
                                    Icon(Lucide.Check, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider(color = colors.glassBorder)
                        }
                    }
                } else {
                    var textValue by remember(sheetData) { mutableStateOf(sheetData.currentValue) }
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = { Text(sheetData.title) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(20.dp))
                    ChromaPillButton(
                        onClick = { 
                            sheetData.onSave(textValue)
                            editSheetState = null
                        },
                        variant = ChromaPillVariant.Blue,
                        height = 46.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(if(isDark) Color(0xFF0F0F11) else Color(0xFFF6F7FB))) {
        
        // Clean Subtle Apple Gradient Header (Light/Dark adaptive)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF007AFF),
                            Color(0xFF007AFF).copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 140.dp)
        ) {
            Spacer(Modifier.height(statusBarTop + 54.dp))
            
            // Clean Apple-style Profile Header Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Avatar with perfectly anchored camera action badge
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clickable { showAvatarDialog = true }
                ) {
                    val presetRes = remember(avatarUrl) { getAvatarPresetDrawableRes(avatarUrl) }
                    val displayUri = selectedAvatarUri

                    // Main Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.TopStart)
                            .shadow(6.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.18f))
                            .background(if (isDark) Color(0xFF1C1C1E) else Color.White, CircleShape)
                            .padding(2.5.dp)
                            .clip(CircleShape)
                    ) {
                        if (displayUri != null) {
                            AsyncImage(displayUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else if (presetRes != null) {
                            Image(painterResource(id = presetRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else if (avatarUrl.isNotBlank()) {
                            AsyncImage(avatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Image(painterResource(id = getDefaultAvatarDrawableRes(gender)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }

                    // Anchored Camera Action Badge overlapping bottom-right
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .shadow(3.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.22f))
                            .background(if (isDark) Color(0xFF1C1C1E) else Color.White, CircleShape)
                            .padding(2.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF006FFF), Color(0xFF111C42)),
                                    start = Offset.Zero,
                                    end = Offset.Infinite
                                ),
                                CircleShape
                            ),
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
                
                Spacer(Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        displayName.trim().ifEmpty { "Mr. Joy" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = colors.textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Username", username.ifBlank { "joykumbhakar" })
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Handle copied!", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Text(
                            "@${username.ifBlank { "joykumbhakar" }}",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(Lucide.Copy, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(11.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { 
                            editSheetState = EditSheetData("Role", role, listOf("Developer", "Designer", "Manager", "Creator", "Student", "Other")) { role = it } 
                        }
                    ) {
                        Icon(Lucide.Code, null, tint = Color(0xFF007AFF), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (role.isNotBlank()) role else "Developer", color = Color(0xFF007AFF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Horizontal Tab Bar (Apple Styled)
            Spacer(Modifier.height(18.dp))
            val tabs = listOf(
                "Profile" to Lucide.User,
                "Media" to Lucide.Image,
                "Content" to Lucide.Zap,
                "Highlights" to Lucide.Heart,
                "Settings" to Lucide.Settings
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabs.forEachIndexed { index, (title, icon) ->
                    val selected = selectedTabIndex == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTabIndex = index }
                    ) {
                        Icon(
                            icon,
                            null,
                            tint = if (selected) Color(0xFF007AFF) else Color(0xFF8E8E93),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            title,
                            color = if (selected) Color(0xFF007AFF) else Color(0xFF8E8E93),
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .height(2.5.dp)
                                    .fillMaxWidth(0.65f)
                                    .background(Color(0xFF007AFF), RoundedCornerShape(2.dp))
                            )
                        } else {
                            Spacer(Modifier.height(2.5.dp))
                        }
                    }
                }
            }
            HorizontalDivider(color = colors.glassBorder)

            // Dynamic Cards Content by Selected Tab
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                
                when (selectedTabIndex) {
                    0 -> {
                        // Profile Tab
                        // 1. About You
                        DesignCard(icon = Lucide.User, title = "About You", subtitle = "Tell others about yourself.") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFF9F9FB))
                                    .border(0.75.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                                    .clickable { editSheetState = EditSheetData("Bio", bio) { bio = it } }
                            ) {
                                Column {
                                    Text(
                                        if (bio.isNotBlank()) bio else "Tell the world about yourself and what you create...",
                                        color = if (bio.isNotBlank()) colors.textMain else Color(0xFF8E8E93),
                                        fontSize = 13.5.sp,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${bio.length}/150",
                                        color = Color(0xFF8E8E93),
                                        fontSize = 10.5.sp,
                                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                                    )
                                }
                            }
                        }

                        // 2. Basic Information
                        DesignCard(icon = Lucide.FileText, title = "Basic Information", subtitle = "Your public identity details.") {
                            DesignCardRow(Lucide.User, "Display Name", displayName.ifBlank { "Set Name" }, { editSheetState = EditSheetData("Display Name", displayName) { displayName = it } })
                            DesignCardRow(Lucide.AtSign, "Username", username.ifBlank { "Set Handle" }, { editSheetState = EditSheetData("Username", username) { username = it } })
                            val formattedDob = remember(dob) {
                                if (dob != null && dob != 0L) {
                                    android.text.format.DateFormat.format("dd MMM yyyy", Date(dob!!)).toString()
                                } else {
                                    "Set Date"
                                }
                            }
                            DesignCardRow(Lucide.Calendar, "Date of Birth", formattedDob, { datePickerDialog.show() })
                            DesignCardRow(Lucide.Users, "Gender", gender.ifBlank { "Prefer not to say" }, { editSheetState = EditSheetData("Gender", gender, listOf("Male", "Female", "Non-Binary", "Prefer not to say")) { gender = it } })
                            DesignCardRow(Lucide.Briefcase, "Role", role.ifBlank { "Developer" }, { editSheetState = EditSheetData("Role", role, listOf("Developer", "Designer", "Manager", "Creator", "Student", "Other")) { role = it } }, showDivider = false)
                        }

                        // 3. Location
                        DesignCard(
                            icon = Lucide.MapPin,
                            title = "Location",
                            subtitle = "Where you are based.",
                            action = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFF007AFF).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                        .clickable {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Lucide.Navigation, null, tint = Color(0xFF007AFF), modifier = Modifier.size(11.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Auto-detect", color = Color(0xFF007AFF), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        ) {
                            DesignCardRow(Lucide.Building, "Office Location", officeLocation.ifBlank { "Remotely" }, { editSheetState = EditSheetData("Office Location", officeLocation, listOf("Remotely", "On-site", "Hybrid")) { officeLocation = it } })
                            DesignCardRow(Icons.Default.Home, "City / Region", homeLocation.ifBlank { "Set City" }, { editSheetState = EditSheetData("Home Location", homeLocation) { homeLocation = it } })
                            DesignCardRow(Lucide.Globe, "Country", countryLocation.ifBlank { "India" }, { editSheetState = EditSheetData("Country", countryLocation, listOf("India", "United States", "United Kingdom", "Canada", "Australia", "Germany", "Other")) { countryLocation = it } }, showDivider = false)
                        }

                        // 4. Social Handles
                        DesignCard(icon = Lucide.Link, title = "Social Handles", subtitle = "Your online presence & portfolios.") {
                            DesignCardRow(Lucide.Github, "GitHub", github.ifBlank { "Not linked" }, { editSheetState = EditSheetData("GitHub", github) { github = it } })
                            DesignCardRow(Lucide.Instagram, "Instagram", instagram.ifBlank { "Not linked" }, { editSheetState = EditSheetData("Instagram", instagram) { instagram = it } })
                            DesignCardRow(Lucide.Globe, "Website", website.ifBlank { "Not linked" }, { editSheetState = EditSheetData("Website", website) { website = it } }, showDivider = false)
                        }
                    }
                    1 -> {
                        // Media Tab: Banner and Avatars
                        DesignCard(icon = Lucide.Image, title = "Profile Banner", subtitle = "Choose a vector wave preset or upload a custom image.") {
                            val displayBannerRes = if (bannerUrl.isNotBlank() && bannerUrl.startsWith("preset:")) {
                                getBannerPresetDrawableRes(bannerUrl)
                            } else null

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(0.75.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                            ) {
                                if (selectedBannerUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(selectedBannerUri),
                                        contentDescription = "Banner Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (displayBannerRes != null) {
                                    Image(
                                        painter = painterResource(id = displayBannerRes),
                                        contentDescription = "Banner Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (bannerUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = bannerUrl,
                                        contentDescription = "Banner Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(Color(0xFF007AFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No Banner Selected", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Remove Banner", color = Color(0xFFFF3B30), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { bannerUrl = ""; selectedBannerUri = null })
                                Text("Upload Custom", color = Color(0xFF007AFF), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { bannerGalleryLauncher.launch("image/*") })
                            }

                            Spacer(Modifier.height(14.dp))
                            val applePresets = AllBannerPresets.filter { !it.id.startsWith("preset:wave_") }
                            if (applePresets.isNotEmpty()) {
                                Text("Apple & Modern Presets", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(applePresets) { preset ->
                                        val isSelected = bannerUrl == preset.id
                                        Image(
                                            painter = painterResource(id = preset.drawableRes),
                                            contentDescription = preset.name,
                                            modifier = Modifier
                                                .width(120.dp).height(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.5.dp,
                                                    color = if (isSelected) Color(0xFF007AFF) else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { 
                                                    bannerUrl = preset.id
                                                    selectedBannerUri = null 
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                            }

                            Text("Vector Wave Presets", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            val wavePresets = AllBannerPresets.filter { it.id.startsWith("preset:wave_") }
                            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(wavePresets) { preset ->
                                    val isSelected = bannerUrl == preset.id
                                    Image(
                                        painter = painterResource(id = preset.drawableRes),
                                        contentDescription = preset.name,
                                        modifier = Modifier
                                            .width(120.dp).height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.5.dp,
                                                color = if (isSelected) Color(0xFF007AFF) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { 
                                                bannerUrl = preset.id
                                                selectedBannerUri = null 
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Content Tab: Categories and Tags
                        DesignCard(
                            icon = Lucide.Tag,
                            title = "Content Categories",
                            subtitle = "Select tags that best describe your content."
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                contentTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF007AFF))
                                            .clickable { contentTags = contentTags - tag }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Lucide.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(tag, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                listOf("Creativity & Art", "Application designing", "UI/UX", "Vlogging", "Photography", "Music", "Writing", "Education", "Gaming", "Health & Fitness", "Lifestyle", "Finance").forEach { tag ->
                                    if (!contentTags.contains(tag)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp))
                                                .clickable { contentTags = contentTags + tag }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(tag, color = colors.textMain, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Highlights Tab: Featured Works
                        DesignCard(icon = Lucide.Heart, title = "Featured Highlights", subtitle = "Works pinned to the top of your profile.") {
                            if (works.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No creations available to pin", color = Color(0xFF8E8E93), fontSize = 12.5.sp)
                                }
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(works) { work ->
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                                        ) {
                                            if (!work.imageUrl.isNullOrBlank()) {
                                                AsyncImage(model = work.imageUrl, contentDescription = work.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            } else {
                                                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF007AFF).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                                    Icon(Lucide.Sparkles, null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4 -> {
                        // Settings Tab: Privacy & Visibility
                        DesignCard(icon = Lucide.Lock, title = "Privacy & Data Visibility", subtitle = "Control who sees your personal information.") {
                            listOf("dob" to "Date of Birth", "gender" to "Gender", "location" to "Location").forEach { (key, title) ->
                                val vis = dataVisibility[key] ?: "public"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(title, color = colors.textMain, fontSize = 14.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                                            .clickable {
                                                val nextVis = when (vis) {
                                                    "public" -> "followers"
                                                    "followers" -> "private"
                                                    else -> "public"
                                                }
                                                dataVisibility = dataVisibility.toMutableMap().apply { put(key, nextVis) }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(vis.replaceFirstChar { it.uppercase() }, color = Color(0xFF007AFF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                HorizontalDivider(color = colors.glassBorder)
                            }
                        }
                    }
                }
            }
        }
        
        // Clean Minimal Apple-style Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarTop + 6.dp, start = 14.dp, end = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Lucide.ArrowLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
                    Text("Identity & Information", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1)
                }
            }
            // Clean minimal Preview button
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Lucide.Eye, contentDescription = "Preview", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Fixed Floating Bottom Save Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            if (isDark) Color(0xFF0F0F11).copy(alpha = 0.95f) else Color(0xFFF6F7FB).copy(alpha = 0.95f),
                            if (isDark) Color(0xFF0F0F11) else Color(0xFFF6F7FB)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ChromaPillButton(
                onClick = { handleSave() },
                variant = ChromaPillVariant.Blue,
                height = 48.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Lucide.Save, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
    
    // Avatar Preset & Upload Sheet
    if (showAvatarDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarDialog = false },
            containerColor = colors.bg,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 36.dp)) {
                Text("Choose Profile Avatar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                Text("Pick from 3D vector avatars or upload your own", fontSize = 12.sp, color = Color(0xFF8E8E93))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChromaPillButton(
                        onClick = { galleryLauncher.launch("image/*"); showAvatarDialog = false },
                        variant = ChromaPillVariant.Blue,
                        height = 40.dp,
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
                        variant = ChromaPillVariant.DarkGlass,
                        height = 40.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Lucide.Camera, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Vector Avatar Catalog", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(AllAvatarPresets) { preset ->
                        val isSelected = avatarUrl == preset.id
                        Image(
                            painter = painterResource(id = preset.drawableRes),
                            contentDescription = preset.name,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.5.dp,
                                    color = if (isSelected) Color(0xFF007AFF) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    avatarUrl = preset.id
                                    selectedAvatarUri = null
                                    showAvatarDialog = false
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

data class EditSheetData(val title: String, val currentValue: String, val options: List<String>? = null, val onSave: (String) -> Unit)

@Composable
fun ChipItem(icon: ImageVector?, text: String, onClick: () -> Unit = {}) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
            .border(0.75.dp, colors.glassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Color(0xFF007AFF), modifier = Modifier.size(12.5.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(text, color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DesignCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1C1C1E) else Color.White
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.75.dp, colors.glassBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(subtitle, color = Color(0xFF8E8E93), fontSize = 11.5.sp)
                }
                if (action != null) action()
            }
            content()
        }
    }
}

@Composable
fun DesignCardRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    val colors = LocalOscellionColors.current
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color(0xFF007AFF), modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(12.dp))
            Text(
                value,
                color = Color(0xFF8E8E93),
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Spacer(Modifier.width(6.dp))
            Icon(Lucide.ChevronRight, null, tint = Color(0xFF8E8E93), modifier = Modifier.size(14.dp))
        }
        if (showDivider) {
            HorizontalDivider(color = colors.glassBorder)
        }
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
