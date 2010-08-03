package org.elite.jdcbot.framework;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class BufferedSocket extends Socket {
	private InputStream in;
	
	public BufferedSocket(String hostname, int port) throws UnknownHostException, IOException {
		super(hostname, port);
	}

	public BufferedSocket() {
		super();
	}

	public synchronized InputStream getInputStream() throws IOException {
		if( in == null) {
			in = new BufferedInputStream(super.getInputStream());
		}
		return in;
	}

}
