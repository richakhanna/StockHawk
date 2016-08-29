package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.Utils.Constant;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by richa.khanna on 8/29/16.
 */
public class StockWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class StockRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private Cursor mCursor;
    private int mAppWidgetId;

    public StockRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
        // Since we reload the cursor in onDataSetChanged() which gets called immediately after
        // onCreate(), we do nothing here.
    }

    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public RemoteViews getViewAt(int position) {
        // Get the data for this position from the content provider
        String symbol = "";
        String bidPrice = "";
        String change = "";
        int isUp = 1;
        if (mCursor.moveToPosition(position)) {
            final int symbolColIndex = mCursor.getColumnIndex(QuoteColumns.SYMBOL);
            final int bidPriceColIndex = mCursor.getColumnIndex(QuoteColumns.BIDPRICE);
            final int changeColIndex = mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE);
            final int isUpIndex = mCursor.getColumnIndex(QuoteColumns.ISUP);
            symbol = mCursor.getString(symbolColIndex);
            bidPrice = mCursor.getString(bidPriceColIndex);
            change = mCursor.getString(changeColIndex);
            isUp = mCursor.getInt(isUpIndex);
        }

        // Construct a RemoteViews item based on the app widget item XML file,
        // and set the text in the view
        RemoteViews rvItem = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);
        rvItem.setTextViewText(R.id.stock_symbol, symbol);
        rvItem.setTextViewText(R.id.bid_price, bidPrice);
        rvItem.setTextViewText(R.id.change, change);
        if (isUp == 1) {
            rvItem.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
        } else {
            rvItem.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
        }

        // Setting a fill-intent, which will be used to fill in the pending intent template
        // that is set on the collection view in StockWidgetProvider.
        final Intent fillInIntent = new Intent();
        final Bundle extras = new Bundle();
        extras.putString(Constant.TAG_COMPANY_STOCK_SYMBOL, symbol);
        fillInIntent.putExtras(extras);
        rvItem.setOnClickFillInIntent(R.id.ll_widget_item, fillInIntent);
        return rvItem;
    }

    public RemoteViews getLoadingView() {
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        // Refresh the cursor
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }
}