package com.hintersphere.booklogger;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

import com.hintersphere.util.DbAdapterUtil;

import java.io.File;
import java.util.Set;

/**
 * Common functionality for generating reports.
 */
public abstract class ReportAdapter {

    protected Context mCtx;
    protected Set<String> mKeywords;

    protected String getReadBy(ReadBy readBy) {
        switch (readBy) {
            case CHILD:
                return mCtx.getString(R.string.report_val_readbychild);
            case PARENT:
                return mCtx.getString(R.string.report_val_readbyparent);
            case CHILD_PARENT:
                return mCtx.getString(R.string.report_val_readbyparentchild);
            default:
                return mCtx.getString(R.string.report_val_readbyme);
        }
    }

    protected File getOutputFile(String filename) {
        File sdDir = Environment.getExternalStorageDirectory();
        return new File(sdDir, "/" + filename);
    }

    protected String getMinutes(Cursor listCursor) {
        int minutes = listCursor.getInt(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_MINUTES));
        if (minutes <= 0) {
            return mCtx.getString(R.string.report_val_minutes_na);
        } else {
            return BookLoggerUtil.formatMinutes(minutes);
        }
    }

    protected String getPagesRead(Cursor listCursor) {
        int pagesRead = listCursor.getInt(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_PAGESREAD));
        if (pagesRead <= 0) {
            return mCtx.getString(R.string.report_val_pagesread_na);
        } else {
            return String.valueOf(pagesRead);
        }
    }

    protected String getComment(Cursor listCursor) {
        String comment = listCursor.getString(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_COMMENT));
        return comment != null ? comment : "";
    }

    protected String getReadBy(Cursor listCursor) {
        int readById = listCursor.getInt(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY));
        return getReadBy(ReadBy.getById[readById]);
    }

    protected String getAuthor(Cursor listCursor) {
        return listCursor.getString(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR));
    }

    protected String getTitle(Cursor listCursor) {
        return listCursor.getString(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE));
    }

    protected String getDateRead(Cursor listCursor) {
        return DbAdapterUtil.getDateInUserFormat(
                listCursor.getString(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_DATEREAD)), mCtx);
    }

    protected String getBookIndex(Cursor listCursor) {
        return "" + (listCursor.getPosition() + 1);
    }
}
