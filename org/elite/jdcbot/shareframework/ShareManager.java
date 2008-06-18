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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.imageio.IIOException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.tools.bzip2.CBZip2OutputStream;
import org.elite.jdcbot.framework.BotException;
import org.elite.jdcbot.framework.DUEntity;
import org.elite.jdcbot.framework.GlobalObjects;
import org.elite.jdcbot.framework.User;
import org.elite.jdcbot.util.GlobalFunctions;
import org.elite.jdcbot.util.InputEntityStream;
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
 * 
 * @author AppleGrew
 * @since 0.7
 * @version 0.2
 */
public class ShareManager {
    private final String fileListHash = "hashDump";
    private final String fileList = "files.xml.bz2";

    private File miscDir;
    private File downloadFLDir;
    /**
     * This is an abstraction of the file list. This is
     * saved into 'fileListHash' under {@link #miscDir}
     * directory using serialization.
     */
    private FileListManager ownFL;
    private Map<User, FileListManager> FLs;

    private HashManager hashMan;
    private Vector<ShareManagerListener> listeners;

    private InputEntityStream hash_ies;
    private UploadStreamManager uploadStreamManager;
    private String hashingFile;
    private double hashSpeed = -1;

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
     */
    public ShareManager() {
	ownFL = new FileListManager();
	FLs = Collections.synchronizedMap(new HashMap<User, FileListManager>());
	listeners = new Vector<ShareManagerListener>();
	uploadStreamManager = new UploadStreamManager();
    }

    /**
     * <b>Note:</b> The given directories' must exist and should be empty, as
     * already existing files in them will overwritten without warning.
     * @param path2DirForMiscData In this directory own file list, hash data, etc. will be kept.
     * @param path2downloadedFileLists The downloaded file lists will be kept here.
     * 
     * @throws FileNotFoundException If the directory paths are not found or 'fileListHash'
     * doesn't exist.
     * @throws IIOException If the given path are not directories.
     * @throws InstantiationException The read object from 'fileListHash' is not instance of FLDir.
     * @throws ClassNotFoundException Class of FLDir serialized object cannot be found.
     * @throws IOException Error occured while reading from 'fileListHash'.
     */
    public void setDirs(String path2DirForMiscData, String path2downloadedFileLists) throws IIOException, FileNotFoundException {
	miscDir = new File(path2DirForMiscData);
	downloadFLDir = new File(path2downloadedFileLists);
	if (!miscDir.exists() || !downloadFLDir.exists())
	    throw new FileNotFoundException();
	if (!miscDir.isDirectory())
	    throw new IIOException("Given path '" + path2DirForMiscData + "' is not a directory.");
	if (!downloadFLDir.isDirectory())
	    throw new IIOException("Given path '" + path2downloadedFileLists + "' is not a directory.");
    }

