package com.hintersphere.util;

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

public abstract class AbstractDatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the original date as the default date in the picker
        final Calendar cal = Calendar.getInstance();
        initialize(cal);
        
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        handleDateSet(view, year, month, day);
    }
    
    /**
     * Initialize calendar object to the starting date displayed in the picker.
     * @param cal to be initialized
     */
    public abstract void initialize(Calendar cal);
    
    /**
     * Handle the date once it has been set.
     * @param view date picker view used
     * @param year set in picker
     * @param month set in picker
     * @param day set in picker
     */
    public abstract void handleDateSet(DatePicker view, int year, int month, int day);
}