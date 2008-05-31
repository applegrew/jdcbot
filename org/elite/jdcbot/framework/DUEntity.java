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

/**
 * Created on 26-May-08<br>
 * Download/Upload Entity.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.2
 * 
 */
public class DUEntity {
    public static final int FILE_TYPE = 1;
    public static final int FILELIST_TYPE = 2;
    public static final int TTHL_TYPE = 3;

    /**
     * This setting is set to zero, i.e. it sets and means nothing.
     * This has been given so that while supplying value for settings
     * you need not supply 0 when you donot want to set anything. 
     */
    public static final int NO_SETTING = 0;
    public static final int NO_AUTO_FILELIST_DECOMPRESS_SETTING = 1;

    /**
     * The name or TTH of the file to download.
     */
    public String file;
    /**
     * The OutputStream to which the downloaded file is to be written.
     */
    public OutputStream os;
    /**
     * The InputStream from which the file to be uploaded is to be read.
     */
    public InputStream in;
    /**
     * The starting byte offset of the file from which to start download. Usually this will be 0.
     */
    public long start;
    /**
     * The total number of bytes starting from <i>start</i> of the file to download. This will be -1 for file lists.
     */
    public long len;

    public int fileType = FILE_TYPE;

    /**
     * It is used to set some flags like, if you donot want the downloaded file list to be automatically decompressed by
     * the framework then set it to NO_AUTO_FILELIST_DECOMPRESS_SETTING. Multiple settings should be ORed together. Better use
     * {@link  #setSetting(int) setSetting} to set the setting and {@link #unsetSetting(int) unsetSetting } to unset
     * an already set setting.
     */
    public int settingFlags = 0;

    public DUEntity() {}

    public DUEntity(int fileType, String file2Download, int Start, int Len, OutputStream OS, int settings) {
	this.fileType = fileType;
	file = file2Download;
	start = Start;
	len = Len;
	os = OS;
	settingFlags = settings;
    }

    public DUEntity(int fileType, OutputStream OS, int settings) {
	this.fileType = fileType;
	os = OS;
	settingFlags = settings;
    }

    public DUEntity(int fileType, String file2Download, int Start, int Len, InputStream IN, int settings) {
	this.fileType = fileType;
	file = file2Download;
	start = Start;
	len = Len;
	in = IN;
	settingFlags = settings;
    }

    public DUEntity(int fileType, InputStream IN) {
	this.fileType = fileType;
	in = IN;
    }

    public DUEntity(DUEntity due) {
	this.file = due.file;
	this.fileType = due.fileType;
	this.start = due.start;
	this.len = due.len;
	this.in = due.in;
	this.os = due.os;
	this.settingFlags = due.settingFlags;
    }

    public DUEntity getDummyCopy() {
	DUEntity due = new DUEntity(this);
	due.in = null;
	due.os = null;
	return due;
    }

    public String getFileType() {
	String ftype = "file";
	if (fileType == TTHL_TYPE)
	    ftype = "tthl";
	return ftype;
    }

    public void setSetting(int flag) {
	settingFlags = settingFlags | flag;
    }

    public void unsetSetting(int flag) {
	settingFlags = settingFlags & ~flag;
    }

    public boolean isSettingSet(int flag) {
	return (settingFlags & flag) == 1;
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

}
