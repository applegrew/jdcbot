/*
 * UploadManager.java
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

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Created on 26-May-08<br>
 * Manages all the uploads to all the users.
 * <p>
 * The framework handels this hence you need not bother
 * about this.
 * <p>
 * This class is thread safe.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1.2
 */
public class UploadManager extends DCIO {
	private static final Logger logger = GlobalObjects.getLogger(UploadManager.class);
	private Map<String, UploadHandler> allUH;
	private jDCBot jdcbot;

	UploadManager(jDCBot bot) {
		jdcbot = bot;
		allUH = Collections.synchronizedMap(new HashMap<String, UploadHandler>());
	}

	synchronized void close() {
		Collection<UploadHandler> uh = allUH.values();
		synchronized (allUH) {
			for (UploadHandler u : uh) {
				try {
					u.close();
				} catch (IOException e) {
					logger.error("Exception in close()", e);
				}
			}
		}
	}

	synchronized void cancelUpoad(User u) {
		UploadHandler uh = allUH.get(u.username());
		if (uh != null)
			uh.cancelUpload();
	}

	synchronized void tasksComplete(UploadHandler uh) {
		allUH.remove(uh.getUserName());
	}

	synchronized int getAllUHCount() {
		return allUH.size();
	}

	/**
	 * Uploads to passive user <i>user</i>.
	 * @param user
	 * @throws BotException
	 */
	void uploadPassive(String user) throws BotException {
		if (!jdcbot.isConnected()) {
			throw new BotException(BotException.Error.NOT_CONNECTED_TO_HUB);
		}
		if (!jdcbot.UserExist(user)) {
			throw new BotException(BotException.Error.USERNAME_NOT_FOUND);
		}
		if (jdcbot.getUser(user).isUploadToUserBlocked()) {
			throw new BotException(BotException.Error.UPLOAD_TO_USER_BLOCKED);
		}

		Socket socket = null;
		try {
			socket = jdcbot.initConnectToMe(user, "Upload");
		} catch (Exception be) {
			logger.error("Exception in DownloadHandler thread: " + be.getMessage(), be);
			return;
		}
		UploadHandler uh;
		synchronized (allUH) {
			if (!allUH.containsKey(user)) {
				uh = new UploadHandler(jdcbot.getUser(user), socket, jdcbot, this);
				allUH.put(user, uh);
			} else
				uh = allUH.get(user);
		}
		uh.startUploads();
	}

	/**
	 * Uploads to active user <i>user</i>.
	 * @param user
	 * @param socket
	 * @param N
	 * @param key
	 * @throws BotException
	 */
	void upload(String user, Socket socket, int N, String key) throws BotException {
		if (!jdcbot.isConnected()) {
			throw new BotException(BotException.Error.NOT_CONNECTED_TO_HUB);
		}
		if (!jdcbot.UserExist(user)) {
			throw new BotException(BotException.Error.USERNAME_NOT_FOUND);
		}
		if (jdcbot.getUser(user).isUploadToUserBlocked()) {
			throw new BotException(BotException.Error.UPLOAD_TO_USER_BLOCKED);
		}

		String buffer;

		try {
			if (N != 0)
				N--;
			buffer = "$Supports " + jdcbot.getBotClientProtoSupports() + "|$Direction Upload " + N + "|$Key " + key + "|";
			SendCommand(buffer, socket);
			logger.debug("From bot: " + buffer);
		} catch (Exception e) {
			logger.error("Exception by SendCommand in upload(): " + e.getMessage(), e);
		}

		UploadHandler uh;
		synchronized (allUH) {
			if (!allUH.containsKey(user)) {
				uh = new UploadHandler(jdcbot.getUser(user), socket, jdcbot, this);
				allUH.put(user, uh);
			} else
				uh = allUH.get(user);
		}
		uh.startUploads();
	}

}
