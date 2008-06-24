/*
 * ProgressMeter.java
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

/**
 * Created on 02-Jun-08<br>
 * Its job is to keep record of progres and provide methods
 * calculate and give the transfer rate and % completion.
 *
 * @author AppleGrew
 * @since 0.7.2
 * @version 0.1
 */
public abstract class ProgressMeter {
    private long progress = 0;
    private boolean isPaused = false;
    private long total = 0;
    private long startTime = -1;

    public ProgressMeter() {}

    public ProgressMeter(long Total) {
	total = Total;
    }

    /**
     * Amount of progrees. This will usually be 1.
     * @param by
     */
    protected void signalProgress(int by) {
	if (!isPaused) {
	    progress += by;
	    if (startTime < 0)
		startTime = System.currentTimeMillis();
	}
    }

    /**
     * Signals progress by 1.
     *
     */
    protected void signalProgress() {
	signalProgress(1);
    }

    protected void reset() {
	progress = 0;
	startTime = -1;
	isPaused = false;
    }

    protected void pause() {
	isPaused = true;
    }

    protected void resume() {
	isPaused = false;
    }

    public long getTotalProgress() {
	return progress;
    }

    public void setTotal(long total) {
	this.total = total;
    }

    public double getPercentageCompletion() {
	if (total == 0)
	    return -1;

	return (progress / ((double) total)) * 100;
    }

    /**
     * @return The rate of transfer in units/second.
     */
    public double getRate() {
	long diff = System.currentTimeMillis() - startTime;
	if (diff < 1000)
	    return 1;
	else
	    return progress / (((double) (diff)) / 1000);
    }

    /**
     * @since 1.0
     * @return The time remaining for the
     * completion of task in seconds.
     */
    public double getTimeRemaining() {
	if (total == 0)
	    return -1;

	return (total - progress) / getRate();
    }

}
