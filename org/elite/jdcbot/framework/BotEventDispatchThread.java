/*
 * BotEventDispatchThread.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 31-May-08
 * @since 0.7.1
 * @version 0.1
 * @author AppleGrew
 * 
 */
public class BotEventDispatchThread extends Thread {
    private final int ON_DOWNLOAD_COMPLETE = 1;
    private final int ON_UPLOAD_COMPLETE = 2;
    private final int ON_UPLOAD_START = 3;
    private final int ON_UPDATE_MY_INFO = 4;
    private final int ON_DOWNLOAD_START = 5;

    private List<DispatchEntity> dispatch;
    private volatile boolean running;
    private jDCBot _bot;

    public BotEventDispatchThread(jDCBot bot) {
	super("jDCBot-EventDispatchThread");
	dispatch = Collections.synchronizedList(new ArrayList<DispatchEntity>());
	_bot = bot;
	running = true;
	start();
    }

    public void run() {
	while (running) {
	    while (!dispatch.isEmpty()) {
		DispatchEntity de = dispatch.get(0);
		dispatch.remove(0);

		int method = de.method;
		Object args[] = de.params;

		if (method == ON_DOWNLOAD_COMPLETE) {
		    BotException e = (BotException) getArg(args, 3);
		    boolean success = (Boolean) getArg(args, 2);
		    if (_bot.getDownloadCentral() != null) {
			e = _bot.getDownloadCentral().onDownloadFinished((User) getArg(args, 0), (DUEntity) getArg(args, 1), success, e);
			if (success) {
			    if (e != null)
				success = false;
			} else if (e == null)
			    continue;
		    }
		    _bot.onDownloadComplete((User) getArg(args, 0), (DUEntity) getArg(args, 1), success, e);
		} else if (method == ON_UPLOAD_COMPLETE)
		    _bot.onUploadComplete((User) getArg(args, 0), (DUEntity) getArg(args, 1), (Boolean) getArg(args, 2),
			    (BotException) getArg(args, 3));
		else if (method == ON_UPLOAD_START)
		    _bot.onUploadStart((User) getArg(args, 0), (DUEntity) getArg(args, 1));
		else if (method == ON_UPDATE_MY_INFO)
		    _bot.onUpdateMyInfo((String) getArg(args, 0));
		else if (method == ON_DOWNLOAD_START) {
		    if (_bot.getDownloadCentral() != null)
			_bot.getDownloadCentral().onDownloadStart((DUEntity) getArg(args, 1), (User) getArg(args, 0));
		    _bot.onDownloadStart((User) getArg(args, 0), (DUEntity) getArg(args, 1));
		} else
		    try {
			throw new NoSuchMethodException("Method number:" + method);
		    } catch (NoSuchMethodException e) {
			_bot.log.println("No method with number " + method + " found.");
			e.printStackTrace();
		    }
	    }
	    try {
		sleep(60000L);
	    } catch (InterruptedException e) {}
	}
    }

    private Object getArg(Object args[], int i) {
	return (args[i] == null ? null : args[i]);
    }

    void callOnDownloadComplete(User user, DUEntity due, boolean success, BotException e) {
	DispatchEntity de = new DispatchEntity();
	de.method = ON_DOWNLOAD_COMPLETE;
	de.params = new Object[] { user, due, success, e };
	dispatch.add(de);

	this.interrupt();
    }

    void callOnUploadComplete(User user, DUEntity due, boolean success, BotException e) {
	DispatchEntity de = new DispatchEntity();
	de.method = ON_UPLOAD_COMPLETE;
	de.params = new Object[] { user, due, success, e };
	dispatch.add(de);

	this.interrupt();
    }

    void callOnUploadStart(User user, DUEntity due) {
	DispatchEntity de = new DispatchEntity();
	de.method = ON_UPLOAD_START;
	de.params = new Object[] { user, due };
	dispatch.add(de);

	this.interrupt();
    }

    void callOnUpdateMyInfo(String user) {
	DispatchEntity de = new DispatchEntity();
	de.method = ON_UPDATE_MY_INFO;
	de.params = new Object[] { user };
	dispatch.add(de);

	this.interrupt();
    }

    void callOnDownloadStart(User user, DUEntity due) {
	DispatchEntity de = new DispatchEntity();
	de.method = ON_DOWNLOAD_START;
	de.params = new Object[] { user, due };
	dispatch.add(de);

	this.interrupt();
    }

    public void stopIt() {
	running = false;
	this.interrupt();
    }

    private class DispatchEntity {
	public int method;
	public Object params[];
    }
}
