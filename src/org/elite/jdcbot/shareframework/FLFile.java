/*
 * FLFile.java
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
import java.io.Serializable;

/**
 * Created on 04-Jun-08<br>
 * This represents a file in
 * the file list. This file may
 * exist on the local storage disk
 * at the location given by
 * {@link #getFullPath()}.
 * <p>
 * You should never use objects of
 * this class as keys of Map
 * and its sub-classes. 
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.2
 */
public class FLFile implements Serializable, FLInterface {
    private static final long serialVersionUID = -2879885769034120896L;
    private final int HASH_CONST = 91;

    /**
     * Hash of the file.
     */
    public String hash;
    /**
     * The virtual name of the file.
     * This can be the same as actual name
     * of the file or anything else.
     */
    public String name;
    /**
     * Size of file in bytes.
     */
    public long size;
    /**
     * Full path to the file in the file system.
     */
    public String path;
    /**
     * If this is set to false then this entry won't show up
     * in the file list, but always execute {@link ShareManager#rebuildFileList()}
     * after changing this value else the file list will still won't reflect the
     * changes.
     */
    public boolean shared;

    /**
     * The last modified time
     * stamp of the file as given
     * by java.io.File.lastModified().
     */
    public long lastModified;

    /**
     * Virtual parent directory of this
     * file.
     */
    public FLDir parent;

    public FLFile(){
	this(null);
    }
    
    public FLFile(FLDir p) {
	this("", 0, null, 0, false, p);
    }

    public FLFile(String Name, long Size, String Path, long LastModified, boolean Shared, FLDir p) {
	hash = null;
	name = Name;
	size = Size;
	path = Path;
	lastModified = LastModified;
	shared = Shared;
	parent = p;
    }
    
    /**
     * @return The virtual path of this file.
     * If <i>parent</i> is null then null
     * is returned. 
     */
    public String getVirtualPath(){
	return parent==null?null:parent.getDirPath()+"/"+name;
    }

    /**
     * Two FLFile are equal if their <i>path</i> point to the
     * very same file and their parent directories too are same,
     * unless either of them or both their <i>parent</i>s are null.
     */
    @Override
    public boolean equals(Object o) {
	if (this == o)
	    return true;

	if (o instanceof FLFile) {
	    FLFile f = (FLFile) o;
	    if (this.path == null || f.path == null)
		return false;
	    if (new File(this.path).equals(new File(f.path)) && (this.parent == null || f.parent == null || this.parent.equals(f.parent)))
		return true;
	}
	return false;
    }

    @Override
    public int hashCode() {
	return HASH_CONST + (path == null ? 0 : path.hashCode());
    }

    @Override
    public String toString() {
	return path;
    }
}
