package com.w2sv.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.w2sv.androidutils.os.getIntExtraOrNull
import com.w2sv.common.di.AppDefaultScope
import com.w2sv.common.utils.log
import com.w2sv.common.utils.toMapString
import com.w2sv.core.widget.R
import com.w2sv.widget.data.WidgetModuleWidgetRepository
import com.w2sv.widget.layout.WidgetLayoutPopulator
import com.w2sv.widget.utils.getWifiWidgetIds
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import slimber.log.i

@AndroidEntryPoint
class WifiWidgetProvider : AppWidgetProvider() {

    @Inject
    internal lateinit var widgetDataRefreshWorkerManager: WifiWidgetRefreshWorker.Manager

    @Inject
    internal lateinit var widgetLayoutPopulator: WidgetLayoutPopulator

    @Inject
    internal lateinit var appWidgetManager: AppWidgetManager

    @Inject
    internal lateinit var widgetRepository: WidgetModuleWidgetRepository

    @Inject
    @AppDefaultScope
    internal lateinit var scope: CoroutineScope

    /**
     * Called upon the first AppWidget instance being created.
     *
     * Enqueues [WifiWidgetRefreshWorker] as UniquePeriodicWork if not already enqueued.
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        i { "onEnabled" }

        widgetDataRefreshWorkerManager.applyRefreshingSettings(widgetRepository.refreshing.value)
    }

    /**
     * Called upon last AppWidget instance of provider being deleted.
     *
     * Cancels [WifiWidgetRefreshWorker].
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        i { "onDisabled" }

        widgetDataRefreshWorkerManager.cancelWorker()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        i { "onReceive | Action=${intent.action} | Extras=${intent.extras?.toMapString()}" }

        if (intent.action == ACTION_REFRESH_DATA) {
            onUpdate(
                context = context,
                appWidgetManager = appWidgetManager,
                appWidgetIds = intent
                    .getIntExtraOrNull(AppWidgetManager.EXTRA_APPWIDGET_ID, WIDGET_ID_UNSPECIFIED)
                    .log { "Got widget id $it for ACTION_REFRESH_DATA" }
                    ?.let { intArrayOf(it) }
                    ?: appWidgetManager.getWifiWidgetIds(context)
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        i { "onAppWidgetOptionsChanged | Bundle=${newOptions?.toMapString()}" }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        scope.launch {
            i { "onUpdate | appWidgetIds=${appWidgetIds.toList()} | ${Thread.currentThread()}" }

            val widgetView = RemoteViews(
                context.packageName,
                R.layout.widget
            )

            appWidgetIds.forEach { id ->
                i { "updateWidget | appWidgetId=$id" }

                appWidgetManager.updateAppWidget(
                    id,
                    widgetLayoutPopulator
                        .populate(
                            widget = widgetView,
                            appWidgetId = id
                        )
                )
            }
        }
    }

    companion object {

        // ===============
        // Refreshing
        // ===============

        fun triggerDataRefresh(context: Context) {
            context.sendBroadcast(getRefreshDataIntent(context))
        }

        fun getRefreshDataIntent(context: Context, widgetId: Int = WIDGET_ID_UNSPECIFIED): Intent =
            Intent(context, WifiWidgetProvider::class.java)
                .setAction(ACTION_REFRESH_DATA)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)

        private const val ACTION_REFRESH_DATA = "com.w2sv.wifiwidget.action.REFRESH_DATA"
        private const val WIDGET_ID_UNSPECIFIED = -1
    }
}
