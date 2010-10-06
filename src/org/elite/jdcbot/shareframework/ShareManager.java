/*
 * ShareManager.java
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
package org.elite.jdcbot.shareframework;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.tools.bzip2.CBZip2OutputStream;
import org.elite.jdcbot.framework.BotException;
import org.elite.jdcbot.framework.BotInterface;
import org.elite.jdcbot.framework.DUEntity;
import org.elite.jdcbot.framework.GlobalObjects;
import org.elite.jdcbot.framework.User;
import org.elite.jdcbot.util.GlobalFunctions;
import org.elite.jdcbot.util.InputEntityStream;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

/**
 * Created on 26-May-08<br>
 * Its purpose is to manage the user shared files, i.e. hashing them,
 * creating/updating file list, etc. You should use this download other
 * users' file lists. This class will automatically parse the file list
 * and create an instance of FileListManager for it. Note that this class
 * won't monitor added directories for changes, you will needed to monitor
 * files and directories for changes and call the appropriate methods. There
 * is one advice though. Whenever you find a new file then get a list of
 * files in the file list that no longer exist and if any one of them 'looks'
 * very similar to the new file (by say size or file name) then ask user
 * if this is really true or not. If true then simply update the converned
 * FLFile to point to this new path.
 * <p>
 * This class is thread safe.
 * 
 * @author AppleGrew
 * @since 0.7
 * @version 0.2.1
 */
public class ShareManager {
	private static final Logger logger = GlobalObjects.getLogger(ShareManager.class);
	private final String fileListHash = "hashDump";
	private final String fileList = "files.xml.bz2";
	private final String downloadedFLDirName = "DowloadedFLs";

	protected File miscDir;
	protected File downloadFLDir;
	/**
	 * This is an abstraction of the file list. This is
	 * saved into 'fileListHash' under {@link #miscDir}
	 * directory using serialization.
	 * <p>
	 * <b>Note:</b> You must call {@link #setDirs(String, String) setDirs(String, String)} as soon as
	 * possible as many exceptions could be thrown and it is actually undefined what will happen if
	 * the directories are not setup. After that call {@link #init() init()}.<br>
	 * So, always follow the following steps:-
	 * <ol>
	 * <li>Call Constructor</li>
	 * <li>call {@link #setDirs(String, String) setDirs(String, String)}</li>
	 * <li>Call {@link #init() init()}</li>
	 * </ol>
	 */
	protected FileListManager ownFL;
	protected Map<User, FileListManager> FLs;

	protected HashManager hashMan;
	protected List<ShareManagerListener> listeners;

	private InputEntityStream hash_ies;
	protected UploadStreamManager uploadStreamManager;
	private String hashingFile;
	private double hashSpeed = -1;
	private int maximumFLtoKeepInRAM = 5;
	protected ShareWorker shareWorker = null;

	protected BotInterface boi;

	/**
	 * <b>Note:</b> You must call {@link #setDirs(String, String) setDirs(String, String)} as soon as
	 * possible as many exceptions could be thrown and it is actually undefined what will happen if
	 * the directories are not setup. After that call {@link #init() init()}.<br>
	 * So, always follow the following steps:-
	 * <ol>
	 * <li>Call Constructor</li>
	 * <li>call {@link #setDirs(String, String) setDirs(String, String)}</li>
	 * <li>Call {@link #init() init()}</li>
	 * </ol>
	 * <b>Note:</b> Since version 1.0, the above methods are called automatically by
	 * jDCBot or MultiHubsAdapter's  setShareManager().
	 * @param boi It should be jDCBot or if MultiHubsAdapter is present then it should
	 * be that.
	 */
	public ShareManager(BotInterface boi) {
		ownFL = new FileListManager();
		FLs = Collections.synchronizedMap(new HashMap<User, FileListManager>());
		listeners = Collections.synchronizedList(new ArrayList<ShareManagerListener>());
		hashMan = new HashManager();
		uploadStreamManager = new UploadStreamManager();
		ownFL.setFilelist(null);
		this.boi = boi;
	}

	/**
	 * Called by jDCBot or MultiHubsAdapter on
	 * setShareManager().
	 * <p>
	 * <b>Note:</b> The given directories' must exist and should be empty, as
	 * already existing files in them will overwritten without warning.
	 * 
	 * @param path2DirForMiscData In this directory own file list, hash data, etc. will be kept.
	 */
	public void setDirs(String path2DirForMiscData) {
		miscDir = new File(path2DirForMiscData);
		downloadFLDir = new File(path2DirForMiscData + File.separator + downloadedFLDirName);
		if (!downloadFLDir.exists())
			downloadFLDir.mkdir();
	}

	/**
	 * Called by jDCBot or MultiHubsAdapter on
	 * setShareManager().
	 * <p>
	 * Loads own file list from the disk.
	 */
	public void init() {
		ownFL.setFilelist(null);

		File fl = new File(miscDir.getPath() + File.separator + fileListHash);
		boolean otherException = false;
		try {
			ownFL.setFilelist(FLDir.readObjectFromStream(new BufferedInputStream(new FileInputStream(fl))));
		} catch (FileNotFoundException e) {
			ownFL.setFilelist(new FLDir("Root", true, null));
			ownFL.getFilelist().setCID(generateUniqueCID());
		} catch (IOException e) {
			logger.error("Exception in init()", e);
			otherException = true;
		} catch (ClassNotFoundException e) {
			logger.error("Exception in init()", e);
			otherException = true;
		} catch (InstantiationException e) {
			logger.error("Exception in init()", e);
			otherException = true;
		}

		if (otherException) {
			fl.delete();
		}

		if (ownFL.getFilelist() == null) {
			ownFL.setFilelist(new FLDir("Root", true, null));
			ownFL.getFilelist().setCID(generateUniqueCID());
		}

	}

