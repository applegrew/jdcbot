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

import org.elite.jdcbot.shareframework.SearchResultSet;
import org.elite.jdcbot.shareframework.SearchSet;
import org.slf4j.Logger;

/**
 * Created on 31-May-08
 * @since 0.7.1
 * @version 0.3
 * @author AppleGrew
 * 
 */
public class BotEventDispatchThread extends Thread {
	private static final Logger logger = GlobalObjects.getLogger(BotEventDispatchThread.class);

	private static enum Method {
		onDownloadComplete,
		onUploadComplete,
		onUploadStart,
		onUpdateMyInfo,
		onDownloadStart,
		onActiveSearch,
		onPassiveSearch,
		onChannelMessage,
		onPrivateMessage,
		onQuit,
		onJoin,
		onPublicMessage,
		onDisconnect,
		onBotQuit,
		onConnect2Client,
		onConnect,
		onSearchResult,
		onSendCommandFailed,
		onHubName
	}

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
				DispatchEntity de = null;
				de = dispatch.get(0);
				dispatch.remove(0);

				Method method = de.method;
				Object args[] = de.params;

				switch (method) {
				case onDownloadComplete:
					BotException e = (BotException) getArg(args, 3);
					boolean success = (Boolean) getArg(args, 2);
					if (_bot.getDownloadCentral() != null) {
						e =
							_bot.getDownloadCentral().onDownloadFinished((User) getArg(args, 0), (DUEntity) getArg(args, 1),
									success, e);
						if (success) {
							if (e != null)
								success = false;
						} else if (e == null)
							continue;
					}
					_bot.onDownloadComplete((User) getArg(args, 0), (DUEntity) getArg(args, 1), success, e);

					break;
				case onUploadComplete:
					_bot.onUploadComplete((User) getArg(args, 0), (DUEntity) getArg(args, 1), (Boolean) getArg(args, 2),
							(BotException) getArg(args, 3));

					break;
				case onUploadStart:
					_bot.onUploadStart((User) getArg(args, 0), (DUEntity) getArg(args, 1));

					break;
				case onUpdateMyInfo:
					_bot.onUpdateMyInfo((String) getArg(args, 0));

					break;
				case onDownloadStart:
					if (_bot.getDownloadCentral() != null)
						_bot.getDownloadCentral().onDownloadStart((DUEntity) getArg(args, 1), (User) getArg(args, 0));
					_bot.onDownloadStart((User) getArg(args, 0), (DUEntity) getArg(args, 1));

					break;
				case onPassiveSearch:
					_bot.onPassiveSearch((String) getArg(args, 0), (SearchSet) getArg(args, 1));

					break;
				case onActiveSearch:
					_bot.onActiveSearch((String) getArg(args, 0), (Integer) getArg(args, 1), (SearchSet) getArg(args, 2));

					break;
				case onChannelMessage:
					_bot.onChannelMessage((String) getArg(args, 0), (String) getArg(args, 1), (String) getArg(args, 2));

					break;
				case onPrivateMessage:
					_bot.onPrivateMessage((String) getArg(args, 0), (String) getArg(args, 1));

					break;
				case onQuit:
					_bot.onQuit((String) getArg(args, 0));

					break;
				case onJoin:
					_bot.onJoin((String) getArg(args, 0));

					break;
				case onPublicMessage:
					_bot.onPublicMessage((String) getArg(args, 0), (String) getArg(args, 1));

					break;
				case onDisconnect:
					_bot.onDisconnect();

					break;
				case onBotQuit:
					_bot.onBotQuit();

					break;
				case onConnect2Client:
					_bot.onConnect2Client();

					break;
				case onConnect:
					_bot.onConnect();

					break;
				case onSearchResult:
					_bot.onSearchResult((String) getArg(args, 0), (String) getArg(args, 1), (Integer) getArg(args, 2),
							(SearchResultSet) getArg(args, 3), (Integer) getArg(args, 4), (Integer) getArg(args, 5), (String) getArg(
									args, 6));

					break;
				case onSendCommandFailed:
					_bot.onSendCommandFailed((String) getArg(args, 0), (Throwable) getArg(args, 1), (JMethod) getArg(args, 1));
					break;
				case onHubName:
					_bot.onHubName((String) getArg(args, 0));
					break;
				default:
					try {
						throw new NoSuchMethodException("Method :" + method);
					} catch (NoSuchMethodException nsme) {
						logger.error("No method " + method + " found.", nsme);
					}
				}
			}
			try {
				if (dispatch.isEmpty()) {
					sleep(100L);
				}
			} catch (InterruptedException e) {}
		}
	}

	private Object getArg(Object args[], int i) {
		return (args == null ? null : args[i]);
	}

	public void stopIt() {
		running = false;
		this.interrupt();
	}

	private class DispatchEntity {
		public Method method;
		public Object params[];
	}

	private void addToDispath(DispatchEntity de) {
		dispatch.add(de);
		this.interrupt();
	}

	//*********Proxy functions*********/
	void callOnDownloadComplete(User user, DUEntity due, boolean success, BotException e) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onDownloadComplete;
		de.params = new Object[] { user, due, success, e };
		addToDispath(de);
	}

	void callOnUploadComplete(User user, DUEntity due, boolean success, BotException e) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onUploadComplete;
		de.params = new Object[] { user, due, success, e };
		addToDispath(de);
	}

	void callOnUploadStart(User user, DUEntity due) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onUploadStart;
		de.params = new Object[] { user, due };
		addToDispath(de);
	}

	void callOnUpdateMyInfo(String user) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onUpdateMyInfo;
		de.params = new Object[] { user };
		addToDispath(de);
	}

	void callOnDownloadStart(User user, DUEntity due) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onDownloadStart;
		de.params = new Object[] { user, due };
		addToDispath(de);
	}

	void callOnPassiveSearch(String user, SearchSet search) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onPassiveSearch;
		de.params = new Object[] { user, search };
		addToDispath(de);
	}

	void callOnActiveSearch(String ip, int port, SearchSet search) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onActiveSearch;
		de.params = new Object[] { ip, port, search };
		addToDispath(de);
	}

	void callOnChannelMessage(String user, String channel, String message) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onChannelMessage;
		de.params = new Object[] { user, channel, message };
		addToDispath(de);
	}

	void callOnPrivateMessage(String user, String message) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onPrivateMessage;
		de.params = new Object[] { user, message };
		addToDispath(de);
	}

	void callOnQuit(String user) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onQuit;
		de.params = new Object[] { user };
		addToDispath(de);
	}

	void callOnJoin(String user) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onJoin;
		de.params = new Object[] { user };
		addToDispath(de);
	}

	void callOnPublicMessage(String user, String message) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onPublicMessage;
		de.params = new Object[] { user, message };
		addToDispath(de);
	}

	void callOnDisconnect() {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onDisconnect;
		de.params = null;
		addToDispath(de);
	}

	void callOnBotQuit() {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onBotQuit;
		de.params = null;
		addToDispath(de);
	}

	void callOnConnect2Client() {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onConnect2Client;
		de.params = null;
		addToDispath(de);
	}

	void callOnConnect() {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onConnect;
		de.params = null;
		addToDispath(de);
	}

	void callOnSearchResult(String senderNick, String senderIP, int senderPort, SearchResultSet result, int free_slots, int total_slots,
			String hubName) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onSearchResult;
		de.params = new Object[] { senderNick, senderIP, senderPort, result, free_slots, total_slots, hubName };
		addToDispath(de);
	}

	public void callOnSendCommandFailed(String msg, Throwable e, JMethod src) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onSendCommandFailed;
		de.params = new Object[] { msg, e, src };
		addToDispath(de);
	}
	
	public void callOnHubName(String hubName) {
		DispatchEntity de = new DispatchEntity();
		de.method = Method.onHubName;
		de.params = new Object[] { hubName };
		addToDispath(de);
	}
}
