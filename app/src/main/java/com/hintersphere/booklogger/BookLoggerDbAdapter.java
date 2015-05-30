package com.hintersphere.booklogger;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.hintersphere.util.DbAdapterUtil;

/**
 * @author Michael V Landis
 */
public class BookLoggerDbAdapter {

    /**
     * CLASSNAME used for logging
     */
    private static final String CLASSNAME = BookLoggerDbAdapter.class.getName();

    /**
     * DB Tables...
     */
    public static final String DB_TAB_BOOKLIST = "booklist";
    public static final String DB_TAB_LISTENTRY = "listentry";

    /**
     * DB Columns...
     */
    public static final String DB_COL_NAME = "name"; // name of the book list
    public static final String DB_COL_PRIORITY = "priority"; // used to remember last selected list
    public static final String DB_COL_ID = "_id"; // primary key for both tables
    public static final String DB_COL_PREFS = "prefs"; // list preferences
    public static final String DB_COL_LISTID = "listid"; // foreign key to booklist._id
    public static final String DB_COL_TITLE = "title"; // title of a book
    public static final String DB_COL_AUTHOR = "author"; // author of a book
    public static final String DB_COL_THUMB = "thumb"; // author of a book
    public static final String DB_COL_ISBN = "isbn"; // isbn of a book
    public static final String DB_COL_ACTIVITY = "activity"; // type of activityto log
    public static final String DB_COL_ARLEVEL = "arlevel"; // accelerated readerlevel
    public static final String DB_COL_ARPOINTS = "arpoints"; // acceleratedreader points
    public static final String DB_COL_WORDCOUNT = "wordcount"; // word count
    public static final String DB_COL_PAGECOUNT = "pagecount"; // page count
    public static final String DB_COL_PAGESREAD = "pagesread"; // pages read
    public static final String DB_COL_WEBREADER = "webreader"; // web reader link
    public static final String DB_COL_CREATEDT = "createdt"; // timestamp create date
    public static final String DB_COL_COMMENT = "comment"; // comments
    public static final String DB_COL_DATEREAD = "dateRead"; // read date
    public static final String DB_COL_MINUTES = "minutes"; // number of minutes

    /**
     * Activities (what did we do with this book?
     */

    /**
     * Database creation sql statement _id is the autogenerated primary key for the table title, author, isbn - self
     * explanatory activity lookup - child read, parent read, child+parent, etc...
     */
    private static final String DATABASE_NAME = "bookloggerdb";
    private static final int DATABASE_VERSION = 3;
    private static final String DB_CREATE_BOOKLIST = "create table if not exists " + DB_TAB_BOOKLIST + " ("
            + DB_COL_ID + " integer primary key autoincrement, " 
            + DB_COL_NAME + " text not null, " 
            + DB_COL_PREFS + " text null, " 
            + DB_COL_PRIORITY + " integer not null);";
    private static final String DB_CREATE_LISTENTRY = "create table if not exists " + DB_TAB_LISTENTRY + " ("
            + DB_COL_ID + " integer primary key autoincrement, " 
            + DB_COL_LISTID + " integer not null, " 
            + DB_COL_TITLE + " text not null, " 
            + DB_COL_AUTHOR + " text not null, " 
            + DB_COL_THUMB + " text not null, " 
            + DB_COL_ISBN + " text not null, " 
            + DB_COL_ACTIVITY + " integer not null DEFAULT " + ReadBy.ME.id + ", " 
            + DB_COL_ARLEVEL + " integer null, "
            + DB_COL_ARPOINTS + " integer null, "
            + DB_COL_WORDCOUNT + " integer null, "            
            + DB_COL_PAGECOUNT + " integer null, "            
            + DB_COL_PAGESREAD + " integer null, "            
            + DB_COL_WEBREADER + " text null, "            
            + DB_COL_COMMENT + " text null, " 
            + DB_COL_DATEREAD + " text not null DEFAULT CURRENT_TIMESTAMP, " 
            + DB_COL_MINUTES + " integer null, " 
            + DB_COL_CREATEDT + " text not null DEFAULT CURRENT_TIMESTAMP);";

    private static final String DB_WHERE_LISTENTRIES = DB_COL_LISTID + " = ?";

    private static final String DB_WHERE_LISTENTRY = DB_COL_ID + " = ?";

    private static final String DB_WHERE_LASTBOOKLIST = DB_COL_PRIORITY + " = (SELECT MAX(" + DB_COL_PRIORITY
            + ") FROM " + DB_TAB_BOOKLIST + ")";

    private static final String DB_WHERE_BOOKLIST = "_id = ?";

