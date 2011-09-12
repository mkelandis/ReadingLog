package com.hintersphere.booklogger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author Michael V Landis
 */
public class BookLoggerDbAdapter {

	/**
     * CLASSNAME used for logging
     */
    private static final String CLASSNAME = BookLoggerDbAdapter.class.getName();
	
    /**
     * DB Columns...
     */
    public static final String DB_COL_NAME = "name"; // name of the book list
    public static final String DB_COL_PRIORITY = "priority"; // used to remember last selected list
    public static final String DB_COL_ID = "_id"; // primary key for both tables
    public static final String DB_COL_LISTID = "listid"; // foreign key to booklist._id
    public static final String DB_COL_TITLE = "title"; // title of a book
    public static final String DB_COL_AUTHOR = "author"; // author of a book
    public static final String DB_COL_THUMB = "thumb"; // author of a book
    public static final String DB_COL_ISBN = "isbn"; // isbn of a book
    public static final String DB_COL_ACTIVITY = "activity"; // type of activity to log
    public static final String DB_COL_ARLEVEL = "arlevel"; // accelerated reader level
    public static final String DB_COL_ARPOINTS = "arpoints"; // accelerated reader points
    public static final String DB_COL_WORDCOUNT = "wordcount"; // wordcount
    public static final String DB_COL_CREATEDT = "createdt"; // timestamp create date

    /**
     * Activities (what did we do with this book?
     */
    public static final short DB_ACTIVITY_CHILD_READ = 0;
    public static final short DB_ACTIVITY_PARENT_READ = 1;
    public static final short DB_ACTIVITY_CHILD_PARENT_READ = 2;
        
    /**
	 * Database creation sql statement
	 * _id is the autogenerated primary key for the table
	 * title, author, isbn - self explanatory
	 * activity lookup - child read, parent read, child+parent, etc...
	 */
    private static final String DATABASE_NAME = "bookloggerdb";
    private static final int DATABASE_VERSION = 1;
	private static final String DB_CREATE_BOOKLIST =
			"create table booklist ("
			+ "_id integer primary key autoincrement, "
			+ "name text not null, "
			+ "priority integer not null);";
	private static final String DB_CREATE_LISTENTRY = "create table listentry ("
			+ "_id integer primary key autoincrement, "
			+ "listid integer not null, "
			+ "title text not null, "
			+ "author text not null, " 
			+ "thumb text not null, " 
			+ "isbn text not null, " 
			+ "activity integer not null, "
			+ "arlevel integer null, "
			+ "arpoints integer null, "
			+ "wordcount integer null, "
			+ "createdt text not null DEFAULT CURRENT_TIMESTAMP);";

	private static final String DB_WHERE_LISTENTRIES = "listid = ?";
	private static final String DB_WHERE_LISTENTRY = "_id = ?";
	private static final String DB_WHERE_LASTBOOKLIST = "priority = (SELECT MAX(priority) FROM booklist)";
	private static final String DB_WHERE_BOOKLIST = "_id = ?";
	
