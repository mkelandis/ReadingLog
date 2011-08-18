package com.hintersphere.booklogger;


import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteCursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.hintersphere.util.AbstractGestureListener;
import com.hintersphere.util.RestHelper;


/**
 * Main activity class for the Book Logger - displays a list of books from the latest selected
 * list (based on priority column). Includes menu options for
 * - switch list
 * - share list
 * - new list
 * - delete list
 * - scan book
 * - info/about 
 * @author Michael Landis
 */
public class BookLoggerActivity extends Activity {
	
	private static final String CLASSNAME = BookLoggerActivity.class.getName();
	private static final String GOOGLE_BOOKS_ISBN_LOOKUP = "https://www.googleapis.com/books/v1/volumes?q=isbn:";

	// dialogs
	private static final int DIALOG_FIRST_TIME = 0;
	private static final int DIALOG_RESCAN = 1;
	private static final int DIALOG_REMOVE_BOOK = 2;
	private static final int DIALOG_SWITCH_LIST = 3;
	
    // activity
    private static final int ACTIVITY_EDIT_LIST = 0;
    
    // options menu stuff
    private static final int ADDBOOK_ID = Menu.FIRST;
    private static final int NEWLIST_ID = Menu.FIRST + 1;    
    private static final int EDITLIST_ID = Menu.FIRST + 2;    
    private static final int SWITCHLIST_ID = Menu.FIRST + 3;    

    private BookLoggerDbAdapter mDbHelper;

    // currently selected list (Google's coding standard uses "m" prefixed instance variables)
    private Long mListId = Long.MIN_VALUE;  // default value when no lists exist
    private String mListName;
    private ArrayList<Long> mAllListIds = new ArrayList<Long>(5); 
    
    // book id to be removed (need to store between showdialog and onCreateDialog)
    private static final String REMOVE_BOOK_ID = "removeBookId";
    private Long mRemoveBookId = Long.MIN_VALUE;
    
	private GestureDetector mGestureDetector;
	private View.OnTouchListener mGestureListener;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mDbHelper = new BookLoggerDbAdapter(this);
        mDbHelper.open();
        
        // get the last selected list
        populateLastSelectedList(savedInstanceState);
        
        // populate the list ids for swiping right and left
        if (mAllListIds.size() == 0) {
        	populateAllListIds();
        }
        
        // now populate the list for display
        populateBooks();

        // handle first timers (create a default list and prompt for a scan)
        doFirstTimeUser(); 
        
        registerForContextMenu(getListView());
        
