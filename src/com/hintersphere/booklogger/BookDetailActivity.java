package com.hintersphere.booklogger;

import com.hintersphere.util.BaseDbAdapter;

import android.app.ExpandableListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BookDetailActivity extends ExpandableListActivity {

	private static final String CLASSNAME = BookDetailActivity.class.getName();
	
    private TextView mTitle;
    private TextView mAuthor;
    private Long mRowId;
    private BookDetail mBookDetail;
        
    private BookLoggerDbAdapter mDbHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {    	

    	super.onCreate(savedInstanceState);
        
        mDbHelper = new BookLoggerDbAdapter(this);
        mDbHelper.open();
        
        setContentView(R.layout.detail);
        setTitle(R.string.booklist_edit_title);
        
        mTitle = (TextView) findViewById(R.id.title);
        mAuthor = (TextView) findViewById(R.id.author);
        mBookDetail = new BookDetail();

        Button backButton = (Button) findViewById(R.id.back);
        
		mRowId = (savedInstanceState == null) ? null : (Long) savedInstanceState
				.getSerializable(BookLoggerDbAdapter.DB_COL_ID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(BookLoggerDbAdapter.DB_COL_ID) : null;
		}
		
		populateFields();
		
        backButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        	    setResult(RESULT_OK);
        	    finish();
        	}
        });
                
        // need to set a list adapter for the expandable list
        this.setListAdapter(new BookDetailExpandableListAdapter(this, mBookDetail));
    } 
    
    /**
     * populate the book list entry data from the db so we can edit it.
     */
    private void populateFields() {
        if (mRowId != null) {
			Cursor listentry = mDbHelper.fetchListEntry(mRowId);
			if (listentry.isBeforeFirst() && listentry.moveToFirst()) {
				// these fields are used directly in this activity
				mTitle.setText(listentry.getString(listentry
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE)));
				mAuthor.setText(listentry.getString(listentry
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR)));
				
				// book detail data used by expandable form...
				mBookDetail.activity = listentry.getShort(listentry
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY));
				mBookDetail.dateRead = BaseDbAdapter.toDate(listentry.getString(listentry
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_CREATEDT)));
				mBookDetail.chapterBegin = listentry.getString(listentry
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_CHAPTERBEG));
				mBookDetail.chapterEnd = listentry.getString(listentry
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_CHAPTEREND));
				mBookDetail.pagesBegin = listentry.getString(listentry
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_PAGESBEG));
				mBookDetail.pagesEnd = listentry.getString(listentry
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_PAGESEND));
				mBookDetail.comments = listentry.getString(listentry
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_COMMENTS));
				listentry.close();
			}
		}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putSerializable(BookLoggerDbAdapter.DB_COL_ID, mRowId);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	/**
	 * Persist the book detail information in the database
	 */
	private void saveState() {
    	
		BookDetailExpandableListAdapter adapter = (BookDetailExpandableListAdapter) this
				.getExpandableListAdapter();

		// we started with a record in the db, this is just an update
        if (mRowId == null) {
			throw new BookLoggerException(CLASSNAME
					+ "...Cannot persist a detail screen without a book id");
        } 

        // refresh book detail information from expandable form
        mBookDetail = adapter.getBookDetail();

		mDbHelper.updateEntry(mRowId.longValue(), mTitle.getText().toString(), mAuthor.getText()
				.toString(), mBookDetail.activity, mBookDetail.chapterBegin,
				mBookDetail.chapterEnd, mBookDetail.pagesBegin, mBookDetail.pagesEnd,
				mBookDetail.dateRead, mBookDetail.comments);

    }
}
