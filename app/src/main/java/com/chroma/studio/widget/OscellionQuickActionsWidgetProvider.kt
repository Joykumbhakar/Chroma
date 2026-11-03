@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.chroma.studio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.chroma.studio.R

/**
 * Android Home Screen Widget for Oscellion Studio Quick Actions.
 * Features 5 squircle shortcuts: New Post (+), Video Editor (🎬),
 * Audio Studio (🎵), Messages (💬), and Switch Lab (⚡).
 */
class OscellionQuickActionsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        const val EXTRA_WIDGET_ACTION = "com.chroma.studio.WIDGET_ACTION"
        const val ACTION_CREATE_POST = "create_post"
        const val ACTION_VIDEO_EDITOR = "video_editor"
        const val ACTION_MUSIC_PLAYER = "music_player"
        const val ACTION_CHAT = "chat"
        const val ACTION_SWITCH_LAB = "switch_lab"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_studio_quick_actions)

            // Header / App click -> open Home
            val homeIntent = Intent().apply {
                setClassName(context.packageName, "com.chroma.studio.HomeActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val homePendingIntent = PendingIntent.getActivity(
                context,
                0,
                homeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header_action, homePendingIntent)

            // Button 1: Create Post
            views.setOnClickPendingIntent(
                R.id.btn_widget_post,
                createActionPendingIntent(context, ACTION_CREATE_POST, 101)
            )

            // Button 2: Video Editor
            views.setOnClickPendingIntent(
                R.id.btn_widget_video,
                createActionPendingIntent(context, ACTION_VIDEO_EDITOR, 102)
            )

            // Button 3: Music Player
            views.setOnClickPendingIntent(
                R.id.btn_widget_music,
                createActionPendingIntent(context, ACTION_MUSIC_PLAYER, 103)
            )

            // Button 4: Chat
            views.setOnClickPendingIntent(
                R.id.btn_widget_chat,
                createActionPendingIntent(context, ACTION_CHAT, 104)
            )

            // Button 5: Switch Lab
            views.setOnClickPendingIntent(
                R.id.btn_widget_lab,
                createActionPendingIntent(context, ACTION_SWITCH_LAB, 105)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createActionPendingIntent(
            context: Context,
            action: String,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent().apply {
                setClassName(context.packageName, "com.chroma.studio.HomeActivity")
                putExtra(EXTRA_WIDGET_ACTION, action)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
