/*
 * BotException.java
 *
 * Copyright (C) 2005 Kokanovic Branko
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

package org.elite.jdcbot.framework;

/**
 * An BotException class.
 * 
 * @since 0.5
 * @author Kokanovic Branko
 * @author AppleGrew
 * @version 0.7
 */
public class BotException extends Exception {

    private static final long serialVersionUID = -6707371836754480742L;

    static public final int NO_FREE_SLOTS = 1;
    static public final int REMOTE_CLIENT_SENT_WRONG_USRNAME = 2;
    static public final int VALIDATE_DENIED = 3;
    static public final int BAD_PASSWORD = 4;
    static public final int NOT_CONNECTED_TO_HUB = 5;
    static public final int USRNAME_NOT_FOUND = 6;
    static public final int UNEXPECTED_RESPONSE = 7;
    static public final int PROTOCOL_UNSUPPORTED = 8;
    static public final int A_DOWNLOAD_WAS_NOT_REQUESTED = 9;
    static public final int NO_FREE_DOWNLOAD_SLOTS = 10;
    static public final int DOWNLOAD_NOT_POSSIBLE_BOTH_PASSIVE = 11;
    static public final int IO_ERROR = 12;

    private int error_code = 0;
    private String msg = "";

    /**
     * Constructs a new BotException.
     * 
     * @param e
     *                The error message to report.
     */
    public BotException(int errorCode) {
	super();
	error_code = errorCode;
	msg = code2msg(errorCode);
    }

    public BotException(String e, int errorCode) {
	super();
	error_code = errorCode;
	msg = e;
    }

    private String code2msg(int errorCode) {
	String e = "Unknown Error";
	switch (error_code) {
	    case NO_FREE_SLOTS:
		e = "No free slots";
		break;
	    case REMOTE_CLIENT_SENT_WRONG_USRNAME:
		e = "Wrong username sent by remote client";
		break;
	    case VALIDATE_DENIED:
		e = "Validate Denied";
		break;
	    case BAD_PASSWORD:
		e = "Bad Password";
		break;
	    case NOT_CONNECTED_TO_HUB:
		e = "I am not conneced to any hub";
		break;
	    case USRNAME_NOT_FOUND:
		e = "Username doesn't exists";
		break;
	    case UNEXPECTED_RESPONSE:
		e = "Unexpected response";
		break;
	    case PROTOCOL_UNSUPPORTED:
		e = "The protocol is not supported";
		break;
	    case A_DOWNLOAD_WAS_NOT_REQUESTED:
		e = "Download was not requested yet remote client wants to send data";
		break;
	    case NO_FREE_DOWNLOAD_SLOTS:
		e = "No free donwload slots";
		break;
	    case DOWNLOAD_NOT_POSSIBLE_BOTH_PASSIVE:
		e = "This and remote clients both are passive, so download not possible";
		break;
	    case IO_ERROR:
		e = "Input/Output error occured";
		break;
	}
	return e;
    }

    public int getErrCode() {
	return error_code;
    }

    public String getMessage() {
	return msg;
    }

}
