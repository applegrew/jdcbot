/*
 * Constrainer.java
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
 * Its job is to constrain/limit the rate to a particular set value.
 *
 * @author AppleGrew
 * @since 0.7.2
 * @version 0.1.1
 */
public abstract class RateConstrainer {
    private double targetConstrain = -1;

    /**
     * Sets the target rate. To disable contrain set this to &lt;=0, or
     * better call {@link #revokeConstrain()}.
     * @param targetRate The target rate in units per <b>second</b>.
     */
    public void setTargetConstrainValue(double targetRate) {
	targetConstrain = targetRate;
    }

    /**
     * Disables constrain on rate.
     *
     */
    public void revokeConstrain() {
	targetConstrain = -1;
    }

    /**
     * Will make the thread sleep to bring current rate near target rate. 
     * @param currentRate Current rate in units per <b>second</b>.
     * @param currentTotalProgress Total amount of units.
     */
    public void constrain(double currentRate, long currentTotalProgress) {
	if (targetConstrain <= 0 || currentRate <= 0)
	    return;

	/* 
	 * ===================================================================
	 * Below is a note about how I deduced the formula to calculate the
	 * period of time to sleep so that rate can be constrained to a value.
	 * ===================================================================
	 * 
	 * Let a = currentTotalProgress,
	 * r = currentRate, and
	 * R = targetConstrain.
	 * 
	 * Now total duration over which r has been calculated (t) = a/r ...(1)
	 * 
	 * If r > R the we must sleep to make r decrease to R.
	 * 
	 * When we wake from sleep 'a' will still be the same but time would have increased, hence the final rate
	 * then would be (r*) = a/(t+x), where x is the period of time in seconds we slept.
	 * But we want r* to be R
	 * => R = a/(t+x)
	 * => R = a/( (a/r) + x) ...from (1)
	 * => x = (a/R) - (a/r) seconds
	 * => x = ( (a/R) - (a/r) ) * 1000 ms
	 */
	long sleepTime =
		currentRate > targetConstrain ? (long) ((currentTotalProgress / targetConstrain - currentTotalProgress / currentRate) * 1000)
			: 0;
	if (sleepTime >= 1) {
	    try {
		Thread.sleep(sleepTime);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }
}
