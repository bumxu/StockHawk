package com.udacity.stockhawk.ui;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.udacity.stockhawk.R.id.text_symbol;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
    SwipeRefreshLayout.OnRefreshListener,
    StockAdapter.StockAdapterOnClickHandler {

    private static final int STOCK_LOADER = 0;

    private StockAdapter adapter;

    private BroadcastReceiver mIntentReceiver;
    private IntentFilter mIntentFilter;

    @BindView(R.id.recycler_view) RecyclerView stockRecyclerView;
    @BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.text_error_msg) TextView errorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Broadcast receiver for not found symbols during sync job
        mIntentReceiver = new UnknownSymbolsReceiver();
        mIntentFilter = new IntentFilter("com.udacity.stockhawk.ACTION_UNKNOWN_SYMBOLS");

        adapter = new StockAdapter(this, this);
        stockRecyclerView.setHasFixedSize(true);
        stockRecyclerView.setAdapter(adapter);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        onRefresh();

        QuoteSyncJob.initialize(this);
        getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                PrefUtils.removeStock(MainActivity.this, symbol);
                getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
            }
        }).attachToRecyclerView(stockRecyclerView);
    }

    @Override
    protected void onResume() {
        // Register broadcast receiver for not found symbols
        registerReceiver(mIntentReceiver, mIntentFilter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Unregister broadcast receiver for not found symbols
        unregisterReceiver(mIntentReceiver);
        super.onPause();
    }

    @Override
    public void onRefresh() {

        QuoteSyncJob.syncImmediately(this);

        if (!networkUp() && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            showErrorMessage(R.string.error_no_network);
        } else if (!networkUp()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();
        } else if (PrefUtils.getStocks(this).size() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            showErrorMessage(R.string.error_no_stocks);
        } else {
            hideErrorMessage();
        }
    }

    @Override
    public void onClick(final StockAdapter.ClickDetails details, final View view) {
        Timber.d("Symbol clicked: %s", text_symbol);

        final Intent intent = details.getIntent().setClass(this, DetailActivity.class);

        final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
            this,
            Pair.create(findViewById(R.id.toolbar), "toolbar"),
            Pair.create(view.findViewById(R.id.background), "background")
        );

        startActivity(intent, options.toBundle());
    }


    private void showErrorMessage(@StringRes int msgResource) {
        errorTextView.setText(getString(msgResource));
        errorTextView.setVisibility(View.VISIBLE);
    }

    private void hideErrorMessage() {
        errorTextView.setVisibility(View.GONE);
    }

    private boolean networkUp() {
        ConnectivityManager cm =
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }


    public void button(@SuppressWarnings("UnusedParameters") View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }


    void addStock(String symbol) {
        if (symbol != null && !symbol.isEmpty()) {

            if (networkUp()) {
                swipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }

            PrefUtils.addStock(this, symbol);
            QuoteSyncJob.syncImmediately(this);
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Returns a CursorLoader thas automatically updates
        // itself ("forceLoad") when provider data change.
        return new CursorLoader(this,
            Contract.Quote.URI,
            Contract.Quote.QUERY_COLUMNS.toArray(new String[]{}),
            null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (data.getCount() != 0) {
            hideErrorMessage();
        } else {
            showErrorMessage(R.string.error_no_stocks);
        }
        adapter.setCursor(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }


    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(this)
            .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(this);
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * BroadcastReceiver for not found symbols during sync job.
     */
    private final class UnknownSymbolsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            // Get the list of unknown symbols
            final List<String> symbols = intent.getStringArrayListExtra("symbols");

            // Remove from the system
            for (String symbol : symbols) {
                PrefUtils.removeStock(context, symbol);
                context.getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);
            }

            // Show an elegant message
            Snackbar
                .make(
                    findViewById(R.id.root),
                    getString(R.string.snackbar_invalid_symbols) + " " + TextUtils.join(", ", symbols),
                    4000)
                .setAction("OK", new EmptyClickListener())
                .show();
        }
    }

    /**
     * An empty OnClickListener for reusability, nothing more.
     */
    private static final class EmptyClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) { /* NOTHING */ }
    }
}
