/*
 * UDPInputThread.java
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
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.slf4j.Logger;

/**
 * Created on 06-Jun-08<br>
 * It is like InpuThread but
 * it instead reads and interprets
 * UDP packets.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1.2
 */
public class UDPInputThread implements Runnable {
	private static final Logger logger = GlobalObjects.getLogger(UDPInputThread.class);
	private DatagramPacket _packet;
	private DatagramSocket _socket;
	private byte buffer[];
	private UDPInputThreadTarget _inputThreadTrgt;
	private volatile boolean running = false;

	/** Constructs thread that will read raw commands from hub
	 *
	 * @param inputThreadTrgt InputThreadTarget instance
	 * @param in InputStream class from which we will read.
	 */
	public UDPInputThread(UDPInputThreadTarget inputThreadTrgt, DatagramSocket socket) {
		_inputThreadTrgt = inputThreadTrgt;
		_socket = socket;
		buffer = new byte[10240]; //Max. possible size is 64KB
		_packet = new DatagramPacket(buffer, buffer.length);
	}

	public void run() {
		try {
			running = true;
			while (running) {
				//Wait to receive a datagram
				_socket.receive(_packet);

				_inputThreadTrgt.handleUDPCommand(new String(buffer, 0, _packet.getLength()), _packet.getAddress().getHostAddress(),
						_packet.getPort());

				//Reset the length of the packet before reusing it.
				_packet.setLength(buffer.length);
			}
		} catch (IOException e) {
			if (running) {
				logger.error("Exception in run().", e);
				_inputThreadTrgt.onUDPExceptionClose(e);
			}
		}
	}

	/**
	 * Starts the InputThread thread.
	 */
	public void start() {
		Thread th = new Thread(this, "UDPInputThread");
		if (th.getState() == Thread.State.NEW) {
			th.setDaemon(true);
			running = true;
			th.start();
			logger.debug("new UDPInputThread thread started.");
		} else
			throw new IllegalThreadStateException("Thread is already running");
	}

	public void stop() {
		running = false;
		_socket.close();
	}

	public boolean isClosed() {
		return !running;
	}
}
