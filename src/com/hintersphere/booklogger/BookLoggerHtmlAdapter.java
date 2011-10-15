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

		    writer.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/1999/REC-html401-19991224/loose.dtd\">\n");
		    writer.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n\n");
		    writer.append("<html>\n");
		    writer.append("<head><title>").append(title).append("</title></head>\n");
		    writer.append("<body style=\"margin:8px; font-family:Helvetica; background-color:#ffffff;\">\n");
		    
		    writer.append("<!-- SUMMARY TABLE -->\n");
		    writer.append("<table style=\"margin-bottom: 8px; font-size:18; font-weight:bold;\">\n");
		    writer.append("<tr>");
		    summaryHeaderCell(writer, mCtx.getString(R.string.pdf_summary_instructor));
			writer.append("<td width=\"70%\" style=\"border-bottom: 2px solid #000000;\">&nbsp;</td>");
		    writer.append("</tr>");
		    writer.append("<tr>");
		    summaryHeaderCell(writer, mCtx.getString(R.string.pdf_summary_student));
			writer.append("<td style=\"border-bottom: 2px solid #000000;\">&nbsp;</td>");
		    writer.append("</tr>");
		    writer.append("<tr><th align=\"left\" colspan=\"2\">");
		    writer.append(mCtx.getString(R.string.pdf_summary_total) + " " + cursor.getCount());
		    writer.append("</th></tr>");
		    writer.append("</table>");

		    writer.append("<!-- DATA TABLE -->\n");
		    writer.append("<table style=\"border-collapse: collapse;\" cellspacing=\"0\">\n");
		    writer.append("<tr style=\"border: 1px solid #dddddd; font-size: 12px; background-color:#dddddd;\">\n");
		    headerCell(writer, mCtx.getString(R.string.pdf_col_num), "5%");
		    headerCell(writer, mCtx.getString(R.string.pdf_col_date), "10%");
		    headerCell(writer, mCtx.getString(R.string.pdf_col_title), "40%");
		    headerCell(writer, mCtx.getString(R.string.pdf_col_author), "20%");
		    headerCell(writer, mCtx.getString(R.string.pdf_col_activity), "15%");
		    headerCell(writer, mCtx.getString(R.string.pdf_col_initials), "10%");
		    writer.append("</tr>");
		    
			while (cursor.moveToNext()) {
			    writer.append("<tr style=\"font-size: 12px;\" >\n");
			    cell(writer, "" + (cursor.getPosition() + 1));
				cell(writer, formatDate(cursor.getString(cursor
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_CREATEDT))), "center");
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
			    cell(writer, "");  // empty cell for initials
			    writer.append("</tr>");
			}
			
			// footer line
			writer.append("<tr><td align=\"right\" colspan=\"6\" style=\"font-size:8px; font-style:italic\">"
							+ mCtx.getString(R.string.pdf_footer_tagline) + "</td></tr>\n");
		    writer.append("</table>\n");
		    writer.append("</body>\n</html>\n");		    
		    writer.close();
		} catch (Exception e) {
			throw new BookLoggerException("Error writing file.", e);
		}
		
		return outputFile;
	}
	
	private void summaryHeaderCell(Writer writer, String contents) throws IOException {
		writer.append("<th width=\"20%\" align=\"left\">");
		writer.append(contents);
		writer.append("</th>");
	}
	private void headerCell(Writer writer, String contents, String width)
			throws IOException {
		writer.append("<th width=\"" + width + "\">");
		writer.append(contents);
		writer.append("</th>");
	}
	private void cell(Writer writer, String contents) throws IOException {
		cell(writer, contents, "left");
	}
	private void cell(Writer writer, String contents, String align) throws IOException {
		writer.append("<td style=\"border: 1px solid #dddddd;\" align=\"" + align + "\">");
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
