/*
 * BufferdServerSocket.java
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Created on 2-Aug-10<br>
 * Creates instance of BufferedSocket for
 * connection to clients.
 * 
 * @author AppleGrew
 * @since 1.1.3
 * @version 1.0
 * @see BufferedSocket
 */
public class BufferedServerSocket extends ServerSocket {

	public BufferedServerSocket(int port, int backlog) throws IOException {
		super(port, backlog);
	}

	public BufferedServerSocket(int port) throws IOException {
		super(port);
	}

	public BufferedServerSocket() throws IOException {
		super();
	}

	public BufferedServerSocket(int port, int backlog, InetAddress bindAddress)
			throws IOException {
		super(port, backlog, bindAddress);
	}

	@Override
	public BufferedSocket accept() throws IOException {
		BufferedSocket bSocket = new BufferedSocket();
		implAccept(bSocket);
		return bSocket;
	}
	

}
