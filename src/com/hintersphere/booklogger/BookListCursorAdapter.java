package com.hintersphere.booklogger;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.hintersphere.util.RestHelper;

/**
 * This cursor adapter pulls resource strings for the book activity and
 * retrieves the thumbnail image from the web (cached if we have a local copy -
 * caching is handled by RestHelper call to connection.setUseCaches())
 * 
 * @author Michael Landis
 */
public class BookListCursorAdapter extends SimpleCursorAdapter {

	private RestHelper mRestHelper = new RestHelper();

	public BookListCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
		super(context, layout, c, from, to);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		// do the simple bindings
		super.bindView(view, context, cursor);

		// handle the activity text
		TextView activityTextView = (TextView) view.findViewById(R.id.activity);

		switch (cursor.getInt(cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY))) {
			case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ:
				activityTextView.setText(R.string.menu_childread);
				break;
			case BookLoggerDbAdapter.DB_ACTIVITY_PARENT_READ:
				activityTextView.setText(R.string.menu_parentread);
				break;
			case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_PARENT_READ:
				activityTextView.setText(R.string.menu_parentchildread);
				break;
		}

		// handle the book thumbnail
		String imageUrl = cursor.getString(cursor.getColumnIndex(BookLoggerDbAdapter.DB_COL_THUMB));
		Bitmap bitmap = mRestHelper.getBitmap(imageUrl);
		ImageView thumbnailView = (ImageView) view.findViewById(R.id.bookthumb);
		thumbnailView.setImageBitmap(bitmap);
	}

}
