package com.hintersphere.booklogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

import com.hintersphere.util.DbAdapterUtil;

public class BookLoggerHtmlAdapter {

	private Context mCtx;
	private Set<String> mKeywords;

	public BookLoggerHtmlAdapter(Context ctx, Set<String> keywords) {
		super();
		mCtx = ctx;
		mKeywords = keywords;
	}

	public File makeHtml(String title, String subject, Cursor listCursor, Cursor statsCursor) {

		if (listCursor.getCount() <= 0) {
			throw new BookLoggerException("Cursor does not contain any fetched records.");
		}

		// calculate stats
		int totalMinutes = 0;
		int totalPages = 0;
		if (statsCursor.isBeforeFirst() && statsCursor.moveToFirst()) {
			totalMinutes = statsCursor.getInt(0);
			totalPages = statsCursor.getInt(1);
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
		    writer.append("<table style=\"margin-bottom: 8px; font-size:16; font-weight:bold;\">\n");

		    writer.append("<tr>");
		    summaryHeaderCell(writer, mCtx.getString(R.string.pdf_summary_instructor));
			writer.append("<td align=\"left\" width=\"37%\" style=\"border-bottom: 2px solid #000000;\">&nbsp;</td>");
            writer.append("<td width=\"3%\">&nbsp;</td>");
            writer.append("<th width=\"60%\" align=\"left\">");
            writer.append(mCtx.getString(R.string.pdf_summary_total) + "&nbsp;" + listCursor.getCount());
            writer.append("</th>");
			writer.append("</tr>");

		    writer.append("<tr>");
		    summaryHeaderCell(writer, mCtx.getString(R.string.pdf_summary_student));
			writer.append("<td align=\"left\" style=\"border-bottom: 2px solid #000000;\">&nbsp;</td>");
            writer.append("<td>&nbsp;</td>");
			writer.append("<th align=\"left\">");
			if (totalPages > 0) {
				writer.append(mCtx.getString(R.string.pdf_summary_totalpages) + "&nbsp;" + totalPages);
			}
			if (totalMinutes > 0) {
				writer.append((totalPages > 0 ? ", " : "") + mCtx.getString(R.string.pdf_summary_totalminutes)
						+ "&nbsp;" + BookLoggerUtil.formatMinutes(totalMinutes));
			}
            writer.append("</th></tr>");
		    writer.append("</table>");

		    writer.append("<!-- DATA TABLE -->\n");
		    writer.append("<table style=\"border-collapse: collapse;\" cellspacing=\"0\">\n");
		    writer.append("<tr style=\"border: 1px solid #dddddd; font-size: 12px; background-color:#dddddd;\">\n");
		    headerCell(writer, mCtx.getString(R.string.report_col_num), "5%");
		    headerCell(writer, mCtx.getString(R.string.report_col_date), "10%");
		    headerCell(writer, mCtx.getString(R.string.report_col_title), "20%");
		    headerCell(writer, mCtx.getString(R.string.report_col_author), "15%");
		    headerCell(writer, mCtx.getString(R.string.report_col_activity), "10%");
			headerCell(writer, mCtx.getString(R.string.report_col_pages), "5%");
			headerCell(writer, mCtx.getString(R.string.report_col_minutes), "5%");
            headerCell(writer, mCtx.getString(R.string.report_col_comment), "30%");
		    headerCell(writer, mCtx.getString(R.string.report_col_initials), "5%");
		    writer.append("</tr>");		    
		            
		    while (listCursor.moveToNext()) {
				
				String booktitle = listCursor.getString(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE));
				String author = listCursor.getString(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR));
                String comment = listCursor.getString(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_COMMENT));
				int minutes = listCursor.getInt(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_MINUTES));
                int pagesRead = listCursor.getInt(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_PAGESREAD));
				int readById = listCursor.getInt(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY));

				// append some keywords
				mKeywords.add(booktitle);
				mKeywords.add(author);
				
			    writer.append("<tr style=\"font-size: 12px;\" >\n");
			    cell(writer, "" + (listCursor.getPosition() + 1));
                cell(writer, DbAdapterUtil.getDateInUserFormat(
								listCursor.getString(listCursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_DATEREAD)), mCtx),
						"center");
				cell(writer, booktitle);
			    cell(writer, author);
				cell(writer, getReadBy(ReadBy.getById[readById]));

				if (pagesRead <= 0) {
					cell(writer, mCtx.getString(R.string.detail_hint_pagesread));
				} else {
					cell(writer, String.valueOf(pagesRead));
				}

				if (minutes <= 0) {
	                cell(writer, mCtx.getString(R.string.detail_hint_minutes));				    
				} else {
                    cell(writer, BookLoggerUtil.formatMinutes(minutes));                 				    
				}
				
				
                cell(writer, comment != null ? comment : "");
			    cell(writer, "");  // empty cell for initials
			    writer.append("</tr>");
			}
			
			// footer line
			writer.append("<tr><td align=\"right\" colspan=\"9\" style=\"font-size:8px; font-style:italic\">"
							+ mCtx.getString(R.string.report_footer_tagline) + "</td></tr>\n");
		    writer.append("</table>\n");
		    writer.append("</body>\n</html>\n");		    
		    writer.close();
		} catch (Exception e) {
			throw new BookLoggerException("Error writing file.", e);
		}
		
		return outputFile;
	}

	private String getReadBy(ReadBy readBy) {
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
	
	private String getTotalMinutes(Cursor cursor) {
        int totalminutes = 0;
        while (cursor.moveToNext()) {
            int minutes = cursor.getInt(cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_MINUTES));
            if (minutes > 0) {
                totalminutes += minutes;
            }
        }
        cursor.moveToPosition(-1);
        
        if (totalminutes == 0) {
            return  mCtx.getString(R.string.detail_hint_minutes);
        }        
        return BookLoggerUtil.formatMinutes(totalminutes);
	}
}
