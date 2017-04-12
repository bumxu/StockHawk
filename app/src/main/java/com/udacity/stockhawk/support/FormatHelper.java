package com.udacity.stockhawk.support;

import android.content.Context;
import android.text.format.DateUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;


public final class FormatHelper {
    private static final int SHORT_DATE_FLAGS;

    private static final DecimalFormat dollarFormat;
    private static final DecimalFormat signedDollarFormat;
    private static final DecimalFormat percentageFormat;

    static {
        SHORT_DATE_FLAGS = DateUtils.FORMAT_SHOW_DATE
            | DateUtils.FORMAT_NUMERIC_DATE
            | DateUtils.FORMAT_ABBREV_ALL
            | DateUtils.FORMAT_SHOW_YEAR;

        dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);

        signedDollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        signedDollarFormat.setPositivePrefix("+$");

        percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        percentageFormat.setMaximumFractionDigits(2);
        percentageFormat.setMinimumFractionDigits(2);
        percentageFormat.setPositivePrefix("+");
    }

    public static String formatDollar(float value) {
        return dollarFormat.format(value);
    }

    public static String formatSignedDollar(float value) {
        return signedDollarFormat.format(value);
    }

    public static String formatRelativeChange(float value) {
        return percentageFormat.format(value / 100);
    }

    public static String formatShortDate(Context context, long timeStampMillis) {
        return DateUtils.formatDateTime(context, timeStampMillis, SHORT_DATE_FLAGS);
    }
}
