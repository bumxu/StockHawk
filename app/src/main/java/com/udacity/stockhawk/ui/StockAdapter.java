package com.udacity.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;


class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {
    private final Context context;
    private final DecimalFormat dollarFormatWithPlus;
    private final DecimalFormat dollarFormat;
    private final DecimalFormat percentageFormat;
    private Cursor mCursor;
    private final StockAdapterOnClickHandler clickHandler;

    StockAdapter(Context context, StockAdapterOnClickHandler clickHandler) {
        this.context = context;
        this.clickHandler = clickHandler;

        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix("+$");
        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
    }

    void setCursor(final Cursor cursor) {
        // Using DiffUtil instead of replacing cursor let's recover the missing animations
        // when a recyclerview item is added or removed.
        // Great introduction: https://goo.gl/AfbrdR
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new UpdateDiffCallbacks(mCursor, cursor));
        // Finally, replace cursor for viewholders' binding
        mCursor = cursor;
        // Apply diff results
        diffResult.dispatchUpdatesTo(this);
    }

    String getSymbolAtPosition(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(Contract.Quote.POSITION_SYMBOL);
    }

    @Override
    public StockViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View item = LayoutInflater.from(context).inflate(R.layout.item_quote, parent, false);

        return new StockViewHolder(item);
    }

    @Override
    public void onBindViewHolder(StockViewHolder holder, int position) {
        mCursor.moveToPosition(position);

        holder.symbol.setText(mCursor.getString(Contract.Quote.POSITION_SYMBOL));
        holder.name.setText(mCursor.getString(Contract.Quote.POSITION_NAME));
        holder.price.setText(dollarFormat.format(mCursor.getFloat(Contract.Quote.POSITION_PRICE)));


        float rawAbsoluteChange = mCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
        float percentageChange = mCursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

        if (rawAbsoluteChange > 0) {
            holder.change.setBackgroundResource(R.drawable.change_pill_green);
        } else {
            holder.change.setBackgroundResource(R.drawable.change_pill_red);
        }

        String change = dollarFormatWithPlus.format(rawAbsoluteChange);
        String percentage = percentageFormat.format(percentageChange / 100);

        if (PrefUtils.getDisplayMode(context)
            .equals(context.getString(R.string.pref_display_mode_absolute_key))) {
            holder.change.setText(change);
        } else {
            holder.change.setText(percentage);
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (mCursor != null) {
            count = mCursor.getCount();
        }
        return count;
    }

    interface StockAdapterOnClickHandler {
        void onClick(ClickDetails details, View view);
    }

    class StockViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.text_symbol) TextView symbol;
        @BindView(R.id.text_name) TextView name;
        @BindView(R.id.price) TextView price;
        @BindView(R.id.change) TextView change;

        StockViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int adapterPosition = getAdapterPosition();

            mCursor.moveToPosition(adapterPosition);
            final ClickDetails details = new ClickDetails(mCursor);

            clickHandler.onClick(details, view);
        }
    }

    private class UpdateDiffCallbacks extends DiffUtil.Callback {
        private final Cursor mOldCursor, mNewCursor;

        UpdateDiffCallbacks(Cursor oldCursor, Cursor newCursor) {
            mOldCursor = oldCursor;
            mNewCursor = newCursor;
        }

        @Override
        public int getOldListSize() {
            return (mOldCursor != null) ? mOldCursor.getCount() : 0;
        }

        @Override
        public int getNewListSize() {
            return (mNewCursor != null) ? mNewCursor.getCount() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            try {
                mOldCursor.moveToPosition(oldItemPosition);
                mNewCursor.moveToPosition(newItemPosition);

                return mOldCursor.getString(mOldCursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL))
                    .equals(mNewCursor.getString(mNewCursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL)));
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            try {
                mOldCursor.moveToPosition(oldItemPosition);
                mNewCursor.moveToPosition(newItemPosition);

                return mOldCursor.getFloat(mOldCursor.getColumnIndex(Contract.Quote.COLUMN_PRICE))
                    == mNewCursor.getFloat(mNewCursor.getColumnIndex(Contract.Quote.COLUMN_PRICE));
            } catch (Exception e) {
                return false;
            }
        }
    }

    class ClickDetails {
        public String symbol, name;
        float price, absoluteChange, percentageChange;

        private ClickDetails(final Cursor cursor) {
            symbol = cursor.getString(
                cursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
            name = cursor.getString(
                cursor.getColumnIndex(Contract.Quote.COLUMN_NAME));

            price = cursor.getFloat(
                cursor.getColumnIndex(Contract.Quote.COLUMN_PRICE));

            absoluteChange = cursor.getFloat(
                cursor.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE));
            percentageChange = cursor.getFloat(
                cursor.getColumnIndex(Contract.Quote.COLUMN_PERCENTAGE_CHANGE));
        }

        Intent getIntent() {
            return new Intent()
                .putExtra("symbol", symbol)
                .putExtra("name", name)
                .putExtra("price", price)
                .putExtra("absChange", absoluteChange)
                .putExtra("perChange", percentageChange);
        }
    }
}
