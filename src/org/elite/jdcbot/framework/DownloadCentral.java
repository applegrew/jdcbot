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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.elite.jdcbot.shareframework.SearchSet;
import org.elite.jdcbot.util.OutputEntityStream;
import org.slf4j.Logger;

/**
 * Created on 09-Jun-08<br>
 * Manages partial file downloading, auto resume, multi-source download, etc.
 * Segmented download is not yet implemented. Could be in future.
 * <p>
 * This class is thread safe.
 * 
 * @author AppleGrew
 * @since 1.0
 * @version 0.1.2
 */
public class DownloadCentral implements Runnable {
	private static final Logger logger = GlobalObjects.getLogger(DownloadCentral.class);
    private static final String queueFileName = "queue";

    public static enum State {
	QUEUED, RUNNING;
    }

    private List<Download> toDownload = Collections.synchronizedList(new ArrayList<Download>());
    private String incompleteDir;
    private BotInterface boi;
    private Thread th;
    volatile private boolean run = false;
    volatile private boolean supressSearch = false;
    private double transferRate = 0;
    private long timeBetweenSearches = 10 * 1000; //10 sec
    private SrcSearcher searchTh;

    /**
     * Constructs a new instance of DownloadCentral.<br>
     * 
     * @param boi The reference to jDCBot or MultiHubsAdapter (when running multiple hubs support is needed).
     */
    public DownloadCentral(BotInterface boi) {
	this.boi = boi;
    }

    /**
     * Called by jDCBot or MultiHubsAdapter on
     * setDownloadManager().
     * <p>
     * Make sure the directory exists.
     * <p>
     * If the files in download queue (which had been
     * partially downloaded before) exists no more in
     * the given directory then as expected download
     * will start again from scratch.
     * 
     * @param path2IncompleteDir
     */
    public void setDirs(String path2IncompleteDir) {
	incompleteDir = path2IncompleteDir;
    }

    /**
     * This is time minimum interval of searching on the hub for
     * alternate sources. Setting this value to low value can
     * get you banned for search spam. Default is 10s.
     * @param interval Minimum time interval in <u>milliseconds</u>.
     */
    public void setTimeBetweenSearches(long interval) {
	timeBetweenSearches = interval;
    }

    /**
     * Called by jDCBot or MultiHubsAdapter on
     * setDownloadManager().
     * <p>
     * This will load the queue file.
     */
    public void init() {
	File qf = new File(boi.getMiscDir() + File.separator + queueFileName);
	try {
	    loadQ(new BufferedInputStream(new FileInputStream(qf)));
	} catch (FileNotFoundException e) {

	} catch (IOException e) {
	    logger.error("In init().", e);
	    qf.delete();
	} catch (ClassNotFoundException e) {
		logger.error("In init().", e);
	    qf.delete();
	} catch (InstantiationException e) {
		logger.error("In init().", e);
	    qf.delete();
	}
    }

    /**
     * Called by jDCBot or MultiHubsAdapter
     * on terminate().
     * <p>
     * This will call {@link #stopQueueProcessThread()}
     * and save the download queue to a file.
     */
    public void close() {
	stopQueueProcessThread();
	try {
	    saveQ(new BufferedOutputStream(new FileOutputStream(boi.getMiscDir() + File.separator + queueFileName)));
	} catch (FileNotFoundException e) {
		logger.error("In close().", e);
	} catch (IOException e) {
		logger.error("In close().", e);
	}
    }

    /**
     * Called by jDCBot or MultiHubsAdapter on
     * setDownloadManager().
     * <p>
     * Starts new thread that processes and the download
     * queue and searches the hub for more sources.
     */
    public void startNewQueueProcessThread() {
	if (th != null) {
	    logger.warn("DownloadCentral Threads already running.");
	} else {
	    th = new Thread(this, "DownloadCentral Queue Processing Thread");
	    run = true;
	    searchTh = new SrcSearcher();
	    supressSearch = true;
	    searchTh.start();
	    th.start();
	}
    }

