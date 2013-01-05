package com.hintersphere.booklogger;

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
    
}
