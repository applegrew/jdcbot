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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.InflaterInputStream;

import org.apache.tools.bzip2.CBZip2InputStream;

/**
 * Created on 26-May-08<br>
 * Handels all the downloads from a single user for a session.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1.1
 * 
 */
public class DownloadHandler extends DCIO implements Runnable {
    private final int in_buffer_size = 1 * 1024 * 1024; //1MB
    private final int checkInterval = 100000; //This has been defined so polling frequency of CancelEntityQ is minimized. 

    private List<DUEntity> DownloadEntityQ;
    private List<DUEntity> CancelEntityQ;
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
	CancelEntityQ = Collections.synchronizedList(new ArrayList<DUEntity>());
	close = false;
    }

    public void close() {
	close = true;
	if (threadstarted)
	    th.interrupt();
    }

    public void cancelDownload(DUEntity due) {
	CancelEntityQ.add(due);
	if (threadstarted)
	    th.interrupt();
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
	    be.printStackTrace(_jdcbot.log);
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
		if (_socket == null && count >= 3) {
		    _jdcbot.log.println("Timeout. Waited for too long for remote client's (" + _u.username() + ") connection.");
		    return;
		}
	    }
	}

	while (!DownloadEntityQ.isEmpty() && !close) {
	    DUEntity de = DownloadEntityQ.get(0);
	    DownloadEntityQ.remove(0);
	    int index;
	    if ((index = CancelEntityQ.indexOf(de)) != -1) {
		CancelEntityQ.remove(index);
		try {
		    throw new BotException(BotException.TRANSFER_CANCELLED);
		} catch (BotException e) {
		    _jdcbot.log.println("BotException in DownloadHandler thread: " + e.getMessage());
		    _jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false, e);
		}
	    }

	    if ((!_u.isSupports("TTHF") && de.isSettingSet(DUEntity.AUTO_PREFIX_TTH_SETTING))
		    || (!_u.isSupports("TTHL") && de.fileType == DUEntity.TTHL_TYPE)) {
		_jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false, new BotException(BotException.PROTOCOL_UNSUPPORTED_BY_REMOTE));
		continue;
	    }

	    String file = de.file;
	    long start = de.start;
	    long fileLen = de.len;

	    if (de.isSettingSet(DUEntity.AUTO_PREFIX_TTH_SETTING) && de.fileType != DUEntity.FILELIST_TYPE)
		if (!file.startsWith("TTH/"))
		    file = "TTH/" + file;

	    boolean ZLIG = false;
	    try {
		if (_u.isSupports("ADCGet")) {
		    if (de.fileType == DUEntity.FILELIST_TYPE) {
			//If the user wants to download the filelist.
			start = 0;
			fileLen = -1;
			if (_u.isSupports("XmlBZList"))
			    file = "files.xml.bz2";
			else if (_u.isSupports("BZList"))
			    file = "MyList.bz2";
		    }

		    if (_u.isSupports("ZLIG")) {
			buffer = "$ADCGET " + de.getFileType() + " " + file + " " + start + " " + fileLen + " ZL1|";
			_jdcbot.log.println("From bot: " + buffer);
			SendCommand(buffer, _socket);
		    } else {
			buffer = "$ADCGET " + de.getFileType() + " " + file + " " + start + " " + fileLen + "|";
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
			throw new BotException(err, BotException.IO_ERROR);
		    }
		    params = parseRawCmd(buffer);
		    fileLen = Long.parseLong(params[4]);
		    String Z = params[params.length - 1];

		    if (Z.equalsIgnoreCase("ZL1"))
			ZLIG = true;
		} else {
		    _jdcbot.log.println("None of known file transfer method supported by remote client.");
		    throw new BotException(BotException.PROTOCOL_UNSUPPORTED);
		}

	    } catch (Exception be) {
		_jdcbot.log.println("Exception in DownloadHandler thread: " + be.getMessage());
		be.printStackTrace(_jdcbot.log);

		/*_jdcbot.getDispatchThread().call(_jdcbot, "onDownloadComplete",
		 new Class[] { User.class, DUEntity.class, boolean.class, BotException.class }, _u, de, false, be);*/
		_jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false, new BotException(be.getMessage(), BotException.IO_ERROR));
		continue;
	    }

	    try {
		InputStream in = null;

		if (ZLIG)
		    in = new InflaterInputStream(_socket.getInputStream());
		else
		    in = _socket.getInputStream();

		int len = 0, c;

		InputStream fin = in;
		if (de.fileType == DUEntity.FILELIST_TYPE && (_u.isSupports("XmlBZList") || _u.isSupports("BZList"))
			&& !de.isSettingSet(DUEntity.NO_AUTO_FILELIST_DECOMPRESS_SETTING)) {

		    ByteArrayOutputStream bout = new ByteArrayOutputStream();
		    int intervalCount = checkInterval;
		    while ((c = in.read()) != -1 && ++len <= fileLen) {
			intervalCount--;
			if (intervalCount <= 0 && (index = CancelEntityQ.indexOf(de)) != -1) {
			    CancelEntityQ.remove(index);
			    throw new BotException(BotException.TRANSFER_CANCELLED);
			}
			if (intervalCount <= 0)
			    intervalCount = checkInterval;
			bout.write(c);
		    }
		    byte barr[] = bout.toByteArray();
		    bout.reset();
		    //Skipping the first two bytes - BZ. Else decompression will fail and NullPoiterException maybe be thrown.
		    fin = new CBZip2InputStream(new ByteArrayInputStream(barr, 2, barr.length));
		    len = -1;
		}

		fin = new BufferedInputStream(fin, in_buffer_size);
		int intervalCount = checkInterval;
		while ((c = fin.read()) != -1 && (len == -1 || ++len <= fileLen) && !close) {
		    intervalCount--;
		    if (intervalCount <= 0 && (index = CancelEntityQ.indexOf(de)) != -1) {
			CancelEntityQ.remove(index);
			throw new BotException(BotException.TRANSFER_CANCELLED);
		    }
		    if (intervalCount <= 0)
			intervalCount = checkInterval;
		    de.os.write(c);
		}
		de.os.close();

		/*_jdcbot.getDispatchThread().call(_jdcbot, "onDownloadComplete",
		 new Class[] { User.class, DUEntity.class, boolean.class, BotException.class }, _u, de, true, null);*/
		_jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, true, null);

	    } catch (IOException ioe) {
		_jdcbot.log.println("IOException in DownloadHandler thread: " + ioe.getMessage());
		ioe.printStackTrace(_jdcbot.log);
		/*_jdcbot.getDispatchThread().call(_jdcbot, "onDownloadComplete",
		 new Class[] { User.class, DUEntity.class, boolean.class, BotException.class }, _u, de, false,
		 new BotException(ioe.getMessage(), BotException.IO_ERROR));*/
		_jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false, new BotException(ioe.getMessage(), BotException.IO_ERROR));
	    } catch (BotException e) {
		_jdcbot.log.println("BotException in DownloadHandler thread: " + e.getMessage());
		_jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false, e);
	    }
	}

	try {
	    _socket.close();
	} catch (IOException e) {
	    _jdcbot.log.println("IOException during closing socket in DownloadHandler thread: " + e.getMessage());
	    e.printStackTrace(_jdcbot.log);
	}
	threadstarted = false;
	_dm.tasksComplete(this);

    }
}