    public void init() throws ClassNotFoundException, InstantiationException, IOException {
	hashMan = new HashManager();
	ownFL.setFilelist(null);
	try {
	    ownFL.setFilelist(FLDir.readObjectFromStream(new BufferedInputStream(new FileInputStream(miscDir.getPath() + File.separator
		    + fileListHash))));
	} catch (FileNotFoundException e) {
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

    private void notifyMiscMsg(String msg) {
	for (ShareManagerListener sml : listeners)
	    sml.onMiscMsg(msg);
    }

    /**
     * Deletes the 'fileListHash' dump file
     * and frees the internal on RAM data structure,
     * i.e. {@link #ownFL}.
     */
    public void purgeHash() {
	ownFL.setFilelist(new FLDir("Root", true, null));
	ownFL.getFilelist().setCID(generateUniqueCID());
	File f = new File(miscDir.getPath() + File.separator + fileListHash);
	f.delete();
    }

    public void saveOwnFL() throws FileNotFoundException, IOException {
	FLDir.saveObjectToStream(new BufferedOutputStream(new FileOutputStream(miscDir.getPath() + File.separator + fileListHash)), ownFL
		.getFilelist());
    }

    public void saveOthersFLs() throws FileNotFoundException, IOException {
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
    public void saveOthersFL(User u) throws FileNotFoundException, IOException {
	FLDir.saveObjectToStream(new BufferedOutputStream(new FileOutputStream(downloadFLDir.getPath() + File.separator
		+ (u.getClientID().isEmpty() ? u.username() : u.getClientID()))), FLs.get(u).getFilelist());
    }

    /**
     * 
     * @param u
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     */
    public void loadOthersFL(User u) throws FileNotFoundException, IOException, ClassNotFoundException, InstantiationException {
	FLDir root =
		FLDir.readObjectFromStream(new BufferedInputStream(new FileInputStream(downloadFLDir.getPath() + File.separator
			+ (u.getClientID().isEmpty() ? u.username() : u.getClientID()))));
	FLs.put(u, new FileListManager(root));
    }

    public void freeOthersFL(User u) {
	FLs.remove(u);
    }

    public void freeExcessOthersFLs(int max) {
	if (FLs.size() <= max)
	    return;
	User users[] = FLs.keySet().toArray(new User[0]);
	int i = 0;
	while (FLs.size() > max)
	    freeOthersFL(users[i++]);
    }

    public void close() {
	try {
	    saveOwnFL();
	} catch (FileNotFoundException e) {
	    e.printStackTrace(GlobalObjects.log);
	} catch (IOException e) {
	    e.printStackTrace(GlobalObjects.log);
	}
    }

    /**
     * Rebulids the file list using the 
     * 'fileListHash' {@link #ownFL} dump.
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public void rebuildFileList() throws FileNotFoundException, IOException {
	OutputStream bos = new BufferedOutputStream(new FileOutputStream(miscDir.getPath() + File.separator + fileList));
	bos.write("BZ".getBytes());
	bos = new CBZip2OutputStream(bos);
	bos.write("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n".getBytes());
	bos.write(("<FileListing Version=\"1\" CID=\"" + ownFL.getFilelist().getCID() + "\" Base=\"/\" Generator=\"jDCBot "
		+ GlobalObjects.VERSION + "\">\n").getBytes());
	writeDir2FL(bos, ownFL.getFilelist(), "");
	bos.write("</FileListing>\n".getBytes());
	bos.close();
    }

    private void writeDir2FL(OutputStream out, FLDir dir, String indentTabs) throws UnsupportedEncodingException, IOException {
	if (!dir.isShared())
	    return;

	if (!dir.isRoot() || (dir.isRoot() && dir.hasFile()))
	    out.write((indentTabs + "<Directory Name=\"" + dir.getName() + "\">\n").getBytes("utf-8"));

	FLDir dirs[] = dir.getSubDirs().toArray(new FLDir[0]);
	for (FLDir d : dirs) {
	    writeDir2FL(out, d, indentTabs + "\t");
	}

	FLFile files[] = dir.getFiles().toArray(new FLFile[0]);
	for (FLFile f : files) {
	    if (f.shared && f.hash != null) {
		out.write((indentTabs + "\t" + "<File Name=\"" + f.name + "\" Size=\"" + f.size + "\" TTH=\"" + f.hash + "\"/>\n")
			.getBytes("utf-8"));
	    } else
		notifyMiscMsg(f + " not written to file list, since it is not shared or its hash is not set.");
	}

	if (!dir.isRoot() || (dir.isRoot() && dir.hasFile()))
	    out.write((indentTabs + "</Directory>\n").getBytes());
    }

    public Vector<SearchResultSet> searchOwnFileList(SearchSet search, final int maxResult) {
	return ownFL.search(search, maxResult, false);
    }

    public int getPercentageHashCompletion() {
	if (hash_ies == null)
	    return 100;
	return (int) Math.round(hash_ies.getPercentageCompletion());
    }

    public double getHashingSpeed() {
	if (hash_ies == null)
	    return 0;
	return hash_ies.getTransferRate();
    }

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
	    return getFileList();

	if (fileType == DUEntity.Type.TTHL)
	    throw new FileNotFoundException("Not supported");

	FLFile f;
	String tfile = file;
	if (tfile.startsWith("TTH/")) {//file is hash
	    tfile = tfile.substring(4);
	    f = ownFL.getFilelist().getFileInTreeByHash(tfile, true);

	} else {//file is not hash
	    tfile = tfile.replace('\\', '/');
	    if (!tfile.startsWith("/" + ownFL.getFilelist().getName()) || !tfile.startsWith(ownFL.getFilelist().getName()))
		tfile = ownFL.getFilelist().getName() + (tfile.startsWith("/") ? "" : "/") + tfile;
	    FLInterface fd = ownFL.getFilelist().getChildInTree(FLDir.getDirNamesFromPath(tfile), false);
	    if (fd == null || fd instanceof FLDir)
		throw new FileNotFoundException("File not found");
	    f = (FLFile) fd;
	}

	if (f == null)
	    throw new FileNotFoundException("File not found");

	if (!canUpload(u, f.path))
	    throw new FileNotFoundException("File not found");

	File ff = new File(f.path);
	if (!ff.canRead()) {
	    throw new FileNotFoundException("File is not readable");
	}
	if (!ff.exists()) {
	    f.shared = false;
	    throw new FileNotFoundException("File not found");
	}

	DUEntity due =
		new DUEntity(DUEntity.Type.FILE, file, start, Len, uploadStreamManager.getInputEntityStream(u, new BufferedInputStream(
			new FileInputStream(ff))));

	if (start >= f.size)
	    throw new FileNotFoundException("Start offset exceeds or is equal to the file length");

	try {
	    due.in().skip(start);
	} catch (IOException e) {
	    e.printStackTrace(GlobalObjects.log);
	    throw new FileNotFoundException("File was found but IOException occured while skipping to the requested position");
	}

	if (due.in() instanceof InputEntityStream) {
	    InputEntityStream in = (InputEntityStream) due.in();
	    in.setTotalStreamLength(Len); //Needed for calculating percentage completion.
	}

	return due;
    }

    public DUEntity getFileList() throws FileNotFoundException {

	File fl = new File(miscDir.getPath() + File.separator + "files.xml.bz2");
	if (!fl.exists() && fl.canRead())
	    throw new FileNotFoundException("User file list not found or is not readable.");

	DUEntity due = new DUEntity(DUEntity.Type.FILELIST, "", 0, fl.length(), new BufferedInputStream(new FileInputStream(fl)));
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
	return FLs.get(u);
    }

    /**
     * 
     * @param all If set to true then files which are
     * not shared, but still are in the tree;
     * their sizes too will be counted.
     * @return Your total share size.
     */
    public long getOwnShareSize(boolean all) {
	if (ownFL.getFilelist() == null)
	    return 0;
	return ownFL.getFilelist().getSize(all);
    }

    /**
     * Adds new files or directories to share. If these files already exists
     * in share (could be hidden) then they are skipped.
     * @param includes The list of files or directories to share.
     * @param excludes The list of files or directories to exclude
     * from <i>includes</i>.
     * @param filter This allows you to filter out files like ones which are
     * hidden, etc. 
     * @throws HashException When exception occurs while starting hashing.
     */
    public void addShare(Vector<File> includes, Vector<File> excludes, FilenameFilter filter) throws HashException {
	ShareAdder sa = new ShareAdder(includes, excludes, filter);
	sa.addShare();
    }

    /**
     * Removes files and directories from share. Note that
     * they are <b>not</b> deleted from the share, but
     * simply hidden from share. You will need to call
     * {@link #pruneUnsharedShares()} to actually delete them.
     * @param fORd The list of virtual paths to files and/or
     * directories to be removed from share.
     */
    public void removeShare(Vector<String> fORd) {
	FLDir fl = ownFL.getFilelist();
	for (String p : fORd) {
	    if (!p.startsWith("/" + fl.getName()))
		p = "/" + fl.getName() + "/" + p;
	    FLInterface fd = fl.getChildInTree(FLDir.getDirNamesFromPath(p), false);
	    if (fd instanceof FLDir)
		((FLDir) fd).setShared(false);
	    else
		((FLFile) fd).shared = false;
	}
	try {
	    rebuildFileList();
	} catch (FileNotFoundException e) {
	    e.printStackTrace(GlobalObjects.log);
	} catch (IOException e) {
	    e.printStackTrace(GlobalObjects.log);
	}
    }

    /**
     * Rehashes the given files.
     * @param fORd Path to the list of files or
     * directories to hash. If the path is a directory
     * then all files in its tree will be rehashed. The
     * path may or maynot start wiht /Root.
     * @throws HashException When exception occurs while starting hashing.
     */
    public void updateShare(Vector<String> fORd) throws HashException {
	ShareUpdater su = new ShareUpdater(fORd);
	su.updateShare();
    }

    /**
     * Deletes all FLDir and FLFiles in own
     * file list that are not shared.
     */
    public void pruneUnsharedShares() {
	ownFL.getFilelist().pruneUnsharedSharesInTree();
    }

    /**
     * @return All FLFiles in own file list whose path
     * points to not existant files.
     */
    public Collection<FLFile> getAllNotExistantFiles() {
	//TODO
	return null;
    }

    public void downloadOthersFileList(User u) throws BotException {
	u.downloadFileList(new FilelistDownloader(u), DUEntity.NO_SETTING);
    }

    /**
     * Generates a unique ID for the client.
     */
    private String generateUniqueCID() {
	return hashMan.getHash(Long.toString(System.currentTimeMillis()) + "jDCBot" + Double.toString(Math.random()));
    }

    private abstract class ShareWorker implements HashUser {
	protected void notifyHashingOfFileSkipped(String f, String reason) {
	    for (ShareManagerListener sml : listeners)
		sml.hashingOfFileSkipped(f, reason);
	}

	protected void notifyHashingOfFileComplete(String f, boolean success, HashException e) {
	    for (ShareManagerListener s : listeners)
		s.hashingOfFileComplete(f, success, e);
	}

	protected boolean isSubDirOf(File who, File ofWhom) {
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
	    for (ShareManagerListener s : listeners)
		s.hashingOfFileStarting(f);
	}

	protected void notifyHashingJobFinished() {
	    for (ShareManagerListener s : listeners)
		s.hashingJobFinished();
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
	private Vector<File> includes;
	private Vector<File> excludes;
	private FilenameFilter filter;
	private Vector<FLFile> flfiles;
	private Vector<String> updateShare;

	private Vector<FLDir> inc_fldir;
	private Vector<File> inc_dir;

	public ShareAdder(Vector<File> includes, Vector<File> excludes, FilenameFilter filter) {
	    hash_ies = null;
	    this.includes = includes;
	    this.excludes = excludes;
	    this.filter = filter;
	    if (ownFL.getFilelist() == null) {
		ownFL.setFilelist(new FLDir("Root", true, null));
		ownFL.getFilelist().setCID(generateUniqueCID());
	    }
	    flfiles = ownFL.getFilelist().getAllFilesUnderTheTree();

	    inc_dir = new Vector<File>();
	    inc_fldir = new Vector<FLDir>();
	    for (File d : includes) {
		if (d.isDirectory()) {
		    try {
			d = d.getCanonicalFile();
			inc_dir.add(d);
			FLDir fld = new FLDir(d.getName(), false, ownFL.getFilelist());
			inc_fldir.add(fld);
			ownFL.getFilelist().addSubDir(fld);
		    } catch (IOException e) {
			e.printStackTrace(GlobalObjects.log);
		    }
		}
	    }
	    for (int i = 0; i < inc_dir.size(); i++) {
		for (int j = 0; j < inc_dir.size();) {
		    if (i == j) {
			j++;
			continue;
		    }
		    if (isSubDirOf(inc_dir.get(i), inc_dir.get(j))) {
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

	    updateShare = new Vector<String>();
	}

	public void addShare() throws HashException {
	    hashMan.hash(includes, this);
	}

	public boolean canHash(File f) {
	    try {
		f = f.getCanonicalFile();
	    } catch (IOException e) {
		e.printStackTrace(GlobalObjects.log);
	    }

	    if (filter.accept(f.getParentFile(), f.getName())) {
		boolean accept = true;
		String reason = "";
		int in = excludes.indexOf(f);
		if (in != -1) {
		    accept = false;
		    reason = "In exclude list.";
		    excludes.remove(in); //Done so that for next iteration we need to search lesser elements.
		}
		FLFile flf = new FLFile(f.getName(), f.length(), f.getAbsolutePath(), f.lastModified(), true, null);
		in = flfiles.indexOf(flf);
		if (in != -1) {
		    FLFile tflf = flfiles.get(in);
		    if (tflf.lastModified != flf.lastModified || tflf.size != flf.size) {
			//ownFL.getFilelist().deleteFileInTree(tflf);
			if (accept)
			    updateShare.add(tflf.getVirtualPath());
			//} else {
			tflf.shared = accept;
			if (accept)
			    reason = "Already shared.";
			flfiles.remove(in); //Done so that for next iteration we need to search lesser elements.
			accept = false;
		    }
		}
		if (accept)
		    return true;
		else
		    notifyHashingOfFileSkipped(f.getAbsolutePath(), reason);
	    }
	    return false;
	}

	public InputStream getInputStream(File f) {
	    try {
		if (hash_ies == null) {
		    hash_ies = new InputEntityStream(new BufferedInputStream(new FileInputStream(f)));
		    hash_ies.setTotalStreamLength(totalSize(includes) - totalSize(excludes));
		    hash_ies.setTransferLimit(hashSpeed);
		} else {
		    hash_ies.setInputStream(new BufferedInputStream(new FileInputStream(f)));
		}
	    } catch (FileNotFoundException e) {
		e.printStackTrace(GlobalObjects.log);
	    }
	    return hash_ies;
	}

	public void onFileHashed(File f, String hash, boolean success, HashException e) {
	    try {
		f = f.getCanonicalFile();
	    } catch (IOException ioe) {
		ioe.printStackTrace(GlobalObjects.log);
	    }

	    if (success) {
		for (int i = 0; i < inc_dir.size(); i++) {
		    File d = inc_dir.get(i);
		    if (isSubDirOf(f, d)) {
			FLFile flf = new FLFile(f.getName(), f.length(), f.getAbsolutePath(), f.lastModified(), true, null);
			flf.hash = hash;
			FLDir parent = createParentFLDirs(f, d, inc_fldir.get(i));
			parent.addFile(flf);
			flf.parent = parent;
			break;
		    }
		}
	    }
	    notifyHashingOfFileComplete(f.getAbsolutePath(), success, e);
	}

	public void hashingOfFileStarting(File file) {
	    try {
		file = file.getCanonicalFile();
	    } catch (IOException e) {
		e.printStackTrace(GlobalObjects.log);
	    }
	    hashingFile = file.getName();
	    notifyHashingOfFileStarting(file.getAbsolutePath());
	}

	public void onHashingJobFinished() {
	    hash_ies = null;
	    hashingFile = null;

	    boolean updatedShare = false;
	    if (updateShare.size() != 0) {
		try {
		    updateShare(updateShare);
		    updatedShare = true;
		} catch (HashException e) {
		    e.printStackTrace(GlobalObjects.log);
		}
	    }

	    if (!updatedShare) {
		try {
		    saveOwnFL();
		    rebuildFileList();
		} catch (FileNotFoundException e) {
		    e.printStackTrace(GlobalObjects.log);
		} catch (IOException e) {
		    e.printStackTrace(GlobalObjects.log);
		}

		notifyHashingJobFinished();
	    }

	}

    }

    private class ShareUpdater extends ShareWorker {
	private Map<File, FLFile> _updateShares;

	public ShareUpdater(Vector<String> updateShares) {
	    _updateShares = new HashMap<File, FLFile>();
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

	public void updateShare() throws HashException {
	    hashMan.hash(_updateShares.keySet(), this);
	}

	public boolean canHash(File f) {
	    return true;
	}

	public InputStream getInputStream(File f) {
	    try {
		if (hash_ies == null) {
		    hash_ies = new InputEntityStream(new BufferedInputStream(new FileInputStream(f)));
		    hash_ies.setTotalStreamLength(totalSize(_updateShares.keySet()));
		    hash_ies.setTransferLimit(hashSpeed);
		} else {
		    hash_ies.setInputStream(new BufferedInputStream(new FileInputStream(f)));
		}
	    } catch (FileNotFoundException e) {
		e.printStackTrace(GlobalObjects.log);
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
	    hash_ies = null;
	    hashingFile = null;
	    try {
		saveOwnFL();
		rebuildFileList();
	    } catch (FileNotFoundException e) {
		e.printStackTrace(GlobalObjects.log);
	    } catch (IOException e) {
		e.printStackTrace(GlobalObjects.log);
	    }

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
		FLs.put(_u, new FileListManager(root));
		_u.setClientID(root.getCID());
		notifyListeners(true, null);
	    } catch (ParserConfigurationException e) {
		e.printStackTrace(GlobalObjects.log);
		notifyListeners(false, e);
	    } catch (SAXException e) {
		e.printStackTrace(GlobalObjects.log);
		notifyListeners(false, e);
	    } catch (IOException e) {
		e.printStackTrace(GlobalObjects.log);
		notifyListeners(false, e);
	    }
	}

	private void notifyListeners(boolean success, Exception e) {
	    for (ShareManagerListener sml : listeners)
		sml.onFilelistDownloadFinished(_u, success, e);
	}
    }

    /**
     * This is a method that allows you implement
     * a <i>unique and weird</i>, albeit
     * an interesting, feature, to stop particular
     * users from downloading particular files from
     * you. <b>Note:</b> You are advised to use
     * {@link org.elite.jdcbot.framework.User#setBlockUploadToUser(boolean)}
     * to bloack upload of all files to a particular
     * user.
     * <p>
     * You will need to extend this class and overload this
     * method to implement this feature.
     * @param u The User who is trying to download
     * from you.
     * @param path The actual path to the file on the file system that the
     * user is requesting.
     * @return Return true to allow the upload. Passing false will
     * return "File not found" error to the remote user.
     */
    protected boolean canUpload(User u, String path) {
	return true;
    }

    public static void main(String a[]) {
	//This method if for testing only.

	System.out.println(File.pathSeparator);
	System.out.println(File.separator);

	Vector<File> includes = new Vector<File>();
	Vector<File> excludes = new Vector<File>();
	includes.add(new File("/home/appl"));

	ShareManager sm = null;
	sm = new ShareManager();
	try {
	    sm.setDirs("settings", "settings/downloadedFileLists");
	} catch (FileNotFoundException e2) {
	    e2.printStackTrace();
	    return;
	} catch (IIOException e) {
	    e.printStackTrace();
	    return;
	}

	try {
	    System.out.println("loading share from hashDump if possible...");
	    sm.init();
	    System.out.println("Done...");
	} catch (IOException e2) {
	    e2.printStackTrace();
	    sm.purgeHash();
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    sm.purgeHash();
	} catch (InstantiationException e) {
	    e.printStackTrace();
	    sm.purgeHash();
	}

	sm.setMaxHashingSpeed(4 * 1024 * 1024);
	try {
	    if (sm.ownFL.getFilelist().getAllFilesUnderTheTree().size() == 0) {
		System.out.println("Adding share...");
		sm.addShare(includes, excludes, new FilenameFilter() {
		    public boolean accept(File arg0, String arg1) {
			if (arg1.startsWith(".") || new File(arg0 + File.separator + arg1).isHidden())
			    return false;
			else
			    return true;
		    }
		});
	    } else
		System.out.println("Share successfully loaded from the hashDump.");
	} catch (HashException e1) {
	    e1.printStackTrace();
	}

	try {
	    Thread.sleep(4000);
	} catch (InterruptedException e1) {
	    e1.printStackTrace();
	}
	int pc;
	while ((pc = sm.getPercentageHashCompletion()) < 100) {
	    System.out.println(pc + "% " + (sm.getHashingSpeed() / 1024 / 1024) + " MBps");
	    try {
		Thread.sleep(1000);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
	System.out.println(sm.ownFL.getFilelist().printTree());
	try {
	    sm.saveOwnFL();
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

}
