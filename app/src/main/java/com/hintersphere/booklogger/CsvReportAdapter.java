package com.hintersphere.booklogger;

import android.content.Context;
import android.database.Cursor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Generate a CSV version of the book log report.
 */
public class CsvReportAdapter extends ReportAdapter {

    public CsvReportAdapter(Context ctx) {
        mCtx = ctx;
    }

    public File makeCsv(Cursor listCursor) {

        if (listCursor.getCount() <= 0) {
            throw new BookLoggerException("Cursor does not contain any fetched records.");
        }

        File csvFile = getOutputFile("readinglog.csv");
        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(csvFile));
            writer.writeNext(new String[] {
                    mCtx.getString(R.string.report_col_num),
                    mCtx.getString(R.string.report_col_date),
                    mCtx.getString(R.string.report_col_title),
                    mCtx.getString(R.string.report_col_author),
                    mCtx.getString(R.string.report_col_activity),
                    mCtx.getString(R.string.report_col_pages),
                    mCtx.getString(R.string.report_col_minutes),
                    mCtx.getString(R.string.report_col_comment)
            });

            while (listCursor.moveToNext()) {
                writer.writeNext(new String[] {
                        getBookIndex(listCursor),
                        getDateRead(listCursor),
                        getTitle(listCursor),
                        getAuthor(listCursor),
                        getReadBy(listCursor),
                        getPagesRead(listCursor),
                        getMinutes(listCursor),
                        getComment(listCursor)
                });
            }
        } catch (IOException e) {
            throw new BookLoggerException("Error writing file.", e);
        } finally {
            BookLoggerUtil.closeQuietly(writer);
        }

        return csvFile;
    }
}
