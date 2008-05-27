/*
 * DownloadHandler.java
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
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.DeflaterInputStream;

import org.apache.tools.bzip2.CBZip2InputStream;

/**
 * Created on 26-May-08
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1
 * 
 */
public class DownloadHandler extends DCIO implements Runnable {
    private List<DUEntity> DownloadEntityQ;
    private User _u;
    private jDCBot _jdcbot;
    private DownloadManager _dm;
    private Socket _socket = null;
    private volatile boolean close = false;
    private volatile boolean threadstarted = false;
    private Thread th;

    public DownloadHandler(User user, jDCBot jdcbot, DownloadManager dm) {
	_u = user;
	_jdcbot = jdcbot;
	_dm = dm;
	th = null;
	DownloadEntityQ = Collections.synchronizedList(new ArrayList<DUEntity>());
    }

    public void close() {
	close = true;
	if (threadstarted)
	    this.notify();
    }

    public void download(DUEntity de) {
	if (!DownloadEntityQ.contains(de))
	    DownloadEntityQ.add(de);
	if (!threadstarted) {
	    threadstarted = true;
	    th = new Thread(this);
	    th.start();
	}
    }

    public void notifyPassiveConnect(Socket socket) {
	_socket = socket;
	th.interrupt();
    }

    public String getUserName() {
	return _u.username();
    }

    public void run() {
	String buffer;
	String params[];
	try {
	    if (!_jdcbot.isPassive())
		_socket = _jdcbot.initConnectToMe(_u.username(), "Download");
	    else {
		buffer = "$RevConnectToMe " + _jdcbot.botname() + " " + _u.username() + "|";
		_jdcbot.log.println("From bot: " + buffer);
		_jdcbot.SendCommand(buffer);
	    }
	} catch (Exception be) {
	    _jdcbot.log.println("Exception in DownloadHandler thread: " + be.getMessage());
	    be.printStackTrace();
	    return;
	}

	if (_jdcbot.isPassive()) {
	    int count = 0;
	    while (_socket == null) {
		count++;
		try {
		    Thread.sleep(60000L); //Wait for passive connection
		} catch (InterruptedException e1) {
		    _jdcbot.log.println("DownloadHandler thread woken up.");
		}
		if (_socket == null && count>=3) {
		    _jdcbot.log.println("Timeout. Waited for too long for remote client's ("+_u.username()+") connection.");
		    return;
		}
	    }
	}

	while (!DownloadEntityQ.isEmpty() && !close) {
	    DUEntity de = DownloadEntityQ.get(0);
	    DownloadEntityQ.remove(0);

	    boolean ZLIG = false;
	    long fileLen = 0;
	    try {
		if (_u.isSupports("ADCGet")) {
		    if (de.fileType == DUEntity.FILELIST_TYPE) {
			//If the user wants to download the filelist.
			de.start = 0;
			de.len = -1;
			if (_u.isSupports("XmlBZList"))
			    de.file = "files.xml.bz2";
			else if (_u.isSupports("BZList"))
			    de.file = "MyList.bz2";
		    }

		    if (_u.isSupports("ZLIG")) {
			buffer = "$ADCGET " + de.getFileType() + " " + de.file + " " + de.start + " " + de.len + " ZL1|";
			_jdcbot.log.println("From bot: " + buffer);
			SendCommand(buffer, _socket);
		    } else {
			buffer = "$ADCGET " + de.getFileType() + " " + de.file + " " + de.start + " " + de.len + "|";
			_jdcbot.log.println("From bot: " + buffer);
			SendCommand(buffer, _socket);
		    }

		    buffer = ReadCommand(_socket);//Reading $ADCSnd K F S LZ| OR $MaxedOut| OR $Error msg|//Z is ' ZL1' or nothing.
		    _jdcbot.log.println(buffer);
		    if (buffer.equals("$MaxedOut|")) {
			throw new BotException(BotException.NO_FREE_SLOTS);
		    }
		    if (buffer.startsWith("$Error")) {
			String err = "Transfer failed due to: " + buffer.substring(buffer.indexOf(' ') + 1, buffer.indexOf('|'));
			_jdcbot.log.println(err);
			throw new IOException(err);
		    }
		    params = parseRawCmd(buffer);
		    fileLen = Long.parseLong(params[4]);
		    String Z = params[params.length - 1];

		    if (Z.equalsIgnoreCase("ZL1"))
			ZLIG = true;
		}
	    } catch (Exception be) {
		_jdcbot.log.println("Exception in DownloadHandler thread: " + be.getMessage());
		be.printStackTrace();
		return;
	    }

	    try {
		InputStream in = null;
		if (ZLIG)
		    in = new DeflaterInputStream(_socket.getInputStream());
		else
		    in = _socket.getInputStream();

		int len = 0, c;

		if (de.fileType == DUEntity.FILELIST_TYPE && (_u.isSupports("XmlBZList") || _u.isSupports("BZList"))) {
		    c = in.read(); //Reading out the first two bytes - BZ. Else decompression will fail and NullPoiterException will be thrown.
		    if (c != -1)
			in.read();
		    in = new CBZip2InputStream(in);
		}

		while ((c = in.read()) != -1 && ++len <= fileLen) {
		    de.os.write(c);
		}
		de.os.close();
	    } catch (IOException ioe) {
		_jdcbot.log.println("IOException in DownloadHandler thread: " + ioe.getMessage());
		ioe.printStackTrace();
	    }
	    _jdcbot.getDispatchThread().call(_jdcbot, "onDownloadComplete", de);
	    //_jdcbot.onDownloadComplete(de);
	}

	try {
	    _socket.close();
	} catch (IOException e) {
	    _jdcbot.log.println("IOException during closing socket in DownloadHandler thread: " + e.getMessage());
	    e.printStackTrace();
	}
	threadstarted = false;
	_dm.tasksComplete(this);

    }
}
