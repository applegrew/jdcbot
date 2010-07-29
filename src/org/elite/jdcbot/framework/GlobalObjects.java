/*
 * GlobalObjects.java
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

import java.io.PrintStream;

/**
 * Created on 04-Jun-08<br>
 * Common global objects used by all classes.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class GlobalObjects {
    /**
     * Initialized by jDCBot.
     */
    public static PrintStream log = System.out;
    /**
     * The definitive version number of this release of jDCBot.
     */
    public static final String VERSION = "1.0";
    /**
     * The name of the client.
     */
    public static final String CLIENT_NAME = "jDCBot";
}