	public void addListener(ShareManagerListener sml) {
		listeners.add(sml);
	}

	public void removeListener(ShareManagerListener sml) {
		listeners.remove(sml);
	}

	protected void notifyMiscMsg(String msg) {
		synchronized (listeners) {
			for (ShareManagerListener sml : listeners)
				sml.onMiscMsg(msg);
		}
	}

	/**
	 * Deletes the 'fileListHash' dump file
	 * and frees the internal on RAM data structure,
	 * i.e. {@link #ownFL}.
	 */
	public void purgeHash() {
		synchronized (ownFL) {
			ownFL.setFilelist(new FLDir("Root", true, null));
			ownFL.getFilelist().setCID(generateUniqueCID());
			File f = new File(miscDir.getPath() + File.separator + fileListHash);
			f.delete();
		}
	}

	protected void saveOwnFL() throws FileNotFoundException, IOException {
		FLDir.saveObjectToStream(new BufferedOutputStream(new FileOutputStream(miscDir.getPath() + File.separator + fileListHash)), ownFL
				.getFilelist());
	}

	/**
	 * The number of file lists to keep in the
	 * RAM. Excess file lists are unloaded and saved
	 * into secondary storage disk. When required it
	 * will be automatically restored back into the RAM. 
	 * @param count The number of file lists to keep including
	 * bot's own. This can have a minimum value of 2. If it
	 * is not then it is ignored.
	 */
	public void setMaximumFLtoKeepInRAM(int count) {
		if (count < 2)
			return;
		maximumFLtoKeepInRAM = count;
	}

	protected void saveOthersFLs() throws FileNotFoundException, IOException {
		Set<User> users = FLs.keySet();
		synchronized (FLs) {
			for (User u : users)
				FLDir.saveObjectToStream(new BufferedOutputStream(new FileOutputStream(downloadFLDir.getPath() + File.separator
						+ (u.getClientID().isEmpty() ? u.username() : u.getClientID()))), FLs.get(u).getFilelist());
		}
	}

	/**
	 * 
	 * @param u
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void saveOthersFL(User u) throws FileNotFoundException, IOException {
		File fl = new File(downloadFLDir.getPath() + File.separator + (u.getClientID().isEmpty() ? u.username() : u.getClientID()));
		fl.deleteOnExit();
		FLDir.saveObjectToStream(new BufferedOutputStream(new FileOutputStream(fl)), FLs.get(u).getFilelist());
	}

	/**
	 * 
	 * @param u
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @return A new instance of FileListManager with the file list of User <i>u</i>
	 * loaded from the secondary disk.
	 */
	protected FileListManager loadOthersFL(User u) throws FileNotFoundException, IOException, ClassNotFoundException,
	InstantiationException {
		FLDir root =
			FLDir.readObjectFromStream(new BufferedInputStream(new FileInputStream(downloadFLDir.getPath() + File.separator
					+ (u.getClientID().isEmpty() ? u.username() : u.getClientID()))));
		return new FileListManager(root);
	}

	protected void freeOthersFL(User u) {
		try {
			saveOthersFL(u);
		} catch (FileNotFoundException e) {
			logger.error("Exception in loadOthersFL()", e);
		} catch (IOException e) {
			logger.error("Exception in loadOthersFL()", e);
		}
		FLs.remove(u);
	}

	protected void freeExcessOthersFLs(int max) {
		synchronized (FLs) {
			if (FLs.size() <= max)
				return;
			User users[] = FLs.keySet().toArray(new User[0]);
			int i = 0;
			while (FLs.size() > max)
				freeOthersFL(users[i++]);
		}
	}

	public void close() {
		try {
			saveOwnFL();
		} catch (FileNotFoundException e) {
			logger.error("Exception in close()", e);
		} catch (IOException e) {
			logger.error("Exception in close()", e);
		}
	}

