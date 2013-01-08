package com.hintersphere.booklogger;


import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.google.ads.AdRequest;
import com.google.ads.InterstitialAd;
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
	private static final int DIALOG_DELETE_LIST = 5;
	
    // activity
    private static final int ACTIVITY_EDIT_LIST = 0;
    private static final int ACTIVITY_NEW_ENTRY = 1;
    private static final int ACTIVITY_SEND_LIST = 2;
    private static final int ACTIVITY_DETAILS = 3;
    
    // options menu stuff
    private static final int ADDBOOK_ID = Menu.FIRST;
    private static final int NEWLIST_ID = Menu.FIRST + 1;    
    private static final int EDITLIST_ID = Menu.FIRST + 2;    
    private static final int SWITCHLIST_ID = Menu.FIRST + 3;    
    private static final int SENDLIST_ID = Menu.FIRST + 4;    
    private static final int DELETELIST_ID = Menu.FIRST + 5;    
    private static final int NEWENTRY_ID = Menu.FIRST + 6;    

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

    // interstitial ad displayed after a send
	InterstitialAd mInterstitial = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mDbHelper = new BookLoggerDbAdapter(this);
        mDbHelper.open();
        
        // get the last selected list
        populateLastSelectedList(savedInstanceState);
        
        // populate the list ids for swiping right and left
        /**
         * TODO::we're not using the swipe left/right feature anymore, so remove this
         */
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

		final Context ctx = this;
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
			    		   Intent i = new Intent(ctx, BookListEntryActivity.class);
			    		   i.putExtra(BookLoggerDbAdapter.DB_COL_LISTID, mListId);
			    		   startActivityForResult(i, ACTIVITY_NEW_ENTRY);
			    		   mListEntriesCursorDirty = true;
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
			        	      if (BookLoggerUtil.LOG_ENABLED) {
			        	          Log.e(CLASSNAME, "Unable to find id of book to be removed.");
			        	      }
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
        case DIALOG_DELETE_LIST:
            builder.setTitle(R.string.dialog_removelist_title).setMessage(R.string.dialog_removelist_instructions)
                    .setCancelable(true).setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (mListId != Long.MIN_VALUE) {
                                mDbHelper.deleteList(mListId);
                                mListId = Long.MIN_VALUE;
                                mListEntriesCursorDirty = true;
                                populateState();
                                populateBooks();
                            } else {
                                String msg = "Unable to find id of book to be removed.";
                                if (BookLoggerUtil.LOG_ENABLED) {
                                    Log.e(CLASSNAME, msg);
                                }
                                throw new BookLoggerException(msg);
                            }
                        }
                    }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
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
                            if (BookLoggerUtil.LOG_ENABLED) {
                                Log.d(CLASSNAME, "Selected: " + selectedId);
                            }
                            dialog.dismiss();
						}
					});
			builder.show();
			dialog = builder.create();			
			LayoutInflater li = getLayoutInflater();
			dialog.setContentView(li.inflate(R.layout.switch_list, null, false));
			/**
			 * TODO::This next line (dialog = null) makes no sense, yet must be here???...
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
		} else if (requestCode == ACTIVITY_NEW_ENTRY) {
	        super.onActivityResult(requestCode, resultCode, intent);
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
                    if (BookLoggerUtil.LOG_ENABLED) {
                        Log.e(CLASSNAME, "Could not find the book: ", e);
                    }
                    // prompt for a re-scan
                    showDialog(DIALOG_RESCAN);
                }
				// here we want to ensure the list is refreshed...
				mListEntriesCursorDirty = true;
				populateBooks();
			} else {
				// prompt for a re-scan
				showDialog(DIALOG_RESCAN);
			}
		} else if (requestCode == ACTIVITY_SEND_LIST) {
			// show an ad after the list was sent
			if (mInterstitial != null && mInterstitial.isReady()) {
				mInterstitial.show();
			} else if (mInterstitial != null) {
				mInterstitial.stopLoading();
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
	protected void onDestroy() {
		super.onDestroy();
		
		// close the database connection so we don't leave it...
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        menu.add(0, ADDBOOK_ID, 0, R.string.options_menu_addbook);
        menu.add(0, NEWENTRY_ID, 0, R.string.options_menu_newentry);
        menu.add(0, SENDLIST_ID, 0, R.string.options_menu_sendlist);
        menu.add(0, SWITCHLIST_ID, 0, R.string.options_menu_switchlist);
        menu.add(0, NEWLIST_ID, 0, R.string.options_menu_newlist);
        menu.add(0, EDITLIST_ID, 0, R.string.options_menu_editlist);
        menu.add(0, DELETELIST_ID, 0, R.string.options_menu_deletelist);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case ADDBOOK_ID:
            	IntentIntegrator.initiateScan(BookLoggerActivity.this);
                return true;
            case NEWLIST_ID:
            	Intent intent = new Intent(this, BookListEditActivity.class);
            	startActivityForResult(intent, ACTIVITY_EDIT_LIST);            	
            	// ensure the list is pulled again
            	mListEntriesCursorDirty = true;            	
            	return true;
            case EDITLIST_ID:
                intent = new Intent(this, BookListEditActivity.class);
                intent.putExtra(BookLoggerDbAdapter.DB_COL_ID, mListId);
                startActivityForResult(intent, ACTIVITY_EDIT_LIST);
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
            	
            	// create some keywords
            	Set<String> keywords = new HashSet<String>();            	
            	String[] staticKeywords = getString(R.string.admob_keywords).split("\\|");
            	for (int i=0;i < staticKeywords.length;i++) {
            		keywords.add(staticKeywords[i]);
            	}
            	
            	// create the pdf to send (had to switch to HTML for now)
            	BookLoggerHtmlAdapter htmlAdapter = new BookLoggerHtmlAdapter(this, keywords);
            	String title = (String) getTitle();
            	File outputFile = htmlAdapter.makeHtml(title, title, cursor);            	
            	intent = new Intent(Intent.ACTION_SEND);
            	intent.putExtra(Intent.EXTRA_SUBJECT, getTitle());
            	intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.pdf_eml_extratext)); 
            	intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + outputFile.getAbsolutePath()));
            	intent.setType("text/html");
            	
            	// kick off the ad load before we start the send activity...
            	String pubId = getString(R.string.admob_pubid);
            	mInterstitial = new InterstitialAd(this, pubId);
            	AdRequest adRequest = BookLoggerUtil.createAdRequest();
            	adRequest.setKeywords(keywords);
            	mInterstitial.loadAd(adRequest);
            	
            	startActivityForResult(Intent.createChooser(intent, getString(R.string.pdf_eml_intenttitle)),
                    ACTIVITY_SEND_LIST);
            	return true;
            case DELETELIST_ID:
            	showDialog(DIALOG_DELETE_LIST);
            	return true;
            case NEWENTRY_ID:
                intent = new Intent(this, BookListEntryActivity.class);
                intent.putExtra(BookLoggerDbAdapter.DB_COL_LISTID, mListId);
                startActivityForResult(intent, ACTIVITY_NEW_ENTRY);
                mListEntriesCursorDirty = true;
            	return true;
		}

        return super.onMenuItemSelected(featureId, item);
    }
    
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_context, menu);
		menu.setHeaderTitle(R.string.context_menu_title);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		mListEntriesCursorDirty = true; // mark dirty so the list is refreshed
		switch (item.getItemId()) {
        case R.id.details:
            startBookDetailsActivity(this, info.id);            
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
        ListView listView = (ListView) findViewById(R.id.mainlist);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {                
                startBookDetailsActivity(parent.getContext(), id);
            }
        });
        return listView;
    }
    
    private void startBookDetailsActivity(Context ctx, long rowid) {
        Intent intent = new Intent(ctx, BookListDetailActivity.class);
        intent.putExtra(BookLoggerDbAdapter.DB_COL_ID, Long.valueOf(rowid));
        startActivityForResult(intent, ACTIVITY_DETAILS);        
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
	 * TODO::Are we happy to just get the first author in the list?
	 * TODO::Pull the accelerated reader information from somewhere 
	 * TODO::Enable AR info (includes wordcount?)
	 * 
	 * @param isbn to pull data from
	 */
	private void addBookByISBN(final String isbn) throws BookNotFoundException {
		
		JSONObject jsonObject = null;
		try {
		    
		    ExecutorService executor = Executors.newSingleThreadExecutor();
		    Future<JSONObject> future = executor.submit(new Callable<JSONObject>() {
		        public JSONObject call() {
		            JSONObject json = null;
		            try {
		                json = RestHelper.getJson(GOOGLE_BOOKS_ISBN_LOOKUP + isbn); 
		            } catch (JSONException e) {
		                throw new BookLoggerException("Could not retrieve JSON data for book.", e);
		            }
		            return json;
		        }
            });
		    
		    try {
		        jsonObject = future.get();
		    } catch (Exception e) { 
		        throw new BookLoggerException("Error while executing thread to retrieve JSON.", e);
		    }
		    
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
                    if (BookLoggerUtil.LOG_ENABLED) {
                        Log.d(CLASSNAME, "Could not find the author in the JSON for isbn: " + isbn, e);
                    }
				}
				
				String smallThumbnail = "";
				try {
					JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");
					smallThumbnail = imageLinks.getString("smallThumbnail"); 
				} catch (JSONException e) {
                    if (BookLoggerUtil.LOG_ENABLED) {
                        Log.d(CLASSNAME, "Could not find the smallThumbnail in the JSON for isbn: " + isbn, e);
                    }
				}
				
				mDbHelper.createListEntry(mListId.longValue(), title, author, smallThumbnail, isbn,
						BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ, -1, -1);
			}
		} catch (JSONException e) {
			String msg = "Could not process JSON for isbn: [" + isbn + "], JSON: [" + jsonObject
					+ "]";
            if (BookLoggerUtil.LOG_ENABLED) {
                Log.e(CLASSNAME, msg, e);
            }
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
        if (BookLoggerUtil.LOG_ENABLED) {
            Log.d(CLASSNAME, "*************\n" + DatabaseUtils.dumpCursorToString(cursor) + "*************\n");
        }
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