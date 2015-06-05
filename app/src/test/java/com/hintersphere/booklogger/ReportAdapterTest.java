package com.hintersphere.booklogger;

import android.content.Context;
import android.database.Cursor;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test {@link ReportAdapter}.
 */
public class ReportAdapterTest {

    public static final String CONTEXT_STRING = "context-string";
    public static final String CURSOR_STRING = "cursor-string";

    ReportAdapter reportAdapter;
    Cursor mockCursor;

    @Before
    public void setUp() throws Exception {
        reportAdapter = new ReportAdapter() {};
        reportAdapter.mCtx = mock(Context.class);
        when(reportAdapter.mCtx.getString(any(int.class))).thenReturn("context-string");

        mockCursor = mock(Cursor.class);
    }

    @Test
    public void testGetReadBy() throws Exception {
        when(mockCursor.getInt(any(int.class))).thenReturn((int) ReadBy.CHILD.id);
        assertEquals(CONTEXT_STRING, reportAdapter.getReadBy(mockCursor));
    }

    @Test
    public void testGetMinutes() throws Exception {
        when(mockCursor.getInt(any(int.class))).thenReturn((int) 312);
        assertEquals("5:12", reportAdapter.getMinutes(mockCursor));
    }

    @Test
    public void testGetMinutesNotApplicable() throws Exception {
        when(mockCursor.getInt(any(int.class))).thenReturn((int) 0);
        assertEquals(CONTEXT_STRING, reportAdapter.getMinutes(mockCursor));
    }

    @Test
    public void testGetPagesRead() throws Exception {
        when(mockCursor.getInt(any(int.class))).thenReturn((int) 12);
        assertEquals("12", reportAdapter.getPagesRead(mockCursor));
    }

    @Test
    public void testGetPagesReadNotApplicable() throws Exception {
        when(mockCursor.getInt(any(int.class))).thenReturn((int) 0);
        assertEquals(CONTEXT_STRING, reportAdapter.getPagesRead(mockCursor));
    }

    @Test
    public void testGetComment() throws Exception {
        when(mockCursor.getString(any(int.class))).thenReturn(CURSOR_STRING);
        assertEquals(CURSOR_STRING, reportAdapter.getComment(mockCursor));
    }

    @Test
    public void testGetCommentNull() throws Exception {
        when(mockCursor.getString(any(int.class))).thenReturn(null);
        assertEquals("", reportAdapter.getComment(mockCursor));
    }

    @Test
    public void testGetAuthor() throws Exception {
        when(mockCursor.getString(any(int.class))).thenReturn(CURSOR_STRING);
        assertEquals(CURSOR_STRING, reportAdapter.getAuthor(mockCursor));
    }

    @Test
    public void testGetAuthorNull() throws Exception {
        when(mockCursor.getString(any(int.class))).thenReturn(null);
        assertEquals("", reportAdapter.getAuthor(mockCursor));
    }

    @Test
    public void testGetTitle() throws Exception {
        when(mockCursor.getString(any(int.class))).thenReturn(CURSOR_STRING);
        assertEquals(CURSOR_STRING, reportAdapter.getTitle(mockCursor));
    }

    @Test
    public void testGetTitleNull() throws Exception {
        when(mockCursor.getString(any(int.class))).thenReturn(null);
        assertEquals("", reportAdapter.getTitle(mockCursor));
    }

    @Test
    @Ignore("Requires dependency injection")
    public void testGetDateRead() throws Exception {
    }

    @Test
    public void testGetBookIndex() throws Exception {
        when(mockCursor.getPosition()).thenReturn(3);
        assertEquals("4", reportAdapter.getBookIndex(mockCursor));
    }
}