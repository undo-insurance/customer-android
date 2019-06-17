package com.kustomer.kustomersdk.Helpers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.format.DateUtils;

import com.kustomer.kustomersdk.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Junaid on 1/20/2018.
 */

public class KUSDate {

    //region properties
    private static float TWO_DAYS_MILLIS = 48 * 60 * 60 * 1000f;
    private static float SECONDS_PER_MINUTE = 60f;
    private static float MINUTES_PER_HOUR = 60f;
    private static float HOURS_PER_DAY = 24f;
    private static int DAYS_PER_WEEK = 7;

    private static DateFormat shortDateFormat;
    private static DateFormat shortTimeFormat;
    //endregion

    //region Static Methods
    @Nullable
    public static String humanReadableTextFromDate(@NonNull Context context, @Nullable Date date) {
        if (date == null)
            return null;

        long timeAgo = (Calendar.getInstance().getTimeInMillis() - date.getTime()) / 1000;
        if (timeAgo < SECONDS_PER_MINUTE)
            return context.getString(R.string.com_kustomer_just_now);

        return localizedHumanReadableTextFromDate(context, timeAgo);
    }

    @NonNull
    private static String localizedHumanReadableTextFromDate(Context context, long timeAgo) {
        int count;
        @StringRes int stringId;

        if (timeAgo >= SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY * DAYS_PER_WEEK) {
            count = (int) (timeAgo / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY * DAYS_PER_WEEK));
            stringId = (count > 1) ?
                    R.string.com_kustomer_param_weeks_ago :
                    R.string.com_kustomer_param_week_ago;

        } else if (timeAgo >= SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY) {
            count = (int) (timeAgo / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY));
            stringId = (count > 1) ?
                    R.string.com_kustomer_param_days_ago :
                    R.string.com_kustomer_param_day_ago;

        } else if (timeAgo >= SECONDS_PER_MINUTE * MINUTES_PER_HOUR) {
            count = (int) (timeAgo / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR));
            stringId = (count > 1) ?
                    R.string.com_kustomer_param_hours_ago :
                    R.string.com_kustomer_param_hour_ago;

        } else {
            count = (int) (timeAgo / (SECONDS_PER_MINUTE));
            stringId = (count > 1) ?
                    R.string.com_kustomer_param_minutes_ago :
                    R.string.com_kustomer_param_minute_ago;
        }

        return context.getString(stringId, count);
    }

    public static String upfrontVolumeControlApproximateWaitingTimeFromSeconds(Context context,
                                                                        int seconds){
        int count;
        @StringRes int stringId;

        if (seconds < SECONDS_PER_MINUTE) {
            count = seconds;
            stringId = seconds <= 1 ?
                    R.string.com_kustomer_our_expected_wait_time_is_approximately_param_second :
                    R.string.com_kustomer_our_expected_wait_time_is_approximately_param_seconds;

        } else if (seconds < SECONDS_PER_MINUTE * MINUTES_PER_HOUR) {
            count = (int) Math.ceil(seconds / SECONDS_PER_MINUTE);
            stringId = count <= 1 ?
                    R.string.com_kustomer_our_expected_wait_time_is_approximately_param_minute :
                    R.string.com_kustomer_our_expected_wait_time_is_approximately_param_minutes;

        } else if (seconds < SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY) {
            count = (int) Math.ceil(seconds / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR));
            stringId = count <= 1 ?
                    R.string.com_kustomer_our_expected_wait_time_is_approximately_param_hour :
                    R.string.com_kustomer_our_expected_wait_time_is_approximately_param_hours;

        } else {
            return context.getString(R.string.com_kustomer_our_expected_wait_time_is_approximately_greater_than_one_day);
        }

        return context.getString(stringId, count);
    }

    public static String humanReadableUpfrontVolumeControlWaitingTimeFromSeconds(Context context,
                                                                                 int seconds) {
        if (seconds == 0)
            return context.getString(R.string.com_kustomer_someone_should_be_with_you_momentarily);
        else {
            int count;
            @StringRes int stringId;

            if (seconds < SECONDS_PER_MINUTE) {
                count = seconds;
                stringId = seconds <= 1 ?
                        R.string.com_kustomer_your_expected_wait_time_is_param_second :
                        R.string.com_kustomer_your_expected_wait_time_is_param_seconds;

            } else if (seconds < SECONDS_PER_MINUTE * MINUTES_PER_HOUR) {
                count = (int) Math.ceil(seconds / SECONDS_PER_MINUTE);
                stringId = count <= 1 ?
                        R.string.com_kustomer_your_expected_wait_time_is_param_minute :
                        R.string.com_kustomer_your_expected_wait_time_is_param_minutes;

            } else if (seconds < SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY) {
                count = (int) Math.ceil(seconds / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR));
                stringId = count <= 1 ?
                        R.string.com_kustomer_your_expected_wait_time_is_param_hour :
                        R.string.com_kustomer_your_expected_wait_time_is_param_hours;

            } else {
                return context.getString(R.string.com_kustomer_your_expected_wait_time_is_greater_than_one_day);
            }

            return context.getString(stringId, count);
        }
    }

    public static String messageTimeStampTextFromDate(Date date) {

        if (date != null) {
            long now = System.currentTimeMillis();

            //2days
            if (now - date.getTime() <= TWO_DAYS_MILLIS) {
                return DateUtils.getRelativeTimeSpanString(date.getTime(),
                        now,
                        DateUtils.DAY_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE).toString() + shortTimeFormatter().format(date);
            } else
                return shortRelativeDateFormatter().format(date);
        } else {
            return "";
        }

    }

    public static Date dateFromString(String string) {
        if (string != null && string.length() > 0)
            try {
                return ISO8601DateFormatterFromString().parse(string);
            } catch (ParseException ignore) {
            }

        return null;
    }

    public static String stringFromDate(Date date) {
        if (date != null) {
            return ISO8601DateFormatterFromDate().format(date);
        } else
            return null;
    }
    //endregion

    //region Private Methods
    private static String textWithCountAndUnit(Context context, int unitCount, int unitString) {
        return String.format(Locale.getDefault(), "%d %s", unitCount,
                context.getString(unitString));
    }

    private static String agoWithTextCountAndUnit(long count, String unit) {
        int mCount = (int) count;
        if (mCount > 1)
            return String.format(Locale.getDefault(), "%d %ss ago", mCount, unit);
        else
            return String.format(Locale.getDefault(), "%d %s ago", mCount, unit);
    }

    private static DateFormat shortRelativeDateFormatter() {
        if (shortDateFormat == null) {
            shortDateFormat = new SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault());
        }

        return shortDateFormat;
    }

    private static DateFormat shortTimeFormatter() {
        if (shortTimeFormat == null) {
            shortTimeFormat = new SimpleDateFormat(", h:mm a", Locale.getDefault());
        }

        return shortTimeFormat;
    }

    private static DateFormat ISO8601DateFormatterFromString() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                new Locale("en_US_POSIX"));
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        return dateFormat;
    }

    private static DateFormat ISO8601DateFormatterFromDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                new Locale("en_US_POSIX"));
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        return dateFormat;
    }
    //endregion

}
