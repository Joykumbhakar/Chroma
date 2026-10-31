package com.chroma.studio.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.SavedAccount

/**
 * High-performance, animated entry point for Oscellion / Chroma Studio Onboarding.
 *
 * Flow: Welcome / SignIn -> Interests (2-5) -> Account Identity -> Set Password (2 boxes) -> Privacy Settings -> onFinished(state).
 */
@Composable
fun OnboardingFlow(
    onFinished: (OnboardingState) -> Unit,
    onGoogleSignIn: () -> Unit,
    onContinueSaved: () -> Unit,
    onLoginSuccess: () -> Unit = {},
    onSignInExisting: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    savedAccountName: String? = null,
    savedAccounts: List<SavedAccount> = emptyList(),
    googleLoading: Boolean = false,
    discoverRepo: DiscoverRepository? = null,
    modifier: Modifier = Modifier,
    vm: OnboardingViewModel = viewModel()
) {
    val s = vm.state
    BackHandler(enabled = s.step != ObStep.Welcome) { s.back() }

    AnimatedContent(
        targetState = s.step,
        modifier = modifier,
        label = "onboarding",
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            (slideInHorizontally(tween(380)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(380))) togetherWith
                (slideOutHorizontally(tween(380)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(220)))
        }
    ) { step ->
        when (step) {
            ObStep.Welcome -> WelcomeScreen(
                savedAccountName = savedAccountName,
                googleLoading = googleLoading,
                onContinueSaved = onContinueSaved,
                onGoogle = onGoogleSignIn,
                onCreateAccount = { s.step = ObStep.Interests },
                onSignInExisting = { s.step = ObStep.SignIn },
                onTerms = onOpenTerms
            )
            ObStep.SignIn -> SignInScreen(
                onBack = { s.step = ObStep.Welcome },
                onLoginSuccess = onLoginSuccess,
                onCreateAccount = { s.step = ObStep.Interests },
                onGoogleSignIn = onGoogleSignIn,
                onOpenTerms = onOpenTerms,
                savedAccounts = savedAccounts,
                googleLoading = googleLoading,
                discoverRepo = discoverRepo
            )
            ObStep.Interests -> InterestsScreen(
                state = s,
                onBack = s::back,
                onNext = s::next
            )
            ObStep.Account -> AccountScreen(
                state = s,
                onBack = s::back,
                onNext = s::next,
                discoverRepo = discoverRepo
            )
            ObStep.Password -> SetPasswordScreen(
                state = s,
                onBack = s::back,
                onNext = s::next
            )
            ObStep.Privacy -> PrivacyScreen(
                state = s,
                onBack = s::back,
                onLaunch = { onFinished(s) }
            )
        }
    }
}
