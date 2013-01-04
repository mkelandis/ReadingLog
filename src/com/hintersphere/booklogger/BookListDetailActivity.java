package com.hintersphere.booklogger;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class BookListDetailActivity extends Activity {

    private BookLoggerDbAdapter mDbHelper;
    private Long mRowId;
    
    // data fields
    private Spinner mReadBySpinner;
    private TextView mComment;
    private short mSelectedReadBy;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);
        
        mRowId = (savedInstanceState == null) ? null : (Long) savedInstanceState
                .getSerializable(BookLoggerDbAdapter.DB_COL_ID);
        if (mRowId == null) {
            Bundle extras = getIntent().getExtras();
            mRowId = extras != null ? extras.getLong(BookLoggerDbAdapter.DB_COL_ID) : null;
        }

        mDbHelper = new BookLoggerDbAdapter(this);
        mDbHelper.open();

        mComment = (EditText) findViewById(R.id.entry_comment);
        mReadBySpinner = getReadActivitySpinner(); 
        
        populateFields();
    }

    /**
     * populate the book list entry data from the db so we can edit it.
     */
    private void populateFields() {

        throwIfMissing(mRowId, "row id missing");
        
        Cursor listentry = mDbHelper.fetchListEntry(mRowId);
        if (listentry.isBeforeFirst() && listentry.moveToFirst()) {
            
            // these fields are used directly in this activity
            setTitle(listentry.getString(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE))
                    + " " + getString(R.string.title_delim) + " " 
                    + listentry.getString(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR)));
            
            mComment.setText(listentry.getString(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_COMMENT)));
            mSelectedReadBy = listentry.getShort(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY));
            mReadBySpinner.setSelection((int) mSelectedReadBy);
            
            listentry.close();
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
        throwIfMissing(mRowId, "row id missing");
        mDbHelper.updateBookEntry(mRowId, mSelectedReadBy, mComment.getText().toString());
    }
    
    private void throwIfMissing(Long value, String msg) {
        if (value == null || value < 0) {
            throw new BookLoggerException(msg);
        }                
    }
    
    private Spinner getReadActivitySpinner() {

        Spinner activitySpinner = (Spinner) findViewById(R.id.entry_activity_spinner); 
     
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.read_by,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        activitySpinner.setAdapter(adapter);
        activitySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // the position should reflect the code for the Read Activity...
                mSelectedReadBy = (short) pos;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // do nothing                
            }
            
        });
        
        return activitySpinner;
    }

}
