package com.chroma.studio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chroma.studio.ui.screens.HomeScreen
import com.chroma.studio.ui.theme.ChromaTheme
import com.chroma.studio.viewmodel.HomeViewModel

import android.content.Context
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class HomeActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

        val isDarkModePref = prefs.getBoolean("is_dark_mode", false)
        val viewModePref = prefs.getString("view_mode", "GRID") ?: "GRID"
        val initialViewMode = try { com.chroma.studio.ui.screens.ViewMode.valueOf(viewModePref) } catch(e: Exception) { com.chroma.studio.ui.screens.ViewMode.GRID }
        vm.updateDarkMode(isDarkModePref)

        setContent {
            ChromaTheme(darkTheme = vm.isDarkMode) {
                // Refresh works list every time this screen is shown
                LaunchedEffect(Unit) { vm.refresh() }
                
                val navController = rememberNavController()
                
                NavHost(navController, startDestination = if (isFirstLaunch) "splash" else "home") {
                    composable("splash") {
                        com.chroma.studio.ui.screens.SplashScreen(
                            onAnimationFinished = {
                                prefs.edit().putBoolean("is_first_launch", false).apply()
                                navController.navigate("home") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("home") {
                        HomeScreen(
                            works = vm.works,
                            selectedWorkIds = vm.selectedWorkIds,
                            selectionMode = vm.selectionMode,
                            isDarkMode = vm.isDarkMode,
                            initialViewMode = initialViewMode,
                            onToggleDarkMode = {
                                val newDark = !vm.isDarkMode
                                vm.updateDarkMode(newDark)
                                prefs.edit().putBoolean("is_dark_mode", newDark).apply()
                            },
                            onViewModeChanged = { newMode ->
                                prefs.edit().putString("view_mode", newMode.name).apply()
                            },
                            onNewWork = {
                                startActivity(Intent(this@HomeActivity, MainActivity::class.java))
                            },
                            onOpenWork = { work ->
                                val intent = Intent(this@HomeActivity, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_WORK_ID, work.id)
                                    putExtra(MainActivity.EXTRA_WORK_NAME, work.name)
                                    putExtra(MainActivity.EXTRA_WORK_DESCRIPTION, work.description)
                                    putExtra(MainActivity.EXTRA_LAYERS_JSON, work.layersJson)
                                    putExtra(MainActivity.EXTRA_CANVAS_SHAPE, work.canvasShape)
                                }
                                startActivity(intent)
                            },
                            onRenameWork = { id, name, desc -> vm.rename(id, name, desc) },
                            onDuplicateWork = { work -> 
                                vm.duplicate(work)
                                com.chroma.studio.ui.components.ToastManager.showToast(
                                    message = "Work duplicated"
                                )
                            },
                            onToggleSelection = { id -> vm.toggleSelection(id) },
                            onUpdateSelection = { newSelection -> vm.updateSelection(newSelection) },
                            onClearSelection = { vm.clearSelection() },
                            onDeleteSelected = { 
                                vm.softDeleteSelected() 
                                com.chroma.studio.ui.components.ToastManager.showToast(
                                    message = "Items moved to trash",
                                    actionLabel = "View",
                                    onAction = { navController.navigate("trash") }
                                )
                            },
                            onSoftDelete = { id -> 
                                vm.softDelete(id) 
                                com.chroma.studio.ui.components.ToastManager.showToast(
                                    message = "Moved to trash",
                                    actionLabel = "View",
                                    onAction = { navController.navigate("trash") }
                                )
                            },
                            onEditHomeBackground = {
                                val intent = Intent(this@HomeActivity, MainActivity::class.java).apply {
                                    putExtra(MainActivity.EXTRA_IS_HOME_BACKGROUND, true)
                                    if (vm.customHomeBgLayersJson != null) {
                                        putExtra(MainActivity.EXTRA_LAYERS_JSON, vm.customHomeBgLayersJson)
                                        putExtra(MainActivity.EXTRA_CANVAS_SHAPE, vm.customHomeBgShape)
                                    }
                                }
                                startActivity(intent)
                            },
                            customHomeBgLayersJson = vm.customHomeBgLayersJson,
                            customHomeBgShape = vm.customHomeBgShape,
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToBackgrounds = { navController.navigate("backgrounds") },
                            onNavigateToTrash = { navController.navigate("trash") },
                            onSetAsBackground = { work ->
                                prefs.edit()
                                    .putString("custom_home_background_layers", work.layersJson)
                                    .putString("custom_home_background_shape", work.canvasShape)
                                    .apply()
                                vm.refresh()
                                com.chroma.studio.ui.components.ToastManager.showToast(message = "Home background updated")
                            },
                            repository = vm.repository
                        )
                    }
                    
                    composable("backgrounds") {
                        com.chroma.studio.ui.screens.BackgroundsScreen(
                            works = vm.works,
                            repository = vm.repository,
                            onBack = { navController.popBackStack() },
                            onSetAsBackground = { work ->
                                prefs.edit()
                                    .putString("custom_home_background_layers", work.layersJson)
                                    .putString("custom_home_background_shape", work.canvasShape)
                                    .apply()
                                vm.refresh()
                                com.chroma.studio.ui.components.ToastManager.showToast(message = "Home background updated")
                                navController.popBackStack()
                            }
                        )
                    }
                    
                    composable("settings") {
                        com.chroma.studio.ui.screens.SettingsScreen(
                            onResetHomeBackground = {
                                prefs.edit()
                                    .remove("custom_home_background_layers")
                                    .remove("custom_home_background_shape")
                                    .apply()
                                vm.refresh()
                                com.chroma.studio.ui.components.ToastManager.showToast(message = "Home background reset to default")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("trash") {
                        com.chroma.studio.ui.screens.TrashScreen(
                            deletedWorks = vm.deletedWorks,
                            selectedWorkIds = vm.trashSelectedWorkIds,
                            selectionMode = vm.trashSelectionMode,
                            onToggleSelection = { id -> vm.toggleTrashSelection(id) },
                            onClearSelection = { vm.clearTrashSelection() },
                            onDeleteSelected = { vm.permanentDeleteTrashSelected() },
                            onRestoreSelected = { vm.restoreTrashSelected() },
                            repository = vm.repository,
                            onRestore = { id -> vm.restore(id) },
                            onPermanentDelete = { id -> vm.permanentDelete(id) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                com.chroma.studio.ui.components.ChromaToastHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh when returning from editor (user may have saved a new/updated work)
        vm.refresh()
    }
}