    private static final String DB_UPDATE_PRIORITY = "UPDATE " + DB_TAB_BOOKLIST + " SET " + DB_COL_PRIORITY + " = "
            + "((SELECT MAX(" + DB_COL_PRIORITY + ") from " + DB_TAB_BOOKLIST + ") + 1) WHERE " + DB_COL_ID + "=";

    private static final int DEF_PRIORITY = 0;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.beginTransaction();
            try {
                db.execSQL(DB_CREATE_BOOKLIST);
                db.execSQL(DB_CREATE_LISTENTRY);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                if (BookLoggerUtil.LOG_ENABLED) {
                    Log.e(CLASSNAME, "Exception creating db tables", e);
                }
                throw new BookLoggerException("Exception creating db tables", e);
            } finally {
                db.endTransaction();
            }
        }

        /**
         * (non-Javadoc) strategy for upgrading live database appropriated from here:
         * http://stackoverflow.com/questions/3505900/sqliteopenhelper- onupgrade-confusion-android
         * 
         * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (BookLoggerUtil.LOG_ENABLED) {
                Log.w(CLASSNAME, "Upgrading database from version " + oldVersion + " to " + newVersion);
            }
            db.beginTransaction();
            try {
                /**
                 * run a table creation with "if not exists" (we are doing an upgrade, so the table might not exist yet,
                 * it will then fail alter and drop) this should normally fail on an upgrade unless a table is new
                 */
                db.execSQL(DB_CREATE_BOOKLIST);
                db.execSQL(DB_CREATE_LISTENTRY);

                // get the column names from the tables
                List<String> colsBooklist = DbAdapterUtil.getColumnsAsList(db, DB_TAB_BOOKLIST);
                List<String> colsListEntry = DbAdapterUtil.getColumnsAsList(db, DB_TAB_LISTENTRY);

                // back up the tables
                db.execSQL("ALTER table " + DB_TAB_BOOKLIST + " RENAME TO 'temp_" + DB_TAB_BOOKLIST + "'");
                db.execSQL("ALTER table " + DB_TAB_LISTENTRY + " RENAME TO 'temp_" + DB_TAB_LISTENTRY + "'");

                // now create the tables again for real...
                db.execSQL(DB_CREATE_BOOKLIST);
                db.execSQL(DB_CREATE_LISTENTRY);

                // get the intersection of the columns
                colsBooklist.retainAll(DbAdapterUtil.getColumnsAsList(db, DB_TAB_BOOKLIST));
                colsListEntry.retainAll(DbAdapterUtil.getColumnsAsList(db, DB_TAB_LISTENTRY));

                // restore data from backup
                String cols = TextUtils.join(",", colsBooklist);
                db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from temp_%s", DB_TAB_BOOKLIST, cols, cols,
                        DB_TAB_BOOKLIST));

                // do something special with date read - if new pull from create date
                cols = TextUtils.join(",", colsListEntry);
                String fromCols = new String(cols);
                
                if (!cols.contains("," + DB_COL_DATEREAD)) {

                    cols = cols.concat(", " + DB_COL_DATEREAD);
                    fromCols = fromCols.concat(", " + DB_COL_CREATEDT);
                    
                    if (BookLoggerUtil.LOG_ENABLED) {
                        Log.d(CLASSNAME, "initializing read date as create date.");
                        Log.d(CLASSNAME, "orig cols: [" + cols + "]");
                        Log.d(CLASSNAME, "temp cols: [" + fromCols + "]");
                    }

                }
                db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from temp_%s", DB_TAB_LISTENTRY, cols,
                        fromCols, DB_TAB_LISTENTRY));

                // drop the temp tables
                db.execSQL("DROP table 'temp_" + DB_TAB_BOOKLIST + "'");
                db.execSQL("DROP table 'temp_" + DB_TAB_LISTENTRY + "'");

                db.setTransactionSuccessful();
            } catch (Exception e) {
                if (BookLoggerUtil.LOG_ENABLED) {
                    Log.e(CLASSNAME, "Exception upgrading db tables", e);
                }
                throw new BookLoggerException("Exception upgrading db tables", e);
            } finally {
                db.endTransaction();
            }
        }
    }

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mCtx;

    public BookLoggerDbAdapter(Context mCtx) {
        this.mCtx = mCtx;
    }

    /**
     * Open the booklogger database. If it cannot be opened, try to create a new instance of the database. If it cannot
     * be created, throw an exception to signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public BookLoggerDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    /**
     * Create a new booklist using the name provided. If the list is successfully created return the new rowId for that
     * note, otherwise return a -1 to indicate failure.
     * 
     * @param name the title of the note
     * @return rowId or -1 if failed
     */
    public long createBookList(String name) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(DB_COL_NAME, name);
        initialValues.put(DB_COL_PRIORITY, DEF_PRIORITY);
        return mDb.insert(DB_TAB_BOOKLIST, null, initialValues);
    }

    /**
     * Delete the booklist with the given rowId
     * 
     * @param rowId id of booklist to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteBookList(long rowId) {
        return mDb.delete(DB_TAB_BOOKLIST, DB_COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all booklists in the database
     * 
     * @return Cursor over all booklists
     */
    public Cursor fetchAllBookLists() {
        return mDb.query(DB_TAB_BOOKLIST, new String[] { DB_COL_ID, DB_COL_NAME }, null, null, null, null, null);
    }

    public Cursor fetchLastBookList() {
        return mDb.query(DB_TAB_BOOKLIST, new String[] { DB_COL_ID, DB_COL_NAME }, DB_WHERE_LASTBOOKLIST, null, null,
                null, null);
    }

    public Cursor fetchBookList(long rowid) {
        try {
            return mDb.query(DB_TAB_BOOKLIST, new String[] { DB_COL_ID, DB_COL_NAME }, DB_WHERE_BOOKLIST,
                    new String[] { String.valueOf(rowid) }, null, null, null);
        } catch (Exception e) {
            if (BookLoggerUtil.LOG_ENABLED) {
                Log.e(CLASSNAME, "Error fetching booklist for id = " + rowid, e);
            }
            throw new BookLoggerException("Error fetching booklist for id = " + rowid, e);
        }
    }

    /**
     * Update the booklist using the details provided. The booklist to be updated is specified using the rowId, and it
     * is altered to use the name value passed in
     * 
     * @param rowId id of booklist to update
     * @param name value to set booklist title to
     * @return true if the booklist was successfully updated, false otherwise
     */
    public boolean updateBookList(long rowId, String name) {
        ContentValues args = new ContentValues();
        args.put("name", name);
        return mDb.update(DB_TAB_BOOKLIST, args, DB_COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Set the booklist to 1 > than the current highest priority, to make it the selected booklist.
     * 
     * @param rowId to select
     * @return true if the booklist was successfully updated
     */
    public void selectBookList(long rowId) {
        mDb.execSQL(DB_UPDATE_PRIORITY + rowId);
    }

    /**
     * Create a new listentry using the parameters provided. If the list is successfully created return the new rowId
     * for that note, otherwise return a -1 to indicate failure. createdt will be assigned automagically
     * 
     * @param listid the list to add the entry to
     * @param title the title of the book
     * @param author the author of the book
     * @param thumb the url of the book thumbnail
     * @param isbn the isbn number of the book
     * @param activity represents if the book was read, read to or shared
     * @param arlevel accelerated reading level (-1 for null)
     * @param arpoints accelerated reading points (-1 for null)
     * @return rowId or -1 if failed
     */
    public long createListEntry(long listid, String title, String author, String thumb, String isbn, short activity,
            int arlevel, int arpoints) {

        ContentValues initialValues = new ContentValues();
        initialValues.put(DB_COL_LISTID, listid);
        initialValues.put(DB_COL_TITLE, title);
        initialValues.put(DB_COL_AUTHOR, author);
        initialValues.put(DB_COL_THUMB, thumb);
        initialValues.put(DB_COL_ISBN, isbn);
        initialValues.put(DB_COL_ACTIVITY, activity);
        initialValues.put(DB_COL_ARLEVEL, (arlevel == -1 ? null : arlevel));
        initialValues.put(DB_COL_ARPOINTS, (arpoints == -1 ? null : arpoints));
        initialValues.put(DB_COL_WORDCOUNT, (String) null);

        long id = 0;
        try {
            id = mDb.insert(DB_TAB_LISTENTRY, null, initialValues);
        } catch (Exception e) {
            if (BookLoggerUtil.LOG_ENABLED) {
                Log.e(CLASSNAME, "Error creating list entry.", e);
            }
        }

        return id;
    }

    /**
     * Update the title and author of a book log entry
     * 
     * @param rowId to be updated
     * @param title to be updated to
     * @param author to be updated to
     * @return true or false if the update failed.
     */
    public boolean updateTitleAndAuthor(long rowId, String title, String author) {
        ContentValues args = new ContentValues();
        args.put(DB_COL_TITLE, title);
        args.put(DB_COL_AUTHOR, author);
        return mDb.update(DB_TAB_LISTENTRY, args, DB_COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Update the data entry metadata fields of a book log entry
     * 
     * @param rowId to be updated
     * @param minutes spent reading to be updated
     * @param readBy person who read the book (parent/child/together)
     * @param comment to be updated
     * @param pagesRead number of pages read
     * @return true or false if the update failed.
     */
    public boolean updateBookEntry(long rowId, int minutes, short readBy, String comment, int pagesRead) {
        ContentValues args = new ContentValues();
        args.put(DB_COL_MINUTES, minutes);
        args.put(DB_COL_PAGESREAD, pagesRead);
        args.put(DB_COL_ACTIVITY, readBy);
        args.put(DB_COL_COMMENT, comment);
        return mDb.update(DB_TAB_LISTENTRY, args, DB_COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Update the read date field of a book log entry
     * 
     * @param rowId to be updated
     * @param utcDate to be updated
     * @return true or false if the update failed.
     */
    public boolean updateReadDate(long rowId, String utcDate) {
        ContentValues args = new ContentValues();
        args.put(DB_COL_DATEREAD, utcDate);
        return mDb.update(DB_TAB_LISTENTRY, args, DB_COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Delete the listentry with the given rowId
     * 
     * @param rowId id of listentry to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteListEntry(long rowId) {
        return mDb.delete(DB_TAB_LISTENTRY, DB_COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Delete an entire Book List
     * 
     * @param rowId of the book log to remove
     * @return true if deleted, false otherwise
     */
    public boolean deleteList(long rowId) {

        // first delete the entries (checking return val messes up - legit not to have rows)
        mDb.delete(DB_TAB_LISTENTRY, DB_COL_LISTID + "=" + rowId, null);

        // then delete the list
        return mDb.delete(DB_TAB_BOOKLIST, DB_COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all list entries in the database. Returns summary information - we can retrieve
     * the full row as a separate call for performance (at least that is what I am thinking now)
     * 
     * @param listid to query list entries for
     * @return Cursor over all listentries
     */
    public Cursor fetchListEntries(long listid) {
        try {
            return mDb.query(DB_TAB_LISTENTRY, new String[] { DB_COL_ID, DB_COL_TITLE, DB_COL_AUTHOR, DB_COL_THUMB,
                            DB_COL_ACTIVITY, DB_COL_COMMENT, DB_COL_DATEREAD, DB_COL_MINUTES, DB_COL_CREATEDT,
                            DB_COL_PAGESREAD},
                    DB_WHERE_LISTENTRIES, new String[] { String.valueOf(listid) }, null, null, DB_COL_DATEREAD + ", "
                            + DB_COL_CREATEDT);
        } catch (Exception e) {
            if (BookLoggerUtil.LOG_ENABLED) {
                Log.e(CLASSNAME, "Error fetching list entries by id", e);
            }
            throw new BookLoggerException("Error fetching list entries by list id " + listid, e);
        }
    }

    /**
     * Return a single list entry by id
     * 
     * @param listEntryId used in where clause
     * @return Cursor with single row returned.
     */
    public Cursor fetchListEntry(long listEntryId) {
        try {
            return mDb.query(DB_TAB_LISTENTRY,
                    new String[] { DB_COL_ID, DB_COL_TITLE, DB_COL_AUTHOR, DB_COL_THUMB, DB_COL_ACTIVITY,
                            DB_COL_DATEREAD, DB_COL_CREATEDT, DB_COL_MINUTES, DB_COL_DATEREAD, DB_COL_COMMENT,
                            DB_COL_PAGESREAD },
                    DB_WHERE_LISTENTRY, new String[] { String.valueOf(listEntryId) }, null, null, null);
        } catch (Exception e) {
            if (BookLoggerUtil.LOG_ENABLED) {
                Log.e(CLASSNAME, "Error fetching list entries by id", e);
            }
            throw new BookLoggerException("Error fetching list entries by entry id " + listEntryId , e);
        }
    }

    /**
     * @param listid to retrieve stats
     * @return stats for a single list (sum of minutes, sum of pages read)
     */
    public Cursor fetchListStats(long listid) {
        try {
            return mDb.rawQuery("SELECT SUM(" + DB_COL_MINUTES + "), SUM(" + DB_COL_PAGESREAD + ") FROM "
                            + DB_TAB_LISTENTRY + " " + DB_WHERE_LISTENTRIES, new String[] { String.valueOf(listid) });
        } catch (Exception e) {
            if (BookLoggerUtil.LOG_ENABLED) {
                Log.e(CLASSNAME, "Error fetching list entries by id", e);
            }
            throw new BookLoggerException("Error fetching list stats by list id " + listid, e);
        }
    }
}