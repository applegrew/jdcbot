/*
 * UploadStreamManager.java
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.elite.jdcbot.framework.User;
import org.elite.jdcbot.util.InputEntityStream;

/**
 * Created on 14-Jun-08<br>
 * This is meant to be used by users to control the
 * uploads like their transfer speeds, cancelling them, etc.,
 * and also collecting various stats related to uploads.
 * <p>
 * This class is thread safe.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1 
 */
public class UploadStreamManager {
    private Map<User, UploadInputStream> uploadStreams;
    private Map<User, Double> customRates;
    private double transferRate = 0;

    public UploadStreamManager() {
	uploadStreams = Collections.synchronizedMap(new HashMap<User, UploadInputStream>());
	customRates = Collections.synchronizedMap(new HashMap<User, Double>());
    }

    InputEntityStream getInputEntityStream(User u, InputStream stream) {
	UploadInputStream uis = new UploadInputStream(stream);
	Double rate = customRates.get(u);
	if (rate != null)
	    uis.setTransferLimit(rate);
	else
	    uis.setTransferLimit(transferRate);
	uploadStreams.put(u, uis);
	return uis;
    }

    /**
     * Sets the maximum rate at which upload is allowed.
     * If rate is &lt;=0 then transfer limit is revoked.
     * @param rate It should be in bytes per second.
     */
    public void setUploadTransferLimit(double rate) {
	transferRate = rate;
	synchronized (uploadStreams) {
	    Collection<UploadInputStream> uis = uploadStreams.values();
	    for (UploadInputStream is : uis)
		is.setTransferLimit(rate);
	}
    }

    /**
     * Allows you to set maximum upload rate for the
     * currently running upload to a particular User
     * (if she exists).
     * @param u The User whose currently running transfer need to be
     * limited.
     * @param rate The rate in bytes per second (set this to
     * &lt;=0 to disable upload transfer limit).
     */
    public void setUploadTransferLimit(User u, double rate) {
	UploadInputStream uis = uploadStreams.get(u);
	if (uis != null)
	    uis.setTransferLimit(rate);
	if (rate > 0)
	    customRates.put(u, rate);
	else
	    customRates.remove(u);
    }

    /**
     * This is here just for your convinience. It
     * does nothing else other than calling
     * {@link org.elite.jdcbot.framework.User#cancelUpload()}.
     * You can very call that instead.
     * @param u The user's to whom upload
     * should be cancelled.
     */
    public void cancelUpload(User u) {
	u.cancelUpload();
    }

    /**
     * 
     * @param u
     * @return Upload speed in bytes/second.
     */
    public double getUploadSpeed(User u) {
	synchronized (uploadStreams) {
	    if (!uploadStreams.containsKey(u))
		return 0;
	    else
		return uploadStreams.get(u).getTransferRate();
	}
    }

    public double getUploadProgress(User u) {
	synchronized (uploadStreams) {
	    if (!uploadStreams.containsKey(u))
		return 0;
	    else
		return uploadStreams.get(u).getPercentageCompletion();
	}
    }

    /**
     * @since 1.0
     * @return The time remaining for the
     * completion of upload in seconds.
     */
    public double getTimeRemaining(User u) {
	synchronized (uploadStreams) {
	    if (!uploadStreams.containsKey(u))
		return -1;
	    else
		return uploadStreams.get(u).getTimeRemaining();
	}
    }

    private class UploadInputStream extends InputEntityStream {
	public UploadInputStream(InputStream in) {
	    super(in);
	}

	@Override
	public void close() throws IOException {
	    super.close();
	    Set<User> users = uploadStreams.keySet();
	    synchronized (uploadStreams) {
		for (User u : users)
		    if (uploadStreams.get(u).equals(this))
			uploadStreams.remove(u);
	    }
	}
    }
}
