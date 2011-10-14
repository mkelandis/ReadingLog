package com.hintersphere.booklogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

public class BookLoggerHtmlAdapter {

	private static final SimpleDateFormat DATE_FORMAT_SQL = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	private static final SimpleDateFormat DATE_FORMAT_PDF = new SimpleDateFormat("MM/dd/yy");

	private Context mCtx;

	public BookLoggerHtmlAdapter(Context ctx) {
		super();
		mCtx = ctx;
	}

	public File makeHtml(String title, String subject, Cursor cursor) {

		if (cursor.getCount() <= 0) {
			throw new BookLoggerException("Cursor does not contain any fetched records.");
		}

		File outputFile = null;
		File sdDir = Environment.getExternalStorageDirectory();
		outputFile = new File(sdDir, "/booklog.html");

		// // add metadata
		// mDocument.addTitle(title);
		// mDocument.addSubject(subject);
		// mDocument.addKeywords(mCtx.getString(R.string.pdf_doc_keywords));
		// mDocument.addAuthor(mCtx.getString(R.string.pdf_doc_author));
		// mDocument.addCreator(mCtx.getString(R.string.pdf_doc_creator));

		try {
		    Writer writer = new BufferedWriter(new FileWriter(outputFile));

		    writer.append("<html>\n");
		    writer.append("<title>").append(title).append("</title>\n");
		    writer.append("<body>\n");
		    
		    writer.append("<!-- SUMMARY TABLE -->\n");
		    writer.append("<table>\n");
		    writer.append("<tr>");
		    summaryHeaderCell(writer, mCtx.getString(R.string.pdf_summary_instructor));
		    summaryCell(writer, "____________________");
		    writer.append("</tr>");
		    writer.append("<tr>");
		    summaryHeaderCell(writer, mCtx.getString(R.string.pdf_summary_student));
		    summaryCell(writer, "____________________");
		    writer.append("</tr>");
		    writer.append("<tr>");
		    summaryHeaderCell(writer, mCtx.getString(R.string.pdf_summary_total));
		    summaryCell(writer, "" + cursor.getCount());
		    writer.append("</tr>");
		    writer.append("</table>");

		    writer.append("<!-- DATA TABLE -->\n");
		    writer.append("<table>\n");
		    writer.append("<tr>\n");
		    headerCell(writer, mCtx.getString(R.string.pdf_col_date));
		    headerCell(writer, mCtx.getString(R.string.pdf_col_title));
		    headerCell(writer, mCtx.getString(R.string.pdf_col_author));
		    headerCell(writer, mCtx.getString(R.string.pdf_col_activity));
		    headerCell(writer, mCtx.getString(R.string.pdf_col_initials));
		    writer.append("</tr>");
		    
			while (cursor.moveToNext()) {
			    writer.append("<tr>\n");
				cell(writer, formatDate(cursor.getString(cursor
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_CREATEDT))));
				cell(writer, cursor.getString(cursor
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE)));
			    cell(writer, cursor.getString(cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR)));
				switch (cursor.getInt(cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY))) {
				case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ:
					cell(writer, mCtx.getString(R.string.context_menu_childread));
					break;
				case BookLoggerDbAdapter.DB_ACTIVITY_PARENT_READ:
					cell(writer, mCtx.getString(R.string.context_menu_parentread));
					break;
				case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_PARENT_READ:
					cell(writer, mCtx.getString(R.string.context_menu_parentchildread));
					break;
				}
			    writer.append("</tr>");
			}
			
		    writer.append("</table>\n");
		    writer.append("</body>\n</html>\n");		    
		    writer.close();
		} catch (Exception e) {
			throw new BookLoggerException("Error writing file.", e);
		}
		
		return outputFile;
	}
	
	private void summaryHeaderCell(Writer writer, String contents) throws IOException {
		writer.append("<th>");
		writer.append(contents);
		writer.append("</th>");
	}
	private void summaryCell(Writer writer, String contents) throws IOException {
		writer.append("<td>");
		writer.append(contents);
		writer.append("</d>");
	}
	private void headerCell(Writer writer, String contents) throws IOException {
		writer.append("<th>");
		writer.append(contents);
		writer.append("</th>");
	}
	private void cell(Writer writer, String contents) throws IOException {
		writer.append("<td>");
		writer.append(contents);
		writer.append("</d>");
	}
	private String formatDate(String date) {
		Date origDate = null;
		try {
			origDate = DATE_FORMAT_SQL.parse(date);
		} catch (Exception e) {
			throw new BookLoggerException("Could not parse create date from db", e);
		}
		return DATE_FORMAT_PDF.format(origDate);
	}

}
