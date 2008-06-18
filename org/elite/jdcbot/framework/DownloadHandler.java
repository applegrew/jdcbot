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
 * <p>
 * This is used internally, you are not required do anything with this.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1.2
 * 
 */
public class DownloadHandler extends DCIO implements Runnable {
    private final int in_buffer_size = 64 * 1024; //64 KMB
    private final int checkInterval = 100000; //This has been defined so polling frequency of CancelEntityQ is minimized. 

    private List<DUEntity> DownloadEntityQ;
    private List<DUEntity> CancelEntityQ;
    private User _u;
    private jDCBot _jdcbot;
    private DownloadManager _dm;
    private Socket _socket = null;
    private volatile boolean close = false;
    private volatile boolean threadstarted = false;
    private volatile boolean connectionFailed = false;
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

    public synchronized void cancelDownload(DUEntity due) {
	CancelEntityQ.add(due);
	if (threadstarted)
	    th.interrupt();
    }

    public synchronized void download(DUEntity de) {
	if (!DownloadEntityQ.contains(de))
	    DownloadEntityQ.add(de);
	if (!threadstarted) {
	    th = new Thread(this, "DownloadHandler Thread");
	    th.start();
	    threadstarted = true;
	}
    }

    public synchronized void notifyPassiveConnect(Socket socket) {
	_socket = socket;
	if (th != null)
	    th.interrupt();
    }

    public String getUserName() {
	return _u.username();
    }

