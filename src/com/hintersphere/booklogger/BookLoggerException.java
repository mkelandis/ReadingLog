package com.hintersphere.booklogger;

/**
 * Main exception class for the Book Logger
 * @author mlandis
 */
public class BookLoggerException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1679088102367439955L;

	public BookLoggerException() {
		super();
	}

	public BookLoggerException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public BookLoggerException(String detailMessage) {
		super(detailMessage);
	}

	public BookLoggerException(Throwable throwable) {
		super(throwable);
	}

	


}
