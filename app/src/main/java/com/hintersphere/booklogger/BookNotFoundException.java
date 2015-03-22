package com.hintersphere.booklogger;

/**
 * Throw an exception when a book cannot be found.
 * @author mlandis
 */
public class BookNotFoundException extends Exception {

	private static final long serialVersionUID = -3833112083342218653L;

	public BookNotFoundException() {
		super();
	}

	public BookNotFoundException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public BookNotFoundException(String detailMessage) {
		super(detailMessage);
	}

	public BookNotFoundException(Throwable throwable) {
		super(throwable);
	}
}