    public boolean isConnectionFailed() {
	return connectionFailed;
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
	} catch (IOException be) {
	    _jdcbot.log.println("IOException in DownloadHandler thread: " + be.getMessage());
	    be.printStackTrace(_jdcbot.log);
	    connectionFailed = true;
	    notifyFailedConnection(new BotException(be.getMessage(), BotException.Error.IO_ERROR));
	    theEnd();

	} catch (BotException e) {
	    _jdcbot.log.println("BotException in DownloadHandler thread: " + e.getMessage());
	    e.printStackTrace(_jdcbot.log);
	    connectionFailed = true;
	    notifyFailedConnection(e);
	    theEnd();
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
		    connectionFailed = true;
		    notifyFailedConnection(new BotException(BotException.Error.TIMEOUT));
		    theEnd();
		}
	    }
	}

	while (!DownloadEntityQ.isEmpty() && !close) {
	    DUEntity de = DownloadEntityQ.get(0);
	    DownloadEntityQ.remove(0);

	    _jdcbot.getDispatchThread().callOnDownloadStart(_u, de);

	    boolean osClosed = false;
	    try {
		int index;
		if ((index = CancelEntityQ.indexOf(de)) != -1) {
		    CancelEntityQ.remove(index);

		    BotException e = new BotException(BotException.Error.TRANSFER_CANCELLED);
		    _jdcbot.log.println("BotException in DownloadHandler thread: " + e.getMessage());
		    _jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false, e);

		    continue;
		}

		if (((!_u.isSupports("TTHF") || !_u.isSupports("TTHL")) && de.isSettingSet(DUEntity.AUTO_PREFIX_TTH_SETTING))
			|| (!_u.isSupports("TTHL") && de.fileType() == DUEntity.Type.TTHL)) {
		    _jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false,
			    new BotException(BotException.Error.PROTOCOL_UNSUPPORTED_BY_REMOTE));
		    continue;
		}

		String file = de.file();
		long start = de.start();
		long fileLen = de.len();

		if (de.isSettingSet(DUEntity.AUTO_PREFIX_TTH_SETTING) && de.fileType() != DUEntity.Type.FILELIST)
		    if (!file.startsWith("TTH/"))
			file = "TTH/" + file;

		boolean ZLIG = false;
		try {
		    if (_u.isSupports("ADCGet")) {
			if (de.fileType() == DUEntity.Type.FILELIST || de.fileType() == DUEntity.Type.TTHL) {
			    start = 0;
			    fileLen = -1;
			}
			if (de.fileType() == DUEntity.Type.FILELIST) {
			    //If the user wants to download the filelist.
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

			buffer = ReadCommand(_socket);//Reading $ADCSND K F S LZ| OR $MaxedOut| OR $Error msg|//Z is ' ZL1' or nothing.
			_jdcbot.log.println(buffer);
			if (buffer.equals("$MaxedOut|")) {
			    throw new BotException(BotException.Error.NO_FREE_SLOTS);
			}
			if (buffer.startsWith("$Error")) {
			    String err = "Transfer failed due to: " + buffer.substring(buffer.indexOf(' ') + 1, buffer.indexOf('|'));
			    _jdcbot.log.println(err);
			    throw new BotException(err, BotException.Error.IO_ERROR);
			}
			params = parseRawCmd(buffer);
			fileLen = Long.parseLong(params[4]);
			String Z = params[params.length - 1];

			if (Z.equalsIgnoreCase("ZL1"))
			    ZLIG = true;
		    } else {
			_jdcbot.log.println("None of known file transfer method supported by remote client.");
			throw new BotException(BotException.Error.PROTOCOL_UNSUPPORTED);
		    }

		} catch (Exception be) {
		    _jdcbot.log.println("Exception in DownloadHandler thread: " + be.getMessage());
		    be.printStackTrace(_jdcbot.log);

		    _jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false,
			    new BotException(be.getMessage(), BotException.Error.IO_ERROR));
		    continue;
		}

		try {
		    InputStream in = null;

		    if (ZLIG)
			in = new InflaterInputStream(_socket.getInputStream());
		    else
			in = _socket.getInputStream();

		    long len = 0;
		    int c;

		    InputStream fin = in;
		    if (de.fileType() == DUEntity.Type.FILELIST && (_u.isSupports("XmlBZList") || _u.isSupports("BZList"))
			    && !de.isSettingSet(DUEntity.NO_AUTO_FILELIST_DECOMPRESS_SETTING)) {

			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			while ((c = in.read()) != -1 && ++len <= fileLen) {
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
			    de.os().close();
			    throw new BotException(BotException.Error.TRANSFER_CANCELLED);
			}
			if (intervalCount <= 0)
			    intervalCount = checkInterval;
			de.os().write(c);
		    }
		    de.os().close();
		    osClosed = true;

		    _jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, true, null);

		} catch (IOException ioe) {
		    _jdcbot.log.println("IOException in DownloadHandler thread: " + ioe.getMessage());
		    ioe.printStackTrace(_jdcbot.log);
		    _jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false,
			    new BotException(ioe.getMessage(), BotException.Error.IO_ERROR));
		} catch (BotException e) {
		    _jdcbot.log.println("BotException in DownloadHandler thread: " + e.getMessage());
		    _jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false, e);
		}
	    } finally {
		try {
		    /* To make sure the close is always called. This is needed for proper invokation
		     * of onFilelistDownloadFinished() of ShareManagerListener.
		     */
		    if (!osClosed)
			de.os().close();
		} catch (IOException e) {
		    e.printStackTrace(_jdcbot.log);
		}
	    }

	    /*
	     * The synchronized block below is critical because take this case,
	     * when DownloadEntityQ is empty and before the control could goto
	     * next statement and set threadstarted to false, download() gets called
	     * and its if(!threadstarted) comparision is already over then
	     * this thread will close and no new thread will be created causing the
	     * the newly scheduled download to lay stranded and will never get
	     * downloaded.
	     */
	    synchronized (this) {
		if (DownloadEntityQ.isEmpty() || close) {
		    threadstarted = false;
		}
	    }
	}

	theEnd();
    }

    private synchronized void theEnd() {
	try {
	    if (_socket != null)
		_socket.close();
	} catch (IOException e) {
	    _jdcbot.log.println("IOException during closing socket in DownloadHandler thread: " + e.getMessage());
	    e.printStackTrace(_jdcbot.log);
	}
	_dm.tasksComplete(this);
    }

    private void notifyFailedConnection(BotException e) {
	for (DUEntity de : DownloadEntityQ)
	    _jdcbot.getDispatchThread().callOnDownloadComplete(_u, de, false, e);
    }
}
