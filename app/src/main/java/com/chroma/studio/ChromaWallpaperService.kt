package com.chroma.studio

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.chroma.studio.data.WorkRepository
import com.chroma.studio.model.AnimStatus
import com.chroma.studio.model.AnimStyle
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.theme.OscellionTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChromaWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ChromaEngine()

    inner class ChromaEngine : Engine() {

        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var workRepo: WorkRepository? = null

        // Observed by Compose inside Presentation
        private val layersState: MutableState<List<GradientLayer>> = mutableStateOf(emptyList())
        private val timeMillisState: MutableLongState = mutableLongStateOf(0L)

        private var virtualDisplay: VirtualDisplay? = null
        private var presentation: ChromaPresentation? = null

        private var isVisible = false
        private var lastFrameNanos = 0L

        // Choreographer drives aurora/mesh animation time from the engine side
        private val choreographer: Choreographer by lazy { Choreographer.getInstance() }
        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!isVisible) return
                if (lastFrameNanos != 0L) {
                    val deltaNanos = frameTimeNanos - lastFrameNanos
                    val deltaMs = deltaNanos / 1_000_000L
                    timeMillisState.longValue += deltaMs
                }
                lastFrameNanos = frameTimeNanos
                choreographer.postFrameCallback(this)
            }
        }

        // ── Lifecycle ─────────────────────────────────────────────────────────

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            workRepo = WorkRepository(this@ChromaWallpaperService)
            surfaceHolder.setFormat(PixelFormat.RGBA_8888)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            teardown()
            setup(holder, width, height)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            if (visible) {
                lastFrameNanos = 0L
                choreographer.postFrameCallback(frameCallback)
                refreshLayers()
            } else {
                choreographer.removeFrameCallback(frameCallback)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isVisible = false
            choreographer.removeFrameCallback(frameCallback)
            teardown()
        }

        override fun onDestroy() {
            super.onDestroy()
            isVisible = false
            choreographer.removeFrameCallback(frameCallback)
            teardown()
            scope.cancel()
        }

        // ── VirtualDisplay + Presentation ─────────────────────────────────────

        private fun setup(holder: SurfaceHolder, width: Int, height: Int) {
            val context = this@ChromaWallpaperService
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

            try {
                val vd = dm.createVirtualDisplay(
                    "ChromaWallpaper",
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    holder.surface,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                )
                virtualDisplay = vd

                val pres = ChromaPresentation(
                    context,
                    vd.display,
                    layersState,
                    timeMillisState
                )
                pres.show()
                presentation = pres

                refreshLayers()

                if (isVisible) {
                    lastFrameNanos = 0L
                    choreographer.postFrameCallback(frameCallback)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChromaWallpaper", "VirtualDisplay setup failed", e)
            }
        }

        private fun teardown() {
            try { presentation?.dismiss() } catch (_: Exception) {}
            presentation = null
            try { virtualDisplay?.release() } catch (_: Exception) {}
            virtualDisplay = null
        }

        // ── Layer loading ─────────────────────────────────────────────────────

        private fun refreshLayers() {
            scope.launch {
                val work = withContext(Dispatchers.IO) { workRepo?.getLiveWallpaperWork() }
                layersState.value =
                    work?.let { workRepo?.deserializeLayers(it.layersJson) } ?: emptyList()
            }
        }
    }

    // ── Presentation hosting Compose content ──────────────────────────────────

    class ChromaPresentation(
        context: Context,
        display: android.view.Display,
        private val layersState: MutableState<List<GradientLayer>>,
        private val timeMillisState: MutableLongState
    ) : Presentation(context, display), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val _viewModelStore = ViewModelStore()

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = _viewModelStore

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            savedStateRegistryController.performRestore(savedInstanceState)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.BLACK))
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                // Ensure hardware acceleration for AGSL shaders (RuntimeShader)
                addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            }

            val composeView = ComposeView(context).apply {
                setContent {
                    val layers = layersState.value
                    // Read timeMillis from engine-driven Choreographer state
                    val timeMillis = timeMillisState.longValue
                    // Compute drift exactly as CanvasPreview does internally,
                    // but we pass it via animStatus=PLAYING and let CanvasPreview
                    // drive its OWN LaunchedEffect using withFrameNanos on the
                    // virtual display's choreographer.
                    OscellionTheme(darkTheme = true) {
                        if (layers.isNotEmpty()) {
                            CanvasPreview(
                                layers = layers,
                                shape = "full",
                                borderColor = ComposeColor.Transparent,
                                animStatus = AnimStatus.PLAYING,
                                animStyle = AnimStyle.DRIFT,
                                animSpeed = layers.firstOrNull()?.animSpeed ?: 50f,
                                animAmount = 50f,
                                isStatic = false,
                                timeMillisOverride = timeMillis
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ComposeColor.Black)
                            )
                        }
                    }
                }
            }

            setContentView(composeView)

            // Compose requires these owners to be attached to the view tree
            val decorView = window?.decorView
            decorView?.setViewTreeLifecycleOwner(this)
            decorView?.setViewTreeSavedStateRegistryOwner(this)
            decorView?.setViewTreeViewModelStoreOwner(this)
        }

        override fun onStart() {
            super.onStart()
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun onStop() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            super.onStop()
        }
    }
}
