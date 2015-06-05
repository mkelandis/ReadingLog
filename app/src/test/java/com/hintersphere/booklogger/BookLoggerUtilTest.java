package com.hintersphere.booklogger;

import com.google.ads.AdRequest;

import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link BookLoggerUtil}.
 */
public class BookLoggerUtilTest {

    @Test(expected = BookLoggerException.class)
    public void testThrowIfMissingThrowsWhenNull() throws Exception {
        BookLoggerUtil.throwIfMissing(null, "testmsg");
    }

    @Test(expected = BookLoggerException.class)
    public void testThrowIfMissingThrowsWhenLessThanZero() throws Exception {
        BookLoggerUtil.throwIfMissing(new Long(-12), "testmsg");
    }

    @Test
    public void testThrowIfMissingWontThrowWhenGreaterThanZero() throws Exception {
        BookLoggerUtil.throwIfMissing(new Long(12), "testmsg");
    }

    @Test
    public void testFormatMinutes() throws Exception {
        assertEquals("5:12", BookLoggerUtil.formatMinutes(312));
    }

    @Test
    public void testFormatMinutesZero() throws Exception {
        assertEquals("0:00", BookLoggerUtil.formatMinutes(0));
    }

    @Test
    public void testCreateAdRequest() throws Exception {
        AdRequest adRequest = BookLoggerUtil.createAdRequest();
        assertNotNull(adRequest);
    }

    @Test
    public void testCloseQuietly() throws Exception {
        Closeable mockCloseable = mock(Closeable.class);
        doThrow(new IOException()).when(mockCloseable).close();
        BookLoggerUtil.closeQuietly(mockCloseable);
    }
}