/*
 * InputThead.java
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

import java.io.*;
import java.net.SocketException;

import org.slf4j.Logger;

/**
 * Threads that reads raw commands from hub and passes them to classes
 * that implement InputThreadTarget.
 *
 * @since 0.5
 * @author Kokanovic Branko
 * @author AppleGrew
 * @version 0.7.1
 * 
 */
class InputThread extends DCIO implements Runnable {
	private static final Logger logger = GlobalObjects.getLogger(InputThread.class);
	private InputStream _in;
	private InputThreadTarget _inputThreadTrgt;
	private volatile boolean running = false;
	private String threadName;

	/** 
	 * Constructs thread that will read raw commands from hub
	 *
	 * @param inputThreadTrgt InputThreadTarget instance
	 * @param in InputStream class from which we will read.
	 */
	public InputThread(InputThreadTarget inputThreadTrgt, InputStream in) {
		this(inputThreadTrgt, in, "InputThread");
	}

	public InputThread(InputThreadTarget inputThreadTrgt, InputStream in, String threadName) {
		_inputThreadTrgt = inputThreadTrgt;
		_in = in;
		this.set_IOExceptionMsg("Disconnected");
		if (threadName == null)
			threadName = "InputThread";
		this.threadName = threadName;
	}

	public void run() {
		try {
			running = true;
			while (running) {
				String rawCommand = null;
				rawCommand = this.ReadCommand(_in);
				if ((rawCommand == null) || (rawCommand.length() == 0)) {
					running = false;
					_inputThreadTrgt.disconnected();
				} else
					_inputThreadTrgt.handleCommand(rawCommand);
				onReadingCommand();
			}
		} catch (Exception e) {
			if (!(e instanceof SocketException && e.getMessage().equals("Socket closed")))
				logger.error("Exception in run()", e);
			_inputThreadTrgt.disconnected();
		}
	}

	protected void onReadingCommand() {}

	/**
	 * Starts the InputThread thread.
	 */
	public void start() {
		Thread th = new Thread(this, threadName);
		if (th.getState() == Thread.State.NEW) {
			running = true;
			th.start();
			logger.debug("new InputThread thread started.");
		} else
			throw new IllegalThreadStateException("Thread is already running");
	}

	public void stop() {
		running = false;
		try {
			_in.close();
		} catch (IOException e) {
			logger.error("Exception in stop()", e);
		}
	}
}
