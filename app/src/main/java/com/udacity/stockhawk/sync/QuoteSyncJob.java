package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;


@SuppressWarnings("WeakerAccess")
public final class QuoteSyncJob {
    private static final int ONE_OFF_ID = 2;
    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    public static final String ACTION_SYNC_COMPLETED = "com.udacity.stockhawk.ACTION_SYNC_COMPLETED";
    public static final String ACTION_UNKNOWN_SYMBOLS = "com.udacity.stockhawk.ACTION_UNKNOWN_SYMBOLS";
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    private QuoteSyncJob() {
    }

    static void getQuotes(Context context) {
        Timber.d("Running sync job");

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        // Storage for unknown symbols
        ArrayList<String> unknownSymbols = new ArrayList<>();

        try {
            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                context.sendBroadcast(new Intent(ACTION_SYNC_COMPLETED));
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            // Update ShredPreference (maybe it has changed during the request)
            stockPref = PrefUtils.getStocks(context);

            while (iterator.hasNext()) {
                String symbol = iterator.next();

                // If now it's not included, ignore
                // (This only REDUCE the failure probability)
                if (!stockPref.contains(symbol)) {
                    continue;
                }

                Stock stock;
                String name;
                float price, change, percentChange;

                try {
                    stock = quotes.get(symbol);
                    StockQuote quote = stock.getQuote();

                    name = stock.getName();

                    price = quote.getPrice().floatValue();
                    change = quote.getChange().floatValue();
                    percentChange = quote.getChangeInPercent().floatValue();
                } catch (NullPointerException e) {
                    // This symbol doesn't exist
                    unknownSymbols.add(symbol);
                    // Next one
                    continue;
                }

                List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                StringBuilder historyBuilder = new StringBuilder();

                for (HistoricalQuote it : history) {
                    historyBuilder.append(it.getDate().getTimeInMillis());
                    historyBuilder.append(", ");
                    historyBuilder.append(it.getClose());
                    historyBuilder.append("\n");
                }

                ContentValues quoteCV = new ContentValues();
                quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                quoteCV.put(Contract.Quote.COLUMN_NAME, name);
                quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);


                quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());

                quoteCVs.add(quoteCV);

            }

            // If there is unknown symbols, notify listeners
            if (unknownSymbols.size() > 0) {
                final Intent unknownSymbolsIntent = new Intent(ACTION_UNKNOWN_SYMBOLS);
                unknownSymbolsIntent.putStringArrayListExtra("symbols", unknownSymbols);
                context.sendBroadcast(unknownSymbolsIntent);
            }

            context.getContentResolver()
                .bulkInsert(
                    Contract.Quote.URI,
                    quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

            context.sendBroadcast(new Intent(ACTION_SYNC_COMPLETED));

        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
        }
    }

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");

        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));

        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(PERIOD)
            .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context);

    }

    public static synchronized void syncImmediately(Context context) {

        ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {

            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));


            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());
        }
    }
}
