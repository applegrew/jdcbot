/*
 * GlobalFunctions.java
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

import java.util.regex.Pattern;

/**
 * Created on 11-Jun-08<br>
 * This are common functions that is used by many classes of framework
 * and example bots, and are ideal to be used by classes that will use jDCBot. 
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1.1
 */
public class GlobalFunctions {
	private static final Pattern validUserName = Pattern.compile("^[a-zA-Z0-9_-]+$");
	public static final String EMPTY = "";

    /**
     * Searches for a match in <i>what</i>.
     * @param with The array of keywords (each keywords being in different array index)
     * to search for.
     * @param what Inside which to search for.
     * @param useORconnector If true then keywords are assumed to hold
     * OR relation else AND relation is assumed.<br>
     * e.g.<br>
     * <i>Apple Orange</i> will match <i>Apple</i> OR <i>Pine</i>,
     * but not with <i>Apple</i> AND <i>Pine</i>.
     * @return Returns true if <i>what</i> matches with <i>with</i>.
     */
    public static boolean matches(String with[], String what, boolean useORconnector) {
	what = what.trim().toLowerCase();
	int ec = 0;
	for (String w : with) {
	    w = w.trim().toLowerCase();
	    if (w.isEmpty()) {
		ec++;
		continue;
	    }
	    if (useORconnector) {
		if (what.contains(w))
		    return true;
	    } else {
		if (!what.contains(w))
		    return false;
	    }
	}
	if (ec == with.length)
	    return true;
	return false;
    }

    public static boolean matches(String with[], String what) {
	return matches(with, what, true);
    }

    /**
     * @param what The one to search into.
     * @param ext The array of extensions to match with. <b>Note:</b> Donot
     * put dots as prefix for the extension, i.e. <i>txt</i> is correct, but
     * <i><b>.</b>txt</i> is incorrect.
     * @return Returns true any of the extensions in<i>ext</i> match with<i>what</i>.
     */
    public static boolean hasExt(String what, String ext[]) {
	what = what.trim().toLowerCase();
	for (String w : ext) {
	    w = w.trim().toLowerCase();
	    if (w.isEmpty())
		continue;
	    if (what.endsWith("." + w))
		return true;
	}
	return false;
    }

    /**
     * @return Returns true if the current operating system on
     * which this program is running is Windows.
     */
    public static boolean isWindowsOS() {
	return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    /**
     * Returns String representation of a double value trimmed to
     * the number of decimals required.<br>
     * e.g. 23.34565678766 trimmed to 2 decimals will
     * look like<br>
     * 23.34
     * @param no The number to trim.
     * @param decimalCount The number of decimals to retain.
     * @return The trimmed number. The number is returned as
     * it is if the input number has no decimals or
     * <i>decimalCount</i> is negative.
     */
    public static String trimDecimals(double no, int decimalCount) {
	String sno = String.valueOf(no);
	int dp = sno.indexOf('.');
	if (dp == -1 || decimalCount < 0 || (dp + decimalCount + 1) > sno.length())
	    return sno;
	else {
	    return sno.substring(0, decimalCount == 0 ? dp : dp + decimalCount + 1);
	}
    }
    
    /**
     * 
     * @param str
     * @return true is str is null or empty.
     */
    public static boolean isEmpty(String str) {
    	return str == null || str.equals(EMPTY);
    }
    
    /**
	 * Can be used to test if given username
	 * is valid or not.
	 * @param botname The username to test.
	 * @return
	 */
	public static boolean isUserNameValid(String botname) {
		if(GlobalFunctions.isEmpty(botname)) {
			return false;
		}
		if(validUserName.matcher(botname).matches()) {
			return true;
		}
		return false;
	}
}
