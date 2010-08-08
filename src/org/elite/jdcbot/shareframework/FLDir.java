/*
 * FLDir.java
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.elite.jdcbot.util.GlobalFunctions;

/**
 * Created on 04-Jun-08<br>
 * This represents a virtual directory of
 * a file list.
 * <p>
 * You should avoid using this class' object as
 * key in Map (and its sub-classes). If you need to
 * do that then call {@link #setImmutable()} to render
 * the object immutable. This will make {@link #setParent(FLDir)}
 * and {@link #setName(String)}
 * have no effect at all.
 * <p>
 * This class is thread-safe.
 * 
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class FLDir implements Serializable, FLInterface {
    private static final long serialVersionUID = 8442912993644448963L;
    private final int HASH_CONST = 11;

    private String _name;
    private FLDir _parent;
    private volatile boolean _isRoot;
    private List<FLFile> _files;
    private List<FLDir> _dirs;
    private volatile boolean isJDCBotGenerated = false;
    private volatile boolean isShared = true;
    /**
     * Should be set only if this dir is
     * the root.
     */
    private String CID = null;
    /**
     * Not setting this to volatile as it is
     * always accessed in synchronized
     * mthods.
     */
    private boolean isImmutable = false;

    /**
     * 
     * @param name The name of the virtual directory in the file list.
     * @param isRoot Is this the virtual root directory. This directory
     * is normaly not shown in the file list. This is merely a way to emcompass
     * all shared files and directories under a single directory. Hence,
     * only ONE directory must be set as root per file list. If this is
     * root then its name is set to 'Root'.
     * @param parent The parent of this FLDir. This should be null for
     * root.
     */
    public FLDir(String name, boolean isRoot, FLDir parent) {
	_files = Collections.synchronizedList(new ArrayList<FLFile>());
	_dirs = Collections.synchronizedList(new ArrayList<FLDir>());
	_name = name;
	_isRoot = isRoot;
	_parent = parent;
	if (isRoot)
	    _name = "Root";
    }

    /**
     * Once rendered Immutable
     * then the object cannot
     * made mutable again.
     *
     */
    synchronized public void setImmutable() {
	isImmutable = true;
    }

    synchronized public FLDir getParent() {
	return _parent;
    }

    /**
     * Once rendered immutable
     * calling this method will have
     * no effect.
     * @param d
     */
    synchronized public void setParent(FLDir d) {
	if (!isImmutable)
	    _parent = d;
    }

    synchronized public void setCID(String cid) {
	CID = cid;
    }

    public void setJDCBotGenerated(boolean flag) {
	isJDCBotGenerated = flag;
    }

    /**
     * @return true if the file list (which is represented by
     * this FLDir and FLFile tree) is jDCBot generated or not.
     * This can be checked for downloaded file lists too.
     * <p>
     * Only Root FLDir has this value set.
     */
    public boolean isJDCBotGenerated() {
	return isJDCBotGenerated;
    }

    /**
     * Sets the isShared flag of this directory.
     * When this becomes unshared then all its
     * children become invisble in the file list
     * too. (Note that their shared flag is not
     * altered though).
     * <p>
     * Root can never be unshared.
     * @param flag
     */
    synchronized public void setShared(boolean flag) {
	if (!_isRoot)
	    isShared = flag;
    }

    public boolean isShared() {
	return isShared;
    }

    /**
     * This will set this directory's
     * isShared flag to true and will
     * also make sure that all its parent directories
     * too have their isShared flag to true
     * so that this directory can get actually
     * shared.
     */
    synchronized public void actuallyShareIt() {
	isShared = true;
	if (_parent != null)
	    _parent.actuallyShareIt();
    }

    synchronized public String getCID() {
	return CID;
    }

    public boolean isRoot() {
	return _isRoot;
    }

    synchronized public String getName() {
	return _name;
    }

    /**
     * Once rendered immutable
     * calling this method will have
     * no effect.
     * @param name
     */
    synchronized public void setName(String name) {
	if (!isImmutable)
	    _name = name;
    }

    /**
     * Searches for matching file or directory.
     * @param For The searching term and criteria.
     * @param maxResult Maximum number of results to return.
     * Set this to &lt;=0 to get all the results found.
     * @param all If true then it will search files with <i>shared</i> == false too.
     * @return An ArrayList of the matching files/directories. null is never returned.
     */
    public List<SearchResultSet> search(SearchSet For, final int maxResult, boolean all) {
	String ss[] = For.string.toLowerCase().trim().split(" ");
	List<FLInterface> sr = new ArrayList<FLInterface>();
	List<String> owners = new ArrayList<String>();
	synchronized (_files) {
	    synchronized (_dirs) {
		search(For, ss, sr, owners, maxResult, all);
	    }
	}
	return convertFLItoSRS(sr, owners);
    }

    /**
     * Not thread-safe in itself and hence must be called from a synchronized block.
     * @param For
     * @param ss
     * @param sr
     * @param owners
     * @param maxResult
     * @param all
     */
    private void search(SearchSet For, String ss[], List<FLInterface> sr, List<String> owners, final int maxResult, boolean all) {
	String pwd = this.getDirPath() + "/";
	if (For.data_type != SearchSet.DataType.DIRECTORY) {
	    for (FLFile f : _files) {
		if (maxResult > 0 && sr.size() > maxResult)
		    break;
		if (!all && f.shared == false)
		    continue;

		switch (For.data_type) {
		    case ANY:
			if (GlobalFunctions.matches(ss, f.name) && fulfillsSizeCriteria(f, For)) {
			    sr.add(f);
			    owners.add(pwd);
			}
			break;
		    case AUDIO:
			if (GlobalFunctions.hasExt(f.name, new String[] { "mp3", "mp2", "wav", "au", "rm", "mid", "sm", "ogg" })
				&& GlobalFunctions.matches(ss, f.name) && fulfillsSizeCriteria(f, For)) {
			    sr.add(f);
			    owners.add(pwd);
			}
			break;
		    case COMPRESSED:
			if (GlobalFunctions.hasExt(f.name, new String[] { "zip", "arj", "rar", "lzh", "gz", "z", "arc", "pak", "bz2" })
				&& GlobalFunctions.matches(ss, f.name) && fulfillsSizeCriteria(f, For)) {
			    sr.add(f);
			    owners.add(pwd);
			}
			break;
		    case DOCUMENT:
			if (GlobalFunctions.hasExt(f.name, new String[] { "doc", "txt", "wri", "pdf", "ps", "tex", "ppt", "pptx", "docx" })
				&& GlobalFunctions.matches(ss, f.name) && fulfillsSizeCriteria(f, For)) {
			    sr.add(f);
			    owners.add(pwd);
			}
			break;
		    case EXECUTABLE:
			if (GlobalFunctions.hasExt(f.name, new String[] { "pm", "exe", "bat", "com", "sh", "class" })
				&& GlobalFunctions.matches(ss, f.name) && fulfillsSizeCriteria(f, For)) {
			    sr.add(f);
			    owners.add(pwd);
			} else if (!GlobalFunctions.isWindowsOS()) {
			    File ff = new File(f.path);
			    if (ff.exists() && ff.canExecute() && GlobalFunctions.matches(ss, f.name) && fulfillsSizeCriteria(f, For)) {
				sr.add(f);
				owners.add(pwd);
			    }
			}
			break;
		    case PICTURE:
			if (GlobalFunctions.hasExt(f.name, new String[] { "gif", "jpg", "jpeg", "bmp", "pcx", "png", "wmf", "psd", "tif" })
				&& GlobalFunctions.matches(ss, f.name) && fulfillsSizeCriteria(f, For)) {
			    sr.add(f);
			    owners.add(pwd);
			}
			break;
		    case VIDEO:
			if (GlobalFunctions.hasExt(f.name, new String[] { "mpg", "mpeg", "avi", "asf", "mov", "mp4", "mkv", "divx", "rmvb",
				"rm", "ogg" })
				&& GlobalFunctions.matches(ss, f.name) && fulfillsSizeCriteria(f, For)) {
			    sr.add(f);
			    owners.add(pwd);
			}
			break;
		    case TTH:
			if (f.hash.equalsIgnoreCase(For.string.trim()) && fulfillsSizeCriteria(f, For)) {
			    sr.add(f);
			    owners.add(pwd);
			}
			break;
		}
	    }
	}

	for (FLDir d : _dirs) {
	    if (maxResult > 0 && sr.size() > maxResult)
		break;
	    if (!all && d.isShared == false)
		continue;

	    if ((For.data_type == SearchSet.DataType.DIRECTORY || For.data_type == SearchSet.DataType.ANY)
		    && (GlobalFunctions.matches(ss, d._name)))
		sr.add(d);
	    d.search(For, ss, sr, owners, maxResult, all);
	}

    }

    /**
     * No issue of thread safety here as this is re-entrant.
     * @param f
     * @param SS
     * @return true if <i>f</i> fulfills size criteria as specified
     * in <i>SS</i>.
     */
    private boolean fulfillsSizeCriteria(FLFile f, SearchSet SS) {
	SearchSet.SizeCriteria c = SS.size_criteria;
	SearchSet.SizeUnit u = SS.size_unit;
	long size = SS.size;

	if (c == SearchSet.SizeCriteria.NONE)
	    return true;
	size = u == SearchSet.SizeUnit.BYTE ? size : u.getValue() * 1024 * size;
	if (c == SearchSet.SizeCriteria.ATLEAST) {
	    if (f.size >= size)
		return true;
	    else
		return false;
	} else {
	    if (f.size <= size)
		return true;
	    else
		return false;
	}
    }

    /**
     * No thread safety issue here as this is re-entrant.
     * @param sr
     * @param owners For every FLFile in <i>sr</i> there is one entry in this, in
     * exact order of its appearance in <i>sr</i>. This and <i>sr</i> size
     * may not be same as no owner information is stored for FLDirs. This is an
     * old code when FLFiles had no reference of their parents, hence it was
     * required to store their parents's path to create FLFiles full path. 
     * @return An ArrayList of SearchResultSet created using list of FLInterface.
     */
    private List<SearchResultSet> convertFLItoSRS(List<FLInterface> sr, List<String> owners) {
	List<SearchResultSet> SR = new ArrayList<SearchResultSet>();
	int i = 0;
	for (FLInterface fd : sr) {
	    SearchResultSet srs = new SearchResultSet();
	    if (fd instanceof FLDir) {
		FLDir d = (FLDir) fd;
		srs.isDir = true;
		srs.name = d.getDirPath();
		srs.size = 0;
		srs.TTH = "";
	    } else {
		FLFile f = (FLFile) fd;
		srs.isDir = false;
		srs.name = owners.get(i++) + f.name;
		srs.size = f.size;
		srs.TTH = f.hash;
	    }
	    SR.add(srs);
	}
	return SR;
    }

    public boolean removeFile(FLFile f) {
	return _files.remove(f);
    }

    /**
     * This will add the file only if
     * didn't already existed. It will
     * automatically set its parent.
     * @param f
     * @return True if the file has been
     * added, and false it it already existed.
     */
    public boolean addFile(FLFile f) {
	synchronized (_files) {
	    if (_files.indexOf(f) == -1) {
		_files.add(f);
		f.parent = this;
		return true;
	    } else
		return false;
	}
    }

    public void addFile(List<FLFile> files) {
	_files.addAll(files);
    }

    public boolean isFileExistsInTree(FLFile f) {
	if (_files.indexOf(f) != -1)
	    return true;
	else {
	    synchronized (_dirs) {
		for (FLDir d : _dirs)
		    if (d.isFileExistsInTree(f))
			return true;
	    }
	}
	return false;
    }

    /**
     * Deletes the file <i>f</i> that could be
     * at any depth in the tree. It is assumed
     * that a file exists only once in the
     * tree hence it will be removed from at its
     * first occurrence.
     * @param f
     * @return true if the file was found and deleted.
     */
    public boolean deleteFileInTree(FLFile f) {
	synchronized (_files) {
	    int in = _files.indexOf(f);
	    if (in != -1) {
		_files.remove(in);
		return true;
	    } else {
		synchronized (_dirs) {
		    for (FLDir d : _dirs)
			if (d.deleteFileInTree(f))
			    return true;
		}
	    }
	    return false;
	}
    }

    /**
     * @return List of FLFile. A new ArrayList is
     * created before returing.
     */
    public List<FLFile> getFiles() {
	synchronized (_files) {
	    return new ArrayList<FLFile>(_files);
	}
    }

    /**
     * @param hash
     * @param all If this is true then even FLFiles with share==false
     * will be searched and FLDir with isShared==false will be looked into.
     * @return
     */
    public FLFile getFileInTreeByHash(String hash, boolean all) {
	synchronized (_files) {
	    for (FLFile f : _files)
		if ((all || f.shared) && f.hash.equalsIgnoreCase(hash))
		    return f;
	}
	synchronized (_dirs) {
	    for (FLDir d : _dirs) {
		if (!all && !d.isShared)
		    continue;

		FLFile f = d.getFileInTreeByHash(hash, all);
		if (f != null)
		    return f;
	    }
	}
	return null;
    }

    /**
     * Recursively returns all files under this directory
     * and its sub-directories.
     * @return It will never be null. A new ArrayList is
     * created for returning.
     */
    public List<FLFile> getAllFilesUnderTheTree() {
	synchronized (_files) {
	    List<FLFile> files = new ArrayList<FLFile>(_files);
	    synchronized (_dirs) {
		for (FLDir d : _dirs) {
		    files.addAll(d.getAllFilesUnderTheTree());
		}
	    }
	    return files;
	}
    }

    /**
     * Returns a reference to a FLDir in the
     * tree which is same as the given <i>d</i>.
     * @param d The FLDir to search for.
     * @return Reference to the similar FLDir from
     * anywhere inside the tree.
     */
    public FLFile getFileInTree(FLFile f) {
	synchronized (_files) {
	    int in = _files.indexOf(f);
	    if (in != -1)
		return _files.get(in);
	    else {
		FLFile rf = null;
		synchronized (_dirs) {
		    for (FLDir D : _dirs)
			if ((rf = D.getFileInTree(f)) != null)
			    return rf;
		}
	    }
	    return null;
	}
    }

    /**
     * @return All FLFiles whose <i>path</i> point to
     * non-existant files. It will never be null.
     */
    public List<FLFile> getAllNonExistantFiles() {
	synchronized (_files) {
	    List<FLFile> files = new ArrayList<FLFile>();
	    for (FLFile f : _files)
		if (!new File(f.path).exists())
		    files.add(f);
	    synchronized (_dirs) {
		for (FLDir d : _dirs)
		    files.addAll(d.getAllNonExistantFiles());
	    }
	    return files;
	}
    }

    /**
     * @return true if this directory has atleast one file
     * directly in it.
     */
    synchronized public boolean hasFile() {
	return _files.size() != 0;
    }

    /**
     * @return true if this directory has no sub-directories
     * or files.
     */
    synchronized public boolean isEmpty() {
	return _files.size() != 0 && _dirs.size() != 0;
    }

    public boolean removeSubDir(FLDir d) {
	return _dirs.remove(d);
    }

    /**
     * Adds a sub-directory only if it didn't exist.
     * It will automatically set it parent if it isn't
     * set as immutable.
     * @param d The directory to add.
     * @return true of the directory didn't exist and now
     * it has been added, else false.
     */
    public boolean addSubDir(FLDir d) {
	synchronized (_dirs) {
	    if (_dirs.indexOf(d) != -1)
		return false;
	    _dirs.add(d);
	    d.setParent(this);
	    return true;
	}
    }

    public void addSubDirs(List<FLDir> dirs) {
	_dirs.addAll(dirs);
    }

    /**
     * @return All immediate sub-directories of this directory in
     * a newly created ArrayList.
     */
    public List<FLDir> getSubDirs() {
	synchronized (_dirs) {
	    return new ArrayList<FLDir>(_dirs);
	}
    }

    /**
     * Name of the sub-directory to get. Note, only
     * immediate sub-directories are searched.
     * @param name The sub-directory's name.
     * @return Reference to that directory. null if its
     * not found.
     */
    public FLDir getSubDir(String name) {
	synchronized (_dirs) {
	    for (FLDir d : _dirs)
		if (d._name.equals(name))
		    return d;
	    return null;
	}
    }

    /**
     * Deletes the sub-directory <i>d</i> that could be
     * at any depth in the tree. It is assumed
     * that a directory exists only once in the
     * tree hence it will be removed from at its
     * first occurence.
     * @param d
     * @return true if the directory was found and deleted.
     */
    public boolean deleteSubDirInTree(FLDir d) {
	synchronized (_dirs) {
	    int in = _dirs.indexOf(d);
	    if (in != -1) {
		_dirs.remove(in);
		return true;
	    } else {
		for (FLDir D : _dirs)
		    if (D.deleteSubDirInTree(d))
			return true;
	    }
	    return false;
	}
    }

    /**
     * Returns a reference to a FLDir in the
     * tree which is same as the given <i>d</i>.
     * @param d The FLDir to search for.
     * @return Reference to the similar FLDir from
     * anywhere inside the tree.
     */
    public FLDir getDirInTree(FLDir d) {
	synchronized (_dirs) {
	    int in = _dirs.indexOf(d);
	    if (in != -1)
		return _dirs.get(in);
	    else {
		FLDir rd = null;
		for (FLDir D : _dirs)
		    if ((rd = D.getDirInTree(d)) != null)
			return rd;
	    }
	    return null;
	}
    }

    /**
     * Returns a FLFile or FLDir instance from
     * the tree that has matches the given virtual
     * path.
     * @param path The virtual path split around '/'.
     * The path must start with 'Root'. You
     * can use {@link #getDirNamesFromPath(String)}
     * to convert path to Vector&lt;String&gt;. 
     * @param dirOnly If true then will look for FLDir only.
     * @return Returns null if no such node found.
     */
    public FLInterface getChildInTree(List<String> path, boolean dirOnly) {
	if (path.size() == 0)
	    return null;
	synchronized (_name) {
	    if (!path.get(0).equals(_name))
		return null;
	}
	if (path.size() == 1)
	    return this;
	else {
	    path.remove(0);

	    synchronized (_files) {
		synchronized (_dirs) {

		    String name = path.get(0);
		    for (FLDir d : _dirs)
			if (d._name.equals(name))
			    return d.getChildInTree(path, dirOnly);
		    if (dirOnly)
			return null;
		    if (path.size() != 1)
			return null;
		    for (FLFile f : _files)
			if (f.name.equals(name))
			    return f;
		    return null;
		}
	    }
	}
    }

    /**
     * Use this to split path to List&lt;String&gt;. The forward
     * slash at the beginning of the path is optional.
     * <p>
     * This method is re-entrant.
     * @param path
     * @return
     */
    public static List<String> getDirNamesFromPath(String path) {
	String p[] = path.split("/");
	List<String> v = new ArrayList<String>();
	for (int i = path.startsWith("/") ? 1 : 0; i < p.length; i++)
	    v.add(p[i]);
	return v;
    }

    /**
     * @param i
     * @return true if this directory has <i>i</i>
     * as its immediate file or sub-directory.
     */
    public boolean hasChild(FLInterface i) {
	if (i instanceof FLDir)
	    return _dirs.contains((FLDir) i);
	else if (i instanceof FLFile)
	    return _files.contains((FLFile) i);
	else
	    return false;
    }

    /**
     * Removes all directories in the whole
     * tree which are empty.
     */
    public void pruneEmptyDirsFromTree() {
	synchronized (_dirs) {
	    for (int i = 0; i < _dirs.size(); i++) {
		if (_dirs.get(i).isEmpty())
		    _dirs.remove(i);
		else
		    _dirs.get(i).pruneEmptyDirsFromTree();
	    }
	}
    }

    /**
     * Deletes all FLDir and FLFiles in this
     * tree that are not shared.
     */
    public void pruneUnsharedSharesInTree() {
	pruneUnsharedSharesInTree(isShared);
    }

    private void pruneUnsharedSharesInTree(boolean isShared) {
	synchronized (_files) {
	    Iterator<FLFile> i = _files.iterator();
	    while (i.hasNext()) {
		FLFile f = i.next();
		if (!f.shared || !isShared)
		    i.remove();
	    }
	}
	synchronized (_dirs) {
	    Iterator<FLDir> i = _dirs.iterator();
	    while (i.hasNext()) {
		FLDir d = i.next();
		if (!d.isShared || !isShared) {
		    d.pruneUnsharedSharesInTree(d.isShared && isShared);
		    i.remove();
		}
	    }
	}
    }

    /**
     * You must remember that FLDir are
     * virtual directories and have no
     * physical significance hence
     * this method returns virtual
     * path of the directory.<br>
     * e.g.<br>
     * If A is the parent of B and A
     * is the topmost FLDir (i.e. it is
     * the root and hence has not parent),
     * then B.getDirPath() will return<br>
     * &nbsp;&nbsp;&nbsp;<code>/A/B</code>
     * @return The virtual path of the FLDir.
     */
    synchronized public String getDirPath() {
	String p = "/" + _name;
	if (_parent != null)
	    p = _parent.getDirPath() + p;
	return p;
    }

    /**
     * Saves FLDir and all the sub-FLDirs and FLFiles in its tree to the given
     * OutputStream. The OutputStream will most probably be
     * FileOutputStream.
     * @param out The stream to which this should be saved.
     * @param object The FLDir object to save.
     * @throws IOException Ths is thrown if there is any error while writng to the stream.
     */
    public static void saveObjectToStream(OutputStream out, FLDir object) throws IOException {
	ObjectOutputStream obj_out = new ObjectOutputStream(out);
	obj_out.writeObject(object);
	obj_out.close();
    }

    /**
     * Reads FLDir and all the sub-FLDirs and FLFiles in its tree from the given
     * InputStream. The InputStream will most probably be FileInputStream.
     * @param in The stream from which to read.
     * @return A new instance of FLDir initialized form the data read from the stream.
     * @throws IOException Thrown when error occurs while reading form the stream.
     * @throws ClassNotFoundException Class of a serialized object cannot be found.
     * @throws InstantiationException The read object is not instance of FLDir.
     */
    public static FLDir readObjectFromStream(InputStream in) throws IOException, ClassNotFoundException, InstantiationException {
	ObjectInputStream obj_in = new ObjectInputStream(in);
	Object obj = obj_in.readObject();
	obj_in.close();
	if (obj instanceof FLDir) {
	    // Cast object to a FLDir
	    return (FLDir) obj;
	} else
	    throw new InstantiationException("The object read is not instance of FLDir.");
    }

    /**
     * Two FLDir are equal if their names and parents are equal.
     */
    public boolean equals(Object o) {
	if (this == o)
	    return true;

	if (o instanceof FLDir) {
	    FLDir d = (FLDir) o;
	    if (this._name.equals(d._name) && (this._parent == d._parent || this._parent.equals(d._parent)))
		return true;
	}
	return false;
    }

    synchronized public int hashCode() {
	int hashCode = 1;
	hashCode = hashCode * HASH_CONST + (_name == null ? 0 : _name.hashCode());
	hashCode = hashCode * HASH_CONST + (_parent == null ? 0 : _parent.hashCode());

	return hashCode;
    }

    synchronized public String toString() {
	return "Name: " + _name + ", Parent: " + (_parent == null ? "null" : _parent._name);
    }

    synchronized public String printTree() {
	return "* = Hidden" + "\n" + (isShared ? "" : "*") + _name + "\n" + printTree("|-", isShared);
    }

    private String printTree(String prefix, boolean isShared) {
	String tree = "";
	for (FLFile f : _files) {
	    tree = tree + prefix + (f.shared && isShared ? "" : "*") + f.name + "\n";
	}
	for (FLDir d : _dirs) {
	    tree = tree + prefix + (d.isShared && isShared ? "" : "*") + d._name + "\n";
	    tree = tree + d.printTree(prefix.replace('-', ' ') + "|-", isShared && d.isShared);
	}
	return tree;
    }

    /**
     * Returns the total size of
     * the tree of which this directory is
     * the root. 
     * @param all If set to true then files which are
     * not shared, but still are in the tree;
     * their sizes too will be counted.
     * @return The total size.
     */
    public long getSize(boolean all) {
	long size = 0;
	synchronized (_files) {
	    for (FLFile f : _files)
		size += all || f.shared ? f.size : 0;
	}
	synchronized (_dirs) {
	    for (FLDir d : _dirs)
		size += all || d.isShared ? d.getSize(all) : 0;
	}
	return size;
    }
}
