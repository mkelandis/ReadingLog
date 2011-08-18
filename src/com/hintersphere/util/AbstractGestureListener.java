package com.hintersphere.util;

import com.hintersphere.booklogger.R;
import com.hintersphere.booklogger.R.anim;
import com.hintersphere.booklogger.R.id;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewFlipper;

/**
 * This class handles the right->left, left->right swipe scroll to change lists
 * @author Michael Landis
 *
 */
public abstract class AbstractGestureListener extends SimpleOnGestureListener {
	
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	private Animation slideLeftIn;
	private Animation slideLeftOut;
	private Animation slideRightIn;
	private Animation slideRightOut;
	private ViewFlipper viewFlipper;

	public AbstractGestureListener(Activity activity) {
		
		super();

        viewFlipper = (ViewFlipper)activity.findViewById(R.id.flipper);
        slideLeftIn = AnimationUtils.loadAnimation(activity, R.anim.slide_left_in);
        slideLeftOut = AnimationUtils.loadAnimation(activity, R.anim.slide_left_out);
        slideRightIn = AnimationUtils.loadAnimation(activity, R.anim.slide_right_in);
        slideRightOut = AnimationUtils.loadAnimation(activity, R.anim.slide_right_out);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		try {
			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
				return false;
			
			// right to left swipe <----
			if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				viewFlipper.setInAnimation(slideLeftIn);
				viewFlipper.setOutAnimation(slideLeftOut);
				viewFlipper.showNext();
				doSlideLeft();
			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				viewFlipper.setInAnimation(slideRightIn);
				viewFlipper.setOutAnimation(slideRightOut);
				viewFlipper.showPrevious();
				doSlideRight();
			}
		} catch (Exception e) {
			// nothing
		}
		return false;
	}
	
	protected abstract void doSlideLeft();
	
	protected abstract void doSlideRight();

}