	/**
	 * Rebulids the file list using the 
	 * 'fileListHash' {@link #ownFL} dump.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	synchronized public void rebuildFileList() throws FileNotFoundException, IOException {
		OutputStream bos = new BufferedOutputStream(new FileOutputStream(miscDir.getPath() + File.separator + fileList));
		bos.write("BZ".getBytes());
		bos = new CBZip2OutputStream(bos);
		writeFL(bos, ownFL.getFilelist());
		bos.close();
		boi.updateShareSize();
	}

	protected void writeFL(OutputStream out, FLDir flRoot) throws IOException {
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n".getBytes());
		out.write(("<FileListing Version=\"1\" CID=\"" + flRoot.getCID() + "\" Base=\"/\" Generator=\"" + GlobalObjects.CLIENT_NAME + " "
				+ GlobalObjects.VERSION + "\">\n").getBytes());
		writeDir2FL(out, flRoot, "");
		out.write("</FileListing>\n".getBytes());
	}

	private void writeDir2FL(OutputStream out, FLDir dir, String indentTabs) throws UnsupportedEncodingException, IOException {
		if (!dir.isShared())
			return;

		//if (!dir.isRoot() || (dir.isRoot() && dir.hasFile()))
		if (!dir.isRoot())
			out.write((indentTabs + "<Directory Name=\"" + dir.getName() + "\">\n").getBytes("utf-8"));

		FLDir dirs[] = dir.getSubDirs().toArray(new FLDir[0]);
		for (FLDir d : dirs) {
			writeDir2FL(out, d, indentTabs + "\t");
		}

		FLFile files[] = dir.getFiles().toArray(new FLFile[0]);
		for (FLFile f : files) {
			if (f.shared && f.hash != null && !f.hash.isEmpty()) {
				out.write((indentTabs + "\t" + "<File Name=\"" + f.name + "\" Size=\"" + f.size + "\" TTH=\"" + f.hash + "\"/>\n")
						.getBytes("utf-8"));
			} else
				notifyMiscMsg(f + " not written to file list, since it is not shared or its hash is not set.");
		}

		//if (!dir.isRoot() || (dir.isRoot() && dir.hasFile()))
		if (!dir.isRoot())
			out.write((indentTabs + "</Directory>\n").getBytes());
	}

	/**
	 * 
	 * @param search
	 * @param maxResult
	 * @param user The user who has searched for this. If it was an
	 * active search then it is any one arbitrary user from the source
	 * IP. She may not have actually made this search.
	 * @param certainity It is the probability that the above user did actually
	 * search this. (1.0 being certain)
	 * @return null is never returned.
	 */
	public List<SearchResultSet> searchOwnFileList(SearchSet search, final int maxResult, User user, double certainity) {
		synchronized (ownFL) {
			return getOwnFL(user, certainity).search(search, maxResult, false);
		}
	}

	/**
	 * This is similar to {@link #searchOwnFileList(SearchSet, int, User, double)}
	 * except for the fact that it guarantees that the search is made in bot's
	 * own file list, no  matter what {@link #getOwnFL(User,double)} returns. Furthermore,
	 * it doesn't require you to specify some weird arguments like the user who
	 * is searching and the certainity factor. 
	 * @param search
	 * @param maxResult
	 * @return
	 */
	public List<SearchResultSet> searchOwnFileList(SearchSet search, final int maxResult) {
		synchronized (ownFL) {
			return ownFL.search(search, maxResult, false);
		}
	}

	/**
	 * @return Percentage completion of hashing task.
	 */
	public int getPercentageHashCompletion() {
		if (hash_ies == null)
			return 100;
		return (int) Math.round(hash_ies.getPercentageCompletion());
	}

	/**
	 * @return Speed of hashing in
	 * bytes/second.
	 */
	public double getHashingSpeed() {
		if (hash_ies == null)
			return 0;
		return hash_ies.getTransferRate();
	}

	/**
	 * Will cancel hashing while adding new
	 * share or updating existing share.
	 */
	public void cancelHashing() {
		if (shareWorker != null)
			shareWorker.cancelJob();
	}

	/**
	 * @return Time left to complete hashing
	 * in seconds. -1 is returned when this value
	 * cannot be calculated. 
	 */
	public double getTimeLeft2CompleteHashing() {
		if (hash_ies == null)
			return -1;
		return hash_ies.getTimeRemaining();
	}

	/**
	 * @return The path of the currently hashing
	 * file.
	 */
	public String getCurrentlyHashedFileName() {
		if (hashingFile == null)
			return "";
		return hashingFile;
	}

	/**
	 * You need to use UploadStreamManager to monitor
	 * progress of uploads and restrict the upload
	 * transfer rate to a value.
	 * @return Returns an UploadStreamManager. 
	 */
	public UploadStreamManager getUploadStreamManager() {
		return uploadStreamManager;
	}

	public void setMaxHashingSpeed(double rate) {
		if (hash_ies != null)
			hash_ies.setTransferLimit(rate);
		hashSpeed = rate;
	}

	public double getMaxHashingSpeed() {
		return hashSpeed;
	}

