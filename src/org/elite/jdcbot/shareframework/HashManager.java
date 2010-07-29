/*
 * HashManager.java
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
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import jonelo.jacksum.JacksumAPI;
import jonelo.jacksum.algorithm.AbstractChecksum;

/**
 * Created on 03-Jun-08<br>
 * Generates TTH.
 *
 * @author AppleGrew
 * @since 0.7.2
 * @version 0.1.2
 */
public class HashManager implements Runnable {
    private AbstractChecksum checksum = null;
    private Thread th = null;
    private HashUser hu = null;
    private File _f[] = null;
    private boolean cancel = false;

    private void init() {
	try {
	    checksum = JacksumAPI.getChecksumInstance("tree:tiger");
	    checksum.setEncoding(AbstractChecksum.BASE32);
	} catch (NoSuchAlgorithmException e) {
	    e.printStackTrace();
	}
    }

    public void hash(Collection<File> filesOrDirs, HashUser hashUser) throws HashException {
	File fORd[] = filesOrDirs.toArray(new File[0]);
	hash(fORd, hashUser);
    }

    public void hash(File filesOrDirs[], HashUser hashUser) throws HashException {
	if (checksum == null)
	    init();
	if (th != null)
	    throw new HashException(HashException.Error.HASHING_IN_PROGRESS);

	cancel = false;

	hu = hashUser;
	_f = filesOrDirs;

	th = new Thread(this, "Hashing Thread");
	th.start();

    }

    /**
     * Cancelling the hash job may not
     * terminate hashing instantly since we
     * cannot simply kill the thread.
     * @return
     */
    public boolean isHashingStillRunning() {
	return th != null;
    }

    public void cancelHashing() {
	if (th != null) {
	    cancel = true;
	    th.interrupt();
	}
    }

    public String getHash(InputStream in) throws HashException {
	if (checksum == null)
	    init();
	checksum.reset();
	int c;
	try {
	    while ((c = in.read()) != -1) {
		checksum.update((byte) c);
	    }
	    return checksum.format("#CHECKSUM");
	} catch (IOException e) {
	    throw new HashException("IOException: " + e.getMessage(), HashException.Error.HASHING_FAILED);
	}
    }

    public String getHash(String string) {
	if (checksum == null)
	    init();
	checksum.reset();
	checksum.update(string.getBytes());
	return checksum.format("#CHECKSUM");
    }

    private void hashFile(File f) {
	if (hu.canHash(f)) {
	    InputStream in = hu.getInputStream(f);
	    String hash = null;
	    try {
		hu.hashingOfFileStarting(f);
		if (in == null)
		    throw new HashException(HashException.Error.HASHING_CANCELLED);
		hash = getHash(in);
		hu.onFileHashed(f, hash, true, null);
	    } catch (HashException e) {
		e.printStackTrace();
		hu.onFileHashed(f, hash, false, e);
	    }
	}
    }

    private void hashDir(File dir) {
	File files[] = dir.listFiles(new FileFilter() {

	    public boolean accept(File f) {
		if (f.isFile())
		    return true;
		else
		    return false;
	    }
	});
	if (files != null) {
	    for (File f : files) {
		if (cancel)
		    return;
		hashFile(f);
	    }
	}

	File subdirs[] = dir.listFiles(new FileFilter() {

	    public boolean accept(File dir) {
		if (dir.isDirectory())
		    return true;
		else
		    return false;
	    }
	});
	if (subdirs != null)
	    hashDirs(subdirs);
    }

    private void hashDirs(File dirs[]) {
	for (File d : dirs) {
	    if (cancel)
		return;
	    hashDir(d);
	}
    }

    public void run() {
	for (File f : _f) {
	    if (cancel)
		break;
	    if (f.isFile())
		hashFile(f);
	    else if (f.isDirectory())
		hashDir(f);
	}
	th = null;
	hu.onHashingJobFinished();
    }
}
