@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.chroma.studio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.chroma.studio.R
import com.chroma.studio.chat.sound.SoundManager

/**
 * Android Home Screen Widget for Oscellion Music Player.
 * Provides interactive playback controls, turntable vinyl artwork,
 * track details, and instant launch into the Studio Audio Trimmer & Player.
 */
class OscellionMusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)
                val newState = !isPlaying
                prefs.edit().putBoolean(KEY_IS_PLAYING, newState).apply()

                // Trigger sound reaction
                SoundManager.playReaction()

                // Update all widgets
                updateAllWidgets(context)
            }
            ACTION_NEXT -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val trackIndex = (prefs.getInt(KEY_TRACK_INDEX, 0) + 1) % PRESET_TRACKS.size
                val nextTrack = PRESET_TRACKS[trackIndex]
                prefs.edit()
                    .putInt(KEY_TRACK_INDEX, trackIndex)
                    .putString(KEY_TRACK_TITLE, nextTrack.first)
                    .putString(KEY_TRACK_ARTIST, nextTrack.second)
                    .putBoolean(KEY_IS_PLAYING, true)
                    .apply()

                SoundManager.playReaction()
                updateAllWidgets(context)
            }
            ACTION_PREV -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val currentIndex = prefs.getInt(KEY_TRACK_INDEX, 0)
                val trackIndex = if (currentIndex <= 0) PRESET_TRACKS.size - 1 else currentIndex - 1
                val prevTrack = PRESET_TRACKS[trackIndex]
                prefs.edit()
                    .putInt(KEY_TRACK_INDEX, trackIndex)
                    .putString(KEY_TRACK_TITLE, prevTrack.first)
                    .putString(KEY_TRACK_ARTIST, prevTrack.second)
                    .putBoolean(KEY_IS_PLAYING, true)
                    .apply()

                SoundManager.playReaction()
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val PREFS_NAME = "oscellion_music_widget_prefs"
        const val KEY_TRACK_TITLE = "widget_track_title"
        const val KEY_TRACK_ARTIST = "widget_track_artist"
        const val KEY_IS_PLAYING = "widget_is_playing"
        const val KEY_TRACK_INDEX = "widget_track_index"

        const val ACTION_PLAY_PAUSE = "com.chroma.studio.WIDGET_MUSIC_PLAY_PAUSE"
        const val ACTION_NEXT = "com.chroma.studio.WIDGET_MUSIC_NEXT"
        const val ACTION_PREV = "com.chroma.studio.WIDGET_MUSIC_PREV"

        private val PRESET_TRACKS = listOf(
            "Chroma Studio Beats" to "Oscellion Creative Soundscape • 432Hz",
            "Midnight Frequency" to "Deep Ambient Synth • Master 24-bit",
            "Neon Horizon Groove" to "Cyberpunk Lo-Fi • Oscellion Audio",
            "Acoustic Reverie" to "Studio Vinyl Warmth • Chroma Lab"
        )

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val title = prefs.getString(KEY_TRACK_TITLE, PRESET_TRACKS[0].first) ?: PRESET_TRACKS[0].first
            val artist = prefs.getString(KEY_TRACK_ARTIST, PRESET_TRACKS[0].second) ?: PRESET_TRACKS[0].second
            val isPlaying = prefs.getBoolean(KEY_IS_PLAYING, false)

            val views = RemoteViews(context.packageName, R.layout.widget_music_player)

            views.setTextViewText(R.id.widget_track_title, title)
            views.setTextViewText(R.id.widget_track_artist, artist)
            views.setImageViewResource(
                R.id.btn_music_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            // Click track info -> open HomeActivity audio studio
            val openIntent = Intent().apply {
                setClassName(context.packageName, "com.chroma.studio.HomeActivity")
                putExtra(OscellionQuickActionsWidgetProvider.EXTRA_WIDGET_ACTION, OscellionQuickActionsWidgetProvider.ACTION_MUSIC_PLAYER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                201,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_music_art, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_track_info_container, openPendingIntent)

            // Play/Pause Action
            val playPauseIntent = Intent(context, OscellionMusicWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                202,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_music_play_pause, playPausePendingIntent)

            // Next Track Action
            val nextIntent = Intent(context, OscellionMusicWidgetProvider::class.java).apply {
                action = ACTION_NEXT
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                203,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_music_next, nextPendingIntent)

            // Prev Track Action
            val prevIntent = Intent(context, OscellionMusicWidgetProvider::class.java).apply {
                action = ACTION_PREV
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                204,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_music_prev, prevPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, OscellionMusicWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }
}