	/**
	 * 
	 * @param file The hash (should start with "TTH/") or virtual path to file to download. The
	 * path may start with Root or may not. Also the path may start with / or may not, it is always
	 * assumed that the path is an absolute path name. And yes of course the path deliminator can be
	 * forward '/' or backward '\' slashes.
	 * @param fileType The file type. See {@link org.elite.jdcbot.framework.DUEntity#fileType fileType}
	 * @param start
	 * @param fileLen
	 * @return It returns an instance of DUEntity with its properly initialized and its <i>in</i>
	 * field set to a valid InputStream.
	 * @throws FileNotFoundException If the file is not found in the file list or it no more exists
	 * on the file system.
	 */
	public DUEntity getFile(User u, String file, DUEntity.Type fileType, long start, long Len) throws FileNotFoundException {
		if (fileType == DUEntity.Type.FILELIST)
			return getFileList(u);

		if (fileType == DUEntity.Type.TTHL)
			throw new FileNotFoundException("Not supported");

		FLDir root = getOwnFL(u, 1.0);

		FLFile f;
		String tfile = file;
		if (tfile.startsWith("TTH/")) {//file is hash
			tfile = tfile.substring(4);
			f = root.getFileInTreeByHash(tfile, true);

		} else {//file is not hash
			tfile = tfile.replace('\\', '/');
			if (!tfile.startsWith("/" + root.getName()) || !tfile.startsWith(root.getName()))
				tfile = root.getName() + (tfile.startsWith("/") ? "" : "/") + tfile;
			FLInterface fd = root.getChildInTree(FLDir.getDirNamesFromPath(tfile), false);
			if (fd == null || fd instanceof FLDir)
				throw new FileNotFoundException("File not found");
			f = (FLFile) fd;
		}

		if (f == null)
			throw new FileNotFoundException("File not found");

		if (!canUpload(u, f.path))
			throw new FileNotFoundException("File not found");

		DUEntity due;
		if (!f.path.startsWith("cache://")) {

			File ff = new File(f.path);
			if (!ff.canRead()) {
				throw new FileNotFoundException("File is not readable");
			}
			if (!ff.exists()) {
				f.shared = false;
				throw new FileNotFoundException("File not found");
			}

			due =
				new DUEntity(DUEntity.Type.FILE, file, start, Len, uploadStreamManager.getInputEntityStream(u, new BufferedInputStream(
						new FileInputStream(ff))));

		} else {
			//This allows you to send 'virtual files', i.e. files which are stored in RAM as byte stream.
			//These files' FLFile.path should start with cache://.

			byte b[] = getCacheFileData(f.path.substring(8));
			if (b == null)
				throw new FileNotFoundException("File not found");
			due =
				new DUEntity(DUEntity.Type.FILE, file, start, Len, uploadStreamManager.getInputEntityStream(u, new BufferedInputStream(
						new ByteArrayInputStream(b))));
			f.size = b.length;
		}

		if (start >= f.size)
			throw new FileNotFoundException("Start offset exceeds or is equal to the file length");

		try {
			due.in().skip(start);
		} catch (IOException e) {
			logger.error("Exception in getFile()", e);
			throw new FileNotFoundException("File was found but IOException occured while skipping to the requested position");
		}

		if (due.in() instanceof InputEntityStream) {
			InputEntityStream in = (InputEntityStream) due.in();
			in.setTotalStreamLength(Len); //Needed for calculating percentage completion.
		}

		return due;
	}

	public DUEntity getFileList(User u) throws FileNotFoundException {

		byte b[] = getVirtualFLData(u);
		InputStream ifl;
		long len;
		if (b == null) {

			File fl = new File(miscDir.getPath() + File.separator + "files.xml.bz2");
			if (!fl.exists() && fl.canRead())
				throw new FileNotFoundException("User file list not found or is not readable.");

			ifl = new FileInputStream(fl);
			len = fl.length();

		} else {
			len = b.length;
			ifl = new ByteArrayInputStream(b);
		}

		DUEntity due = new DUEntity(DUEntity.Type.FILELIST, "", 0, len, new BufferedInputStream(ifl));
		due.in();

		return due;
	}

	public FileListManager getOwnFileListManager() {
		return ownFL;
	}

	/**
	 * 
	 * @param u The user whose file list is required.
	 * @return Returns the FileListManager of the user. If the user's file list
	 * has not been downloaded then null is returned.
	 */
	public FileListManager getOthersFileListManager(User u) {
		FileListManager flm = FLs.get(u);
		if (flm == null) {
			try {
				flm = loadOthersFL(u);
				if (FLs.size() + 1 > maximumFLtoKeepInRAM) {
					freeExcessOthersFLs(FLs.size() + 1 - maximumFLtoKeepInRAM + 1);
				}
				FLs.put(u, flm);
			} catch (FileNotFoundException e) {
				logger.error("Exception in getOthersFileListManager()", e);
			} catch (IOException e) {
				logger.error("Exception in getOthersFileListManager()", e);
			} catch (ClassNotFoundException e) {
				logger.error("Exception in getOthersFileListManager()", e);
			} catch (InstantiationException e) {
				logger.error("Exception in getOthersFileListManager()", e);
			}
		}
		return flm;
	}

	/**
	 * 
	 * @param all If set to true then files which are
	 * not shared, but still are in the tree;
	 * their sizes too will be counted.
	 * @return Your total share size.
	 */
	public long getOwnShareSize(boolean all) {
		synchronized (ownFL) {
			if (ownFL.getFilelist() == null)
				return 0;
			return ownFL.getFilelist().getSize(all);
		}
	}

	/**
	 * Adds new files or directories to share. If these files already exists
	 * in share (could be hidden) then they are skipped.
	 * @param includes The list of files or directories to share.
	 * @param excludes The list of files or directories to exclude
	 * from <i>includes</i>.
	 * @param filter This allows you to filter out files like ones which are
	 * hidden, etc. 
	 * @param inside This is the virtual path in the file list where you want the
	 * shares to get added. If this is null then root of the file list is assumed. 
	 * @throws HashException When exception occurs while starting hashing.
	 * @throws ShareException When hashing is in progress since the last time
	 * this method or {@link #updateShare(Vector)} was called or <i>inside</i>
	 * path is not null and it is not found.
	 */
	public void addShare(List<File> includes, List<File> excludes, FilenameFilter filter, String inside) throws HashException,
	ShareException {
		if (shareWorker != null)
			throw new ShareException(ShareException.Error.HASHING_JOB_IN_PROGRESS);
		FLDir root = null;
		if (inside != null) {
			FLInterface fi = ownFL.getFilelist().getChildInTree(FLDir.getDirNamesFromPath(sanitizeVirtualPath(inside)), true);
			if (fi == null || fi instanceof FLFile)
				throw new ShareException(ShareException.Error.FILE_OR_DIR_NOT_FOUND);
			root = (FLDir) fi;
		}
		ShareAdder sa = new ShareAdder(includes, excludes, filter, root);
		shareWorker = sa;
		sa.addShare();
	}

