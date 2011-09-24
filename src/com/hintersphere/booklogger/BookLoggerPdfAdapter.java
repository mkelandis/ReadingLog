package com.hintersphere.booklogger;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import crl.android.pdfwriter.PDFWriter;
import crl.android.pdfwriter.PaperSize;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

public class BookLoggerPdfAdapter {
	
	private static final SimpleDateFormat DATE_FORMAT_SQL = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final SimpleDateFormat DATE_FORMAT_PDF = new SimpleDateFormat("MM/dd/yy");

	private Context mCtx;
		
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
		PDFWriter pdfWriter = new PDFWriter(PaperSize.LETTER_WIDTH, PaperSize.LETTER_WIDTH);

		
//		// add metadata
//		mDocument.addTitle(title);
//		mDocument.addSubject(subject);
//		mDocument.addKeywords(mCtx.getString(R.string.pdf_doc_keywords));
//		mDocument.addAuthor(mCtx.getString(R.string.pdf_doc_author));
//		mDocument.addCreator(mCtx.getString(R.string.pdf_doc_creator));
//
//		// Summary table for entering name and teacher's name
//		float[] colswidth = {20f, 70f};
//		PdfPTable table = new PdfPTable(colswidth);
//		table.setHorizontalAlignment(Element.ALIGN_LEFT);
//		table.setWidthPercentage(70f);
//		table.setSpacingAfter(18);
//		
//		// Font for styling summary cells
//		Font summaryFont = new Font(FontFamily.HELVETICA, 18, Font.BOLD);
//		PdfPCell cell = new PdfPCell(new Phrase(mCtx.getString(R.string.pdf_summary_instructor),
//				summaryFont));
//		cell.setBorder(PdfPCell.NO_BORDER);
//		cell.setHorizontalAlignment(Element.ALIGN_LEFT);		
//		table.addCell(cell);
//		PdfPCell summaryValueCell = new PdfPCell(new Phrase("", summaryFont));
//		summaryValueCell.setBorder(PdfPCell.BOTTOM);
//		summaryValueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
//		table.addCell(summaryValueCell);
//		cell.setPhrase(new Phrase(mCtx.getString(R.string.pdf_summary_student), summaryFont));
//		table.addCell(cell);		
//		table.addCell(summaryValueCell);		
//		cell.setPhrase(new Phrase(mCtx.getString(R.string.pdf_summary_total) + " "
//				+ cursor.getCount(), summaryFont));
//		cell.setColspan(2);
//		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
//		
//		table.addCell(cell);
//
//		try {
//			mDocument.add(table);
//		} catch (DocumentException e) {
//			throw new BookLoggerException("Could not add summary table to the document.", e);
//		}
//
//		
//		// kick off the table
//		float[] colswidth2 = {5f, 10f, 40f, 20f, 15f, 10f};
//		table = new PdfPTable(colswidth2);
//		table.setHorizontalAlignment(Element.ALIGN_LEFT);
//		table.setWidthPercentage(100f);
//		table.setSpacingAfter(8);
//		
//		// Font for styling header cells
//		Font headerFont = new Font(FontFamily.HELVETICA, 12, Font.BOLD);
//
//		// Font for styling list cells
//		Font listFont = new Font(FontFamily.HELVETICA, 10, Font.NORMAL);
//
//		cell = new PdfPCell(new Phrase(mCtx.getString(R.string.pdf_col_num), headerFont));
//		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//		cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
//		cell.setBorderColor(BaseColor.LIGHT_GRAY);
//		table.addCell(cell);
//
//		cell.setPhrase(new Phrase(mCtx.getString(R.string.pdf_col_date), headerFont));
//		table.addCell(cell);
//
//		cell.setPhrase(new Phrase(mCtx.getString(R.string.pdf_col_title), headerFont));
//		table.addCell(cell);
//
//		cell.setPhrase(new Phrase(mCtx.getString(R.string.pdf_col_author), headerFont));
//		table.addCell(cell);
//
//		cell.setPhrase(new Phrase(mCtx.getString(R.string.pdf_col_activity), headerFont));
//		table.addCell(cell);
//
//		cell.setPhrase(new Phrase(mCtx.getString(R.string.pdf_col_initials), headerFont));
//		table.addCell(cell);
//
//		table.setHeaderRows(1);
//		cell = new PdfPCell();
//		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
//		cell.setBorderColor(BaseColor.LIGHT_GRAY);
//		
//		while (cursor.moveToNext()) {
//
//			// assemble the components of the record...
//			cell.setPhrase(new Phrase("" + (cursor.getPosition() + 1), listFont));
//			table.addCell(cell); // 0 indexed
//			cell.setPhrase(new Phrase(formatDate(cursor.getString(cursor
//					.getColumnIndex(BookLoggerDbAdapter.DB_COL_CREATEDT))), listFont));
//			cell.setHorizontalAlignment(Element.ALIGN_CENTER);			
//			table.addCell(cell);
//			
//			cell.setPhrase(new Phrase(cursor.getString(cursor
//					.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE)), listFont));
//			cell.setHorizontalAlignment(Element.ALIGN_LEFT);			
//			table.addCell(cell);
//			
//			cell.setPhrase(new Phrase(cursor
//					.getString(cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR)), listFont));
//			table.addCell(cell);
//			
//			switch (cursor.getInt(cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY))) {
//			case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ:
//				cell.setPhrase(new Phrase(mCtx.getString(R.string.context_menu_childread), listFont));
//				break;
//			case BookLoggerDbAdapter.DB_ACTIVITY_PARENT_READ:
//				cell.setPhrase(new Phrase(mCtx.getString(R.string.context_menu_parentread), listFont));
//				break;
//			case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_PARENT_READ:
//				cell.setPhrase(new Phrase(mCtx.getString(R.string.context_menu_parentchildread), listFont));
//				break;
//			}
//
//			table.addCell(cell);
//
//			// empty cell for the initials
//			cell.setPhrase(new Phrase("", listFont));
//			table.addCell(cell);			
//		}		
//
//		try {
//			mDocument.add(table);
//		} catch (DocumentException e) {
//			throw new BookLoggerException("Could not add list table to the document.", e);
//		}
//
//		// Font for styling list cells
//		Font footerFont = new Font(FontFamily.HELVETICA, 8, Font.ITALIC);
//		cell = new PdfPCell(new Phrase(mCtx.getString(R.string.pdf_footer_tagline), footerFont));
//		cell.setBorder(PdfPCell.NO_BORDER);
//		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
//
//		table = new PdfPTable(1);
//		table.setHorizontalAlignment(Element.ALIGN_RIGHT);
//		table.setWidthPercentage(100f);
//		table.addCell(cell);
//		try {
//			mDocument.add(table);
//		} catch (DocumentException e) {
//			throw new BookLoggerException("Could not add footer table to the document.", e);
//		}
//		
//		mDocument.close();
//		
//        try {
//        	FileOutputStream pdfFile = new FileOutputStream(newFile);
//        	pdfFile.write(pdfContent.getBytes(encoding));
//            pdfFile.close();
//        } catch(FileNotFoundException e) {
//        	//
//        }

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