	private static final String DB_UPDATE_PRIORITY = "UPDATE booklist SET priority = "
		+ "((SELECT MAX(priority) from booklist) + 1) WHERE _id=";
	private static final int DEF_PRIORITY = 0;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL(DB_CREATE_BOOKLIST);
				db.execSQL(DB_CREATE_LISTENTRY);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(CLASSNAME, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS booklist");
            db.execSQL("DROP TABLE IF EXISTS listentry");
            onCreate(db);
        }
    }
    
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mCtx;
    
	public BookLoggerDbAdapter(Context mCtx) {
		this.mCtx = mCtx;
	}
	
    /**
     * Open the booklogger database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
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
     * Create a new booklist using the name provided. If the list is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure.
     * 
     * @param name the title of the note
     * @return rowId or -1 if failed
     */
    public long createBookList(String name) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("name", name);
        initialValues.put("priority", DEF_PRIORITY);
        return mDb.insert("booklist", null, initialValues);
    }

    /**
     * Delete the booklist with the given rowId
     * 
     * @param rowId id of booklist to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteBookList(long rowId) {
        return mDb.delete("booklist", "_id" + "=" + rowId, null) > 0;
    }
    
    /**
     * Return a Cursor over the list of all booklists in the database
     * @return Cursor over all booklists
     */
	public Cursor fetchAllBookLists() {
		return mDb.query("booklist", new String[] { DB_COL_ID, DB_COL_NAME }, null, null, null,
				null, null);
	}

	public Cursor fetchLastBookList() {
		return mDb.query("booklist", new String[] { DB_COL_ID, DB_COL_NAME },
				DB_WHERE_LASTBOOKLIST, null, null, null, null);
	}
    
    public Cursor fetchBookList(long rowid) {

		Cursor cursor = null;
		try {
			cursor = mDb.query("booklist", new String[] { DB_COL_ID, DB_COL_NAME },
					DB_WHERE_BOOKLIST, new String[] { String.valueOf(rowid) }, null, null, null);
		} catch (Exception e) {
			Log.e(CLASSNAME, "Error fetching booklist for id = " + rowid, e);
		}

		return cursor;
    }

    /**
     * Update the booklist using the details provided. The booklist to be updated is
     * specified using the rowId, and it is altered to use the name
     * value passed in
     * 
     * @param rowId id of booklist to update
     * @param name value to set booklist title to
     * @return true if the booklist was successfully updated, false otherwise
     */
    public boolean updateBookList(long rowId, String name) {
        ContentValues args = new ContentValues();
        args.put("name", name);
        return mDb.update("booklist", args, "_id" + "=" + rowId, null) > 0;
    }
    
    
    /**
     * Set the booklist to 1 > than the current highest priority, to make it the selected
     * booklist.
     * @param rowId to select
     * @return true if the booklist was successfully updated
     */
    public void selectBookList(long rowId) {
        mDb.execSQL(DB_UPDATE_PRIORITY + rowId);    	
    }
    
    /**
     * Create a new listentry using the parameters provided. If the list is
     * successfully created return the new rowId for that note, otherwise return
     * a -1 to indicate failure. createdt will be assigned automagically
     * 
	 * @param listid the list to add the entry to
	 * @param title the title of the book
	 * @param author the author of the book
	 * @param thumb the url of the book thumbnail
	 * @param isbn the isbn number of the book 
	 * @param activity represents if the book was read, read to or shared
	 * @param arlevel accelerated reading level (-1 for null)
	 * @param arpoints accelerated reading points (-1 for null)
	 * @param wordcount number of words in the book (-1 for null)
     * @return rowId or -1 if failed
	 */
	public long createListEntry(long listid, String title, String author, String thumb, String isbn,
			short activity, int arlevel, int arpoints, int wordcount) {
		
		ContentValues initialValues = new ContentValues();
		initialValues.put("listid", listid);
		initialValues.put("title", title);
		initialValues.put("author", author);
		initialValues.put("thumb", thumb);
		initialValues.put("isbn", isbn);
		initialValues.put("activity", activity);
		initialValues.put("arlevel", (arlevel == -1 ? null : arlevel));
		initialValues.put("arpoints", (arpoints == -1 ? null : arpoints));
		initialValues.put("wordcount", (wordcount == -1 ? null : wordcount));
		
		long id = 0;
		try {
			id = mDb.insert("listentry", null, initialValues);
    	} catch (Exception e) {
    		Log.e(CLASSNAME, "Error creating list entry.", e);
    	}

		
		return id;
	}
	
    /**
     * Update the activity of a book list entry... possible values include child read, parent
     * read, parent and child read together.
     * 
     * @param rowId to be updated
     * @param activity to record
     * @return true on successfull update
     */
    public boolean updateActivity(long rowId, short activity) {
        ContentValues args = new ContentValues();
        args.put(DB_COL_ACTIVITY, activity);
        return mDb.update("listentry", args, DB_COL_ID + "=" + rowId, null) > 0;
    }
	
    /**
     * Update the title and author of a book log entry
     * @param rowId to be updated
     * @param title to be updated to
     * @param author to be updated to
     * @return true or false if the update failed.
     */
    public boolean updateTitleAndAuthor(long rowId, String title, String author) {
        ContentValues args = new ContentValues();
        args.put(DB_COL_TITLE, title);
        args.put(DB_COL_AUTHOR, author);
        return mDb.update("listentry", args, DB_COL_ID + "=" + rowId, null) > 0;
    }

    /**
     * Delete the listentry with the given rowId
     * 
     * @param rowId id of listentry to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteListEntry(long rowId) {
        return mDb.delete("listentry", "_id" + "=" + rowId, null) > 0;
    }

    
    /**
     * Delete an entire Book List
     * 
     * @param rowId of the book log to remove
     * @return true if deleted, false otherwise
     */
    public boolean deleteList(long rowId) {
    	boolean success;
    	
    	// first delete the entries
        success = mDb.delete("listentry", "listid" + "=" + rowId, null) > 0;
        if (!success) {
        	return false;
        }
        
        // then delete the list
        return mDb.delete("booklist", "_id" + "=" + rowId, null) > 0;
    }
    
    /**
     * Return a Cursor over the list of all list entries in the database.
     * Returns summary information - we can retrieve the full row as a separate call
     * for performance (at least that is what I am thinking now)
     * 
     * @param listid to query list entries for
     * @return Cursor over all listentries
     */
    public Cursor fetchListEntries(long listid) {    	
    	Cursor cursor = null;
    	try {
			cursor = mDb.query("listentry", new String[] { DB_COL_ID, DB_COL_TITLE, DB_COL_AUTHOR,
					DB_COL_THUMB, DB_COL_ACTIVITY, DB_COL_CREATEDT }, DB_WHERE_LISTENTRIES, new String[] { String
					.valueOf(listid) }, null, null, null);
    	} catch (Exception e) {
    		Log.e(CLASSNAME, "Error fetching list entries by id", e);
    	}
    	
    	return cursor;
    }
    
    /**
     * Return a single list entry by id
     * 
     * @param listEntryId used in where clause
     * @return Cursor with single row returned.
     */
    public Cursor fetchListEntry(long listEntryId) {
    	Cursor cursor = null;
    	try {
			cursor = mDb.query("listentry", new String[] { DB_COL_ID, DB_COL_TITLE, DB_COL_AUTHOR,
					DB_COL_THUMB, DB_COL_ACTIVITY, DB_COL_CREATEDT }, DB_WHERE_LISTENTRY, new String[] { String
					.valueOf(listEntryId) }, null, null, null);
    	} catch (Exception e) {
    		Log.e(CLASSNAME, "Error fetching list entries by id", e);
    	}
    	
    	return cursor;    	
    }
}
