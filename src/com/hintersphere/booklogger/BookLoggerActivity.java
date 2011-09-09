package com.hintersphere.booklogger;


import java.io.File;
import java.util.ArrayList;

import org.json.JSONArray;
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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
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
 * TODO::List should scroll to bottom after adding a book
 * TODO::Learn how to invalidate views
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
	private static final int DIALOG_LIST_EMPTY = 4;
	
    // activity
    private static final int ACTIVITY_EDIT_LIST = 0;
    
    // options menu stuff
    private static final int ADDBOOK_ID = Menu.FIRST;
    private static final int NEWLIST_ID = Menu.FIRST + 1;    
    private static final int EDITLIST_ID = Menu.FIRST + 2;    
    private static final int SWITCHLIST_ID = Menu.FIRST + 3;    
    private static final int SENDLIST_ID = Menu.FIRST + 4;    

    private BookLoggerDbAdapter mDbHelper;

    // currently selected list (Google's coding standard uses "m" prefixed instance variables)
    private Long mListId = Long.MIN_VALUE;  // default value when no lists exist
    private String mListName;
    private ArrayList<Long> mAllListIds = new ArrayList<Long>(5); 
    
    // book id to be removed (need to store between showdialog and onCreateDialog)
    private static final String REMOVE_BOOK_ID = "removeBookId";
    private Long mRemoveBookId = Long.MIN_VALUE;
    
    // local copy of book log cursor
    private Cursor mListEntriesCursor = null;
    private boolean mListEntriesCursorDirty = true;
    
        
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
		case DIALOG_LIST_EMPTY:
			builder.setTitle(R.string.dialog_list_empty)
				   .setMessage(R.string.dialog_list_empty_instructions)
			       .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
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
			        		  mListEntriesCursorDirty = true;
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
			builder.setTitle(R.string.dialog_switchlist_instructions);

			/**
			 * ok - this is really weird, if you include a message, than the list options
			 * *do not* display, the message displays in the spot where the list should be.  So
			 * don't do this:
			 * builder.setMessage(R.string.dialog_switchlist_instructions);
			 * 
			 */
			// find the index of the selected item id...
			Cursor cursor = mDbHelper.fetchAllBookLists();
			
			builder.setSingleChoiceItems(cursor, getIndexOfId(cursor),
					BookLoggerDbAdapter.DB_COL_NAME, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int selectedIdx) {
							/**
							 * TODO::this next line is not working...
							 */
							((AlertDialog) dialog).getListView().setItemChecked(selectedIdx, true);
							
							/**
							 * TODO::This is inefficient - need to cache this cursor somewhere 
							 * instead of querying the db again...
							 */
							Cursor cursor = mDbHelper.fetchAllBookLists();
							long selectedId = getIdFromIndex(cursor, selectedIdx);
							mDbHelper.selectBookList(selectedId);
							populateState();
							mListEntriesCursorDirty = true;
							populateBooks();							
							Log.d(CLASSNAME, "Selected: " + selectedId);
							dialog.dismiss();
						}
					});
			builder.show();
			dialog = builder.create();			
			LayoutInflater li = getLayoutInflater();
			dialog.setContentView(li.inflate(R.layout.switch_list, null, false));
			/**
			 * TODO::This next line makes no sense, yet must be here???...
			 */
			dialog = null;
			break;
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
				try {
					addBookByISBN(scanResult.getContents());
				} catch (BookNotFoundException e) {
					// TODO show a dialog to allow user to enter manually...
					e.printStackTrace();
				}
				// here we want to ensure the list is refreshed...
				mListEntriesCursorDirty = true;
				populateBooks();
				/**
				 * TODO::figure out how to take the user to the bottom of the list
				 */
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
        menu.add(0, SENDLIST_ID, 0, R.string.menu_sendlist);
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
            	
            	// ensure the list is pulled again
            	mListEntriesCursorDirty = true;
            	
            	return true;
            case EDITLIST_ID:
                i = new Intent(this, BookListEditActivity.class);
                i.putExtra(BookLoggerDbAdapter.DB_COL_ID, mListId);
                startActivityForResult(i, ACTIVITY_EDIT_LIST);
            	return true;
            case SWITCHLIST_ID:
            	showDialog(DIALOG_SWITCH_LIST);
            	return true;
            case SENDLIST_ID:
            	
            	// need a cursor to make a pdf
            	Cursor cursor = getListEntriesCursor();
            	
            	// test cursor to ensure that there is at least one record
            	if (cursor.getCount() <= 0) {
                	showDialog(DIALOG_LIST_EMPTY);
                	return true;
            	}
            	
            	// create the pdf to send
            	BookLoggerPdfAdapter pdfAdapter = new BookLoggerPdfAdapter(this);
            	String title = (String) getTitle();
            	File outputFile = pdfAdapter.makePdf(title, title, cursor);            	
            	Intent intent = new Intent(Intent.ACTION_SEND);
            	intent.putExtra(Intent.EXTRA_SUBJECT, getTitle());
            	intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.pdf_eml_extratext));            	
            	intent.putExtra(Intent.EXTRA_STREAM,  Uri.parse("file://" + outputFile.getAbsolutePath()));
            	intent.setType("application/pdf");
            	startActivity(Intent.createChooser(intent, getString(R.string.pdf_eml_intenttitle)));
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


	/**
	 * TODO::figure out code to refresh single view. 
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		mListEntriesCursorDirty = true; // mark dirty so the list is refreshed
		switch (item.getItemId()) {
		case R.id.parent:
			item.setChecked(true);
			mDbHelper.updateActivity(info.id, BookLoggerDbAdapter.DB_ACTIVITY_PARENT_READ);
//			refreshView();
			return true;
		case R.id.child:
			item.setChecked(true);
			mDbHelper.updateActivity(info.id, BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ);
//			refreshView();
			return true;
		case R.id.parentchild:
			item.setChecked(true);
			mDbHelper.updateActivity(info.id, BookLoggerDbAdapter.DB_ACTIVITY_CHILD_PARENT_READ);
//			refreshView();
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


	
	private View getListView() {
		return (ListView) findViewById(R.id.mainlist);
	}	

	private ListAdapter getListAdapter() {
		ListView view = (ListView) getListView();
		return view.getAdapter();
	}
	
	/**
	 * TODO::figure out how to properly refresh the view
	 *
	 */
	private void refreshView() {
		ListView view = (ListView) getListView();
		view.invalidate();
		CursorAdapter cursorAdapter = (CursorAdapter) getListAdapter();
		cursorAdapter.notifyDataSetChanged();
		populateBooks();
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
		} else {
			populateState();
		}
	}
	
	/**
	 * have any booklists been created? if not, create a default one and start the scanning activity 
	 * on it.
	 * @return the id of the selected booklist if one exists
	 */
	private void doFirstTimeUser() {
    	if (mListId == Long.MIN_VALUE) {
    		// create the first time book list
    		mListName = this.getString(R.string.default_list_name);
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
	 * TODO::Cannot find book: Mythology Greek Gods, Heroes and Monsters
	 * TODO::Are we happy to just get the first author in the list?
	 * TODO::Pull the accelerated reader information from somewhere 
	 * TODO::Enable multiple activities, allow user to select from radio or turn off the dialog for 
	 * the list (us ea default activity)
	 * TODO::Enable AR info (includes wordcount?)
	 * 
	 * @param isbn to pull data from
	 */
	private void addBookByISBN(String isbn) throws BookNotFoundException {
		
		RestHelper helper = new RestHelper();
		JSONObject jsonObject = null;
		try {
			jsonObject = helper.getJson(GOOGLE_BOOKS_ISBN_LOOKUP + isbn);
			
			JSONArray items = jsonObject.getJSONArray("items");
			if (items.length() == 0) {
				throw new BookNotFoundException("Google Books could not find records for: " + isbn);
			}
			
			// just take the first item
			JSONObject item = items.getJSONObject(0);
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
			String msg = "Could not process JSON for isbn: [" + isbn + "], JSON: [" + jsonObject
					+ "]";
			Log.e(CLASSNAME, msg, e);
			throw new BookNotFoundException(msg, e);
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

		Cursor cursor = getListEntriesCursor();
        startManagingCursor(cursor);
        
        int count = cursor.getCount();
        if (count == 1) {
			setTitle(mListName + " " + getString(R.string.title_delim) + " " + cursor.getCount()
					+ " " + getString(R.string.title_books_singular));
        } else {
			setTitle(mListName + " " + getString(R.string.title_delim) + " " + cursor.getCount()
					+ " " + getString(R.string.title_books_plural));        	
        }
        
		// Now create a simple cursor adapter and set it to display
		CursorAdapter books = new BookListCursorAdapter(this, cursor);
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
	 * find the index of the selected book log id
	 * 
	 * @param cursor for fetching all book logs
	 * @return index of the id in the cursor (we'll reverse this later)
	 */
	protected int getIndexOfId(Cursor cursor) {
		
		while (cursor.moveToNext()) {
			if (cursor.getLong(0) == mListId) {
				return cursor.getPosition();
			}
		}
		
		// if nothing found...
		return -1;
	}
	
	protected long getIdFromIndex(Cursor cursor, int index) {		
		cursor.moveToPosition(index); 
		return cursor.getLong(0);
	}

	/**
	 * @return a cached copy of the cursor if possible
	 */
	private Cursor getListEntriesCursor() {
		
		// return a cached copy if possible
		if (mListEntriesCursorDirty == false && mListEntriesCursor != null
				&& !mListEntriesCursor.isClosed()) {
			mListEntriesCursor.moveToPosition(-1);
			return mListEntriesCursor;
		}
		
		mListEntriesCursor = mDbHelper.fetchListEntries(mListId);
		mListEntriesCursorDirty = false;
		return mListEntriesCursor;
	}
}