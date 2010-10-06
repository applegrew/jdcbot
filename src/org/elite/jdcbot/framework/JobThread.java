package org.elite.jdcbot.framework;

/*
 * JobThread.java
 *
 * Copyright (C) 2010 AppleGrew
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

import java.util.LinkedList;
import java.util.Queue;

/**
 * 
 * @author AppleGrew
 * @since 1.1.4
 * @version 1.0
 */
public class JobThread extends Thread {
	
	/*
	 * For auto-numbering threads.
	 */ 
    private static int threadInitNumber;
    private volatile boolean run = true;
	private Queue<Runnable> jobs = new LinkedList<Runnable>();
	
	private static synchronized int nextThreadNum() {
		return threadInitNumber++;
	}
	
	public JobThread() {
		super("Job Thread - " + nextThreadNum());
	}
	
	public JobThread(String threadName) {
		super(threadName);
	}
	
	public void terminate() {
		run = false;
		interrupt();
	}
	
	public void invokeLater(Runnable job) {
		synchronized (jobs) {
			jobs.add(job);
		}
		interrupt();
	}
	
	private Runnable getJob() {
		synchronized (jobs) {
			return jobs.poll();
		}
	}
	
	public void run() {
		while(run) {
			try {
				Runnable job;
				while((job = getJob()) != null) {
					job.run();
				}
				interrupted(); //Clearing interrupt flag; if any.
				sleep(6000L);
			} catch (InterruptedException e) {}
		}
	}
}
