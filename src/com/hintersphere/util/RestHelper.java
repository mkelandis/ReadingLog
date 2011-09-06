package com.hintersphere.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.json.JSONException;
import org.json.JSONObject;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Helper class to make HTTP requests that return JSON/XML representations, or the resources
 * directly
 * TODO::add a method to return XML and handle methods besides GET
 * @author Michael Landis
 *
 */
public class RestHelper {

	private static final String CLASSNAME = RestHelper.class.getName();
	
	/**
	 * Makes an HTTP Request and gets back a response Object
	 * 
	 * @param requestUrl to retrieve
	 * @return Object response
	 */
	private Object getResponse(String requestUrl) {

		Object obj = null;
		try {
			URL url = new URL(requestUrl);
			URLConnection connection = url.openConnection();
			connection.setUseCaches(true);
			obj = connection.getContent();
		} catch (Exception e) {
			/**
			 * TODO::Determine appropriate error handling approach
			 */
			Log.e(CLASSNAME, "Could not get HTTP Response from request: [" + requestUrl + "]", e);
		}

		return obj;
	}
	
	/**
	 * Make an HTTP request and return response body as a string
	 * 
	 * @param url to retrieve
	 * @return String representation of response body
	 */
	private String getBody(String url) {

		String body = null;
		Object response = getResponse(url);
		if (response instanceof String) {
			body = (String) response;
		} else if (response instanceof InputStream) {
			InputStream stream = (InputStream) response;
			body = convertStreamToString(stream);
		}

		return body;
	}
	
	/**
	 * Make an HTTP request and return response body as a bitmap
	 * 
	 * @param url to retrieve
	 * @return Bitmap representation of response body
	 */
	public Bitmap getBitmap(String url) {
		
		Bitmap bitmap = null;
		Object response = getResponse(url);
		if (response instanceof Bitmap) {
			bitmap = (Bitmap) response;
		} else if (response instanceof InputStream) {
			InputStream stream = (InputStream) response;
			bitmap = BitmapFactory.decodeStream(stream);
		}

		return bitmap;
	}
	
	/**
	 * Make an HTTP request and return response body as a JSON Object
	 * 
	 * @param url to retrieve
	 * @return JSON representation of response body
	 */
	public JSONObject getJson(String url) throws JSONException {		
		// build JSON Object
		return new JSONObject(getBody(url));
	}
	
	
    /**
     * Transform an input stream to a String
     * 
     * @param is to be transfomed
     * @return String value of input stream
     */
    private static String convertStreamToString(InputStream is) {
    	 
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}
