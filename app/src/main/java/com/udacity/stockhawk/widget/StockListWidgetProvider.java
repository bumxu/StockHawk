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

            // (First time) hide "loading..." message
            views.setViewVisibility(R.id.text_loading_data, View.GONE);
            // Show list or "empty" message
            showListOrEmpty(context, views);

            // Set click action for "empty" message
            PendingIntent appIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
            // · · ·
            views.setOnClickPendingIntent(R.id.text_empty, appIntent);

            // Set click action for list items (1st part)
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

            // Show list or "empty" message
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stocklist_widget);
            showListOrEmpty(context, views);
            appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views);

            // Update list
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_stocks);
        }
    }

    /**
     * Shows the stock list or the "empty" message if there are no items.
     *
     * @param context An application context.
     * @param views   A RemoteViews instance for the widget.
     */
    private void showListOrEmpty(final Context context, RemoteViews views) {
        if (PrefUtils.getStocks(context).size() == 0) {
            views.setViewVisibility(R.id.text_empty, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.text_empty, View.GONE);
            views.setViewVisibility(R.id.list_stocks, View.VISIBLE);
        }
    }
}

