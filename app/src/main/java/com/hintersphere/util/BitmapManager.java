package com.hintersphere.util;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hintersphere.booklogger.BookLoggerUtil;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;


/**
 * This code appropriated from: http://negativeprobability.blogspot.com/2011/08/lazy-loading-of-images-in-listview.html
 * 
 * TODO::How does android handle singletons? If another app were to use this, would the default image possibly be
 * affected by a second app's thread?
 * 
 * @author mlandis
 * 
 */
public enum BitmapManager {
	
	INSTANCE;

    private final Map<String, SoftReference<Bitmap>> cache;
    private final ExecutorService pool;
    private Map<ImageView, String> imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    private Bitmap placeholder;
    private int width = 0;
    private int height = 0;

	BitmapManager() {
		cache = new HashMap<String, SoftReference<Bitmap>>();
		pool = Executors.newFixedThreadPool(5);
	}

	/**
	 * Initialize the manager with a placeholder and density information
	 * @param bmp to be used as a placeholder when an image cannot be loaded.
	 * @param width of the desired scaled image
	 * @param height of the desired scaled image
	 * @param density retrieved from getResources().getDisplayMetrics().density
	 */
	public void initialize(Bitmap bmp, int width, int height, float density) {
	    // scale width and height using the display metrics density
	    this.width = (int) (width * density + 0.5f);
        this.height = (int) (width * density + 0.5f);	    
		placeholder = Bitmap.createScaledBitmap(bmp, this.width, this.height, true);;
	}

	public Bitmap getBitmapFromCache(String url) {
		if (cache.containsKey(url)) {
			return cache.get(url).get();
		}

		return null;
	}

    public void queueJob(final String url, final ImageView imageView) {
        /* Create handler in UI thread. */
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String tag = imageViews.get(imageView);
                if (tag != null && tag.equals(url)) {
                    if (msg.obj != null) {
                        imageView.setImageBitmap((Bitmap) msg.obj);
                    } else {
                        imageView.setImageBitmap(placeholder);
                        if (BookLoggerUtil.LOG_ENABLED) {
                            Log.d(null, "fail " + url);
                        }
                    }
                }
            }
        };

        pool.submit(new Runnable() {
            @Override
            public void run() {
                final Bitmap bmp = downloadBitmap(url);
                Message message = Message.obtain();
                message.obj = bmp;
                if (BookLoggerUtil.LOG_ENABLED) {
                    Log.d(null, "Item downloaded: " + url);
                }
                handler.sendMessage(message);
            }
        });
    }

    public void loadBitmap(final String url, final ImageView imageView) {

        imageViews.put(imageView, url);
        Bitmap bitmap = getBitmapFromCache(url);

        // check in UI thread, so no concurrency issues
        if (bitmap != null) {
            if (BookLoggerUtil.LOG_ENABLED) {
                Log.d(null, "Item loaded from cache: " + url);
            }
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageBitmap(placeholder);
            if (!TextUtils.isEmpty(url)) {
                queueJob(url, imageView);
            }
        }
    }

    private Bitmap downloadBitmap(String url) {
        Bitmap bitmap = RestHelper.getBitmap(url);
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        cache.put(url, new SoftReference<Bitmap>(bitmap));
        return bitmap;
    }
}