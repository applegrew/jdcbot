package org.elite.jdcbot.framework;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

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

	public BufferedServerSocket(int arg0, int arg1, InetAddress arg2)
			throws IOException {
		super(arg0, arg1, arg2);
	}

	public BufferedSocket accept() throws IOException {
		BufferedSocket bSocket = new BufferedSocket();
		implAccept(bSocket);
		return bSocket;
	}
	

}
