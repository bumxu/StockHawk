package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.support.FormatHelper;


public class StockListWigetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor mCursor;

            @Override
            public void onCreate() {
            }

            @Override
            public void onDataSetChanged() {
                if (mCursor != null) {
                    mCursor.close();
                }

                ///
                final long identityToken = Binder.clearCallingIdentity();
                ///
                mCursor = getContentResolver().query(
                    // TODO: Reduce DB load
                    Contract.Quote.URI,
                    null, null, null,
                    Contract.Quote.COLUMN_SYMBOL + " ASC");
                ///
                Binder.restoreCallingIdentity(identityToken);
                ///
            }

            @Override
            public void onDestroy() {
                if (mCursor != null) {
                    mCursor.close();
                }
                mCursor = null;
            }

            @Override
            public int getCount() {
                return (mCursor == null) ? 0 : mCursor.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == -1 || mCursor == null || !mCursor.moveToPosition(position)) {
                    return null;
                }

                // Prepare fields
                final String symbol = mCursor.getString(Contract.Quote.POSITION_SYMBOL);
                final Float price = mCursor.getFloat(Contract.Quote.POSITION_PRICE);
                final Float perChange = mCursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);
                final Float absChange = mCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                // · · ·
                final String displayMode = PrefUtils.getDisplayMode(getApplicationContext());

                // Prepare views handler
                final RemoteViews views = new RemoteViews(getPackageName(), R.layout.item_widget_quote);

                // Set the name
                views.setTextViewText(R.id.text_symbol, symbol);
                // Set the formated price
                views.setTextViewText(R.id.price, FormatHelper.formatDollar(price));

                // Set the variation according with preferences
                final String value;
                if (displayMode.equals(getString(R.string.pref_display_mode_absolute_key))) {
                    value = FormatHelper.formatSignedDollar(absChange);
                } else {
                    value = FormatHelper.formatRelativeChange(perChange);
                }
                views.setTextViewText(R.id.change, value);

                // Set the background color for variation
                views.setInt(R.id.change, "setBackgroundResource",
                    (absChange > 0)
                        ? R.drawable.change_pill_green
                        : R.drawable.change_pill_red);

                // Set click action for list items (2nd part)
                final Intent fillInIntent = new Intent()
                    .putExtra("symbol", symbol);
                views.setOnClickFillInIntent(R.id.quo2, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return null;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