    public void stopQueueProcessThread() {
	run = false;
	if (th != null)
	    th.interrupt();
	th = null;
	if (searchTh != null)
	    searchTh.stopIt();
	searchTh = null;
    }

    /**
     * Call this to force processing of download queue.
     * This is done periodically at some interval of time.
     * @param supressSearch If this is false then alternative
     * sources of files in the download queue is searched too.
     * It is recommended that you set this to true and let
     * DownloadCentral do the searching at its convinience,
     * else you could get banned from hub for search spamming.
     */
    public void triggerProcessQ(boolean supressSearch) {
	this.supressSearch = supressSearch;
	if (th != null && run)
	    th.interrupt();
    }

    public void setTransferRate(double rate) {
	transferRate = rate;
	synchronized (toDownload) {
	    for (Download d : toDownload) {
		if (d.due.os() != null && d.due.os() instanceof OutputEntityStream) {
		    OutputEntityStream oes = (OutputEntityStream) d.due.os();
		    oes.setTransferLimit(rate);
		}
	    }
	}
    }

    /**
     * @return The transfer rate limit that
     * has been set by you. A value of
     * &lt;=0 means no limit has been
     * specified. To know the download
     * speed of any particular download
     * see {@link #getDownloadSpeed(String)}.
     */
    public double getTransferRate() {
	return transferRate;
    }

    /**
     * @param file The file or file's TTH about which
     * you want to query about. It is the same as you
     * used when calling
     * {@link #download(String, boolean, long, File, User) download()}.
     * @return Variuos statistics about the download, like its
     * state in queue (running or queued), etc. null is
     * returned if no such <i>file</i> is found.
     */
    public DownloadQEntry getStats(String file) {
	Download d = getDownloadByFilename(file);
	if (d != null)
	    return new DownloadQEntry(d);
	return null;
    }

    /**
     * @param file The file or file's TTH about which
     * you want to query about. It is the same as you
     * used when calling
     * {@link #download(String, boolean, long, File, User) download()}.
     * @return Download speed in bytes/second. Returns -1
     * if no such <i>file</i> is found.
     */
    public double getDownloadSpeed(String file) {
	Download d = getDownloadByFilename(file);
	if (d != null)
	    if (d.due.os() instanceof OutputEntityStream)
		((OutputEntityStream) d.due.os()).getTransferRate();
	return -1;
    }

    /**
     * @param file The file or file's TTH about which
     * you want to query about. It is the same as you
     * used when calling
     * {@link #download(String, boolean, long, File, User) download()}.
     * @return The overall percentage completion of download. Returns -1
     * if no such <i>file</i> is found.
     */
    public double getDownloadPercentageCompletion(String file) {
	Download d = getDownloadByFilename(file);
	if (d != null)
	    if (d.due.os() instanceof OutputEntityStream) {
		double pc = ((OutputEntityStream) d.due.os()).getPercentageCompletion();
		if (d.due.len() == d.totalLen)
		    return pc;
		else
		    return (pc * d.due.len() + d.due.start() * 100) / d.totalLen;
	    }
	return -1;
    }

    /**
     * @param file The file or file's TTH about which
     * you want to query about. It is the same as you
     * used when calling
     * {@link #download(String, boolean, long, File, User) download()}.
     * @return Tentative time remaining to complete the download.
     * Returns -1 if no such <i>file</i> is found or it can't be
     * calculated.
     */
    public double getDownloadTimeRemaining(String file) {
	Download d = getDownloadByFilename(file);
	if (d != null)
	    if (d.due.os() instanceof OutputEntityStream)
		((OutputEntityStream) d.due.os()).getTimeRemaining();
	return -1;
    }

    private Download getDownloadByFilename(String file) {
	synchronized (toDownload) {
	    for (Download d : toDownload)
		if (d.due.file().equals(file))
		    return d;
	}
	return null;
    }

