/*
 * Hub.java
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

/**
 * Created on 19-Jun-08<br>
 * This is allows you to specify
 * to MultiHubsAdapter custom settings
 * for any specific hub, as is provided by
 * DC++, but it is much much more flexible
 * as it allows you to have different
 * description, email address and connection type.
 * <p>
 * All feilds are optional except <i>hubHostname</i>
 * and <i>hubPort</i>.
 * 
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class Hub {
    public String username = null;
    public String password = "";
    public boolean isPassive = false;
    public String description = "";
    public String conn_type = "LAN(T1)" + User.NORMAL_FLAG;
    public String email = "";

    public String hubHostname;
    public int hubPort;

    public Hub(String hubHostname, int hubPort) {
	this.hubHostname = hubHostname;
	this.hubPort = hubPort;
    }

    public String getHubSignature() {
	return prepareHubSignature(hubHostname, hubPort);
    }

    /**
     * You should use this method to create hub signatures.
     * @param hubHostname
     * @param hubPort
     * @return
     */
    public static String prepareHubSignature(String hubHostname, int hubPort) {
	return hubHostname + ":" + String.valueOf(hubPort);
    }
}
