# Implementation Plan: Global Gallery Unification, Apple Compact UI & Bottom Sheets, OneUI Login/Onboarding, Community & Flash Enhancements, Profile Fixes & Solid Settings Icons

## 1. Global Unified Gallery with Exact Apple Borderless Pills (`OscellionGallery.kt`)
- **Single Global Gallery Engine**:
  - Consolidate all media picking flows across **Creator Studio (Posts & Flash)**, **Stories (`StoryCreationBottomSheet.kt`)**, **Chat (`CustomGalleryPicker.kt` / `CustomSharedGalleryModal.kt`)**, and **User Profile Avatar/Banner (`UserProfileScreen.kt` / `ProfileIdentityScreen.kt`)** into `OscellionGalleryLayout` (`OscellionGallery.kt`).
- **Exact Apple-Designed Borderless Pills & Compact UI**:
  - Replace bordered dropdowns/buttons in `OscellionGallery.kt` with **borderless Apple HIG segmented pills** (`Surface` with `0.dp` border, subtle translucent fill `Color(0x14767680)` / `Color(0x2E767680)`, and spring-animated sliding active pill indicator).
  - Compact top header and filter bar (All / Photos / Videos, Albums selector pill, Sort pill, and Grid column density pill) with smooth `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)` transitions.
  - Optimize thumbnail grid loading with explicit Coil `size(240, 240)`, hardware bitmap caching, and stable item keys (`key = { it.id }`) for 60fps scrolling.

## 2. Apple-Style Bottom Menu Sheet Sliders with Hairline (`hr`) Dividers
- **Unified Apple Bottom Sheet Component (`AppleBottomSheetContainer` & `AppleSheetActionGroup` in `AppleComponents.kt`)**:
  - Create reusable Apple-style bottom sheet primitives featuring:
    - Compact grabber pill (`36.dp × 4.dp`, rounded `50%`, centered at top with `8.dp` vertical padding).
    - Borderless grouped inset sections (`RoundedCornerShape(14.dp)`) separated by crisp `0.5.dp` hairline horizontal dividers (`HorizontalDivider(thickness = 0.5.dp)`).
    - Compact row height (`46.dp`–`50.dp`), clean typography, and smooth spring sheet animations.
- **Apply Across All Bottom Sheets & Sliders**:
  - Upgrade `BottomDrawer.kt`, `StoryCreationBottomSheet.kt`, `ShareStoryAudienceSheet.kt`, `OnlyFriendsManagementSheet.kt`, `RemixQuoteRepostSheet.kt`, `ChatWallpaperPickerSheet.kt`, `TransitionPickerSheet.kt`, and action/options sheets in `DiscoverScreen.kt`, `FlashFeedScreen.kt`, and `UserProfileScreen.kt`.

## 3. Discovery / Community Tab Performance & UI Enhancements (`DiscoverScreen.kt`)
- **Smooth Scrolling & Performance Optimization**:
  - Ensure every `LazyColumn` and `LazyRow` item uses stable keys and `contentType` declarations so Compose reuses compositions without jank during fast scrolling.
  - Memoize expensive date formatting, aspect ratio calculations, and filtered post lists with `remember` / `derivedStateOf`.
  - Downsample feed images in `PostMediaCarousel` and `DiscoverScreen` cards via Coil `Size` constraints and memory cache policies.
- **Compact Apple Pill Filter Bar**:
  - Upgrade `CommunityFeedTabBar` and `PlusCreatePopoverMenu` to use borderless Apple-style pills with smooth animated selection states and hairline dividers inside popover menus.

## 4. Flash Video Screen UI & Solid Fill Play/Pause Icon (`FlashFeedScreen.kt`)
- **Solid Filled Play/Pause Icon**:
  - Replace outlined/stroke Play and Pause vector icons in `FlashFeedScreen.kt` (center tap indicator and video transport controls) with custom **Solid Fill Play** and **Solid Fill Pause** vector icons (`ChromaIcons.PlayFilled` / `ChromaIcons.PauseFilled`).