    public void download(String file, boolean isHash, long len, File saveto, User u) throws BotException, FileNotFoundException {
	Download dwn = new Download(this);
	dwn.totalLen = len;
	dwn.isHash = isHash;
	dwn.temp = String.valueOf(System.currentTimeMillis()) + file;

	OutputEntityStream dos = new OutputEntityStream(new BufferedOutputStream(new FileOutputStream(dwn.getTempPath())));
	dos.setTransferLimit(transferRate);
	dos.setTotalStreamLength(len);

	dwn.due = new DUEntity(DUEntity.Type.FILE, file, 0, len, dos);
	if (isHash)
	    dwn.due.setSetting(DUEntity.AUTO_PREFIX_TTH_SETTING);

	dwn.saveto = saveto;
	if (u != null)
	    dwn.addSrc(new Src(u, boi));

	synchronized (toDownload) {
	    int pos = toDownload.indexOf(dwn);
	    if (pos == -1) {
		toDownload.add(dwn);
		search(dwn);
	    } else {
		if (u != null)
		    toDownload.get(pos).addSrc(new Src(u, boi));
		search(toDownload.get(pos));
	    }
	}
	if (u != null)
	    triggerProcessQ(true);
	else
	    triggerProcessQ(false);
    }

    private synchronized void processQ() {
	synchronized (toDownload) {
	    for (Download d : toDownload) {
		if (d.state == State.QUEUED) {
		    File t = new File(d.getTempPath());
		    if (t.exists()) {
			d.due.start(t.length());
			d.due.len(d.totalLen - t.length());
			if (d.due.os() instanceof OutputEntityStream)
			    ((OutputEntityStream) d.due.os()).setTotalStreamLength(d.due.len());
		    }
		    do {
			User u = d.getNextUser();
			if (u != null) {
			    try {
				d.downloadingFrom = u;
				u.download(d.due);
				break;
			    } catch (BotException e) {
				if (isRecoverableException(e.getError()))
				    logger.warn("Exception (" + e.getMessage()
					    + ")by DownloadManager. Anyway this is not serious, continuing.", e);
				else {
				    logger.error("Un-recoverable exception: " + e.getMessage() + ". Removing this source.", e);
				    d.removeSrc(new Src(u, boi));
				}
			    }
			}
		    } while (!d.isAllSrcsTried());
		}
	    }
	}
    }

    public void run() {
	while (run) {
	    processQ();
	    if (!supressSearch)
		synchronized (toDownload) {
		    for (Download d : toDownload)
			search(d);
		}
	    else
		supressSearch = false;
	    try {
		Thread.sleep(25 * 60 * 1000); //25 mins
	    } catch (InterruptedException e) {}
	}
	th = null;
    }

    /**
     * 
     * @param file
     * @return true when download has been successfully
     * cancelled, else false is returned.
     */
    public boolean cancelDownload(String file) {
	Download d = getDownloadByFilename(file);
	if (d == null)
	    return false;
	else if (d.downloadingFrom == null)
	    return false;
	else
	    d.downloadingFrom.cancelDownload(d.due);
	return true;
    }

    void onDownloadStart(DUEntity due, User u) {
	Download d = getDforDUE(due);
	//d should never be null here, except when downloading file list,
	//which is initiated by DonwloadManager.
	if( d != null ) {
		d.state = State.RUNNING;
	}
    }

    BotException onDownloadFinished(User user, DUEntity due, boolean success, BotException e) {
	Download d = getDforDUE(due);
	//d should never be null here, except when downloading file list,
	//which is initiated by DonwloadManager.
	if( d == null ) {
		logger.debug("DownloadCentral.onDownloadFinished(): Download object not found for DUEntity = " + due);
		return null;
	}
	if (success) {
	    toDownload.remove(d);
	    try {
		moveFile(new File(d.getTempPath()), d.saveto);
	    } catch (IOException e1) {
		return new BotException(
			"Failed to move temporary file (" + d.temp + ") to its file destination. Due to: " + e.getMessage(),
			BotException.Error.IO_ERROR);
	    } catch (BotException e2) {
		return e2;
	    }
	    return null;
	} else {
	    if (!isRecoverableException(e.getError())) {
		toDownload.remove(d);
		if (!new File(d.getTempPath()).delete()) {
		    //e = new BotException(BotException.Error.FAILED_TO_DELETE_TEMP_FILE);
		}
		return e;
	    } else {
		d.state = State.QUEUED;
		processQ();
		return null;
	    }
	}
    }

