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
 * @version 0.1
 * 
 */
public class DUEntity {
    public static final int FILE_TYPE = 0;
    public static final int FILELIST_TYPE = 1;
    public static final int TTHL_TYPE = 2;

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
    public int start;
    /**
     * The total number of bytes starting from <i>start</i> of the file to download. This will be -1 for file lists.
     */
    public int len;

    public int fileType = FILE_TYPE;

    public DUEntity() {}

    public DUEntity(int fileType, String file2Download, int Start, int Len, OutputStream OS) {
	this.fileType = fileType;
	file = file2Download;
	start = Start;
	len = Len;
	os = OS;
    }

    public DUEntity(int fileType, OutputStream OS) {
	this.fileType = fileType;
	os = OS;
    }

    public DUEntity(int fileType, String file2Download, int Start, int Len, InputStream IN) {
	this.fileType = fileType;
	file = file2Download;
	start = Start;
	len = Len;
	in = IN;
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

}
