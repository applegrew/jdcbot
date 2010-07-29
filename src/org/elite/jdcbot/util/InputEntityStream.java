/*
 * InputEntityStream.java
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
package org.elite.jdcbot.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created on 02-Jun-08<br>
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1.1
 */
public class InputEntityStream extends InputStream {
    private final int updateInterval = 1000; //After this interval is over the meter and constrainer are invoked.

    private int updateCounter;
    private Meter meter;
    private Constrainer constrainer;
    private InputStream in;

    public InputEntityStream(InputStream in) {
	this(in, 0);
    }

    public InputEntityStream(InputStream in, long total) {
	super();
	if (total == 0)
	    meter = new Meter();
	else
	    meter = new Meter(total);
	constrainer = new Constrainer();
	this.in = in;
	updateCounter = updateInterval;
    }

    public void setInputStream(InputStream in) {
	this.in = in;
    }

    public InputEntityStream(InputStream in, long total, long transferLimit) {
	this(in, total);
	setTransferLimit(transferLimit);
    }

    public void setTotalStreamLength(long total) {
	meter.setTotal(total);
    }

    /**
     * The progress till now will be lost, but
     * the original stream length and other values will be retained.
     * Use {@link #setTotalStreamLength(long) setTotalStreamLength}
     * to set stream length to new value.
     */
    public void resetProgress() {
	meter.reset();
    }

    /**
     * To disable limiting transfer rate set this to &lt;=0.
     * @param rate In bytes per second.
     */
    public void setTransferLimit(double rate) {
	constrainer.setTargetConstrainValue(rate);
    }

    public void revokeTransferLimit() {
	constrainer.revokeConstrain();
    }

    @Override
    public int read() throws IOException {
	int v = in.read();
	updateCounter--;
	if (updateCounter <= 0 && v != -1) {
	    updateCounter = updateInterval;
	    meter.signalProgress(updateInterval);
	    constrainer.constrain(meter.getRate(), meter.getTotalProgress());
	}
	return v;
    }

    @Override
    public int read(byte b[]) throws IOException {
	if (b == null)
	    throw new NullPointerException("b is null");
	return read(b, 0, b.length);
    }

    @Override
    public int read(byte b[], int offset, int len) throws IOException {
	if (b == null)
	    throw new NullPointerException("b is null");
	if (offset < 0 || offset + len > b.length)
	    throw new IndexOutOfBoundsException("offset=" + offset + ", and offset+len=" + offset + len);

	int cnt = in.read(b, offset, len);

	updateCounter--;
	if (updateCounter <= 0 && cnt != -1) {
	    updateCounter = updateInterval;
	    meter.signalProgress(cnt + updateInterval);
	    constrainer.constrain(meter.getRate(), meter.getTotalProgress());
	}
	return cnt;
    }

    @Override
    public void close() throws IOException {
	in.close();
	super.close();
    }

    public double getPercentageCompletion() {
	return meter.getPercentageCompletion();
    }

    /**
     * @return Transfer rate in bytes per second.
     */
    public double getTransferRate() {
	return meter.getRate();
    }
    
    /**
     * @since 1.0
     * @return The time remaining for the
     * completion of transfer in seconds.
     */
    public double getTimeRemaining(){
	return meter.getTimeRemaining();
    }

    private class Meter extends ProgressMeter {
	public Meter() {}

	public Meter(long total) {
	    super(total);
	}
    }

    private class Constrainer extends RateConstrainer {}

}
