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

import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 26-May-08
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1
 */
public class UploadManager extends DCIO {
    private Map<String, UploadHandler> allUH;
    private jDCBot jdcbot;

    public UploadManager(jDCBot bot) {
	jdcbot = bot;
	allUH = Collections.synchronizedMap(new HashMap<String, UploadHandler>());
    }

    protected synchronized void tasksComplete(UploadHandler uh) {
	allUH.remove(uh.getUserName());
    }

    protected synchronized int getAllUHCount() {
	return allUH.size();
    }

    /**
     * Uploads to passive user <i>user</i>.
     * @param user
     * @throws BotException
     */
    protected void uploadPassive(String user) throws BotException {
	if (!jdcbot.UserExist(user)) {
	    throw new BotException(BotException.USRNAME_NOT_FOUND);
	}

	Socket socket = null;
	try {
	    socket = jdcbot.initConnectToMe(user, "Upload");
	} catch (Exception be) {
	    jdcbot.log.println("Exception in DownloadHandler thread: " + be.getMessage());
	    be.printStackTrace();
	    return;
	}
	UploadHandler uh;
	if (!allUH.containsKey(user)) {
	    uh = new UploadHandler(socket, jdcbot, this);
	    allUH.put(user, uh);
	} else
	    uh = allUH.get(user);
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
    protected void upload(String user, Socket socket, int N, String key) throws BotException {
	if (!jdcbot.UserExist(user)) {
	    throw new BotException(BotException.USRNAME_NOT_FOUND);
	}

	String buffer;

	try {
	    if (N != 0)
		N--;
	    buffer = "$Supports " + jdcbot.getBotClientProtoSupports() + "|$Direction Upload " + N + "|$Key " + key + "|";
	    SendCommand(buffer, socket);
	    jdcbot.log.println("From bot: " + buffer);
	} catch (Exception e) {
	    jdcbot.log.println("Exception by SendCommand in upload(): " + e.getMessage());
	    e.printStackTrace();
	}

	UploadHandler uh;
	if (!allUH.containsKey(user)) {
	    uh = new UploadHandler(socket, jdcbot, this);
	    allUH.put(user, uh);
	} else
	    uh = allUH.get(user);
	uh.startUploads();
    }

}
