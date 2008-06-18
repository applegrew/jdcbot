/*
 * HashUser.java
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
import java.io.InputStream;

/**
 * Created on 03-Jun-08<br>
 * All classes that intend to use {@link HashManager HashManager} must implement this
 * interface.
 *
 * @author AppleGrew
 * @since 0.7.2
 * @version 0.1.1
 */
public interface HashUser {
    /**
     * This should return an InputStream that streams
     * the file <i>f</i>. {@link HashManager HashManager} will use this
     * stream to read the file for hashing. This gives
     * the user of {@link HashManager HashManager} an oppurtunity to  wrap
     * the stream inside {@link org.elite.jdcbot.util.InputEntityStream InputEntityStream} and control
     * the transfer rate and monitor the progress of hashing.
     * @param f The file for which InputStream is needed. <b>It is <u>never</u> directory</b>.
     * @return InputStream to <i>f</i>.
     */
    public InputStream getInputStream(File f);

    /**
     * Called when a file finishes hashing.
     * @param f The file which has been hashed.<b>It is <u>never</u> directory</b>.
     * @param hash The hash of <i>f</i>. This is null if <i>success</i> is false.
     * @param success If there is any error during hashing then this is false else true.
     * @param e The exception that occured during hashing. This is null if
     * <i>success</i> is true.
     */
    public void onFileHashed(File f, String hash, boolean success, HashException e);

    /**
     * Helps HashManager to decide a file should be hashes or not. You may not
     * want to hash file based on wheather conditiond like (say) it is hidden or
     * the user have explicitly asked not to share such that file or simply that
     * it is already hashed.
     * @param f A file File, <b>it is <u>never</u> directory</b>.
     * @return Returns true if HashManager is allowed to hash this file else false.
     */
    public boolean canHash(File f);
    
    /**
     * Hashing of all files complete.
     *
     */
    public void onHashingJobFinished();
    
    /**
     * @param file The name of the file which
     * is now just beginning to getting hashed. 
     */
    public void hashingOfFileStarting(File file);

}
