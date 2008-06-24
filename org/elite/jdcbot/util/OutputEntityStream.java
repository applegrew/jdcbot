/*
 * OutputEntityStream.java
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
import java.io.OutputStream;

/**
 * Created on 02-Jun-08<br>
 * An OutputStream which allows you to monitor download progress and
 * transfer rate. It allows you to set an upper cap on the maximum
 * allowed download rate. See {@link org.elite.jdcbot.examples.DownloadBot2 DownloadBot2} for example.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1.1
 */
public class OutputEntityStream extends OutputStream {
    private final int updateInterval = 1000; //After this interval is over the meter and constrainer are invoked.

    private int updateCounter;
    private Meter meter;
    private Constrainer constrainer;
    private OutputStream out;

    public OutputEntityStream(OutputStream out) {
	this(out, 0);
    }

    public OutputEntityStream(OutputStream out, long total) {
	super();
	if (total == 0)
	    meter = new Meter();
	else
	    meter = new Meter(total);
	constrainer = new Constrainer();
	this.out = out;
	updateCounter = updateInterval;
    }

    public OutputEntityStream(OutputStream out, long total, long transferLimit) {
	this(out, total);
	setTransferLimit(transferLimit);
    }

    public void setOutputStream(OutputStream out) {
	this.out = out;
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
    public void write(int b) throws IOException {
	out.write(b);

	updateCounter--;
	if (updateCounter <= 0) {
	    updateCounter = updateInterval;
	    meter.signalProgress(updateInterval);
	    constrainer.constrain(meter.getRate(), meter.getTotalProgress());
	}
    }

    @Override
    public void write(byte b[]) throws IOException {
	if (b == null)
	    throw new NullPointerException("b is null");
	write(b, 0, b.length);
    }

    @Override
    public void write(byte b[], int offset, int len) throws IOException {
	if (b == null)
	    throw new NullPointerException("b is null");
	if (offset < 0 || offset + len > b.length)
	    throw new IndexOutOfBoundsException("offset=" + offset + ", and offset+len=" + offset + len);

	out.write(b, offset, len);

	updateCounter--;
	if (updateCounter <= 0) {
	    updateCounter = updateInterval;
	    meter.signalProgress(len + updateInterval);
	    constrainer.constrain(meter.getRate(), meter.getTotalProgress());
	}
    }

    @Override
    public void close() throws IOException {
	super.close();
	out.close();
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
    public double getTimeRemaining() {
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