        //  handle add button
		Button addButton = (Button) findViewById(R.id.add_book);
		addButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				IntentIntegrator.initiateScan(BookLoggerActivity.this);
			}
		});

		// handle activity swipe/slide to next/previous list
		mGestureDetector = new GestureDetector(new AbstractGestureListener(this) {
			@Override
			protected void doSlideLeft() {
				mDbHelper.selectBookList(getNextId());
				populateState();
				populateBooks();
			}

			@Override
			protected void doSlideRight() {
				mDbHelper.selectBookList(getPreviousId());
				populateState();
				populateBooks();
			}
		});

		mGestureListener = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (mGestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};
	}
    
	private View getListView() {
		return (ListView) findViewById(R.id.mainlist);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {

		Dialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (id) {
		case DIALOG_FIRST_TIME:
			builder.setTitle(R.string.dialog_firstTime_title)
				   .setMessage(R.string.dialog_firstTime_instructions)
			       .setCancelable(false)
			       .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   IntentIntegrator.initiateScan(BookLoggerActivity.this);
			           }
			       });
			dialog = builder.create();			
			break;
		case DIALOG_RESCAN:
			builder.setTitle(R.string.dialog_rescan_title)
				   .setMessage(R.string.dialog_rescan_instructions)
			       .setCancelable(true)
			       .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   IntentIntegrator.initiateScan(BookLoggerActivity.this);
			           }
			       })
			       .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
			    	   public void onClick(DialogInterface dialog, int id) {
			    		   dialog.cancel();
			    	   }
			       });
			dialog = builder.create();			
			break;
		case DIALOG_REMOVE_BOOK:
			builder.setTitle(R.string.dialog_removebook_title)
				   .setMessage(R.string.dialog_removebook_instructions)
			       .setCancelable(true)
			       .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	  if (mRemoveBookId != Long.MIN_VALUE) {
			        		  mDbHelper.deleteListEntry(mRemoveBookId);
			        		  mRemoveBookId = Long.MIN_VALUE;			        		  
			        	  } else {
			        		  Log.e(CLASSNAME, "Unable to find id of book to be removed.");
			        	  }
			        	  populateBooks();
			           }
			       })
			       .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			dialog = builder.create();			
			break;
		case DIALOG_SWITCH_LIST:
			builder.setTitle(R.string.dialog_switch_title);
			builder.setMessage(R.string.dialog_switchlist_instructions);
			builder.setSingleChoiceItems(mDbHelper.fetchAllBookLists(), -1,
					BookLoggerDbAdapter.DB_COL_NAME, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int selectedId) {
							dialog.dismiss();
							Log.d(CLASSNAME, "Selected: " + selectedId);
						}
					});
			builder.show();
			dialog = builder.create();			
			LayoutInflater li = getLayoutInflater();
			dialog.setContentView(li.inflate(R.layout.switch_list, null, false));
		default:
			dialog = null;
		}
		return dialog;
	}
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		// handle the edit activity result
		if (requestCode == ACTIVITY_EDIT_LIST) {
	        super.onActivityResult(requestCode, resultCode, intent);
	        populateState();
	        populateAllListIds(); // we may have added one
			populateBooks();
		} else if (requestCode == IntentIntegrator.REQUEST_CODE && resultCode != 0) {
			// default behavior is to handle scan results
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode,
					intent);
			if (scanResult != null && scanResult.getFormatName().startsWith("EAN")) {
				// get the isbn...
				addBookByISBN(scanResult.getContents());
				populateBooks();
			} else {
				// prompt for a re-scan
				showDialog(DIALOG_RESCAN);
			}
		}
	}
			
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putSerializable(BookLoggerDbAdapter.DB_COL_LISTID, mListId);
        outState.putSerializable(BookLoggerDbAdapter.DB_COL_NAME, mListName);
        
        /**
         * TODO::Figure out if we need to persist remove book id in the db and where should
         * we be pulling it out of this bundle.
         */
        outState.putSerializable(REMOVE_BOOK_ID, mRemoveBookId);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        populateState();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        menu.add(0, ADDBOOK_ID, 0, R.string.menu_addbook);
        menu.add(0, NEWLIST_ID, 0, R.string.menu_newlist);
        menu.add(0, EDITLIST_ID, 0, R.string.menu_editlist);
        menu.add(0, SWITCHLIST_ID, 0, R.string.menu_switchlist);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case ADDBOOK_ID:
            	IntentIntegrator.initiateScan(BookLoggerActivity.this);
                return true;
            case NEWLIST_ID:
            	Intent i = new Intent(this, BookListEditActivity.class);
            	startActivityForResult(i, ACTIVITY_EDIT_LIST);
            	return true;
            case EDITLIST_ID:
                i = new Intent(this, BookListEditActivity.class);
                i.putExtra(BookLoggerDbAdapter.DB_COL_ID, mListId);
                startActivityForResult(i, ACTIVITY_EDIT_LIST);
            	return true;
            case SWITCHLIST_ID:
            	showDialog(DIALOG_SWITCH_LIST);
            	return true;
		}

        return super.onMenuItemSelected(featureId, item);
    }
    
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_context, menu);
		menu.setHeaderTitle(R.string.menu_title);
		
		// display current selection as checked/selected
		AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo) menuInfo;
		BookListCursorAdapter adapter = (BookListCursorAdapter) getListAdapter();
		SQLiteCursor cursor = (SQLiteCursor) adapter.getItem(mInfo.position);
		MenuItem item = menu.getItem(cursor.getShort(4));
		if (item != null) {
			item.setChecked(true);
		}
		
	}

	private ListAdapter getListAdapter() {
		ListView view = (ListView) getListView();
		return view.getAdapter();
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.parent:
			item.setChecked(true);
			mDbHelper.updateActivity(info.id, BookLoggerDbAdapter.DB_ACTIVITY_PARENT_READ);
			populateBooks();
			return true;
		case R.id.child:
			item.setChecked(true);
			mDbHelper.updateActivity(info.id, BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ);
			populateBooks();
			return true;
		case R.id.parentchild:
			item.setChecked(true);
			mDbHelper.updateActivity(info.id, BookLoggerDbAdapter.DB_ACTIVITY_CHILD_PARENT_READ);
			populateBooks();
			return true;
		case R.id.delete:
			// persist the id in a member variable - we'll pull it out when the
			// dialog is handled.
			mRemoveBookId = info.id;
			showDialog(DIALOG_REMOVE_BOOK);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	
    	if (mAllListIds.size() == 1) {
    		return false;
    	} else if (mGestureDetector.onTouchEvent(event))
	        return true;
	    else
	    	return false;
    }
    
	/**
	 * Populates the last selected list from the saved instance state or the database depending 
	 * on availability
	 * @return
	 */
	private void populateLastSelectedList(Bundle savedInstanceState) {		
		
		// when no lists exist...
		mListId = Long.MIN_VALUE;
		
		// try to pull the list we're working on from the saved instance state
		if (savedInstanceState != null) {
			mListId = (Long) savedInstanceState.getSerializable(BookLoggerDbAdapter.DB_COL_LISTID);
			mListName = (String) savedInstanceState
					.getSerializable(BookLoggerDbAdapter.DB_COL_NAME);
//			setTitle(getString(R.string.app_name) + " " + getString(R.string.title_delim) + " "
//					+ mListName);
		} else {
			populateState();
		}
	}
	
	/**
	 * have any booklists been created? if not, create a default one and start
	 * the scanning activity on it.
	 * @return the id of the selected booklist if one exists
	 */
	private void doFirstTimeUser() {
    	if (mListId == Long.MIN_VALUE) {
    		// create the first time book list
    		mListName = this.getString(R.string.default_list_name);
//			setTitle(getString(R.string.app_name) + " " + getString(R.string.title_delim) + " "
//					+ mListName);
    		mListId = Long.valueOf(mDbHelper.createBookList(mListName));		
    		// prompt for a first time scan
    		showDialog(DIALOG_FIRST_TIME);
    	}
    }
    
	/**
	 * Populate the last selected list from the database
	 */
	private void populateState() {
		Cursor cursor = mDbHelper.fetchLastBookList(); // returns one result
		if (cursor.isBeforeFirst() && cursor.moveToFirst()) {
			mListId = (Long) cursor.getLong(0);
			mListName = (String) cursor.getString(1);
//			setTitle(getString(R.string.app_name) + " " + getString(R.string.title_delim) + " "
//					+ mListName);
		}		
		cursor.close();
	}
	
	/**
	 * Stores the last selected list id in the db (by setting it's priority 1 higher than MAX)
	 */
	private void saveState() {
        mDbHelper.selectBookList(mListId.longValue());
	}
	
	/**
	 * Look up all the data we need from Google Books API and get accelerated reader info from ??? 
	 * Populate the selected list with the book information.
	 *  
	 * TODO::Are we happy to just get the first author in the list?
	 * TODO::Pull the accelerated reader information from somewhere 
	 * TODO::Enable multiple activities, allow user to select from radio or turn off the dialog for 
	 * the list 
	 * TODO::Enable AR info (includes wordcount?)
	 * 
	 * @param isbn to pull data from
	 */
	private void addBookByISBN(String isbn) {
		
		RestHelper helper = new RestHelper();
		JSONObject jsonObject = null;
		try {
			jsonObject = helper.getJson(GOOGLE_BOOKS_ISBN_LOOKUP + isbn);
			
			// just take the first item
			JSONObject item = jsonObject.getJSONArray("items").getJSONObject(0);
			if (item != null) {
				JSONObject volumeInfo = item.getJSONObject("volumeInfo");
				String title = volumeInfo.getString("title");
				
				String author = "";
				try {
					author = volumeInfo.getJSONArray("authors").getString(0);
				} catch (JSONException e) {
					Log.d(CLASSNAME, "Could not find the author in the JSON for isbn: " + isbn, e);
				}
				
				String smallThumbnail = "";
				try {
					JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");
					smallThumbnail = imageLinks.getString("smallThumbnail"); 
				} catch (JSONException e) {
					Log.d(CLASSNAME, "Could not find the smallThumbnail in the JSON for isbn: " + isbn, e);					
				}
				
				mDbHelper.createListEntry(mListId.longValue(), title, author, smallThumbnail, isbn,
						BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ, -1, -1, -1);
			}
		} catch (JSONException e) {
			Log.e(CLASSNAME, "Could not process JSON for isbn: [" + isbn + "], JSON: ["
					+ jsonObject + "]", e);
		}
	}
	
	/**
	 * Populate the list with books from the database...
	 * TODO::introduce a dirty bit that determines if we need a fresh list
	 */
	private void populateBooks() {
		
		// this won't work without a selected list...
		if (mListId == Long.MIN_VALUE) {
			return;
		}

		Cursor cursor = mDbHelper.fetchListEntries(mListId.longValue());
        startManagingCursor(cursor);
        
//		setTitle(getString(R.string.app_name) + " " + getString(R.string.title_delim) + " "
//				+ mListName + " " + getString(R.string.title_delim) + " " + cursor.getCount());
        
        int count = cursor.getCount();
        if (count == 1) {
			setTitle(mListName + " " + getString(R.string.title_delim) + " " + cursor.getCount()
					+ " " + getString(R.string.title_books_singular));
        } else {
			setTitle(mListName + " " + getString(R.string.title_delim) + " " + cursor.getCount()
					+ " " + getString(R.string.title_books_plural));        	
        }
        
        // Create an array to specify the fields we want to display in the list (only TITLE)
		String[] from = new String[] { BookLoggerDbAdapter.DB_COL_TITLE,
				BookLoggerDbAdapter.DB_COL_AUTHOR, BookLoggerDbAdapter.DB_COL_ACTIVITY};
		
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.title, R.id.author, R.id.activity};
        
		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter books = 
			new BookListCursorAdapter(this, R.layout.main_row, cursor, from, to);
		setListAdapter(books);
        
		// log the cursor for now
		Log.d(CLASSNAME, "*************\n" + DatabaseUtils.dumpCursorToString(cursor)
				+ "*************\n");
	}
	
	
	private void setListAdapter(ListAdapter books) {
		ListView view = (ListView) getListView();
		view.setAdapter(books);
	}

	/**
	 * Populate a list of all the book list ids so we can slide between lists
	 */
	private void populateAllListIds() {		
		// first clear the list since we could be re-populating it
		mAllListIds.clear();
		
		Cursor cursor = mDbHelper.fetchAllBookLists();		
		while (cursor.moveToNext()) {
			mAllListIds.add(cursor.getLong(0));
		}
	}
	
	/**
	 * return the previous list id for swiping right
	 * @return Long value of previous list id
	 */
	private Long getPreviousId() {
		
		int prevIndex = mAllListIds.indexOf(mListId);
		prevIndex--;
		
		// handle bounds like a circular queue (move to the end)
		if (prevIndex < 0) {
			prevIndex = mAllListIds.size() - 1;			
		}

		return (Long) mAllListIds.get(prevIndex);
	}
	
	/**
	 * return the next list id for swiping left
	 * @return Long value of next list id
	 */
	private Long getNextId() {
		
		int nextIndex = mAllListIds.indexOf(mListId);
		nextIndex++;
		
		// handle bounds like a circular queue (move to the beginning)
		if (nextIndex > mAllListIds.size() - 1) {
			nextIndex = 0;			
		}

		return (Long) mAllListIds.get(nextIndex);
	}

}