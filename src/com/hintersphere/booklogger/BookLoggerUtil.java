package com.hintersphere.booklogger;

import java.util.Locale;

import com.google.ads.AdRequest;

public class BookLoggerUtil {
    
    /**
     * Turn off logging for a release.
     */
    public static final boolean LOG_ENABLED = true;
    
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
    
    /**
     * Create an ad request, taking into account debugging scenarios.
     * @return newly created ad request object
     */
    public static AdRequest createAdRequest() {
        AdRequest adRequest = new AdRequest();
        if (BookLoggerUtil.LOG_ENABLED) {
            adRequest.addTestDevice("66AE4425C6895E23FCD3DE8C581FCCD6");
        }
        return adRequest;        
    }
}