	/**
	 * Removes files and directories from share. Note that
	 * they are <b>not</b> deleted from the share, but
	 * simply hidden from share. You will need to call
	 * {@link #pruneUnsharedShares()} to actually delete them.
	 * @param fORd The list of virtual paths to files and/or
	 * directories to be removed from share.
	 * @throws ShareException When any of the given paths are not found.
	 * The process is not interrupted, rather at the completion of this task
	 * the last path that was not found is returned in the message. All the
	 * paths that were not found are reported using onMiscMsg() event.
	 */
	public void removeShare(List<String> fORd) throws ShareException {
		String pathNotFound = null;

		FLDir fl = ownFL.getFilelist();
		for (String p : fORd) {
			p = sanitizeVirtualPath(p);
			FLInterface fd = fl.getChildInTree(FLDir.getDirNamesFromPath(p), false);
			if (fd != null) {
				if (fd instanceof FLDir)
					((FLDir) fd).setShared(false);
				else
					((FLFile) fd).shared = false;
			} else {
				pathNotFound = p;
				notifyMiscMsg("Path: '" + p + "' not found and hence cannot be removed.");
			}
		}
		try {
			rebuildFileList();
		} catch (FileNotFoundException e) {
			logger.error("Exception in removeShare()", e);
		} catch (IOException e) {
			logger.error("Exception in removeShare()", e);
		}
		if (pathNotFound != null) {
			throw new ShareException(pathNotFound, ShareException.Error.FILE_OR_DIR_NOT_FOUND);
		}
	}

	/**
	 * Has mercy over petty users who don't care
	 * give a proper virtual path.
	 * <p>
	 * Converts \ to /, if the path doesn't start
	 * with / then prefixes one, if the path doesn't
	 * start with /Root then prefixes one. Note
	 * that use this to sanitize ONLY absolute
	 * paths, not relative paths.
	 * <p>
	 * This method is re-entrant.
	 * 
	 * @param p The absolute virtual path to sanitize.
	 * @return Sanitized virtual path.
	 */
	protected String sanitizeVirtualPath(String p) {
		p = p.replace('\\', '/');
		FLDir fl = ownFL.getFilelist();
		if (!p.startsWith("/" + fl.getName()))
			p = "/" + fl.getName() + (p.startsWith("/") ? "" : "/") + p;
		return p;
	}

	/**
	 * Rehashes the given files.
	 * @param fORd Path to the list of files or
	 * directories to hash. If the path is a directory
	 * then all files in its tree will be rehashed. The
	 * path may or maynot start wiht /Root.
	 * @throws HashException When exception occurs while starting hashing.
	 * @throws ShareException When hashing is in progress since the last time
	 * this method or {@link #addShare(Vector, Vector, FilenameFilter)} was called.
	 */
	public void updateShare(List<String> fORd) throws HashException, ShareException {
		if (shareWorker != null)
			throw new ShareException(ShareException.Error.HASHING_JOB_IN_PROGRESS);

		shareWorker = prvUpdateShare(fORd);
	}

	private ShareUpdater prvUpdateShare(List<String> fORd) throws HashException {
		ShareUpdater su = new ShareUpdater(fORd);
		shareWorker = su;
		su.updateShare();
		return su;
	}

	/**
	 * Deletes all FLDir and FLFiles in own
	 * file list that are not shared.
	 */
	public void pruneUnsharedShares() {
		synchronized (ownFL) {
			ownFL.getFilelist().pruneUnsharedSharesInTree();
		}
	}

	/**
	 * @return All FLFiles in own file list whose path
	 * points to not existant files. It will never be
	 * null.
	 */
	public Collection<FLFile> getAllNonExistantFiles() {
		synchronized (ownFL) {
			return ownFL.getFilelist().getAllNonExistantFiles();
		}
	}

	/**
	 * Downloads other user's file list.
	 * @param u
	 * @throws BotException
	 */
	public void downloadOthersFileList(User u) throws BotException {
		if (u != null)
			u.downloadFileList(new FilelistDownloader(u), DUEntity.NO_SETTING);
	}

	/**
	 * Generates a unique ID for the client.
	 */
	protected String generateUniqueCID() {
		return hashMan.getHash(Long.toString(System.currentTimeMillis()) + "jDCBot" + Double.toString(Math.random()));
	}

	//************Private Classes*******************/

	private abstract class ShareWorker implements HashUser {
		protected void notifyHashingOfFileSkipped(String f, String reason) {
			synchronized (listeners) {
				for (ShareManagerListener sml : listeners)
					sml.hashingOfFileSkipped(f, reason);
			}
		}

		public abstract void cancelJob();

		protected void notifyHashingOfFileComplete(String f, boolean success, HashException e) {
			synchronized (listeners) {
				for (ShareManagerListener s : listeners)
					s.hashingOfFileComplete(f, success, e);
			}
		}

