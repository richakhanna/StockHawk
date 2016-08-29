package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.Utils.Constant;
import com.sam_chordas.android.stockhawk.ui.StockDetailActivity;

/**
 * Created by richa.khanna on 8/28/16.
 */
public class StockWidgetProvider extends AppWidgetProvider {

    public static final String CLICK_ACTION = "com.sam_chordas.android.stockhawk.widget.CLICK_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(CLICK_ACTION)) {
            final String symbol = intent.getStringExtra(Constant.TAG_COMPANY_STOCK_SYMBOL);

            Intent detailIntent = new Intent(context, StockDetailActivity.class);
            detailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            detailIntent.putExtra(Constant.TAG_COMPANY_STOCK_SYMBOL, symbol);
            context.startActivity(detailIntent);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // update each of the app widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
            RemoteViews rvLayout = creteRemoteViewsLayout(context, appWidgetIds[i]);
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetIds[i], rvLayout);
            //notify the collection view to invalidate its data when someone add/remove stocks:
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds[i], R.id.lv_widget_list);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private RemoteViews creteRemoteViewsLayout(Context context, int appWidgetId) {
        // Sets up the intent that points to the StockViewService that will
        // provide the views for this collection.
        final Intent intent = new Intent(context, StockWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        RemoteViews rvLayoutList = new RemoteViews(context.getPackageName(), R.layout.stock_widget);
        // Set up the RemoteViews object to use a RemoteViews adapter.
        // This adapter connects to a RemoteViewsService through the specified intent.
        // This is how we populate the data.
        rvLayoutList.setRemoteAdapter(R.id.lv_widget_list, intent);

        // The empty view is displayed when the collection has no items.
        rvLayoutList.setEmptyView(R.id.lv_widget_list, R.id.empty_view);


        // Individuals items of a collection cannot set up their own pending intents.
        // Instead, the collection as a whole sets up a pending intent template, and
        // the individual items set a fillInIntent to create unique behavior on an item-by-item basis.
        final Intent clickIntent = new Intent(context, StockWidgetProvider.class);
        // Set the action for the intent.
        // When the user touches a particular view, it will have the effect of
        // broadcasting CLICK_ACTION.
        clickIntent.setAction(StockWidgetProvider.CLICK_ACTION);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        clickIntent.setData(Uri.parse(clickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent clickPendingIntent = PendingIntent.getBroadcast(context, 0,
                clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rvLayoutList.setPendingIntentTemplate(R.id.lv_widget_list, clickPendingIntent);

        return rvLayoutList;
    }
}
