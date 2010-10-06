/*
 * DCIO.java
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
package org.elite.jdcbot.framework;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;

/**
 * Created on 26-May-08<br>
 * The purpose of this class is to implement methods that are needed for IO easily send and receive and parse commands.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1.2
 * 
 */
class DCIO {
	private static final Logger logger = GlobalObjects.getLogger(DCIO.class);
	private String ioexception_msg;
	
	void set_IOExceptionMsg(String msg) {
		ioexception_msg = msg;
	}

	/**
	 * Reading raw command from <i>in</i>.
	 * 
	 * @param The socket stream from which to read the command.
	 * @return Command from hub
	 * @throws IOException 
	 */
	final String ReadCommand(InputStream in) throws IOException {
		int c;
		//Changing to StringBuffer from String. Artifact#2934462.
		StringBuffer buffer = new StringBuffer();
		do {
			c = in.read();
			if (c == -1) {
				if (ioexception_msg == null)
					ioexception_msg = "Premature End of Socket stream or no data in it";
				throw new IOException(ioexception_msg);
			}
			buffer.append((char) c);
		} while (c != '|');

		logger.debug("From remote: " + buffer);
		return buffer.toString();
	}

	final String ReadCommand(Socket socket) throws IOException {
		//Removed BufferedInputStream from here,
		//which was added as part of artifact#2934462.
		//The local BufferedInputStream caused loss in data.
		return ReadCommand(socket.getInputStream());
	}

	/**
	 * Sends raw command to <i>out</i>.
	 * 
	 * @param buffer
	 *                Line which needs to be send. This method won't append "|" on the end on the string if it doesn't exist, so it is up to make
	 *                sure buffer ends with "|" if you calling this method.
	 * @param out The socket stream into which to write the raw command.              
	 * @throws IOException 
	 */
	final void SendCommand(String buffer, OutputStream out) throws IOException {
		byte[] bytes = new byte[buffer.length()];
		for (int i = 0; i < buffer.length(); i++)
			bytes[i] = (byte) buffer.charAt(i);

		logger.debug("From bot: " + buffer);
		out.write(bytes);
	}

	final void SendCommand(final String buffer, final Socket socket) throws IOException {
		final BufferedOutputStream bufOut = new BufferedOutputStream(socket.getOutputStream()); 
		SendCommand(buffer, bufOut);
		bufOut.flush();
	}

	/**
	 * Parses the given raw command and returns the command name in position 0 and the rest arguments in later slots.<br>
	 * <b>Note:</b> This is a simple generalized parser. It simply splits at point of white space, hence it is not useful to
	 * parse private/public messages etc.
	 * @param cmd The raw command to parse.
	 * @return
	 */
	final String[] parseRawCmd(String cmd) {
		String tbuffer[] = null;
		String buffer[] = cmd.split(" ");
		if (buffer[0].startsWith("$"))
			buffer[0] = buffer[0].substring(1);
		int last = buffer.length - 1;
		if (buffer[last].endsWith("|")) {
			if (buffer[last].length() == 1) {
				tbuffer = new String[buffer.length - 1];
				System.arraycopy(buffer, 0, tbuffer, 0, tbuffer.length);
			} else {
				buffer[last] = buffer[last].substring(0, buffer[last].length() - 1);
				tbuffer = buffer;
			}
		}
		return tbuffer;
	}

	/**
	 * Parses a raw command for the command name.
	 * @param cmd The raw command to parse.
	 * @return
	 */
	final String parseCmdName(String cmd) {
		return cmd.substring(1, cmd.indexOf(' '));
	}

	/**
	 * Parses a raw command for the command's arguments, i.e. Everything in the raw command except the command name and the trailing pipe (|).
	 * @param cmd The raw command to parse.
	 * @return
	 */
	final String parseCmdArgs(String cmd) {
		return cmd.substring(cmd.indexOf(' '), cmd.lastIndexOf('|')).trim();
	}
}