    /**
     * This method is re-entrant.
     * @param error
     * @return
     */
    private boolean isRecoverableException(BotException.Error error) {
	switch (error) {
	    case IO_ERROR:
	    case NO_FREE_DOWNLOAD_SLOTS:
	    case NO_FREE_SLOTS:
	    case NOT_CONNECTED_TO_HUB:
	    case REMOTE_CLIENT_SENT_WRONG_USERNAME:
	    case UNEXPECTED_RESPONSE:
	    case USERNAME_NOT_FOUND:
	    case CONNECTION_TO_REMOTE_CLIENT_FAILED:
	    case TIMEOUT:
	    case TASK_FAILED_SHUTTING_DOWN:
	    case USER_HAS_NO_INFO:
		return true;
	    default:
		return false;
	}
    }

    /**
     * Movies a file to another directory. Actually copies <i>src</i> to
     * <i>dest</i> by opening <i>src</i> and reading its bytes into <i>dest</i>.
     * It then deletes the <i>src</i> file. This has been done as File.renameTo(File)
     * doesn't always work.
     * <p>
     * This method is re-entrant.
     * 
     * @param src
     * @param dest
     * @throws IOException
     * @throws BotException
     */
    private void moveFile(File src, File dest) throws IOException, BotException {
	BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
	BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
	byte b[] = new byte[42 * 1024];
	int c;
	while ((c = in.read(b)) != -1) {
	    out.write(b, 0, c);
	}
	in.close();
	out.close();

	if (!src.delete()) {
	    throw new BotException(src.getAbsolutePath(), BotException.Error.FAILED_TO_DELETE_TEMP_FILE);
	}
    }

    private Download getDforDUE(DUEntity due) {
	synchronized (toDownload) {
	    for (Download d : toDownload)
		if (d.due.equals(due))
		    return d;
	}
	return null;
    }

    private void search(Download d) {
	synchronized (searchTh) {
	    if (searchTh != null)
		searchTh.search(d);
	}
    }

    void searchResult(String tth, User u) {
	if (u == null || tth.equals(""))
	    return;
	if (tth.startsWith("TTH:"))
	    tth = tth.substring(4);

	synchronized (toDownload) {
	    for (Download d : toDownload) {
		String file = d.due.file();
		if (file.startsWith("TTH/"))
		    file = file.substring(4);
		if (d.isHash && file.equalsIgnoreCase(tth)) {
		    d.addSrc(new Src(u, boi));
		    triggerProcessQ(true);
		    break;
		}
	    }
	}
    }

    /**
     * Saves DownloadCentral's download queue to the given
     * OutputStream. The OutputStream will most probably be
     * FileOutputStream.
     * @param out The stream to which this should be saved.
     * @throws IOException Ths is thrown if there is any error while writng to the stream.
     */
    public void saveQ(OutputStream out) throws IOException {
	logger.debug("DownloadQ saving...");
	ObjectOutputStream obj_out = new ObjectOutputStream(out);
	synchronized (toDownload) {
	    obj_out.writeObject(new DownloadQ(toDownload));
	}
	obj_out.close();
	logger.debug("DownloadQ saved.");
    }

