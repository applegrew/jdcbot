/*
 * UploadHandler.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.zip.DeflaterInputStream;

import org.elite.jdcbot.shareframework.ShareManager;

/**
 * Created on 26-May-08<br>
 * Handels all the uploads to a single user for a session.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1.1
 */
public class UploadHandler extends InputThreadTarget {
    private Socket socket;
    private UploadManager um;
    private jDCBot jdcbot;
    private ShareManager sm;
    private TimeoutInputThread inputThread = null;
    private boolean isfirstUpload = true;
    private User user;
    private volatile boolean close;

    UploadHandler(User usr, Socket socket, jDCBot jdcbot, UploadManager um) {
	this.um = um;
	this.socket = socket;
	this.jdcbot = jdcbot;
	sm = jdcbot.getShareManager();
	user = usr;
	close = false;
    }

    public void startUploads() {
	if (inputThread == null)
	    try {
		inputThread = new TimeoutInputThread(this, socket.getInputStream());
	    } catch (IOException e) {
		jdcbot.log.println("IOException by socket.getInputStream() in startUploads(): " + e.getMessage());
		e.printStackTrace();
	    }
	close = false;
	isfirstUpload = true;
	inputThread.start();
    }

    public String getUserName() {
	return user.username();
    }

    public void close() throws IOException {
	inputThread.stop();
	close = true;
	socket.close();
	socket = null;
    }

    @Override
    public void handleCommand(String cmd) {
	upload(cmd);
    }

    public void upload(String cmd) { //Called by inputThread thread only.
	if (socket == null)
	    return;

	jdcbot.log.println("From remote client:" + cmd);

	boolean ZLIG = false;
	InputStream in = null;
	long fileLen = 0;
	String buffer;
	DUEntity due = null;

	if (cmd.startsWith("$ADCGET")) {
	    String params[] = parseRawCmd(cmd); //Parsing $ADCGET F S LZ then //Z is ' ZL1' or nothing
	    String fileType = params[1].toLowerCase().trim();
	    String file = params[2];
	    long start = Long.parseLong(params[3]);
	    fileLen = Long.parseLong(params[4]);
	    String Z = params[params.length - 1]; //Using this weird method because Z may or may not be present.

	    if (Z.equalsIgnoreCase("ZL1"))
		ZLIG = true;

	    int fType = DUEntity.FILE_TYPE;
	    if (fileType.equals("file")) {
		if (file.equals("files.xml.bz2") || file.equals("MyList.bz2"))
		    fType = DUEntity.FILELIST_TYPE;
		else
		    fType = DUEntity.FILE_TYPE;
	    } else if (fileType.equals("tthl"))
		fType = DUEntity.TTHL_TYPE;

	    if (isfirstUpload && fType != DUEntity.FILELIST_TYPE && !user.isGrantedExtraSlot() && um.getAllUHCount() >= jdcbot.getMaxUploadSlots()) {
		buffer = "$MaxedOut|";
		try {
		    SendCommand(buffer, socket);
		    jdcbot.log.println("From bot: " + buffer);
		} catch (Exception e) {
		    jdcbot.log.println("Exception by SendCommand in upload(): " + e.getMessage());
		    e.printStackTrace();
		} finally {
		    try {
			socket.close();
		    } catch (IOException e) {
			jdcbot.log.println("Exception by socket.close() in upload(): " + e.getMessage());
			e.printStackTrace();
		    }
		}
		return;
	    }

	    if (fType != DUEntity.FILELIST_TYPE)
		isfirstUpload = false;

	    try {
		if (fType == DUEntity.FILELIST_TYPE)
		    due = sm.getFileList();
		else
		    due = sm.getFile(file, fType, start, fileLen);
	    } catch (FileNotFoundException e1) {
		buffer = "$Error " + e1.getMessage() + "|";
		try {
		    SendCommand(buffer, socket);
		} catch (Exception e) {
		    jdcbot.log.println("Exception by SendCommand in upload(): " + e.getMessage());
		    e.printStackTrace();
		}
		jdcbot.log.println("From bot: " + buffer);
		return;
	    }

	    try {
		if (ZLIG) {
		    in = due.in;
		    buffer = "$ADCSend " + due.getFileType() + " " + due.file + " " + due.start + " " + due.len + " ZL1|";
		    SendCommand(buffer, socket);
		    jdcbot.log.println("From bot: " + buffer);
		} else {
		    in = new DeflaterInputStream(due.in);
		    buffer = "$ADCSend " + due.getFileType() + " " + due.file + " " + due.start + " " + due.len + "|";
		    SendCommand(buffer, socket);
		    jdcbot.log.println("From bot: " + buffer);
		}
	    } catch (Exception e) {
		jdcbot.log.println("Exception by SendCommand in upload(): " + e.getMessage());
		e.printStackTrace();
	    }
	}

	if (in != null) {
	    /*jdcbot.getDispatchThread().call(jdcbot, "onUploadStart", new Class[] { User.class, DUEntity.class }, user, due);*/
	    jdcbot.getDispatchThread().callOnUploadStart(user, due);
	    int len = 0, c;
	    try {
		while ((c = in.read()) != -1 && ++len <= fileLen && !close) {
		    socket.getOutputStream().write(c);
		}
		in.close();
	    } catch (IOException ioe) {
		jdcbot.log.println("IOException in startUploads(): " + ioe.getMessage());
		ioe.printStackTrace();
		/*jdcbot.getDispatchThread().call(jdcbot, "onUploadComplete",
		 new Class[] { User.class, DUEntity.class, boolean.class, BotException.class }, user, due, false,
		 new BotException(ioe.getMessage(), BotException.IO_ERROR));*/
		jdcbot.getDispatchThread().callOnUploadComplete(user, due, false, new BotException(ioe.getMessage(), BotException.IO_ERROR));
	    }
	    /*jdcbot.getDispatchThread().call(jdcbot, "onUploadComplete",
	     new Class[] { User.class, DUEntity.class, boolean.class, BotException.class }, user, due, true, null);*/
	    jdcbot.getDispatchThread().callOnUploadComplete(user, due, true, null);
	}
    }

    @Override
    public void onDisconnect() {
	um.tasksComplete(this);
    }
}
