/*
 * BotConfig.java
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

import org.elite.jdcbot.util.GlobalFunctions;

/**
 * Created on 3-Aug-10<br>
 * A simple POJO to capture bot
 * settings. Passing settings as arguments
 * to constructor requires that we look at the javadoc
 * of the class to determine the position of an
 * argument. That is very cumbersome and error
 * prone.
 * 
 * @author AppleGrew
 * @since 1.1.3
 * @version 1.0
 */
public class BotConfig {
	private String botname = "jDCBot";
	private String botIP;
	private int listenPort = 9000;
	private int UDP_listenPort = 10000;
	private String password = "";
	private String description = "";
	private String conn_type = "LAN(T1)" + User.NORMAL_FLAG;
	private	String email = "";
	private String sharesize = "0";
	private int uploadSlots = 1;
	private int downloadSlots = 3;
	private boolean passive = false;
	
	public String getBotname() {
		return botname;
	}
	public void setBotname(String botname) throws BotException {
		if(!GlobalFunctions.isUserNameValid(botname)) {
			throw new BotException(BotException.Error.INVALID_USERNAME);
		}
		this.botname = botname;
	}
	public String getBotIP() {
		return botIP;
	}
	public void setBotIP(String botIP) {
		this.botIP = botIP;
	}
	public int getListenPort() {
		return listenPort;
	}
	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}
	public int getUDP_listenPort() {
		return UDP_listenPort;
	}
	public void setUDP_listenPort(int uDPListenPort) {
		UDP_listenPort = uDPListenPort;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getConn_type() {
		return conn_type;
	}
	public void setConn_type(String connType) {
		conn_type = connType;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getSharesize() {
		return sharesize;
	}
	public void setSharesize(String sharesize) {
		this.sharesize = sharesize;
	}
	public int getUploadSlots() {
		return uploadSlots;
	}
	public void setUploadSlots(int uploadSlots) {
		this.uploadSlots = uploadSlots;
	}
	public int getDownloadSlots() {
		return downloadSlots;
	}
	public void setDownloadSlots(int downloadSlots) {
		this.downloadSlots = downloadSlots;
	}
	public boolean isPassive() {
		return passive;
	}
	public void setPassive(boolean passive) {
		this.passive = passive;
	}
	
}
