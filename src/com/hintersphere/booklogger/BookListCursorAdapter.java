package com.hintersphere.booklogger;

import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hintersphere.util.BitmapManager;

/**
 * This cursor adapter pulls resource strings for the book activity and
 * retrieves the thumbnail image from the web (cached if we have a local copy -
 * caching is handled by RestHelper call to connection.setUseCaches())
 * 
 * @author Michael Landis
 */
public class BookListCursorAdapter extends CursorAdapter {

	private LayoutInflater mInflater;
	private int mColIdxTitle;
	private int mColIdxAuthor;
	private int mColIdxReadDate;
	private int mColIdxThumb;	
	private Cursor mCur;
	private Context mParentContext;

	// keep a reference to the views so they can be lazily loaded
	static class ViewHolder {
		TextView title = null;
		TextView author = null;
		TextView readDate = null;
		ImageView thumbnail = null;
	}
		
	public BookListCursorAdapter(Context context, Cursor cursor) {
		super(context, cursor);
		mInflater = LayoutInflater.from(context);
		mColIdxTitle = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE);
		mColIdxAuthor = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR);
		mColIdxReadDate = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_DATEREAD);
		mColIdxThumb = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_THUMB);
		mCur = cursor;
		init(context);
		mParentContext = context;
	}

	public BookListCursorAdapter(Context context, Cursor cursor, boolean autoRequery) {
		super(context, cursor, autoRequery);
		mInflater = LayoutInflater.from(context);
		mColIdxTitle = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE);
		mColIdxAuthor = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR);
        mColIdxReadDate = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_DATEREAD);
		mColIdxThumb = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_THUMB);
		mCur = cursor;
		init(context);
        mParentContext = context;
	}
	
	private void init(Context context) {		  
        BitmapManager.INSTANCE.setPlaceholder(BitmapFactory.decodeResource(context.getResources(),
                R.drawable.defbookcover));
	}
	
	
	/**
	 * Should we show the index of the book in the view? --Right now I think it's ok without.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder viewHolder;
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.main_row, null);
            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
            viewHolder.author = (TextView) convertView.findViewById(R.id.author);
            viewHolder.readDate = (TextView) convertView.findViewById(R.id.read_date);
            viewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.bookthumb);
            convertView.setTag(viewHolder);
        } else {
        	viewHolder = (ViewHolder) convertView.getTag();
        }
        
        mCur.moveToPosition(position);
        
		// get the title and author
		viewHolder.title.setText(mCur.getString(mColIdxTitle));
		viewHolder.author.setText(mCur.getString(mColIdxAuthor));
        viewHolder.readDate.setText(getSQLiteDateAsString(mCur.getLong(mColIdxReadDate)));
		
		// handle the book thumbnail
		String imageUrl = mCur.getString(mColIdxThumb);
		viewHolder.thumbnail.setTag(imageUrl);  
		BitmapManager.INSTANCE.loadBitmap(imageUrl, viewHolder.thumbnail, 75, 75);  
		
        return convertView;
	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
		return null;
	}

	@Override
	public void bindView(View arg0, Context arg1, Cursor arg2) {
	}
	
	private String getSQLiteDateAsString(Long dateToSecs) {
        // use a locale based string for the date
        DateFormat formatter = android.text.format.DateFormat.getDateFormat(mParentContext);
        Date dateObj = new Date(dateToSecs * 1000);
        return formatter.format(dateObj);
	}
}
