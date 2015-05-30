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

    private static final int MAX_MINUTES = 6 * 60;
    private static final int MAX_PAGES_READ = 300;
    public static final int ONE_MINUTE = 1;
    public static final int FIVE_MINUTES = 5;
    public static final int FIFTEEN_MINUTES = 15;

    private BookLoggerDbAdapter mDbHelper;
    private Long mRowId;
    
    // data fields
    private TextView mReadDate;
    private TextView mMinutesDisplayed;
    private SeekBar mMinutesSeekbar;
    private Spinner mReadBySpinner;
    private Spinner mPagesReadSpinner;
    private TextView mComment;

    private int mMinutes;
    private int mPagesRead;
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
        mPagesReadSpinner = getPagesReadSpinner();
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


            mPagesReadSpinner.setSelection(
                    listentry.getInt(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_PAGESREAD)));

            mSelectedReadBy = listentry.getShort(listentry.getColumnIndex(BookLoggerDbAdapter.DB_COL_ACTIVITY));
            mReadBySpinner.setSelection(ReadBy.getDisplayPosition(mSelectedReadBy));

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
        mDbHelper.updateBookEntry(mRowId, mMinutes, mSelectedReadBy, mComment.getText().toString(), mPagesRead);
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

    private Spinner getPagesReadSpinner() {
        Spinner pagesReadSpinner = (Spinner) findViewById(R.id.entry_pagesread_spinner);

        // backing array of values
        String[] pages = new String[MAX_PAGES_READ + 1];
        pages[0] = this.getString(R.string.detail_hint_pagesread);
        for (int i=1;i <= MAX_PAGES_READ; i++) {
            pages[i] = String.valueOf(i);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pages);
        pagesReadSpinner.setAdapter(adapter);

        pagesReadSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                // the position should reflect the number of pages read...
                mPagesRead = pos;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // do nothing
            }

        });


        return pagesReadSpinner;
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
                mSelectedReadBy = ReadBy.displayPositions[pos].id;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // do nothing                
            }
            
        });
        
        return activitySpinner;
    }

    /**
     * Rounding Increment is variable:
     * less than 30 mins -- 1 min
     * 30 > minutes > 3 hours -- 5 min
     * > 3 hours -- 15 min
     *
     * @param progress
     * @return
     */
    private int roundProgress(int progress) {

        // rounding increment is variable based on number of minutes
        int roundingIncrement = ONE_MINUTE;
        if (progress > 30 && progress < 180) {
            roundingIncrement = FIVE_MINUTES;
        } else if (progress >= 180) {
            roundingIncrement = FIFTEEN_MINUTES;
        }

        return (progress + (roundingIncrement - 1)) / roundingIncrement * roundingIncrement;
    }
    
    private void trackProgress(int minutes) {
        if (minutes == 0) {
            mMinutesDisplayed.setText(R.string.detail_hint_minutes);            
        } else {
            mMinutesDisplayed.setText(BookLoggerUtil.formatMinutes(minutes));
        }
        mMinutes = minutes;
    }
}
