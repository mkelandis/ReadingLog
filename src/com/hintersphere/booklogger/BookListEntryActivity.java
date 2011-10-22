package com.hintersphere.booklogger;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * This class is used to manually enter a book by title and author if scanning it is ineffective
 * @author mlandis
 */
public class BookListEntryActivity extends Activity {

	private static final String CLASSNAME = BookListEntryActivity.class.getName();
	
    private EditText mBookTitle;
    private EditText mAuthor;
    private Long mEntryId;
    private Long mListId;
    private BookLoggerDbAdapter mDbHelper;
    private boolean mIsCanceled = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {    	

    	super.onCreate(savedInstanceState);
        
        mDbHelper = new BookLoggerDbAdapter(this);
        mDbHelper.open();
        
        setContentView(R.layout.manual_entry);
        setTitle(R.string.manual_entry_activitytitle);
        
        mBookTitle = (EditText) findViewById(R.id.entry_title);
        mAuthor = (EditText) findViewById(R.id.entry_author);
        Button saveButton = (Button) findViewById(R.id.save);
        Button cancelButton = (Button) findViewById(R.id.cancel);
        
		mListId = (savedInstanceState == null) ? null : (Long) savedInstanceState
				.getSerializable(BookLoggerDbAdapter.DB_COL_LISTID);
		if (mListId == null) {
			Bundle extras = getIntent().getExtras();
			mListId = extras != null ? extras.getLong(BookLoggerDbAdapter.DB_COL_LISTID) : null;
		}

		mEntryId = (savedInstanceState == null) ? null : (Long) savedInstanceState
				.getSerializable(BookLoggerDbAdapter.DB_COL_ID);
		if (mEntryId == null) {
			Bundle extras = getIntent().getExtras();
			mEntryId = extras != null ? extras.getLong(BookLoggerDbAdapter.DB_COL_ID) : null;
		}

		populateFields();
		
        saveButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View view) {
        	    setResult(RESULT_OK);
        	    finish();
        	}
        });
        
        cancelButton.setOnClickListener(new View.OnClickListener() {
        	/** 
        	 * This seems weird, but we need to delete the record if it's there because the db 
        	 * is also being used to persist the title and author state of the view (following one 
        	 * of the basic android sample code examples)
        	 */
        	public void onClick(View view) {
        		mIsCanceled = true;
        	    if (mEntryId != null && mEntryId.longValue() > 0) {
        	    	mDbHelper.deleteListEntry(mEntryId);
        	    }
        	    setResult(RESULT_CANCELED);
        	    finish();
        	}
        });		
        mIsCanceled = false;
    }
    
    @Override
    public void onBackPressed() {
		mIsCanceled = true;
	    if (mEntryId != null && mEntryId.longValue() > 0) {
	    	mDbHelper.deleteListEntry(mEntryId);
	    }	    
	    super.onBackPressed();
    }
    
    /**
     * populate the title and author of the list from the db so we can edit it.
     */
    private void populateFields() {
		if (mEntryId != null && mEntryId.longValue() > 0) {
			Cursor listEntryCursor = mDbHelper.fetchListEntry(mEntryId);
			if (listEntryCursor.isBeforeFirst() && listEntryCursor.moveToFirst()) {
				mBookTitle.setText(listEntryCursor.getString(listEntryCursor
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE)));
				mAuthor.setText(listEntryCursor.getString(listEntryCursor
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR)));
				listEntryCursor.close();
			}
		}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putSerializable(BookLoggerDbAdapter.DB_COL_LISTID, mListId);
        outState.putSerializable(BookLoggerDbAdapter.DB_COL_ID, mEntryId);
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

	private void saveState() {
    	
    	// don't save if we have canceled the operation (lifecycle concern)
    	/**
    	 * TODO::we may need to handle the back button in the same manner
    	 */
    	if (mIsCanceled) {
    		return;
    	}
    	
        String title = mBookTitle.getText().toString();        
        String author = mAuthor.getText().toString();        
        if (mEntryId == null || mEntryId.longValue() <= 0) {
            long id = mDbHelper.createListEntry(mListId.longValue(), title, author, "", "",
					BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ, -1, -1, -1);
            if (id > 0) {
                mEntryId = id;
            }
        } else {
            mDbHelper.updateTitleAndAuthor(mEntryId, title, author);
        }
    }
}
