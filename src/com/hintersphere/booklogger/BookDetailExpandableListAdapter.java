package com.hintersphere.booklogger;

import java.util.EnumMap;
import java.util.Map;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

/**
 * This class maps book detail information to an expandable form and provides methods for
 * retrieval of any fields that were updated inside the view.
 * @author mlandis
 *
 */
public class BookDetailExpandableListAdapter extends BaseExpandableListAdapter {
	
	// expandable form sections on the detail page
	// TODO::Add section for the ar level and points (?)
	enum FormSection {		
		ACTIVITY(R.string.detail_section_header_activity, R.layout.detail_activity) {
			void populateView(BookDetail bookDetail, View view) {
				RadioButton radio = null;
				switch (bookDetail.activity) {
				case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ:
					radio = (RadioButton) view.findViewById(R.id.child);
				case BookLoggerDbAdapter.DB_ACTIVITY_PARENT_READ:
					radio = (RadioButton) view.findViewById(R.id.parent);
				case BookLoggerDbAdapter.DB_ACTIVITY_CHILD_PARENT_READ:
					radio = (RadioButton) view.findViewById(R.id.parentchild);
				}
				radio.setChecked(true);				
			}
			void populateBook(View view, BookDetail bookDetail) {
				RadioButton radioChild = (RadioButton) view.findViewById(R.id.child);
				RadioButton radioParent = (RadioButton) view.findViewById(R.id.parent);
				
				if (radioChild.isChecked()) {
					bookDetail.activity = BookLoggerDbAdapter.DB_ACTIVITY_CHILD_READ;
				} else if (radioParent.isChecked()) {
					bookDetail.activity = BookLoggerDbAdapter.DB_ACTIVITY_PARENT_READ;					
				} else { // radioParentChild must be checked...
					bookDetail.activity = BookLoggerDbAdapter.DB_ACTIVITY_CHILD_PARENT_READ;										
				}
			}
		},
		DATE(R.string.detail_section_header_date, R.layout.detail_date) {
			void populateView(BookDetail bookDetail, View view) {
				
			}
			public void populateBook(View view, BookDetail bookDetail) {
				// TODO Auto-generated method stub
				
			}
			
		},
		CHAPTERS(R.string.detail_section_header_chapters, R.layout.detail_chapters) {
			void populateView(BookDetail bookDetail, View view) {
				
			}			
			public void populateBook(View view, BookDetail bookDetail) {
				// TODO Auto-generated method stub
				
			}
		}, 
		PAGES(R.string.detail_section_header_pages, R.layout.detail_pages) {
			void populateView(BookDetail bookDetail, View view) {
				
			}			
			public void populateBook(View view, BookDetail bookDetail) {
				// TODO Auto-generated method stub
				
			}
		}, 
		COMMENTS(R.string.detail_section_header_comments, R.layout.detail_comments) {
			void populateView(BookDetail bookDetail, View view) {
				
			}			
			public void populateBook(View view, BookDetail bookDetail) {
				// TODO Auto-generated method stub
				
			}
		};
		
		private final int nameid;
		private final int layoutid;

		FormSection(int nameid, int layoutid) {
			this.nameid = nameid;
			this.layoutid = layoutid;
		}

		int getNameid() {
			return nameid;
		}

		int getLayoutid() {
			return layoutid;
		}
		
		abstract void populateView(BookDetail bookDetail, View view);
		abstract void populateBook(View view, BookDetail bookDetail);
	};

	private final Context mCtx;
	private final BookDetail mBookDetail;
    private final Map<FormSection, View> mFormSectionViews;
	
	/**
	 * no-args constructor not used
	 * ...seems slightly weird...
	 */
	@SuppressWarnings("unused")
	private BookDetailExpandableListAdapter() {
		this.mCtx = null;
		this.mBookDetail = null;
		this.mFormSectionViews = new EnumMap<FormSection, View>(FormSection.class);
	}

	/**
	 * @param ctx context used to pull strings and such
	 */
	public BookDetailExpandableListAdapter(Context ctx, BookDetail bookDetail) {
		super();
		this.mCtx = ctx;
		this.mBookDetail = bookDetail;		
		this.mFormSectionViews = new EnumMap<FormSection, View>(FormSection.class);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	/** (non-Javadoc)
	 * I don't think we can actually re-use convertview, since our child views are form sections
	 * that we need to be able to pull data from later.
	 * @see android.widget.ExpandableListAdapter#getChildView(int, int, boolean, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
			View convertView, ViewGroup parent) {
		
		// we only ever have one child per group, since each single group's child is a form section
		if (childPosition > 0) {
			throw new BookLoggerException("Each form section is a group with only 1 child");
		}
		
		// here we are specifically not re-using convert view...
		FormSection formSection = FormSection.values()[groupPosition];
		LayoutInflater inflater = (LayoutInflater) mCtx
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(formSection.getLayoutid(), null);
		
		// populate the view based on the form section
		formSection.populateView(mBookDetail, view);
		mFormSectionViews.put(formSection, view);
		
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return 1;
	}

	@Override
	public Object getGroup(int groupPosition) {
        return FormSection.values()[groupPosition];
    }

	@Override
	public int getGroupCount() {
		return FormSection.values().length;
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	/**
	 * TODO::You should prolly use a layout inflater here, see:
	 * http://code.google
	 * .com/p/myandroidwidgets/source/browse/trunk/CustomExpandableListView
	 * /src/com/beanie/example/list/adapter/ExpandableListAdapter.java
	 * 
	 * @return
	 */
    public TextView getGenericView() {
        // Layout parameters for the ExpandableListView
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, 64);

        TextView textView = new TextView(mCtx);
        textView.setLayoutParams(lp);
        // Center the text vertically
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        // Set the text starting position
        textView.setPadding(36, 0, 0, 0);
        return textView;
    }

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
			ViewGroup parent) {		
        TextView textView = getGenericView();
        int nameid = ((FormSection) getGroup(groupPosition)).getNameid();
        textView.setText(mCtx.getText(nameid));
        return textView;
	}

	@Override
	public boolean hasStableIds() {
//		// TODO Auto-generated method stub
//		return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#isChildSelectable(int, int)
	 * TODO::Pretty sure this should be returning false
	 */
	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	/**
	 * Refresh the book detail object from the views and return it...
	 * @return
	 */
	public BookDetail getBookDetail() {
		
		// iterate through the form sections and refresh the book data...
		for (FormSection formSection : FormSection.values()) {
			View view = mFormSectionViews.get(formSection);
			if (view != null) {
				formSection.populateBook(view, mBookDetail);
			}
		}
		
		return mBookDetail;
	}
	
}
