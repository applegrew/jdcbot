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

import java.io.FileNotFoundException;

import org.elite.jdcbot.framework.DUEntity;

/**
 * Created on 26-May-08<br>
 * Its purpose is to manager the user shared files, i.e. hashing them, creating/updating file list, etc.<br>
 * TODO: Writing code for this is deferred for later time. 
 * 
 * @author AppleGrew
 * @since 0.7
 * @version 0.1
 */
public class ShareManager {

    public DUEntity getFile(String file, int fileType, long start, long fileLen) throws FileNotFoundException {
	// TODO Auto-generated method stub
	throw new FileNotFoundException(file + " not found.");
	//return null;
    }

    public DUEntity getFileList() throws FileNotFoundException {
	DUEntity due = new DUEntity();
	due.file = "";
	due.fileType = DUEntity.FILELIST_TYPE;
	due.start = 0;
	due.len = 0; //TODO
	due.in = null; //TODO
	throw new FileNotFoundException("User file list not yet available.");
	//return due;
    }

}
