package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;
import com.udacity.stockhawk.ui.DetailActivity;
import com.udacity.stockhawk.ui.MainActivity;


public class StockListWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Each widget of this kind...
        for (int appWidgetId : appWidgetIds) {
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stocklist_widget);

            // Set the adapter service for listview
            views.setRemoteAdapter(R.id.list_stocks,
                new Intent(context, StockListWigetRemoteViewsService.class));

            // (First time) hide "loading..." message and show listview
            views.setViewVisibility(R.id.text_loading_data, View.GONE);
            views.setViewVisibility(R.id.list_stocks, View.VISIBLE);

            // Set click action (1st part)
            final Intent clickIntentTemplate = new Intent(context, DetailActivity.class);
            // (Detail activity with parent)
            final PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(clickIntentTemplate)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            // · · ·
            views.setPendingIntentTemplate(R.id.list_stocks, clickPendingIntentTemplate);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // If intent is our "data updated!!" or system's "widget updated!!" action
        if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) ||
            intent.getAction().equals(QuoteSyncJob.ACTION_DATA_UPDATED)) {

            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            // Get widgets from this provider
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, getClass()));

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_stocks);
        }
    }
}

