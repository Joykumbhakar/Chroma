package com.chroma.studio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

        val isDarkModePref = prefs.getBoolean("is_dark_mode", false)
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
                            onToggleDarkMode = {
                                val newDark = !vm.isDarkMode
                                vm.updateDarkMode(newDark)
                                prefs.edit().putBoolean("is_dark_mode", newDark).apply()
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
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToTrash = { navController.navigate("trash") },
                            repository = vm.repository
                        )
                    }
                    
                    composable("settings") {
                        com.chroma.studio.ui.screens.SettingsScreen(
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
