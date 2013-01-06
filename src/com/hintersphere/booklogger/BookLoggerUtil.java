package com.hintersphere.booklogger;

import java.util.Locale;

public class BookLoggerUtil {
    
    /**
     * Test value and throw exception with msg if missing. (null or less than zero).
     * @param value to be tested
     * @param msg to be included in the exception.
     */
    public static void throwIfMissing(Long value, String msg) {
        if (value == null || value < 0) {
            throw new BookLoggerException(msg);
        }                
    }
    
    /**
     * Format minutes for display.
     * @param minutes to be formatted
     * @return String representation of minutes as "hh:mm"
     */
    public static String formatMinutes(int minutes) {
        return String.format(Locale.getDefault(), "%d:%02d", minutes/60, (minutes%60));
    }
}