- **Flash UI & Scrubber Refinements**:
  - Refine the right-side vertical action rail (Like, Comment, Remix, Share, Save) with compact spacing, crisp drop shadows, and smooth scale bounce on tap.
  - Upgrade the Flash Comments & Drafts bottom sheets to the new Apple-style bottom menu sheet layout with `0.5.dp` hairline dividers.

## 5. User Profile Logic & UI Fixes (`UserProfileScreen.kt` & `UserProfileHeaderComponents.kt`)
- **Accurate Content Tabs Sync**:
  - Fix filtering and synchronization across **Posts**, **Flash Videos**, **Saved Works**, and **Bookmarked/Saved** tabs so newly created posts, Flash videos, and saved items appear immediately with accurate badge counts.
- **Follow / Unfollow & Share Profile Logic**:
  - Fix optimistic UI state + Firestore sync for Follow/Unfollow (updating both `followersCount` and `followingCount` atomically and persisting state across navigation).
  - Fix **Share Profile** and **Profile Pass Card** actions with working deep-link/clipboard copy and Android system share sheet.
- **Edit Profile & Header Upgrade**:
  - Fix **Edit Profile** (display name, username validation, bio, role, website link, avatar & banner selection via `OscellionGalleryLayout` + cropper) so changes save reliably and reflect immediately in the header.
  - Upgrade Profile action menus and settings sheets to compact Apple bottom sheets with hairline dividers.

## 6. First-Time Login, Onboarding & Welcome Screen in Samsung OneUI Design System (`LoginScreen.kt` & `WelcomeBanner.kt`)
- **Samsung OneUI Design System Architecture**:
  - Redesign `LoginScreen.kt` following **Samsung OneUI 6/7** principles:
    - **Upper Viewing Area (~38% height)**: Generous vertical focal space featuring the genuine **Oscellion Studio** brand mark, large clean OneUI title typography (`28.sp–32.sp`, `FontWeight.Bold`), and concise, authentic subtitle copy describing Oscellion Studio's creative community, shaders, and Flash video tools.
    - **Lower Reachability Interaction Area (~62% height)**: Contained in OneUI signature `26.dp` rounded squircle surface cards (`OneUISectionCard`) with clean borderless filled inputs, clear validation feedback, and thumb-friendly `52.dp` pill buttons at the bottom.
- **Genuine Auth Flows + Guest Mode**:
  - Support **Email Sign In / Sign Up**, **Continue with Google**, **Saved Accounts quick-switch**, and a **"Continue as Guest (Explore Oscellion)"** action so first-time users can explore the app immediately without getting stuck.
  - Streamline the multi-step Sign-Up onboarding (Username → Full Name → Profile Details → Avatar via `OscellionGallery` → Creative Interests) with a OneUI step progress indicator and hairline dividers.

## 7. Solid Filled Icons Across All Settings Screens & Codebase Cleanup
- **Solid Filled Icons for All Settings Screens**:
  - Create/extend solid-fill vector icons in `AppleSettingsComponents.kt` / `ChromaIcons` and apply them across **all settings screens**:
    - `AccountCenterScreen.kt`
    - `NotificationsSettingsScreen.kt`
    - `PrivacyScreen.kt`
    - `SecurityScreen.kt`
    - `AppearanceSettingsScreen.kt` & `ProfileSettingsScreen.kt`
    - `ActivityHistoryScreen.kt`, `ArchivesScreen.kt`, `TrashScreen.kt`, and `PrivacyPolicyTermsScreen.kt`
- **App-Wide Consistency Audit & Cleanup**:
  - Fix mismatched branding strings (unifying on **Oscellion** / **Oscellion Studio**), remove dead/duplicate picker code, and verify clean compilation with `compile_applet`.
