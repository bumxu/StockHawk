package com.udacity.stockhawk.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.support.HistoricalDataLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Pair<Date, Float>>> {
    private static final int HISTORICAL_DATA_LOADER = 1;

    @BindView(R.id.chart) LineChart mStockChart;

    private String mSymbol;

//    @Override
//    public void onClick(String symbol) {
//        Timber.d("Symbol clicked: %s", symbol);
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        mSymbol = getIntent().getStringExtra("symbol");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        ((TextView) findViewById(R.id.symbol)).setText(mSymbol);

        LoaderManager loaderManager = getSupportLoaderManager();
        if (loaderManager.getLoader(HISTORICAL_DATA_LOADER) == null) {
            loaderManager.initLoader(HISTORICAL_DATA_LOADER, null, this);
        } else {
            loaderManager.restartLoader(HISTORICAL_DATA_LOADER, null, this);
        }
    }

    private void plot(final List<Pair<Date, Float>> data) {
        // Prepare dataset

        final List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            final Float price = data.get(i).second;

            entries.add(new Entry(i, price));
        }

        final LineDataSet dataSet = new LineDataSet(entries, "value");
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawValues(false);
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorAccent));

        // X Axis

        final XAxis xAxis = mStockChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(true);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(true);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            private SimpleDateFormat mFormat = new SimpleDateFormat("dd/MM/YY");

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return mFormat.format(data.get((int) value).first);
            }
        });

        // Y Axes

        final YAxis leftAxis = mStockChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(170f);

        final YAxis rightAxis = mStockChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Setup

        final LineData lineData = new LineData(dataSet);

        mStockChart.setData(lineData);
        mStockChart.invalidate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<List<Pair<Date, Float>>> onCreateLoader(int id, Bundle args) {
        // id == HISTORICAL_DATA_LOADER
        return new HistoricalDataLoader(this, mSymbol);
    }

    @Override
    public void onLoadFinished(Loader<List<Pair<Date, Float>>> loader, List<Pair<Date, Float>> data) {
        plot(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Pair<Date, Float>>> loader) {
    }
}

