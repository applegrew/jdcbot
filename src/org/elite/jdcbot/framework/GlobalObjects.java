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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 04-Jun-08<br>
 * Common global objects used by all classes.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1.5
 */
public class GlobalObjects {
	
	static {
		Properties props = new Properties();
		try {
			props.load(GlobalObjects.class.getResourceAsStream("/log4j.properties"));
			PropertyConfigurator.configure(props);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Global logger.
	 */
	public static final Logger getLogger(Class<?> c){
		return LoggerFactory.getLogger(c);
	}
    /**
     * The definitive version number of this release of jDCBot.
     */
    public static final String VERSION = "1.2.0";
    /**
     * The name of the client.
     */
    public static final String CLIENT_NAME = "jDCBot";
}
