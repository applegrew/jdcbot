/*
 * DownloadManager.java
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

import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Created on 26-May-08<br>
 * This is used internally by the framework to schedule the downloads.
 * <p>
 * This class is thread safe.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1.2
 * 
 */
public class DownloadManager extends DCIO {
	private static final Logger logger = GlobalObjects.getLogger(DownloadManager.class);
	private Map<String, DownloadHandler> allDH;
	private jDCBot jdcbot;

	DownloadManager(jDCBot bot) {
		jdcbot = bot;
		allDH = Collections.synchronizedMap(new HashMap<String, DownloadHandler>());
	}

	void close() {
		synchronized (allDH) {
			Collection<DownloadHandler> dh = allDH.values();
			for (DownloadHandler d : dh) {
				d.close();
			}
		}
	}

	void tasksComplete(DownloadHandler dh) {
		allDH.remove(dh.getUserName());
	}

	int getAllDHCount() {
		return allDH.size();
	}

	/**
	 * Called by User to start a new download.
	 * <p>
	 * This method will block till connection to remote
	 * client is made (or failed).
	 * 
	 * @param de
	 * @param u
	 * @throws BotException
	 */
	void download(DUEntity de, User u) throws BotException {
		if (!jdcbot.isConnected())
			throw new BotException(BotException.Error.NOT_CONNECTED_TO_HUB);

		if (u.username().equalsIgnoreCase(jdcbot._botname))
			throw new BotException(BotException.Error.CANNOT_DOWNLOAD_FROM_SELF);

		if (!u.isActive() && jdcbot.isPassive())
			throw new BotException(BotException.Error.DOWNLOAD_NOT_POSSIBLE_BOTH_PASSIVE);

		if (!jdcbot.UserExist(u.username()))
			throw new BotException(BotException.Error.USERNAME_NOT_FOUND);

		if (!u.hasInfo())
			throw new BotException(BotException.Error.USER_HAS_NO_INFO);

		DownloadHandler dh;
		synchronized (allDH) {
			if (!allDH.containsKey((u.username()))) {
				if (jdcbot.getFreeDownloadSlots() <= 0) {
					throw new BotException(BotException.Error.NO_FREE_DOWNLOAD_SLOTS);
				}
				dh = new DownloadHandler(u, jdcbot, this);
				allDH.put(u.username(), dh);
			} else
				dh = allDH.get(u.username());

			if (dh.isConnectionFailed())
				throw new BotException(BotException.Error.CONNECTION_TO_REMOTE_CLIENT_FAILED);
			dh.download(de);
		}
	}

	void cancelDownload(DUEntity de, User u) {
		synchronized (allDH) {
			DownloadHandler dh = allDH.get(u.username());
			if (dh != null)
				dh.cancelDownload(de);
		}
	}

	/**
	 * This is called by jDCBot in response to $ConnectToMe
	 * when in passive mode.
	 * @param user
	 * @param socket
	 * @param N
	 * @param key
	 * @throws BotException
	 */
	void download(String user, Socket socket, int N, String key) throws BotException {
		if (!jdcbot.isConnected())
			throw new BotException(BotException.Error.NOT_CONNECTED_TO_HUB);

		if (user.equalsIgnoreCase(jdcbot._botname))
			throw new BotException(BotException.Error.CANNOT_DOWNLOAD_FROM_SELF);

		User u = jdcbot.getUser(user);
		if (u == null)
			throw new BotException(BotException.Error.USERNAME_NOT_FOUND);

		if (!u.hasInfo())
			throw new BotException(BotException.Error.USER_HAS_NO_INFO);

		if (jdcbot.getFreeDownloadSlots() <= 0)
			throw new BotException(BotException.Error.NO_FREE_DOWNLOAD_SLOTS);

		synchronized (allDH) {
			if (!allDH.containsKey(user))
				throw new BotException(BotException.Error.A_DOWNLOAD_WAS_NOT_REQUESTED);

			try {
				String buffer =
					"$Supports " + jdcbot.getBotClientProtoSupports() + "|$Direction Download " + (N + 1) + "|$Key " + key + "|";
				SendCommand(buffer, socket);
				logger.debug("From bot: " + buffer);
			} catch (Exception e) {
				logger.error("Exception while sending raw command: " + e.getMessage(), e);
			}
			allDH.get(user).notifyPassiveConnect(socket);
		}
	}
}
