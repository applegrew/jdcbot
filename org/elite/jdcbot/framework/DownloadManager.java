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

/**
 * Created on 26-May-08<br>
 * This is used internall by the framework to schedule the downloads.<br>
 * TODO: Create a thread that will make searches and search for alternative download sources.
 * It must resolve the conflicts arising out of it among the different DownloadHandlers.
 * TODO: On 2nd thought maybe this thread should be run in another class which will have
 * reference to all running jDCBots (since on jDCBot handels only one hub). Or should jDCBot handel all the hubs?
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1
 * 
 */
public class DownloadManager extends DCIO {
    private Map<String, DownloadHandler> allDH;
    private jDCBot jdcbot;

    DownloadManager(jDCBot bot) {
	jdcbot = bot;
	allDH = Collections.synchronizedMap(new HashMap<String, DownloadHandler>());
    }

    synchronized void close() {
	Collection<DownloadHandler> dh = allDH.values();
	for (DownloadHandler d : dh) {
	    d.close();
	}
    }

    synchronized void tasksComplete(DownloadHandler dh) {
	allDH.remove(dh.getUserName());
    }

    void download(DUEntity de, User u) throws BotException {
	if (!u.isActive() && jdcbot.isPassive()) {
	    throw new BotException(BotException.DOWNLOAD_NOT_POSSIBLE_BOTH_PASSIVE);
	}

	DownloadHandler dh;
	if (!allDH.containsKey((u.username()))) {
	    dh = new DownloadHandler(u, jdcbot, this);
	    allDH.put(u.username(), dh);
	} else
	    dh = allDH.get(u.username());
	dh.download(de);
    }

    synchronized void cancelDownload(DUEntity de, User u) {
	DownloadHandler dh = allDH.get(u.username());
	if (dh != null)
	    dh.cancelDownload(de);
    }

    void download(String user, Socket socket, int N, String key) throws BotException {
	if (!allDH.containsKey(user))
	    throw new BotException(BotException.A_DOWNLOAD_WAS_NOT_REQUESTED);

	if (!jdcbot.UserExist(user)) {
	    throw new BotException(BotException.USRNAME_NOT_FOUND);
	}

	if (allDH.size() >= jdcbot.getMaxDownloadSlots()) {
	    throw new BotException(BotException.NO_FREE_DOWNLOAD_SLOTS);
	}

	try {
	    String buffer = "$Supports " + jdcbot.getBotClientProtoSupports() + "|$Direction Download " + (N + 1) + "|$Key " + key + "|";
	    SendCommand(buffer, socket);
	    jdcbot.log.println("From bot: " + buffer);
	} catch (Exception e) {
	    jdcbot.log.println("Exception while sending raw command: " + e.getMessage());
	    e.printStackTrace(jdcbot.log);
	}
	allDH.get(user).notifyPassiveConnect(socket);
    }
}
