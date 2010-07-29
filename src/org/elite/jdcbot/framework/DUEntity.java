/*
 * DownloadEntity.java
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Created on 26-May-08<br>
 * Download/Upload Entity.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.2
 * 
 */
public class DUEntity implements Serializable {
    private static final long serialVersionUID = 682551418657251180L;
    private final int HASH_CONST = 81;

    /**
     * The file type.
     * @author AppleGrew
     * @since 1.0
     * @version 0.1
     */
    public static enum Type {
	FILE, FILELIST, TTHL
    }

    /**
     * This setting is set to zero, i.e. it sets and means nothing.
     * This has been given so that while supplying value for settings
     * you need not supply 0 when you donot want to set anything. 
     */
    public static final int NO_SETTING = 0;
    /**
     * File lists are usually compressed as bzip2 files. jDCBot
     * by default automatically decompresses such file lists.
     * Set this flag if you want it to remain compressed.  
     */
    public static final int NO_AUTO_FILELIST_DECOMPRESS_SETTING = 1;
    /**
     * Wheaher the entity is of type FILE or TTHL, now-a-days
     * only TTHs are used to identify the files, so usually
     * the field <i>file</i> will have a hash value. This
     * hash value needs to be prefixed by a 'TTH/' string.
     * If you donnot want to do that yourself then set this
     * flag. 
     */
    public static final int AUTO_PREFIX_TTH_SETTING = 2;

    /**
     * The name or TTH of the file to download.
     */
    private String file;
    /**
     * The OutputStream to which the downloaded file is to be written.
     */
    private transient OutputStream os;
    /**
     * The InputStream from which the file to be uploaded is to be read.
     */
    private transient InputStream in;
    /**
     * The starting byte offset of the file from which to start download. Usually this will be 0.
     */
    private long start;
    /**
     * The total number of bytes starting from <i>start</i> of the file to download. This will be -1 for file lists.
     * <b>Note:</b> That it is total bytes to be uploaded or downloaded, i.e. if out of 100 bytes of a file if
     * 5 bytes have been already downloaded and due an interruption the connection broke, then during re-connection
     * <i>start</i> will be 5 and this will be 95 and <b>not</b> 100.  
     */
    private long len;

    /**
     * The file type.
     */
    private Type fileType = Type.FILE;

    /**
     * It is used to set some flags like, if you donot want the downloaded file list to be automatically decompressed by
     * the framework then set it to NO_AUTO_FILELIST_DECOMPRESS_SETTING. Multiple settings should be ORed together. Better use
     * {@link  #setSetting(int) setSetting} to set the setting and {@link #unsetSetting(int) unsetSetting } to unset
     * an already set setting.
     */
    private int settingFlags = 0;

    private int hashCode = -1;

    protected DUEntity() {
	file="";
	fileType = Type.FILELIST;
	start = 0;
	len = -1;
	in = null;
	os = null;
	settingFlags = 0;
	init();
    }

    /**
     * This constructor initializes <i>start</i> to 0 and <i>len</i>
     * to -1.
     */
    public DUEntity(Type filetype, String file2Download) {
	this.fileType = filetype;
	file = file2Download;

	start = 0;
	len = -1;
	in = null;
	os = null;
	settingFlags = 0;
	init();
    }

    public DUEntity(Type filetype, String file2Download, long Start, long Len) {
	this.fileType = filetype;
	file = file2Download;
	start = Start;
	len = Len;

	in = null;
	os = null;
	settingFlags = 0;
	init();
    }

    public DUEntity(Type filetype, String file2Download, long Start, long Len, OutputStream OS) {
	this(filetype, file2Download, Start, Len, OS, NO_SETTING);
    }

    public DUEntity(Type filetype, String file2Download, long Start, long Len, OutputStream OS, int settings) {
	this.fileType = filetype;
	file = file2Download;
	start = Start;
	len = Len;
	os = OS;
	settingFlags = settings;
	init();
    }

    public DUEntity(Type filetype, String file2Download, long Start, long Len, InputStream IN) {
	this(filetype, file2Download, Start, Len, IN, NO_SETTING);
    }

    public DUEntity(Type filetype, String file2Download, long Start, long Len, InputStream IN, int settings) {
	this.fileType = filetype;
	file = file2Download;
	start = Start;
	len = Len;
	in = IN;
	settingFlags = settings;
	init();
    }

    public DUEntity(DUEntity due) {
	this.file = due.file;
	this.fileType = due.fileType;
	this.start = due.start;
	this.len = due.len;
	this.in = due.in;
	this.os = due.os;
	this.settingFlags = due.settingFlags;
	this.hashCode = due.hashCode;
	init();
    }

    private void init() {
	if (fileType == Type.FILELIST || file == null)
	    hashCode = 0;
	else {
	    hashCode = file.hashCode();
	}
	hashCode += HASH_CONST;
    }

    public String file() {
	return file;
    }

    public Type fileType() {
	return fileType;
    }

    public String getFileType() {
	String ftype = "file";
	if (fileType == Type.TTHL)
	    ftype = "tthl";
	return ftype;
    }

    public long start() {
	return start;
    }

    public void start(long newValue) {
	start = newValue;
    }

    public long len() {
	return len;
    }

    public void len(long newValue) {
	len = newValue;
    }

    public InputStream in() {
	return in;
    }

    public void in(InputStream newStream) {
	in = newStream;
    }

    public OutputStream os() {
	return os;
    }

    public void os(OutputStream newStream) {
	os = newStream;
    }

    public void setSetting(int flag) {
	settingFlags = settingFlags | flag;
    }

    public void unsetSetting(int flag) {
	settingFlags = settingFlags & ~flag;
    }

    public boolean isSettingSet(int flag) {
	return (settingFlags & flag) != 0;
    }

    public void resetSetting() {
	settingFlags = 0;
    }

    /**
     * Its only job is to accept lots of setting flags as arguments and create a single integer
     * that represents all those settings. <b>Note</b>, that it doesn't set {@link #settingFlags settingFlags} at all,
     * it will simply return the combined setting flags. You can pass the output of this method to {@link #isSettingSet(int) isSettingSet}
     * to check for multiple settings at one call, or pass this value to constructors. 
     * @param flags The settings as arguments.
     * @return The settings ORed together. 
     */
    public static int prepareSettings(int... flags) {
	int settings = 0;
	for (int flag : flags) {
	    settings = settings | flag;
	}
	return settings;
    }

    @Override
    public boolean equals(Object o) {
	if (this == o)
	    return true;

	if (o instanceof DUEntity) {
	    DUEntity due = (DUEntity) o;
	    if (this.fileType == due.fileType) {
		if (this.fileType == Type.FILELIST)
		    return true;
		else if (this.file.equals(due.file))
		    return true;
	    }
	}
	return false;
    }

    @Override
    public int hashCode() {
	return hashCode;
    }

    @Override
    public String toString() {
	return "{ " + getFileType() + " " + file + " " + start + " " + len + " }";
    }
}
