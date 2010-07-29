/*
 * HashException.java
 *
 * Copyright (C) 2008 AppleGrew
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 
 */
package org.elite.jdcbot.shareframework;

/**
 * Created on 03-Jun-08
 *
 * @author AppleGrew
 * @since 0.7.2
 * @version 1.0
 */
public class HashException extends Exception {

    private static final long serialVersionUID = -419084025412078159L;

    public static enum Error {
	NONE, HASHING_IN_PROGRESS, HASHING_FAILED, HASHING_CANCELLED;

	/**
	 * Returns a better explanatory message for the Error.
	 */
	public String toString() {
	    String e;
	    switch (this) {
		case NONE:
		    e = "No error";
		    break;
		case HASHING_IN_PROGRESS:
		    e = "Hashing of files is in progress";
		    break;
		case HASHING_FAILED:
		    e = "Hashing of a file failed";
		    break;
		case HASHING_CANCELLED:
		    e = "Hashing was cancelled";
		    break;
		default:
		    e = "Unknow Error type";
	    }
	    return e;
	}
    }

    static public final int HASHING_IN_PROGRESS = 1;
    static public final int HASHING_FAILED = 2;

    private Error error = Error.NONE;
    private String msg = "";

    /**
     * Constructs a new HashException.
     * 
     * @param e The error message to report.
     */
    public HashException(Error Error) {
	super();
	error = Error;
	msg = error.toString();
    }

    public HashException(String e, Error Error) {
	super();
	error = Error;
	msg = e;
    }

    public Error getError() {
	return error;
    }

    public String getMessage() {
	return msg;
    }

}
