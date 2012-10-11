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
 * @version 1.1
 */
public class BotException extends RuntimeException {

	private static final long serialVersionUID = -6707371836754480742L;

	public static enum Error {
		NONE,
		NO_FREE_SLOTS,
		REMOTE_CLIENT_SENT_WRONG_USERNAME,
		VALIDATE_DENIED,
		BAD_PASSWORD,
		NOT_CONNECTED_TO_HUB,
		USERNAME_NOT_FOUND,
		UNEXPECTED_RESPONSE,
		PROTOCOL_UNSUPPORTED,
		A_DOWNLOAD_WAS_NOT_REQUESTED,
		NO_FREE_DOWNLOAD_SLOTS,
		DOWNLOAD_NOT_POSSIBLE_BOTH_PASSIVE,
		IO_ERROR,
		TRANSFER_CANCELLED,
		PROTOCOL_UNSUPPORTED_BY_REMOTE,
		UPLOAD_TO_USER_BLOCKED,
		FAILED_TO_DELETE_TEMP_FILE,
		CONNECTION_TO_REMOTE_CLIENT_FAILED,
		TIMEOUT,
		ALREADY_CONNECTED,
		CANNOT_DOWNLOAD_FROM_SELF,
		TASK_FAILED_SHUTTING_DOWN,
		USER_HAS_NO_INFO,
		INVALID_USERNAME;

		/**
		 * Returns a better explanatory message for the Error.
		 */
		public String toString() {
			String e;
			switch (this) {
			case NONE:
				e = "No error";
				break;
			case NO_FREE_SLOTS:
				e = "No free slots";
				break;
			case REMOTE_CLIENT_SENT_WRONG_USERNAME:
				e = "Wrong username sent by remote client";
				break;
			case VALIDATE_DENIED:
				e = "Validate Denied";
				break;
			case BAD_PASSWORD:
				e = "Wrong Password";
				break;
			case NOT_CONNECTED_TO_HUB:
				e = "I am not conneced to hub";
				break;
			case USERNAME_NOT_FOUND:
				e = "Username doesn't exists";
				break;
			case UNEXPECTED_RESPONSE:
				e = "Unexpected response";
				break;
			case PROTOCOL_UNSUPPORTED:
				e = "The protocol or Extended feature is not supported by jDCBot";
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
			case TRANSFER_CANCELLED:
				e = "File transfer has been cancelled";
				break;
			case PROTOCOL_UNSUPPORTED_BY_REMOTE:
				e = "Remote client doesn't support the requested protocol or Extended feature";
				break;
			case UPLOAD_TO_USER_BLOCKED:
				e = "Upload to user has been blocked";
				break;
			case FAILED_TO_DELETE_TEMP_FILE:
				e = "Failed to delete temporary download file";
				break;
			case CONNECTION_TO_REMOTE_CLIENT_FAILED:
				e = "Failed to connect to remote client";
				break;
			case TIMEOUT:
				e = "Connection timed out";
				break;
			case ALREADY_CONNECTED:
				e = "Already connected to remote client or hub";
				break;
			case CANNOT_DOWNLOAD_FROM_SELF:
				e = "Cannot download from self";
				break;
			case TASK_FAILED_SHUTTING_DOWN:
				e = "The task failed to complete as the object was asked to shut down";
				break;
			case USER_HAS_NO_INFO:
				e =
				"User has just logged in and his MyINFO is yet to be received. "
						+ "So, we can't interact reliably with him till his MyINFO is received";
				break;
			case INVALID_USERNAME:
				e = "Username is invalid. Username should not contain any space or symbols" +
						" other than hyphen(-) and underscore (_).";
				break;
			default:
				e = "Unknow Error type";
			}
			return e;
		}
	}

	private Error error = Error.NONE;
	private String msg = "";

	/**
	 * Constructs a new BotException.
	 * @param error
	 */
	public BotException(Error error) {
		super();
		this.error = error;
		msg = error.toString();
	}

	/**
	 * This allows to set arbitrary message for the error. This is intended
	 * to be used only when IO_ERROR occurs.
	 * @param e
	 * @param error
	 */
	public BotException(String e, Error error) {
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
