package com.hintersphere.booklogger;

import java.util.Calendar;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.hintersphere.util.AbstractDatePickerFragment;
import com.hintersphere.util.DbAdapterUtil;

public class BookListDetailActivity extends FragmentActivity {

    private static final int MAX_MINUTES = 3 * 60;
    private static final int ROUNDING_INCREMENT_MINS = 5;
    
    private BookLoggerDbAdapter mDbHelper;
    private Long mRowId;
    
    // data fields
    private TextView mReadDate;
    private TextView mMinutesDisplayed;
    private SeekBar mMinutesSeekbar;
    private Spinner mReadBySpinner;
    private TextView mComment;

    private int mMinutes;
    private short mSelectedReadBy;
    private String mReadDateUtc;
    
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

        mReadDate = (TextView) findViewById(R.id.entry_read_date);
        mMinutesDisplayed = (TextView) findViewById(R.id.minutes);
        mMinutesSeekbar = getMinutesSeekBar(); 
        mReadBySpinner = getReadActivitySpinner(); 
        mComment = (EditText) findViewById(R.id.entry_comment);
        
        populateFields();
    }

    /**
     * populate the book list entry data from the db so we can edit it.
     */
    private void populateFields() {

        BookLoggerUtil.throwIfMissing(mRowId, "row id missing");
        
        Cursor listentry = mDbHelper.fetchListEntry(mRowId);
        if (listentry.isBeforeFirst() && listentry.moveToFirst()) {
            
            // these fields are used directly in this activity
            setTitle(listentry.getString(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_TITLE))
                    + " " + getString(R.string.title_delim) + " " 
                    + listentry.getString(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_AUTHOR)));
            
            mReadDateUtc = listentry.getString(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_DATEREAD)); 
            mReadDate.setText(DbAdapterUtil.getDateInUserFormat(mReadDateUtc, this));
 
            mMinutesSeekbar.setProgress(listentry.getInt(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_MINUTES)));
            
            mSelectedReadBy = listentry.getShort(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY));
            mReadBySpinner.setSelection((int) mSelectedReadBy);

            mComment.setText(listentry.getString(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_COMMENT)));
            
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

    /**
     * Show the Read Date Picker Fragment.
     * @param view invoked on
     */
    public void showReadDatePickerDialog(View view) {
        
        final Context context = this;

        AbstractDatePickerFragment fragment = new AbstractDatePickerFragment() {
            @Override
            public void initialize(Calendar cal) {
                cal.setTime(DbAdapterUtil.toDate(mReadDateUtc));
            }
            @Override
            public void handleDateSet(DatePicker view, int year, int month, int day) {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day);
                mReadDateUtc = DbAdapterUtil.fromDate(cal.getTime());
                mDbHelper.updateReadDate(mRowId, mReadDateUtc);
                mReadDate.setText(DbAdapterUtil.getDateInUserFormat(mReadDateUtc, context));
            };            
        };
        fragment.show(getSupportFragmentManager(), "datePicker");
    }
    
    private void saveState() {        
        BookLoggerUtil.throwIfMissing(mRowId, "row id missing");
        mDbHelper.updateBookEntry(mRowId, mMinutes, mSelectedReadBy, mComment.getText().toString());
    }
    
    private SeekBar getMinutesSeekBar() {

        SeekBar minutesSeekBar = (SeekBar) findViewById(R.id.entry_minutes_seekbar);
        minutesSeekBar.setMax(MAX_MINUTES);
        
        minutesSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                trackProgress(roundProgress(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // nothing to do here.
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setSecondaryProgress(seekBar.getProgress());
            }        
        });
        return minutesSeekBar;
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

    private int roundProgress(int progress) {
        return (progress + (ROUNDING_INCREMENT_MINS - 1)) / ROUNDING_INCREMENT_MINS * ROUNDING_INCREMENT_MINS;
    }
    
    private void trackProgress(int minutes) {
        mMinutesDisplayed.setText(String.format("%d:%02d", minutes/60, (minutes%60)));
        mMinutes = minutes;
    }
}
