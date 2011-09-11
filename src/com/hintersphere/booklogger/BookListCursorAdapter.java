package com.hintersphere.booklogger;

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
import com.hintersphere.util.RestHelper;

/**
 * This cursor adapter pulls resource strings for the book activity and
 * retrieves the thumbnail image from the web (cached if we have a local copy -
 * caching is handled by RestHelper call to connection.setUseCaches())
 * 
 * @author Michael Landis
 */
public class BookListCursorAdapter extends CursorAdapter {

	/**
	 * TODO::Use the resthelper instead
	 */
	private RestHelper mRestHelper = new RestHelper();
	private LayoutInflater mInflater;
	private int mColIdxTitle;
	private int mColIdxAuthor;
	private int mColIdxActivity;
	private int mColIdxThumb;	
	private Cursor mCur;

	// keep a reference to the views so they can be lazily loaded
	static class ViewHolder {
		TextView title = null;
		TextView author = null;
		TextView activity = null;
		ImageView thumbnail = null;
	}
		
	public BookListCursorAdapter(Context context, Cursor cursor) {
		super(context, cursor);
		mInflater = LayoutInflater.from(context);
		mColIdxTitle = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE);
		mColIdxAuthor = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR);
		mColIdxActivity = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY);
		mColIdxThumb = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_THUMB);
		mCur = cursor;
		init(context);
	}

	public BookListCursorAdapter(Context context, Cursor cursor, boolean autoRequery) {
		super(context, cursor, autoRequery);
		mInflater = LayoutInflater.from(context);
		mColIdxTitle = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE);
		mColIdxAuthor = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR);
		mColIdxActivity = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY);
		mColIdxThumb = cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_THUMB);
		mCur = cursor;
		init(context);
	}
	
	private void init(Context context) {		  
        BitmapManager.INSTANCE.setPlaceholder(BitmapFactory.decodeResource(  
                context.getResources(), R.drawable.icon));  
	}
	
	
	/**
	 * TODO::Should we show the index of the book in the view?
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder viewHolder;
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.main_row, null);
            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) convertView.findViewById(R.id.title);
            viewHolder.author = (TextView) convertView.findViewById(R.id.author);
            viewHolder.activity = (TextView) convertView.findViewById(R.id.activity);
            viewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.bookthumb);
            convertView.setTag(viewHolder);
        } else {
        	viewHolder = (ViewHolder) convertView.getTag();
        }
        
        mCur.moveToPosition(position);
        
		// get the title and author
		viewHolder.title.setText(mCur.getString(mColIdxTitle));
		viewHolder.author.setText(mCur.getString(mColIdxAuthor));
		
		// handle the activity text - db stores key and not application resource name
		switch (mCur.getInt(mColIdxActivity)) {
			case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ:
				viewHolder.activity.setText(R.string.context_menu_childread);
				break;
			case BookLoggerDbAdapter.DB_ACTIVITY_PARENT_READ:
				viewHolder.activity.setText(R.string.context_menu_parentread);
				break;
			case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_PARENT_READ:
				viewHolder.activity.setText(R.string.context_menu_parentchildread);
				break;
		}

		// handle the book thumbnail
		String imageUrl = mCur.getString(mColIdxThumb);
//		if (imageUrl != null && !"".equals(imageUrl)) {
			viewHolder.thumbnail.setTag(imageUrl);  
			BitmapManager.INSTANCE.loadBitmap(imageUrl, viewHolder.thumbnail, 75, 75);  
//		}
		
        return convertView;
	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
		return null;
	}

	@Override
	public void bindView(View arg0, Context arg1, Cursor arg2) {
	}
}
