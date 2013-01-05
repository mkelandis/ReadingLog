package com.hintersphere.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.hintersphere.booklogger.BookLoggerException;

/**
 * Utility methods for Android DB Adapters.
 * 
 * @author mlandis
 */
public class DbAdapterUtil {

    private static final String DATE_FORMAT_SQLITE_UTC = "yyyy-MM-dd HH:mm:ss";

    /**
     * Get a list of columns for a table in sqlite.
     * @param db sqlite database to be queried
     * @param tableName to retrieve the columns for
     * @return list of columns 
     */
    public static List<String> getColumnsAsList(SQLiteDatabase db, String tableName) {

        List<String> ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("select * from " + tableName + " limit 1", null);
            if (c != null) {
                ar = new ArrayList<String>(Arrays.asList(c.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
    }

    /**
     * Parse a sqlite UTC date string into a java date.
     * @param dbDateStr to be parsed
     * @return java.util.date representation
     */
    public static Date toDate(String dbDateStr) {
        Date toDate = null;
        try {
            toDate = new SimpleDateFormat(DATE_FORMAT_SQLITE_UTC, Locale.getDefault()).parse(dbDateStr);
        } catch (Exception e) {
            throw new BookLoggerException("Could not parse date from db string: " + dbDateStr, e);
        }
        return toDate;
    }
    
    /**
     * Format a java date into a SQLite UTC date string.
     * @param date to be formatted
     * @return UTC String representation of the date.
     */
    public static String fromDate(Date date) {
        String utcDate = null;
        try {
            utcDate = new SimpleDateFormat(DATE_FORMAT_SQLITE_UTC, Locale.getDefault()).format(date);
        } catch (Exception e) {
            throw new BookLoggerException("Could not format date from Date: " + date, e);
        }
        return utcDate;
    }    

    /**
     * @param utcDateStr used by SQLite when text columns store timestamps
     * @param context android context used to identify user's locale
     * @return date formatted according to user's preference
     */
    public static String getDateInUserFormat(String utcDateStr, Context context) {
        // use a locale based string for the date
        DateFormat formatter = android.text.format.DateFormat.getDateFormat(context);
        return formatter.format(toDate(utcDateStr));
    }

}
