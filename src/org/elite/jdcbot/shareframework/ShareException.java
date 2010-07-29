/*
 * ShareException.java
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
 * Created on 11-Jun-08
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class ShareException extends Exception {
    private static final long serialVersionUID = -6076703447475277574L;

    public static enum Error {
	NONE,
	FILELIST_NOT_YET_SET,
	NOTHING_TO_PASTE,
	CANNOT_PASTE_DIR_INTO_ITSELF,
	CANNOT_PASTE_NAME_ALREADY_EXISTS,
	CANNOT_PASTE_DIR_INTO_ITS_SUB_DIR,
	FILE_OR_DIR_NOT_FOUND,
	INVALID_NAME,
	HASHING_JOB_IN_PROGRESS;

	/**
	 * Returns a better explanatory message for the Error.
	 */
	public String toString() {
	    String e;
	    switch (this) {
		case NONE:
		    e = "No error";
		    break;
		case FILELIST_NOT_YET_SET:
		    e = "No file list set";
		    break;
		case NOTHING_TO_PASTE:
		    e = "Nothing to paste";
		    break;
		case CANNOT_PASTE_DIR_INTO_ITSELF:
		    e = "Cannot paste a directory into itself";
		    break;
		case CANNOT_PASTE_NAME_ALREADY_EXISTS:
		    e = "Cannot paste as this folder already has a file or directory by that name";
		    break;
		case CANNOT_PASTE_DIR_INTO_ITS_SUB_DIR:
		    e = "Cannot paste as a folder into its sub directory";
		    break;
		case FILE_OR_DIR_NOT_FOUND:
		    e = "File or directory not found";
		    break;
		case INVALID_NAME:
		    e = "The name is invalid because it contains illegal characters";
		    break;
		case HASHING_JOB_IN_PROGRESS:
		    e = "Hashing is in progress";
		    break;
		default:
		    e = "Unknow Error type";
	    }
	    return e;
	}
    }

    private String msg = "";
    private Error error = Error.NONE;

    public ShareException(Error error) {
	super();
	this.error = error;
	msg = error.toString();
    }

    /**
     * This allows to set arbitrary message for the error.
     * @param e
     * @param error
     */
    public ShareException(String e, Error error) {
	super();
	this.error = error;
	msg = e;
    }

    public Error getError() {
	return error;
    }

    public String getMessage() {
	return msg;
    }

}
