package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;


public class StockListWigetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return new RemoteViewsFactory() {
            // TODO: Put out!
            private DecimalFormat dollarFormat;
            private DecimalFormat dollarFormatWithPlus;
            private DecimalFormat percentageFormat;

            private Cursor mCursor;

            @Override
            public void onCreate() {
                dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);

                dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus.setPositivePrefix("+$");

                percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
                percentageFormat.setMaximumFractionDigits(2);
                percentageFormat.setMinimumFractionDigits(2);
                percentageFormat.setPositivePrefix("+");
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
                final RemoteViews views = new RemoteViews(getPackageName(), R.layout.stocklist_widget_item);

                // Set the name
                views.setTextViewText(R.id.symbol, symbol);
                // Set the formated price
                views.setTextViewText(R.id.price, dollarFormat.format(price));

                // Set the variation according with preferences
                final String value;
                if (displayMode.equals(getString(R.string.pref_display_mode_absolute_key))) {
                    value = dollarFormatWithPlus.format(absChange);
                } else {
                    value = percentageFormat.format(perChange / 100);
                }
                views.setTextViewText(R.id.change, value);

                // Set the background color for variation
                views.setInt(R.id.change, "setBackgroundResource",
                    (absChange >= 0)
                        ? R.drawable.percent_change_pill_green
                        : R.drawable.percent_change_pill_red);

                // Set click action (2nd part)
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
