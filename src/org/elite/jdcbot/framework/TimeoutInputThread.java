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
 * Created on 27-May-08<br>
 * This provides timeout to the wrapped InputThread.
 * When the timer expires it will close the wrapped
 * InputThread. This is used by UploadHandler so that
 * idle connections can be closed and slots can be freed
 * for the needy.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1
 */
public class TimeoutInputThread extends InputThread {
    private long timeout = 30000; //30 sec
    private Timer timer = null;

    /** Constructs thread that will read raw commands from hub
     *
     * @param inputThreadTrgt InputThreadTarget instance
     * @param in InputStream class from which we will read.
     */
    public TimeoutInputThread(InputThreadTarget inputThreadTrgt, InputStream in) {
	super(inputThreadTrgt, in, "TimeoutInputThread");
	timer = new Timer();
    }

    @Override
    protected void onReadingCommand() {
	timer.resetTimer();
    }

    @Override
    public void start() {
	super.start();
	timer.start();
	timer.resetTimer();
    }

    @Override
    public void stop() {
	timer.stopIt();
	super.stop();
    }

    private void onTimeout() {
	stop();
    }

    private class Timer extends Thread {
	private volatile boolean timerRunning = true;
	private long prevTime = -1;

	public void run() {
	    while (timerRunning) {
		try {
		    sleep(timeout);
		    if (prevTime != -1 && System.currentTimeMillis() - prevTime >= timeout)
			onTimeout();
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

	public void resetTimer() {
	    prevTime = System.currentTimeMillis();
	}

    }
}
