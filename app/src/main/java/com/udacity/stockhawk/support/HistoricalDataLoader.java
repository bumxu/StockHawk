package com.udacity.stockhawk.support;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.util.Pair;

import com.udacity.stockhawk.data.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;


/**
 * Loader for the historical data of a symbol.
 */
public class HistoricalDataLoader extends AsyncTaskLoader<List<Pair<Date, Float>>> {
    // Symbol owner of the data
    private final String mSymbol;
    // Cached results
    private List<Pair<Date, Float>> mCachedResult;

    /**
     * Creates a new loader for the historical data of the given symbol.
     *
     * @param context - The context from which the loader is created.
     * @param symbol  - The symbol whose data will be loaded.
     */
    public HistoricalDataLoader(final Context context, final String symbol) {
        super(context);

        this.mSymbol = symbol;

        // Load from provider or return cached results
        if (mCachedResult == null) {
            forceLoad();
        } else {
            deliverResult(mCachedResult);
        }
    }

    @Override
    public List<Pair<Date, Float>> loadInBackground() {
        // Request historical data
        final Cursor cursor = getContext().getContentResolver().query(
            Contract.Quote.makeUriForStock(mSymbol),
            new String[]{Contract.Quote.COLUMN_HISTORY},
            null, null, null);

        // Initialize results storage
        mCachedResult = new ArrayList<>();

        // Null or empty cases
        if (cursor == null || !cursor.moveToNext()) {
            return mCachedResult;
        }

        // Retrieve
        final String rawData = cursor.getString(
            cursor.getColumnIndex(Contract.Quote.COLUMN_HISTORY));
        final Scanner scanner = new Scanner(rawData);

        // Parse "timestamp, price"
        while (scanner.hasNextLine()) {
            final String[] line = scanner.nextLine().split(", ");

            final long rawTime = Long.parseLong(line[0]);
            final Date time = new Date(rawTime);

            final float price = Float.parseFloat(line[1]);

            mCachedResult.add(new Pair<>(time, price));
        }

        // Reverse order
        Collections.reverse(mCachedResult);
        // Clean
        cursor.close();

        return mCachedResult;
    }
}