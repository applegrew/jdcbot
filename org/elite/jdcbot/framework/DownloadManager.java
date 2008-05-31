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
 * This is used internall by the framework to schedule the downloads.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1
 * 
 */
public class DownloadManager extends DCIO {
    private Map<String, DownloadHandler> allDH;
    private jDCBot jdcbot;

    public DownloadManager(jDCBot bot) {
	jdcbot = bot;
	allDH = Collections.synchronizedMap(new HashMap<String, DownloadHandler>());
    }
    
    protected synchronized void close(){
	Collection<DownloadHandler> dh = allDH.values();
	for(DownloadHandler d:dh){
	    d.close();
	}
    }

    protected synchronized void tasksComplete(DownloadHandler dh) {
	allDH.remove(dh.getUserName());
    }

    protected void download(DUEntity de, User u) throws BotException {
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

    protected void download(String user, Socket socket, int N, String key) throws BotException {
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
	    e.printStackTrace();
	}
	allDH.get(user).notifyPassiveConnect(socket);
    }
}