    /**
     * Reads DownloadCentral's download queue from the given
     * InputStream. The InputStream will most probably be FileInputStream.<br>
     * <p>
     * <b>Note:</b> Donot forget to call {@link #setDirs(String)} <u>before</u>
     * calling this method, and <u>after</u> this method call {@link #startNewQueueProcessThread()}.
     * 
     * @param in The stream from which to read.
     * @throws IOException Thrown when error occurs while reading form the stream.
     * @throws ClassNotFoundException Class of a serialized object cannot be found.
     * @throws InstantiationException The read object is not instance of DownloadCentral.
     */
    public void loadQ(InputStream in) throws IOException, ClassNotFoundException, InstantiationException {
	logger.debug("DownloadQ loading...");
	ObjectInputStream obj_in = new ObjectInputStream(in);
	Object obj = obj_in.readObject();
	obj_in.close();
	if (obj instanceof DownloadQ) {
	    // Cast object to a DownloadQ
	    List<Download> dq = ((DownloadQ) obj).dq;
	    for (Download d : dq) {
		d.reset(this);
	    }
	    synchronized (toDownload) {
		toDownload = dq;
	    }
	    logger.debug("DownloadQ loaded.");
	} else
	    throw new InstantiationException("The object read is not instance of DownloadCentral.DownloadQ.");
    }

    //*****Private Classes******************/
    static class DownloadQ implements Serializable {
	private static final long serialVersionUID = -4991107691439831048L;
	List<Download> dq = null;

	DownloadQ(List<Download> dq) {
	    this.dq = dq;
	}
    }

    static class Download implements Serializable {
	private static final long serialVersionUID = 3738152622537290995L;
	private static final int HASH_CONST = 61;

	public String temp = null;
	public DUEntity due = null;
	transient public User downloadingFrom = null;
	public boolean isHash = false;
	public long totalLen = 0;
	public File saveto = null;
	public State state = State.QUEUED;

	private List<Src> srcs = Collections.synchronizedList(new ArrayList<Src>());
	transient private int curr_src = -1;
	transient volatile private boolean isAllSrcsTried = false;

	transient DownloadCentral dc = null;

	Download(DownloadCentral dc) {
	    this.dc = dc;
	}

	public String getTempPath() {
	    return dc.incompleteDir + File.separator + temp;
	}

	public void addSrc(Src s) {
	    synchronized (srcs) {
		if (!srcs.contains(s))
		    srcs.add(s);
	    }
	}

	public List<Src> getAllSrcs() {
	    synchronized (srcs) {
		return new ArrayList<Src>(srcs);
	    }
	}

	public boolean removeSrc(Src s) {
	    synchronized (srcs) {
		int in = srcs.indexOf(s);
		if (in == -1)
		    return false;

		if (curr_src >= in)
		    curr_src--;
		if (curr_src < 0)
		    curr_src = srcs.size() - 1;
	    }
	    return true;
	}

	public User getNextUser() {
	    synchronized (srcs) {
		if (srcs.size() == 0)
		    return null;
		curr_src++;
		if (curr_src >= srcs.size())
		    curr_src = 0;

		if (curr_src == srcs.size() - 1)
		    isAllSrcsTried = true;
		return srcs.get(curr_src).getUser();
	    }
	}

	public boolean hasAnySrc() {
	    return srcs.size() != 0;
	}

	public boolean isAllSrcsTried() {
	    boolean flag = isAllSrcsTried;
	    isAllSrcsTried = false;
	    return srcs.size() == 0 ? true : flag;
	}

	public void resetCurrSrcPointer() {
	    curr_src = -1;
	}