		protected boolean isSubOf(File who, File ofWhom) {
			if (GlobalFunctions.isWindowsOS())
				return who.getAbsolutePath().toLowerCase().startsWith(ofWhom.getAbsolutePath().toLowerCase());
			else
				return who.getAbsolutePath().startsWith(ofWhom.getAbsolutePath());
		}

		protected FLDir createParentFLDirs(File f, File targetparent, FLDir parent) {
			f = f.getParentFile();
			if (f.equals(targetparent)) {
				return parent;
			} else {
				FLDir myparent = createParentFLDirs(f, targetparent, parent);
				FLDir me = new FLDir(f.getName(), false, myparent);
				if (!myparent.addSubDir(me)) {//then it already exists.
					return myparent.getDirInTree(me);
				}
				return me;
			}
		}

		protected void notifyHashingOfFileStarting(String f) {
			synchronized (listeners) {
				for (ShareManagerListener s : listeners)
					s.hashingOfFileStarting(f);
			}
		}

		protected void notifyHashingJobFinished() {
			synchronized (listeners) {
				for (ShareManagerListener s : listeners)
					s.hashingJobFinished();
			}
		}

		protected long totalSize(Collection<File> all) {
			long size = 0;
			for (File fd : all) {
				size += totalSize(fd);
			}
			return size;
		}

		protected long totalSize(File any) {
			long size = 0;
			if (any.isDirectory()) {
				File fs[] = any.listFiles(new FileFilter() {

					public boolean accept(File pathname) {
						if (pathname.isFile())
							return true;
						else
							return false;
					}
				});
				if (fs != null)
					for (File f : fs) {
						size += f.length();
					}

				File ds[] = any.listFiles(new FileFilter() {

					public boolean accept(File pathname) {
						if (pathname.isDirectory())
							return true;
						else
							return false;
					}
				});
				if (ds != null)
					for (File d : ds)
						size += totalSize(d);
			} else
				size = any.length();
			return size;
		}
	}

	private class ShareAdder extends ShareWorker {
		private List<File> includes;
		private List<File> excludes;
		private FilenameFilter filter;
		private List<FLFile> flfiles;
		private List<String> updateShare;
		private ShareUpdater shareUpdater = null;
		private FLDir root;

		private List<FLDir> inc_fldir;
		private List<File> inc_dir;

		private boolean closing = false;

		public ShareAdder(List<File> includes, List<File> excludes, FilenameFilter filter, FLDir inside) {
			hash_ies = null;
			this.includes = includes;
			this.excludes = excludes;
			this.filter = filter;
			synchronized (ownFL) {
				if (ownFL.getFilelist() == null) {
					ownFL.setFilelist(new FLDir("Root", true, null));
					ownFL.getFilelist().setCID(generateUniqueCID());
				}
				root = inside;
				if (root == null)
					root = ownFL.getFilelist();
				flfiles = ownFL.getFilelist().getAllFilesUnderTheTree();
			}

			inc_dir = Collections.synchronizedList(new ArrayList<File>());
			inc_fldir = Collections.synchronizedList(new ArrayList<FLDir>());
			synchronized (root) {
				for (File d : includes) {
					if (d.isDirectory()) {
						try {
							d = d.getCanonicalFile();
							inc_dir.add(d);
							FLDir fld = new FLDir(d.getName(), false, root);
							inc_fldir.add(fld);
							root.addSubDir(fld);
						} catch (IOException e) {
							logger.error("Exception in ShareAdder()", e);
						}
					}
				}
			}
			for (int i = 0; i < inc_dir.size(); i++) { //removing FLDirs which are sub-dir of an existing FLDir.
				for (int j = 0; j < inc_dir.size();) {
					if (i == j) {
						j++;
						continue;
					}
					if (isSubOf(inc_dir.get(i), inc_dir.get(j))) {
						inc_dir.remove(i);
						inc_fldir.remove(i);
						if (i > j)
							j++;
						else
							i--;
						break;
					} else
						j++;
				}
			}

			updateShare = Collections.synchronizedList(new ArrayList<String>());
		}

		public void addShare() throws HashException {
			hashMan.hash(includes, this);
		}

		public void cancelJob() {
			closing = true;
			if (shareUpdater == null) {
				hashMan.cancelHashing();
				//finish();
			} else {
				shareUpdater.cancelJob();
			}
		}

		public boolean canHash(File f) {
			File cf = f;
			try {
				cf = f.getCanonicalFile();
			} catch (IOException e) {
				logger.error("Exception in canHash()", e);
			}

			if (closing) {
				notifyHashingOfFileSkipped(cf.getAbsolutePath(), "Hashing cancelled.");
				return false;
			}

			if (filter.accept(cf.getParentFile(), cf.getName())) {
				boolean accept = true;
				String reason = "";
				int in = excludes.indexOf(f);
				if (in != -1) {
					accept = false;
					reason = "In exclude list.";
					excludes.remove(in); //Done so that for next iteration we need to search lesser elements.
				}
				FLFile flf = new FLFile(cf.getName(), cf.length(), cf.getAbsolutePath(), cf.lastModified(), true, null);
				in = flfiles.indexOf(flf);
				if (in != -1) {
					FLFile tflf = flfiles.get(in);
					if (tflf.lastModified != flf.lastModified || tflf.size != flf.size) {
						if (accept)
							updateShare.add(tflf.getVirtualPath());
					}
					tflf.shared = accept;
					if (accept)
						reason = "Already shared.";
					//Done so that for next iteration we need to search lesser elements.
					//A possible source of problem could be when this file is in includes more
					//than once, then the second time it will get added becuase of the following
					//line. Anyway it has been assumed that it will not happen.
					flfiles.remove(in);
					accept = false;
				}
				if (accept)
					return true;
				else
					notifyHashingOfFileSkipped(cf.getAbsolutePath(), reason);
			}
			return false;
		}

