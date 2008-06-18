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

/**
 * Threads that reads raw commands from hub and passes them to classes
 * that implement InputThreadTarget.
 *
 * @since 0.5
 * @author Kokanovic Branko
 * @author AppleGrew
 * @version 0.7
 * 
 */
public class InputThread extends DCIO implements Runnable {
    private InputStream _in;
    private InputThreadTarget _inputThreadTrgt;
    private volatile boolean running = false;

    /** Constructs thread that will read raw commands from hub
     *
     * @param inputThreadTrgt InputThreadTarget instance
     * @param in InputStream class from which we will read.
     */
    public InputThread(InputThreadTarget inputThreadTrgt, InputStream in) {
	_inputThreadTrgt = inputThreadTrgt;
	_in = in;
	this.set_IOExceptionMsg("Disconnected");
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
	    e.printStackTrace(GlobalObjects.log);
	    _inputThreadTrgt.disconnected();
	}
    }

    protected void onReadingCommand() {}

    /**
     * Starts the InputThread thread.
     */
    public void start() {
	Thread th = new Thread(this, "InputThread");
	if (th.getState() == Thread.State.NEW) {
	    running = true;
	    th.start();
	    GlobalObjects.log.println("new InputThread thread started.");
	} else
	    throw new IllegalThreadStateException("Thread is already running");
    }

    public void stop() {
	running = false;
    }
}
