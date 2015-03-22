package com.hintersphere.booklogger;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class BookListEditActivity extends Activity {
	
    private EditText mListName;
    private Long mRowId;
    private BookLoggerDbAdapter mDbHelper;
    private boolean mIsCanceled = false;
    private boolean mIsCreatedThisTime = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {    	

    	super.onCreate(savedInstanceState);
        
        mDbHelper = new BookLoggerDbAdapter(this);
        mDbHelper.open();
        
        setContentView(R.layout.booklist_edit);
        setTitle(R.string.booklist_edit_title);
        
        mListName = (EditText) findViewById(R.id.booklist_name);
        Button saveButton = (Button) findViewById(R.id.save);
        Button cancelButton = (Button) findViewById(R.id.cancel);
        
		mRowId = (savedInstanceState == null) ? null : (Long) savedInstanceState
				.getSerializable(BookLoggerDbAdapter.DB_COL_ID);
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(BookLoggerDbAdapter.DB_COL_ID) : null;
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
        	    if (mIsCreatedThisTime && mRowId != null && mRowId.longValue() > 0) {
        	    	mDbHelper.deleteList(mRowId);
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
	    if (mIsCreatedThisTime && mRowId != null && mRowId.longValue() > 0) {
	    	mDbHelper.deleteList(mRowId);
	    }
	    
	    super.onBackPressed();
    }

    
    
    /**
     * populate the name of the list from the db so we can edit it.
     */
    private void populateFields() {
        if (mRowId != null) {
			Cursor booklist = mDbHelper.fetchBookList(mRowId);
			if (booklist.isBeforeFirst() && booklist.moveToFirst()) {
				mListName.setText(booklist.getString(booklist
						.getColumnIndex(BookLoggerDbAdapter.DB_COL_NAME)));
				booklist.close();
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

	private void saveState() {
    	
    	if (mIsCanceled) {
    		return;
    	}

        String name = mListName.getText().toString();        
        if (mRowId == null) {
            long id = mDbHelper.createBookList(name);
            mIsCreatedThisTime = true;
            mDbHelper.selectBookList(id);
            if (id > 0) {
                mRowId = id;
            }
        } else {
            mDbHelper.updateBookList(mRowId, name);
        }
    }
}
