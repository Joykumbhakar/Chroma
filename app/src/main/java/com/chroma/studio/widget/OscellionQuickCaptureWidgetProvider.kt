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
 * Android Home Screen 2x2 Square Widget for Oscellion Quick Creative Capture.
 */
class OscellionQuickCaptureWidgetProvider : AppWidgetProvider() {

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
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_capture)

            // Primary: New Post
            val postIntent = Intent().apply {
                setClassName(context.packageName, "com.chroma.studio.HomeActivity")
                putExtra(OscellionQuickActionsWidgetProvider.EXTRA_WIDGET_ACTION, OscellionQuickActionsWidgetProvider.ACTION_CREATE_POST)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val postPendingIntent = PendingIntent.getActivity(
                context,
                301,
                postIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_capture_new_post, postPendingIntent)

            // Video Studio
            val videoIntent = Intent().apply {
                setClassName(context.packageName, "com.chroma.studio.HomeActivity")
                putExtra(OscellionQuickActionsWidgetProvider.EXTRA_WIDGET_ACTION, OscellionQuickActionsWidgetProvider.ACTION_VIDEO_EDITOR)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val videoPendingIntent = PendingIntent.getActivity(
                context,
                302,
                videoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_capture_video, videoPendingIntent)

            // Chat
            val chatIntent = Intent().apply {
                setClassName(context.packageName, "com.chroma.studio.HomeActivity")
                putExtra(OscellionQuickActionsWidgetProvider.EXTRA_WIDGET_ACTION, OscellionQuickActionsWidgetProvider.ACTION_CHAT)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val chatPendingIntent = PendingIntent.getActivity(
                context,
                303,
                chatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_capture_chat, chatPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