	public void reset(DownloadCentral Dc) {
	    dc = Dc;

	    curr_src = -1;
	    isAllSrcsTried = false;
	    downloadingFrom = null;
	    state = State.QUEUED;
	    OutputEntityStream dos = null;
	    try {
		File t = new File(getTempPath());
		if (t.exists()) {
		    due.start(t.length());
		    due.len(totalLen - t.length());
		    dos = new OutputEntityStream(new BufferedOutputStream(new FileOutputStream(getTempPath(), true)));
		} else {
		    due.start(0);
		    due.len(totalLen);
		    dos = new OutputEntityStream(new BufferedOutputStream(new FileOutputStream(getTempPath())));
		}

		dos.setTransferLimit(dc.transferRate);
		dos.setTotalStreamLength(due.len());
	    } catch (FileNotFoundException e) {
	    	logger.error("In reset().", e);
	    }
	    due.os(dos);
	    for (Src s : srcs)
		s.reset(dc.boi);
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

    static class Src implements Serializable {
	private static final long serialVersionUID = 2442400319508792562L;
	private static final int HASH_CONST = 71;

	transient private User user;
	private String CID;
	private String username;

	transient BotInterface boi;

	public Src(BotInterface Boi) {
	    boi = Boi;
	}

	public Src(User u, BotInterface Boi) {
	    user = u;
	    boi = Boi;
	    if (user != null) {
		CID = user.getClientID();
		username = user.username();
	    }
	}

	public Src(String cid, BotInterface Boi) {
	    CID = cid;
	    boi = Boi;
	    user = boi.getUserByCID(CID);
	    if (user != null)
		username = user.username();
	}

	public User getUser() {
	    if (user == null)
		user = boi.getUserByCID(CID);
	    if (user == null)
		user = boi.getUser(username);
	    System.err.println("DC.Download.getUser: " + user);
	    return user;
	}

	public void setUser(User u) {
	    user = u;
	    if (user != null) {
		CID = user.getClientID();
		username = user.username();
	    }
	}

	public void setUser(String cid) {
	    CID = cid;
	    user = boi.getUserByCID(CID);
	    if (user != null)
		username = user.username();
	}

	public void reset(BotInterface Boi) {
	    boi = Boi;

	    user = boi.getUserByCID(CID);
	    if (user == null) {
		if (boi instanceof MultiHubsAdapter) {
		    for (User u : ((MultiHubsAdapter) boi).getUsers(username))
			try {
			    boi.getShareManager().downloadOthersFileList(u);
			} catch (BotException e) {
				logger.error("In reset().", e);
			}
		} else {
		    User u = ((jDCBot) boi).getUser(username);
		    if (u != null)
			try {
			    boi.getShareManager().downloadOthersFileList(u);
			} catch (BotException e) {
				logger.error("In reset().", e);
			}
		}
	    }
	}

	public String getCID() {
	    return CID;
	}

	public boolean equals(Object o) {
	    if (this == o)
		return true;

	    if (o instanceof Src) {
		Src s = (Src) o;
		if ((this.user != null && s.user != null && this.user.equals(s.user))
			|| (!this.CID.isEmpty() && !s.CID.isEmpty() && this.CID.equalsIgnoreCase(s.CID))
			|| (this.username.equalsIgnoreCase(s.username)))
		    return true;
	    }
	    return false;
	}

	public int hashCode() {
	    return HASH_CONST + (user == null ? CID.isEmpty() || CID == null ? username.hashCode() : CID.hashCode() : user.hashCode());
	}

	public String toString() {
	    return (user != null ? user.username() + " " : "") + CID;
	}
    }

    private class SrcSearcher extends Thread {
	private List<Download> searchFor = Collections.synchronizedList(new ArrayList<Download>());
	private volatile boolean running = true;

	private long lastSearchTime = -1;

	public SrcSearcher() {
	    super("AltSrc Searcher Thread");
	}

	public void run() {
	    while (running) {
		while (!searchFor.isEmpty()) {
		    Download d = null;
		    synchronized (searchFor) {
			if (searchFor.isEmpty())
			    break;
			d = searchFor.get(0);
			searchFor.remove(0);
		    }
		    if (d == null || !d.isHash)
			break;

		    if (lastSearchTime != -1 && System.currentTimeMillis() - lastSearchTime < timeBetweenSearches)
			break;
		    lastSearchTime = System.currentTimeMillis();

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
		    	logger.error("In run().", e);
		    }
		}

		try {
		    Thread.sleep(timeBetweenSearches);
		} catch (InterruptedException e) {}
	    }
	}

	public void search(Download d) {
	    searchFor.add(d);
	    this.interrupt();
	}

	public void stopIt() {
	    running = false;
	    this.interrupt();
	}
    }
}
