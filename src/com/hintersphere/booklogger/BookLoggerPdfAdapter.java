package com.hintersphere.booklogger;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class BookLoggerPdfAdapter {
	
	private static final SimpleDateFormat DATE_FORMAT_SQL = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final SimpleDateFormat DATE_FORMAT_PDF = new SimpleDateFormat("MM/dd/yy");

	private Context mCtx;
	private Document mDocument;
		
	public BookLoggerPdfAdapter(Context ctx) {

		super();
		mCtx = ctx;				
	}

	public File makePdf(String title, String subject, Cursor cursor) {
		
		if (cursor.getCount() <= 0) {
			throw new BookLoggerException("Cursor does not contain any fetched records.");
		}
		
		File outputFile = null;
		File sdDir = Environment.getExternalStorageDirectory(); 
		outputFile = new File(sdDir, "/booklog.pdf");

		mDocument = new Document();
		
		try {
			PdfWriter.getInstance(mDocument, new FileOutputStream(outputFile));
		} catch (Exception e) {
			throw new BookLoggerException("Error creating output stream for pdf email.", e);			
		}
		mDocument.open();
		
		// add metadata
		mDocument.addTitle(title);
		mDocument.addSubject(subject);
		mDocument.addKeywords(mCtx.getString(R.string.pdf_doc_keywords));
		mDocument.addAuthor(mCtx.getString(R.string.pdf_doc_author));
		mDocument.addCreator(mCtx.getString(R.string.pdf_doc_creator));
		
		// kick off the table
		float[] colswidth = {5f, 15f, 40f, 15f, 15f, 10f};
		PdfPTable table = new PdfPTable(colswidth);

		// t.setBorderColor(BaseColor.GRAY);
		// t.setPadding(4);
		// t.setSpacing(4);
		// t.setBorderWidth(1);

		PdfPCell c1 = new PdfPCell(new Phrase(mCtx.getString(R.string.pdf_col_num)));
		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(c1);

		c1 = new PdfPCell(new Phrase(mCtx.getString(R.string.pdf_col_date)));
		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(c1);

		c1 = new PdfPCell(new Phrase(mCtx.getString(R.string.pdf_col_title)));
		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(c1);

		c1 = new PdfPCell(new Phrase(mCtx.getString(R.string.pdf_col_author)));
		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(c1);

		c1 = new PdfPCell(new Phrase(mCtx.getString(R.string.pdf_col_activity)));
		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(c1);

		c1 = new PdfPCell(new Phrase(mCtx.getString(R.string.pdf_col_initials)));
		c1.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(c1);

		table.setHeaderRows(1);

		while (cursor.moveToNext()) {

			// assemble the components of the record...
			table.addCell("" + (cursor.getPosition() + 1)); // 0 indexed
			table.addCell(formatDate(cursor.getString(cursor
					.getColumnIndex(BookLoggerDbAdapter.DB_COL_CREATEDT))));
			table.addCell(cursor.getString(cursor
							.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE)));
			table.addCell(cursor
					.getString(cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR)));
			switch (cursor.getInt(cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY))) {
			case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ:
				table.addCell(mCtx.getString(R.string.menu_childread));
				break;
			case BookLoggerDbAdapter.DB_ACTIVITY_PARENT_READ:
				table.addCell(mCtx.getString(R.string.menu_parentread));
				break;
			case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_PARENT_READ:
				table.addCell(mCtx.getString(R.string.menu_parentchildread));
				break;
			}
			
			// empty cell for the initials
			table.addCell("");			
		}		

		try {
			mDocument.add(table);
		} catch (DocumentException e) {
			throw new BookLoggerException("Could not add table to the document.", e);
		}
		
		mDocument.close();
		return outputFile;
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
