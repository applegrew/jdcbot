/*
 * FileListManager.java
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

import java.util.Vector;

/**
 * Created on 05-Jun-08<br>
 * This provides methods to manipulate,
 * search, etc. the file list. Please
 * note that the virtual paths are UNIX
 * style that they are forward slash '/'
 * delimited and are case sensitive.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class FileListManager {
    private FLDir filelist = null;

    private FLDir pwd;
    private FLInterface cut_buffer = null;
    private FLInterface sel = null;

    public FileListManager() {}

    public FileListManager(FLDir fl) {
	filelist = fl;
    }

    public FLDir getFilelist() {
	return filelist;
    }

    public void setFilelist(FLDir fl) {
	filelist = fl;
	pwd = fl;
    }

    /**
     * Searches in the whole of the tree according to the criteria set.
     * @param For The searching term and criteria.
     * @param maxResult Maximum number of results to return.
     * Set this to &lt;=0 to get all the results found.
     * @param all If true then it will search files with <i>shared</i> == false too.
     * @return A Vector list of the matching files/directories. null is
     * returned only if file list has yet not been set.
     */
    public Vector<SearchResultSet> search(SearchSet For, final int maxResult, boolean all) {
	if (filelist == null)
	    return null;
	else
	    return filelist.search(For, maxResult, all);
    }

    public FLDir getPwd() {
	return pwd;
    }

    public void cd(String path) throws ShareException {
	if (filelist == null)
	    throw new ShareException(ShareException.Error.FILELIST_NOT_YET_SET);

	FLInterface p = filelist.getChildInTree(getDirNamesFromPath(path), true);
	if (p == null)
	    throw new ShareException(path + " not found", ShareException.Error.FILE_OR_DIR_NOT_FOUND);
	pwd = (FLDir) p;
    }

    public void select(String path) throws ShareException {
	if (filelist == null)
	    throw new ShareException(ShareException.Error.FILELIST_NOT_YET_SET);

	FLInterface s = filelist.getChildInTree(getDirNamesFromPath(path), false);
	if (s == null)
	    throw new ShareException(path + " not found", ShareException.Error.FILE_OR_DIR_NOT_FOUND);
	sel = s;
    }

    /**
     * It won't modify the tree
     * until it is pasted.
     */
    public void cut() {
	if (sel != null)
	    cut_buffer = sel;
    }

    public void paste() throws ShareException {
	if (filelist == null)
	    throw new ShareException(ShareException.Error.FILELIST_NOT_YET_SET);
	if (pwd == null)
	    pwd = filelist;

	if (cut_buffer == null)
	    throw new ShareException(ShareException.Error.NOTHING_TO_PASTE);
	if (pwd.equals(cut_buffer))
	    throw new ShareException(ShareException.Error.CANNOT_PASTE_DIR_INTO_ITSELF);
	if (pwd.hasChild(cut_buffer))
	    throw new ShareException(ShareException.Error.CANNOT_PASTE_NAME_ALREADY_EXISTS);
	if (cut_buffer instanceof FLDir) {
	    FLDir c = (FLDir) cut_buffer;
	    if (pwd.getDirPath().startsWith(c.getDirPath()))
		throw new ShareException(ShareException.Error.CANNOT_PASTE_NAME_ALREADY_EXISTS);
	}

	if (cut_buffer instanceof FLDir) {
	    FLDir c = (FLDir) cut_buffer;
	    c.getParent().removeSubDir(c);
	    pwd.addSubDir(c);
	    c.setParent(pwd);
	} else {
	    FLFile c = (FLFile) cut_buffer;
	    filelist.deleteFileInTree(c);
	    pwd.addFile(c);
	}
    }

    /**
     * Renames the given virtual file or
     * directory in the file list to another name.
     * @param what
     * @param to
     */
    public void rename(String what, String to) {
    //TODO
    }

    public Vector<String> getDirNamesFromPath(String path) {
	if (pwd == null)
	    pwd = filelist;
	if (!path.startsWith("/"))
	    path = pwd.getDirPath() + "/" + path;

	String p[] = path.split("/");
	Vector<String> v = new Vector<String>();
	for (int i = 1; i < p.length; i++)
	    v.add(p[i]);
	return v;
    }
}