		public InputStream getInputStream(File f) {
			if (closing)
				return null;

			try {
				if (hash_ies == null) {
					hash_ies = new InputEntityStream(new BufferedInputStream(new FileInputStream(f)));
					hash_ies.setTotalStreamLength(totalSize(includes) - totalSize(excludes));
					hash_ies.setTransferLimit(hashSpeed);
				} else {
					hash_ies.setInputStream(new BufferedInputStream(new FileInputStream(f)));
				}
			} catch (FileNotFoundException e) {
				logger.error("Exception in getInputStream()", e);
			}
			return hash_ies;
		}

		public void onFileHashed(File f, String hash, boolean success, HashException e) {
			try {
				f = f.getCanonicalFile();
			} catch (IOException ioe) {
				logger.error("Exception in onFileHashed()", e);
			}

			if (success) {
				FLFile flf = new FLFile(f.getName(), f.length(), f.getAbsolutePath(), f.lastModified(), true, null);
				flf.hash = hash;

				boolean added = false;
				for (int i = 0; i < inc_dir.size(); i++) {
					File d = inc_dir.get(i);
					if (isSubOf(f, d)) {
						FLDir parent = createParentFLDirs(f, d, inc_fldir.get(i));
						parent.addFile(flf);
						flf.parent = parent;
						added = true;
						break;
					}
				}
				if (!added) {//This file was not a sub of any dirs above, so add it in the Root.
					root.addFile(flf);
					flf.parent = root;
				}
			}
			notifyHashingOfFileComplete(f.getAbsolutePath(), success, e);
		}

		public void hashingOfFileStarting(File file) {
			try {
				file = file.getCanonicalFile();
			} catch (IOException e) {
				logger.error("Exception in hashingOfFileStarting()", e);
			}
			hashingFile = file.getName();
			notifyHashingOfFileStarting(file.getAbsolutePath());
		}

		public void onHashingJobFinished() {
			hash_ies = null;
			hashingFile = null;

			if (closing) {
				finish();
				return;
			}

			boolean updatedShare = false;
			if (updateShare.size() != 0) {
				try {
					shareUpdater = prvUpdateShare(updateShare);
					updatedShare = true;
				} catch (HashException e) {
					logger.error("Exception in onHashingJobFinished()", e);
				}
			}

			if (!updatedShare) {
				finish();
			}

		}

		private void finish() {
			try {
				saveOwnFL();
				rebuildFileList();
			} catch (FileNotFoundException e) {
				logger.error("Exception in finish()", e);
			} catch (IOException e) {
				logger.error("Exception in finish()", e);
			}

			hash_ies = null;
			hashingFile = null;
			shareWorker = null;

			notifyHashingJobFinished();
		}

	}

	private class ShareUpdater extends ShareWorker {
		private Map<File, FLFile> _updateShares;
		private boolean closing = false;

		public ShareUpdater(List<String> updateShares) {
			_updateShares = new HashMap<File, FLFile>();
			synchronized (ownFL) {
				FLDir fl = ownFL.getFilelist();
				for (String p : updateShares) {
					if (!p.startsWith("/" + fl.getName()))
						p = "/" + fl.getName() + "/" + p;
					FLInterface fd = fl.getChildInTree(FLDir.getDirNamesFromPath(p), false);
					if (fd instanceof FLDir) {
						FLDir d = ((FLDir) fd);
						for (FLFile f : d.getAllFilesUnderTheTree())
							_updateShares.put(new File(f.path), f);
					} else {
						FLFile f = ((FLFile) fd);
						_updateShares.put(new File(f.path), f);
					}
				}
			}
		}

		public void updateShare() throws HashException {
			hashMan.hash(_updateShares.keySet(), this);
		}

		public void cancelJob() {
			closing = true;
			hashMan.cancelHashing();
			//finish();
		}

		public boolean canHash(File f) {
			if (closing) {
				notifyHashingOfFileSkipped(f.getAbsolutePath(), "Hashing cancelled.");
				return false;
			}
			return true;
		}

		public InputStream getInputStream(File f) {
			if (closing)
				return null;

			try {
				if (hash_ies == null) {
					hash_ies = new InputEntityStream(new BufferedInputStream(new FileInputStream(f)));
					hash_ies.setTotalStreamLength(totalSize(_updateShares.keySet()));
					hash_ies.setTransferLimit(hashSpeed);
				} else {
					hash_ies.setInputStream(new BufferedInputStream(new FileInputStream(f)));
				}
			} catch (FileNotFoundException e) {
				logger.error("Exception in getInputStream()", e);
			}
			return hash_ies;
		}

		public void onFileHashed(File f, String hash, boolean success, HashException e) {
			if (success) {
				_updateShares.get(f).hash = hash;
			}
			notifyHashingOfFileComplete(f.getAbsolutePath(), success, e);
		}

