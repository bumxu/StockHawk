package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.support.FormatHelper;
import com.udacity.stockhawk.support.HistoricalDataLoader;
import com.udacity.stockhawk.support.TransitionListenerShortener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Pair<Date, Float>>> {
    private static final int HISTORICAL_DATA_LOADER = 1;

    // Symbol & hostorical data required to be fields
    private String mSymbol;
    private List<Pair<Date, Float>> mHistoricalData;

    // Components
    @BindView(R.id.text_symbol) TextView mSymbolTextView;
    @BindView(R.id.text_name) TextView mNameTextView;
    @BindView(R.id.text_price) TextView mPriceTextView;
    @BindView(R.id.text_change) TextView mChangeTextView;
    @BindView(R.id.image_change) ImageView mChangeArrowImageView;
    @BindView(R.id.text_curr_data) TextView mCurrentDataTextView;
    // · · ·
    @BindView(R.id.chart) LineChart mStockChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        // · · ·
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        applyIntent(getIntent());

        LoaderManager loaderManager = getSupportLoaderManager();
        if (loaderManager.getLoader(HISTORICAL_DATA_LOADER) == null) {
            loaderManager.initLoader(HISTORICAL_DATA_LOADER, null, this);
        } else {
            loaderManager.restartLoader(HISTORICAL_DATA_LOADER, null, this);
        }

        getWindow().getSharedElementEnterTransition().addListener(new TransitionListenerShortener() {
            @Override
            public void onTransitionStart(Transition transition) {
                mStockChart.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                mStockChart.startAnimation(AnimationUtils.loadAnimation(DetailActivity.this, R.anim.anim_chart_in));
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // · // · // · // · // · //

    /**
     * Applies the Intent's data received by the Activity.
     *
     * @param intent The Intent received by the Activity with the symbol,
     *               the name, the price and the relative change.
     */
    private void applyIntent(final Intent intent) {
        // Save the symbol globally
        mSymbol = getIntent().getStringExtra("symbol");

        // Temporal data
        final String name = intent.getStringExtra("name");
        final float price = intent.getFloatExtra("price", 0);
        final float perChange = intent.getFloatExtra("perChange", 0);
        final float absChange = intent.getFloatExtra("absChange", 0);

        // Fill
        mSymbolTextView.setText(mSymbol);
        mNameTextView.setText(name);
        // · · ·
        mPriceTextView.setText(FormatHelper.formatDollar(price));
        mChangeTextView.setText(FormatHelper.formatRelativeChange(perChange));
        // · · ·
        if (absChange <= 0) {
            mChangeArrowImageView.setImageResource(R.drawable.ic_price_down);
            mChangeArrowImageView.setColorFilter(ContextCompat.getColor(this, R.color.negativeRed));
        }
    }

    /**
     * Plots the chart with the data stored Historical Data.
     */
    private void plot() {
        // -- Prepare dataset --

        // No data
        if (mHistoricalData == null || mHistoricalData.size() == 0) {
            return;
        }

        // Data entries
        final List<Entry> entries = new ArrayList<>();
        // Min & max for autoscale
        float min, max;
        min = max = mHistoricalData.get(0).second;

        for (int i = 0; i < mHistoricalData.size(); i++) {
            final Float price = mHistoricalData.get(i).second;

            entries.add(new Entry(i, price));

            min = Math.min(min, price);
            max = Math.max(max, price);
        }

        // Apply & format
        final LineDataSet dataSet = new LineDataSet(entries, "value");
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawValues(false);
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorAccent));
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.colorAccent));
        dataSet.setDrawFilled(true);

        // -- X Axis --

        final XAxis xAxis = mStockChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(true);
        xAxis.setGranularity(1f);
        xAxis.setGridColor(Color.parseColor("#35ffffff"));
        xAxis.enableGridDashedLine(6, 4, 0);
        xAxis.setDrawGridLines(true);
        xAxis.setCenterAxisLabels(true);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if (value < 0 || value >= mHistoricalData.size()) {
                    return null;
                }

                return FormatHelper.formatShortDate(
                    DetailActivity.this,
                    mHistoricalData.get((int) value).first.getTime());
            }
        });

        // -- Y Axes --

        final YAxis leftAxis = mStockChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#35ffffff"));
        leftAxis.enableGridDashedLine(6, 4, 0);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setDrawAxisLine(false);

        // Autoscale
        final float extra = (float) ((max - min) * 0.25);
        leftAxis.setAxisMinimum(min - extra);
        leftAxis.setAxisMaximum(max + extra);

        // Disable right axis
        final YAxis rightAxis = mStockChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Tap actions
        mStockChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();

                if (index < 0 || index >= mHistoricalData.size()) {
                    onNothingSelected();
                    return;
                }

                // TODO: i18n!
                mCurrentDataTextView.setText(
                    "Semana "
                        + FormatHelper.formatShortDate(DetailActivity.this, mHistoricalData.get(index).first.getTime())
                        + "  ·  "
                        + FormatHelper.formatDollar(e.getY())
                );
            }

            @Override
            public void onNothingSelected() {
                mCurrentDataTextView.setText("");
            }
        });

        // Final setup

        final LineData lineData = new LineData(dataSet);

        mStockChart.getLegend().setEnabled(false);
        mStockChart.setDescription(null);
        mStockChart.setViewPortOffsets(0, 0, 0, 0);

        mStockChart.setData(lineData);
        mStockChart.invalidate();
    }

    // · // · // · // · // · //

    @Override
    public Loader<List<Pair<Date, Float>>> onCreateLoader(int id, Bundle args) {
        // id == HISTORICAL_DATA_LOADER
        return new HistoricalDataLoader(this, mSymbol);
    }

    @Override
    public void onLoadFinished(Loader<List<Pair<Date, Float>>> loader, List<Pair<Date, Float>> data) {
        mHistoricalData = data;

        plot();
    }

    @Override
    public void onLoaderReset(Loader<List<Pair<Date, Float>>> loader) {
    }
}

