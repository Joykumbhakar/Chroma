package com.chroma.studio.ui.onboarding

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.chroma.studio.data.DiscoverRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

enum class ObStep { Welcome, SignIn, Interests, Account, Password, Privacy }
enum class Gender(val label: String) { Male("Male"), Female("Female"), NonBinary("Non-binary"), Private("Private") }
enum class Aspect(val label: String) { Square("1:1"), Classic("4:3"), Wide("16:9") }
enum class UsernameStatus { TooShort, Checking, Taken, Free }

/** Same five rules as checkPasswordStrength() in LoginScreen.kt. */
data class PasswordRules(
    val length: Boolean,
    val upper: Boolean,
    val lower: Boolean,
    val digit: Boolean,
    val special: Boolean
) {
    val metCount get() = listOf(length, upper, lower, digit, special).count { it }
    val allMet get() = metCount == 5
}

fun evaluatePassword(pw: String) = PasswordRules(
    length = pw.length in 8..24,
    upper = pw.any { it.isUpperCase() },
    lower = pw.any { it.isLowerCase() },
    digit = pw.any { it.isDigit() },
    special = pw.any { !it.isLetterOrDigit() }
)

private val reservedHandles = setOf("admin", "oscellion", "chroma", "chromastudio", "support", "help", "root")

/** Latest birth date (UTC millis) that is still 13+. */
fun latestAllowedBirthMillis(): Long =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { add(Calendar.YEAR, -13) }.timeInMillis

class OnboardingState {
    var step by mutableStateOf(ObStep.Welcome)

    val interests = mutableStateListOf<String>()

    var username by mutableStateOf("")
    var usernameStatus by mutableStateOf(UsernameStatus.TooShort)
    var usernameSuggestions by mutableStateOf<List<String>>(emptyList())
    private var usernameCheckJob: Job? = null

    var fullName by mutableStateOf("")
    var bio by mutableStateOf("")
    var dobMillis by mutableStateOf<Long?>(null)
    var gender by mutableStateOf(Gender.Private)
    var avatarUri by mutableStateOf<Uri?>(null)
    var avatarPresetId by mutableStateOf<String?>(null)

    var password by mutableStateOf("")
    var confirm by mutableStateOf("")

    // Social and privacy behaviors matching reference screens
    var startsConversations by mutableStateOf(true)
    var respondsToOthers by mutableStateOf(true)
    var joinsDebates by mutableStateOf(true)
    var usesHumor by mutableStateOf(true)
    var followsTrends by mutableStateOf(false)
    var postingFrequency by mutableStateOf("Active")

    var allowMentions by mutableStateOf(true)
    var e2ee by mutableStateOf(true)
    var biometric by mutableStateOf(true)
    var showOnline by mutableStateOf(true)
    var privateAccount by mutableStateOf(false)
    var aspect by mutableStateOf(Aspect.Classic)

    val accountValid get() =
        usernameStatus == UsernameStatus.Free && fullName.isNotBlank() &&
            (dobMillis?.let { it <= latestAllowedBirthMillis() } ?: false)

    val passwordValid get() = evaluatePassword(password).allMet && confirm == password

    fun updateUsername(newVal: String, discoverRepo: DiscoverRepository?, scope: CoroutineScope) {
        val clean = newVal.filter { it.code < 128 && (it.isLetterOrDigit() || it == '_') }.lowercase().take(30)
        username = clean

        usernameCheckJob?.cancel()
        if (clean.length < 3) {
            usernameStatus = UsernameStatus.TooShort
            usernameSuggestions = emptyList()
            return
        }
        if (clean in reservedHandles) {
            usernameStatus = UsernameStatus.Taken
            usernameSuggestions = listOf("${clean}_studio", "${clean}_pro", "${clean}art")
            return
        }

        if (discoverRepo == null) {
            usernameStatus = UsernameStatus.Free
            return
        }

        usernameStatus = UsernameStatus.Checking
        usernameCheckJob = scope.launch {
            delay(350)
            try {
                val isAvailable = discoverRepo.checkUsernameAvailable(clean)
                if (isAvailable) {
                    usernameStatus = UsernameStatus.Free
                    usernameSuggestions = emptyList()
                } else {
                    usernameStatus = UsernameStatus.Taken
                    usernameSuggestions = discoverRepo.suggestAvailableUsernames(clean)
                }
            } catch (_: Exception) {
                // If offline or network issue, fallback to free for responsive UI
                usernameStatus = UsernameStatus.Free
            }
        }
    }

    fun ensureAccountDefaults() {
        if (fullName.isBlank()) fullName = "Creator Studio"
        if (username.isBlank()) username = "creator_${(1000..9999).random()}"
        if (usernameStatus != UsernameStatus.Free) usernameStatus = UsernameStatus.Free
        if (dobMillis == null) {
            dobMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { set(2000, 0, 1) }.timeInMillis
        }
    }

    fun next() {
        step = when (step) {
            ObStep.Welcome -> ObStep.Interests
            ObStep.SignIn -> ObStep.Welcome
            ObStep.Interests -> ObStep.Account
            ObStep.Account -> ObStep.Password
            ObStep.Password -> ObStep.Privacy
            ObStep.Privacy -> ObStep.Privacy
        }
    }

    fun back() {
        step = when (step) {
            ObStep.Welcome -> ObStep.Welcome
            ObStep.SignIn -> ObStep.Welcome
            ObStep.Interests -> ObStep.Welcome
            ObStep.Account -> ObStep.Interests
            ObStep.Password -> ObStep.Account
            ObStep.Privacy -> ObStep.Password
        }
    }
}

/** Survives rotation. The password lives in memory only. */
class OnboardingViewModel : ViewModel() {
    val state = OnboardingState()
}
