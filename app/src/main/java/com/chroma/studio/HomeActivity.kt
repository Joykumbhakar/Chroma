package com.chroma.studio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.LaunchedEffect
import com.chroma.studio.ui.screens.HomeScreen
import com.chroma.studio.ui.theme.ChromaTheme
import com.chroma.studio.viewmodel.HomeViewModel

class HomeActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install BEFORE super.onCreate — this is the Android 12 API requirement
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            ChromaTheme(darkTheme = false) {
                // Refresh works list every time this screen is shown
                LaunchedEffect(Unit) { vm.refresh() }

                HomeScreen(
                    works = vm.works,
                    onNewWork = {
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                    onOpenWork = { work ->
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_WORK_ID, work.id)
                            putExtra(MainActivity.EXTRA_WORK_NAME, work.name)
                            putExtra(MainActivity.EXTRA_WORK_DESCRIPTION, work.description)
                            putExtra(MainActivity.EXTRA_LAYERS_JSON, work.layersJson)
                            putExtra(MainActivity.EXTRA_CANVAS_SHAPE, work.canvasShape)
                        }
                        startActivity(intent)
                    },
                    onDeleteWork = { id -> vm.delete(id) },
                    onRenameWork = { id, name, desc -> vm.rename(id, name, desc) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh when returning from editor (user may have saved a new/updated work)
        vm.refresh()
    }
}
