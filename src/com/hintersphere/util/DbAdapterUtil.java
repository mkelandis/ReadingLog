package com.hintersphere.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

	public static Date toDate(String dbDateStr) {
		Date toDate = null;
		try {
			toDate = SimpleDateFormat.getDateTimeInstance().parse(dbDateStr);
		} catch (Exception e) {
			throw new BookLoggerException("Could not parse create date from db", e);
		}
		return toDate;
	}

	public static String fromDate(Date date) {
		String dbDateStr = null;
		try {
			dbDateStr = SimpleDateFormat.getDateTimeInstance().format(date);
		} catch (Exception e) {
			throw new BookLoggerException("Could not parse create date from db", e);
		}
		return dbDateStr;
	}

}