		public void hashingOfFileStarting(File file) {
			hashingFile = file.getName();
			notifyHashingOfFileStarting(file.getAbsolutePath());
		}

		public void onHashingJobFinished() {
			finish();
		}

		private void finish() {
			try {
				saveOwnFL();
				rebuildFileList();
			} catch (FileNotFoundException e) {
				logger.error("Exception in finish()", e);
			} catch (IOException e) {
				logger.error("Exception in finish()", e);
			}

			hash_ies = null;
			hashingFile = null;
			shareWorker = null;

			notifyHashingJobFinished();
		}

	}

	private class FilelistDownloader extends ByteArrayOutputStream {
		User _u;

		public FilelistDownloader(User u) {
			_u = u;
		}

		public void close() {
			FilelistConverter fc = new FilelistConverter(this.toByteArray());
			try {
				FLDir root = fc.parse();
				_u.setClientID(root.getCID());
				if (FLs.size() + 1 >= maximumFLtoKeepInRAM) {
					freeExcessOthersFLs(FLs.size() + 1 - maximumFLtoKeepInRAM + 1);
				}
				FLs.put(_u, new FileListManager(root));
				notifyListeners(true, null);
			} catch (ParserConfigurationException e) {
				logger.error("Exception in close()", e);
				notifyListeners(false, e);
			} catch (SAXException e) {
				logger.error("Exception in close()", e);
				notifyListeners(false, e);
			} catch (IOException e) {
				logger.error("Exception in close()", e);
				notifyListeners(false, e);
			}
		}

		private void notifyListeners(boolean success, Exception e) {
			synchronized (listeners) {
				for (ShareManagerListener sml : listeners)
					sml.onFilelistDownloadFinished(_u, success, e);
			}
		}
	}

	//***********Methods meant to be overridden*************/

	/**
	 * This is a method that allows you implement
	 * a <i>unique and weird</i>, albeit
	 * an interesting, feature, to stop particular
	 * users from downloading particular files from
	 * you. <b>Note:</b> You are advised to use
	 * {@link org.elite.jdcbot.framework.User#setBlockUploadToUser(boolean)}
	 * to block upload of all files to a particular
	 * user.
	 * <p>
	 * You will need to extend this class and overload this
	 * method to implement this feature.
	 * @param u The User who is trying to download
	 * from you. When this method is called you be certain that
	 * remote user's IP is available irrespective of the fact that
	 * you are an Operator in the hub or not.
	 * @param path The actual path to the file on the file system that the
	 * user is requesting.
	 * @return Return true to allow the upload. Passing false will
	 * return "File not found" error to the remote user.
	 */
	protected boolean canUpload(User u, String path) {
		return true;
	}

	/**
	 * Allows you to send data of a virtual
	 * file. A virtual file is simply bytes
	 * stored in main memory (RAM). It could
	 * have been read from a file, but usually
	 * this will be String.getBytes().
	 * @param uri The path of the virtual
	 * file without the 'cache://' prefix.
	 * This path can be anything independent
	 * of its location in file list. This path
	 * is simply FLFile.path without the
	 * 'cache://' prefix.
	 * @return The bytes of the virtual file.
	 */
	protected byte[] getCacheFileData(String uri) {
		return null;
	}

	/**
	 * This provides easy means to set
	 * different exclusive file list
	 * for a particular user. You can
	 * use this (say) return empty file
	 * list to a particular user.
	 * <p>
	 * You probably won't be using this feature
	 * but what's the problem with giving
	 * you the power to do so. ;-)
	 * <p>
	 * You will most probably use it in conjugation with
	 * {@link #getOwnFL(User, double)}.
	 * 
	 * @param u The user who wants to download
	 * your file list.
	 * @return The file list's bytes. It
	 * must have been compressed using
	 * bzip2 and must be in XML format.
	 */
	protected byte[] getVirtualFLData(User u) {
		return null;
	}

	/**
	 * Override this if you want to provide a
	 * custom file list for a user. This will
	 * be called by {@link #getFile(User, String, org.elite.jdcbot.framework.DUEntity.Type, long, long)}
	 * and {@link #searchOwnFileList(SearchSet, int, User, double)}.
	 * <p>
	 * You will most probably use it in conjugation with
	 * {@link #getVirtualFLData(User)}.
	 * @param u The user in question.
	 * @param certainity This is probability value
	 * which specifies how certain we are that the
	 * above user is <u>really</u> one who needs to access
	 * your file list. This weird argument was needed as
	 * during active search only user's IP is known and
	 * many users can actually share that IP. If only
	 * one user was found with this IP then probability
	 * is 1.0, else it gets divided by the total number
	 * of users found with that IP. In passive search,
	 * etc. it is always 1.0. But do note that <b><i>u</i>
	 * can be null</b> sometimes (particularly in case of
	 * active search) as IP information of all users is
	 * not always available.
	 * <p>
	 * See comment in the code of
	 * {@link org.elite.jdcbot.framework.jDCBot#onSearch(String,int,SearchSet) onSearch(String,int,SearchSet)}
	 * to exactly how this probability is calculated.
	 * @return Root FLDir of the file list.
	 */
	protected FLDir getOwnFL(User u, double certainity) {
		synchronized (ownFL) {
			return ownFL.getFilelist();
		}
	}
}
