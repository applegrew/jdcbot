/*
 * TimerThread.java
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

package org.elite.jdcbot.util;

import org.elite.jdcbot.framework.jDCBot;

/**
 * Simple abstract class for generating OnTimer event.
 *
 * You are encouraged to extends this thread with your own class (example is FloodMessageThread)
 * since it enables you to have more than one onTimer event. So you would have one class which prints
 * users joined hub on every hour on main chat and another that prints current weather on every two hours.
 *
 * @since 0.5
 * @author  Kokanovic Branko
 * @author AppleGrew
 * @version    0.7
 */
public abstract class TimerThread extends Thread {

    private long _wait_time;
    private long _startup_wait_time;
    protected jDCBot _bot;
    private volatile boolean running = true;

    /**
     * Constructs new thread that triggers onTimer event.
     *
     * @param bot jDCBot instance needed to send messages...
     * @param wait_time Time (in ms) between triggers.
     */
    public TimerThread(jDCBot bot, long wait_time, String ThreadName) {
	this(bot, wait_time, ThreadName, 0);
    }

    public TimerThread(jDCBot bot, long wait_time, String ThreadName, long startup_wait_time) {
	super(ThreadName);
	_bot = bot;
	_wait_time = wait_time;
	_startup_wait_time = startup_wait_time;
    }

    public void run() {
	if (_startup_wait_time != 0) {
	    try {
		sleep(_startup_wait_time);
		onTimerStart();
	    } catch (InterruptedException e1) {
		e1.printStackTrace();
	    }
	}
	while (running) {
	    onTimer();
	    try {
		sleep(_wait_time);
	    } catch (InterruptedException e) {}
	}
    }

    /**
     * Called every wait_time
     * <p>
     * The implementation of this method in the TimerThread abstract class
     * performs no actions and may be overridden as required.
     */
    protected void onTimer() {}

    protected void onTimerStart() {}

    /**
     * Stops the thread.
     */
    public synchronized void stopIt() {
	running = false;
	try {
	    interrupt();
	} catch (Exception e) {}
    }

}
