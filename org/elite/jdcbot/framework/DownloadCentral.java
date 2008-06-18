/*
 * DownloadCentral.java
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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Vector;

import javax.imageio.IIOException;

import org.elite.jdcbot.shareframework.SearchSet;
import org.elite.jdcbot.util.OutputEntityStream;

/**
 * Created on 09-Jun-08<br>
 * Manages partial file downloading, auto resume, multi-source download, etc.
 * Segmented download is not yet implemented. Could be in future.
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class DownloadCentral implements Serializable, Runnable {
    private static final long serialVersionUID = -1567266569610909427L;

    public static enum State {
	QUEUED, RUNNING;
    }

    private Vector<Download> toDownload = new Vector<Download>();
    transient private String incompleteDir;
    transient private BotInterface boi;
    transient private Thread th;
    transient volatile private boolean run = false;
    double transferRate = 0;

    /**
     * Constructs a new instance of DownloadCentral.<br>
     * <p>
     * You need to call the following methods after this
     * in the order shown.<br>
     * <ol>
     * <li>setDirs(String)</li>
     * <li>startNewQueueProcessThread()</li>
     * </ol>
     * @param boi The reference to jDCBot or MultiHubsAdapter (when running multiple hubs support is needed).
     */
    public DownloadCentral(BotInterface boi) {
	this.boi = boi;
    }

    /**
     * Note that setting value of Incomplete folder to any other value will
     * only make the newly added files for download to be stored there. Previously
     * added files are still expected to be available in the last incomplete
     * directory location.
     * @param path2IncompleteDir
     * @throws IIOException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void setDirs(String path2IncompleteDir) throws IIOException, FileNotFoundException, IOException {
	File incompleteDir = new File(path2IncompleteDir);
	if (!incompleteDir.exists())
	    throw new FileNotFoundException();
	if (!incompleteDir.isDirectory())
	    throw new IIOException("Given path '" + path2IncompleteDir + "' is not a directory.");
	this.incompleteDir = incompleteDir.getCanonicalPath();
    }

    public void startNewQueueProcessThread() {
	if (th != null && th.getState() == Thread.State.RUNNABLE) {
	    GlobalObjects.log.println("DownloadCentral.QueueProcess Thread already running.");
	} else {
	    th = new Thread(this, "DownloadCentral.QueueProcess Thread");
	    run = true;
	    th.start();
	}
    }

    public void stopQueueProcessThread() {
	run = false;
	th = null;
    }

    void triggerProcessQ() {
	if (th != null && run)
	    th.interrupt();
    }

    public void setTransferRate(double rate) {
	transferRate = rate;
	for (Download d : toDownload) {
	    if (d.due.os() instanceof OutputEntityStream) {
		OutputEntityStream oes = (OutputEntityStream) d.due.os();
		oes.setTransferLimit(rate);
	    }
	}

    }

    public double getTransferRate() {
	return transferRate;
    }

    public synchronized void download(String file, boolean isHash, long len, File saveto, User u) throws BotException,
	    FileNotFoundException {
	Download dwn = new Download();
	dwn.totalLen = len;
	dwn.isHash = isHash;
	dwn.temp = new File(incompleteDir + File.separator + String.valueOf(System.currentTimeMillis()) + file);

	OutputEntityStream dos = new OutputEntityStream(new BufferedOutputStream(new FileOutputStream(dwn.temp)));
	dos.setTransferLimit(transferRate);
	dos.setTotalStreamLength(len);

	dwn.due = new DUEntity(DUEntity.Type.FILE, file, 0, len, dos);
	if (isHash)
	    dwn.due.setSetting(DUEntity.AUTO_PREFIX_TTH_SETTING);

	dwn.saveto = saveto;
	dwn.addSrc(new Src(u));

	if (!toDownload.contains(dwn)) {
	    toDownload.add(dwn);
	    processQ();
	}
    }

    private synchronized void processQ() {
	for (Download d : toDownload) {
	    if (d.state == State.QUEUED) {
		File t = d.temp;
		if (t.exists()) {
		    d.due.start(t.length());
		    d.due.len(d.totalLen - t.length());
		}
		do {
		    User u = d.getNextUser();
		    if (u != null) {
			try {
			    u.download(d.due);
			    break;
			} catch (BotException e) {
			    if (isRecoverableException(e.getError()))
				GlobalObjects.log.println("Exception (" + e.getMessage()
					+ ")by DownloadManager. Anyway this is not serious, continuing.");
			    else {
				GlobalObjects.log.println("Un-recoverable exception: " + e.getMessage() + ". Removing this source.");
				d.removeSrc(new Src(u));
			    }
			}
		    }
		} while (!d.isAllSrcsTried());
	    }
	}
    }

    public void run() {
	while (run) {
	    processQ();
	    for (Download d : toDownload)
		search(d);
	    try {
		Thread.sleep(2 * 60 * 1000); //25 mins
	    } catch (InterruptedException e) {}
	}
    }

    synchronized void onDownloadStart(DUEntity due, User u) {
	Download d = getDforDUE(due);
	//d should never be null here.
	d.state = State.RUNNING;
    }

    synchronized BotException onDownloadFinished(User user, DUEntity due, boolean success, BotException e) {
	Download d = getDforDUE(due);
	//d should never be null here
	if (success) {
	    toDownload.remove(d);
	    try {
		moveFile(d.temp, d.saveto);
	    } catch (IOException e1) {
		return new BotException("Failed to move temporary file (" + d.temp.getName() + ") to its file destination. Due to: "
			+ e.getMessage(), BotException.Error.IO_ERROR);
	    } catch (BotException e2) {
		return e2;
	    }
	    return null;
	} else {
	    if (!isRecoverableException(e.getError())) {
		toDownload.remove(d);
		return e;
	    } else {
		d.state = State.QUEUED;
		processQ();
		return null;
	    }
	}
    }

    private boolean isRecoverableException(BotException.Error error) {
	switch (error) {
	    case IO_ERROR:
	    case NO_FREE_DOWNLOAD_SLOTS:
	    case NO_FREE_SLOTS:
	    case NOT_CONNECTED_TO_HUB:
	    case REMOTE_CLIENT_SENT_WRONG_USERNAME:
	    case UNEXPECTED_RESPONSE:
	    case USERNAME_NOT_FOUND:
		return true;
	    default:
		return false;
	}
    }

    private synchronized void moveFile(File src, File dest) throws IOException, BotException {
	BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
	BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
	byte b[] = new byte[1024];
	int c;
	while ((c = in.read(b)) != -1) {
	    out.write(b, 0, c);
	}
	in.close();
	out.close();
	/*TODO remove the comment tags later.
	 * if(!src.delete()){
	 throw new BotException(BotException.Error.FAILED_TO_DELETE_TEMP_FILE);
	 }*/
    }

    private synchronized Download getDforDUE(DUEntity due) {
	for (Download d : toDownload)
	    if (d.due.equals(due))
		return d;
	return null;
    }

    private void search(Download d) {
	if (!d.isHash)
	    return;

	DUEntity due = d.due;

	String file;
	if (due.file().startsWith("TTH/"))
	    file = due.file().substring(4);
	else
	    file = due.file();

	SearchSet ss = new SearchSet();
	ss.data_type = SearchSet.DataType.TTH;
	ss.string = file;
	try {
	    boi.Search(ss);
	} catch (IOException e) {
	    e.printStackTrace(GlobalObjects.log);
	}

    }

    synchronized void searchResult(String tth, User u) {
	if (u == null || tth.equals(""))
	    return;
	for (Download d : toDownload) {
	    DUEntity due = d.due;
	    if (d.isHash && due.file().equals(tth)) {
		if (!d.srcs.contains(u))
		    d.addSrc(new Src(u));
	    }
	}
    }

    /**
     * Saves DownloadCentral and the download queue to the given
     * OutputStream. The OutputStream will most probably be
     * FileOutputStream.
     * @param out The stream to which this should be saved.
     * @param object The DownloadCentral object to save.
     * @throws IOException Ths is thrown if there is any error while writng to the stream.
     */
    public static void saveObjectToStream(OutputStream out, DownloadCentral object) throws IOException {
	ObjectOutputStream obj_out = new ObjectOutputStream(out);
	obj_out.writeObject(object);
	obj_out.close();
    }

    /**
     * Reads DownloadCentral and the download queue from the given
     * InputStream. The InputStream will most probably be FileInputStream.<br>
     * <p>
     * <b>Note:</b> Donot forget to call:-
     * <ol>
     * <li>setDirs(String)</li>
     * <li>startNewQueueProcessThread()</li>
     * </ol>
     * in the order shown above.
     * @param in The stream from which to read.
     * @param boi The reference to jDCBot or MultiHubsAdapter (when running multiple hubs support is needed).
     * @return A new instance of DownloadCentral initialized form the data read from the stream.
     * @throws IOException Thrown when error occurs while reading form the stream.
     * @throws ClassNotFoundException Class of a serialized object cannot be found.
     * @throws InstantiationException The read object is not instance of DownloadCentral.
     */
    public static DownloadCentral readObjectFromStream(InputStream in, BotInterface boi) throws IOException, ClassNotFoundException,
	    InstantiationException {
	ObjectInputStream obj_in = new ObjectInputStream(in);
	Object obj = obj_in.readObject();
	obj_in.close();
	if (obj instanceof DownloadCentral) {
	    // Cast object to a DownloadCentral
	    DownloadCentral dc = (DownloadCentral) obj;
	    dc.boi = boi;
	    dc.run = false;
	    dc.th = null;
	    for (Download d : dc.toDownload) {
		d.reset();
	    }
	    return dc;
	} else
	    throw new InstantiationException("The object read is not instance of DownloadCentral.");
    }

    private class Download implements Serializable {
	private static final long serialVersionUID = 3738152622537290995L;
	private final int HASH_CONST = 61;

	public File temp = null;
	public DUEntity due = null;
	public boolean isHash = false;
	public long totalLen = 0;
	public File saveto = null;
	public State state = State.QUEUED;

	private Vector<Src> srcs = new Vector<Src>();
	transient private int curr_src = -1;
	transient private boolean isAllSrcsTried = false;

	public synchronized void addSrc(Src s) {
	    if (!srcs.contains(s))
		srcs.add(s);
	}

	public synchronized boolean removeSrc(Src s) {
	    int in = srcs.indexOf(s);
	    if (in == -1)
		return false;

	    if (curr_src >= in)
		curr_src--;
	    if (curr_src < 0)
		curr_src = srcs.size() - 1;
	    return true;
	}

	public synchronized User getNextUser() {
	    if (srcs.size() == 0)
		return null;
	    curr_src++;
	    if (curr_src >= srcs.size())
		curr_src = 0;

	    if (curr_src == srcs.size() - 1)
		isAllSrcsTried = true;
	    return srcs.get(curr_src).getUser();
	}

	public boolean hasAnySrc() {
	    return srcs.size() != 0;
	}

	public synchronized boolean isAllSrcsTried() {
	    boolean flag = isAllSrcsTried;
	    isAllSrcsTried = false;
	    return srcs.size() == 0 ? true : flag;
	}

	public void resetCurrSrcPointer() {
	    curr_src = -1;
	}

	public void reset() {
	    curr_src = -1;
	    isAllSrcsTried = false;
	    for (Src s : srcs)
		s.reset();
	}

	public boolean equals(Object o) {
	    if (this == o)
		return true;

	    if (o instanceof Download) {
		Download d = (Download) o;
		if (d.due != null && due != null && d.due.equals(due))
		    return true;
	    }
	    return false;
	}

	public int hashCode() {
	    return HASH_CONST + (due == null ? 0 : due.hashCode());
	}

	public String toString() {
	    return due + " hash:" + isHash + " state:" + state + " saveto:" + saveto;
	}
    }

    private class Src implements Serializable {
	private static final long serialVersionUID = 2442400319508792562L;
	private final int HASH_CONST = 71;

	transient private User user;
	private String CID;

	public Src() {}

	public Src(User u) {
	    user = u;
	}

	public Src(String cid) {
	    CID = cid;
	}

	public User getUser() {
	    user = user == null ? boi.getUserByCID(CID) : user;
	    return user;
	}

	public void setUser(User u) {
	    user = u;
	    if (user == null)
		CID = user.getClientID();
	}

	public void setUser(String cid) {
	    CID = cid;
	}

	public void reset() {
	    user = null;
	    if (boi != null)
		user = boi.getUserByCID(CID);
	}

	public boolean equals(Object o) {
	    if (this == o)
		return true;

	    if (o instanceof Src) {
		Src s = (Src) o;
		if ((this.user != null && s.user != null && this.user.equals(s.user))
			|| (this.CID != null && s.CID != null && this.CID.equalsIgnoreCase(s.CID)))
		    return true;
	    }
	    return false;
	}

	public int hashCode() {
	    return HASH_CONST + (user == null ? CID == null || CID.isEmpty() ? 0 : CID.hashCode() : user.hashCode());
	}

	public String toString() {
	    return user != null ? user.username() : "" + " " + CID;
	}
    }
}
