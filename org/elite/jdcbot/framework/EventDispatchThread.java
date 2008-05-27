/*
 * EventDispatchThread.java
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

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 27-May-08<br>
 * The task of this class is to run a thread (jDCBot-EventDispatchThread), which can be used to call
 * methods of various methods, making the code of those methods run in this thread, asynchornously from
 * the calling method. 
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1
 * 
 */
public class EventDispatchThread extends Thread {
    private List<DispatchEntity> dispatch;
    private volatile boolean running;

    public EventDispatchThread() {
	super("jDCBot-EventDispatchThread");
	dispatch = Collections.synchronizedList(new ArrayList<DispatchEntity>());
	running = true;
	start();
    }

    public void run() {
	while (running) {
	    while (!dispatch.isEmpty()) {
		DispatchEntity de = dispatch.get(0);
		dispatch.remove(0);
		if (de.method != null) {
		    try {
			de.method.invoke(de.owner, de.params);
		    } catch (IllegalArgumentException e) {
			e.printStackTrace();
		    } catch (IllegalAccessException e) {
			e.printStackTrace();
		    } catch (InvocationTargetException e) {
			e.printStackTrace();
		    }
		}
	    }
	    try {
		sleep(60000L);
	    } catch (InterruptedException e) {}
	}
    }

    /**
     * Use this to invoke a call to a method via this (jDCBot-EventDispatchThread) thread.
     * @param owner The object of whom <i>method</i> is memeber.
     * @param method The case-sensitive name of the method.
     * @param args All the arguments of the <i>method</i>. <b>Note:</b> All arguments should be Objects.
     * If <i>method</i> has no arguments then no need to pass any any argument to this parameter, it is
     * a varag and will automatically set to null. 
     */
    public void call(Object owner, String method, Object... args) {
	Class params[] = null;
	if (args != null)
	    params = new Class[args.length];

	for (int i = 0; i < args.length; i++)
	    params[i] = args[i].getClass();

	Method m = null;
	try {
	    m = owner.getClass().getMethod(method, params);
	} catch (SecurityException e) {
	    e.printStackTrace();
	} catch (NoSuchMethodException e) {
	    e.printStackTrace();
	}
	DispatchEntity de = new DispatchEntity();
	de.method = m;
	de.owner = owner;
	de.params = params;
	dispatch.add(de);

	this.interrupt();
    }

    public void stopIt() {
	running = false;
	this.interrupt();
    }

    private class DispatchEntity {
	public Method method;
	public Object owner;
	public Object params[];
    }
}
