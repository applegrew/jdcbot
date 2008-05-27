/*
 * TimeoutInputThread.java
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

import java.io.InputStream;

/**
 * Created on 27-May-08
 *
 * @author AppleGrew
 * 
 */
public class TimeoutInputThread extends InputThread {
    private long timeout = 60000L;
    private Timer timer = null;

    /** Constructs thread that will read raw commands from hub
     *
     * @param inputThreadTrgt InputThreadTarget instance
     * @param in InputStream class from which we will read.
     */
    public TimeoutInputThread(InputThreadTarget inputThreadTrgt, InputStream in) {
	super(inputThreadTrgt, in);
	timer = new Timer();
    }

    protected void onReadingCommand() {
	timer.stopIt();
	timer = new Timer();
	timer.start();
    }

    public void start() {
	super.start();
    }

    private void onTimeout() {
	super.stop();
	timer.stopIt();
    }

    private class Timer extends Thread {
	private boolean timerRunning = true;

	public void run() {
	    while (timerRunning) {
		try {
		    sleep(timeout);
		    onTimeout();
		    timerRunning = false;
		} catch (InterruptedException e) {}
	    }
	}

	/**
	 * Stops the thread.
	 */
	public synchronized void stopIt() {
	    timerRunning = false;
	    try {
		interrupt();
	    } catch (Exception e) {}
	}

    }
}
