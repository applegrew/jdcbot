/*
 * MultiHubsAdapter.java
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

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import org.elite.jdcbot.shareframework.SearchSet;
import org.elite.jdcbot.shareframework.ShareManager;

/**
 * Created on 06-Jun-08<br>
 * This allows you to connect to multiple
 * hubs. This will handle the intricacies of creation of
 * different jDCBot instances for handling a hub and
 * synchronizing them.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class MultiHubsAdapter implements UDPInputThreadTarget, BotInterface {
    private String _botname, _password;
    protected String _description, _conn_type, _email, _sharesize, _hubname;
    protected boolean _passive;
    protected int _udp_port;
    protected String _botIP;
    protected int _listenPort;

    protected int _maxUploadSlots;
    protected int _maxDownloadSlots;

    protected ServerSocket socketServer = null;
    protected DatagramSocket udpSocket = null;

    protected PrintStream log;

    protected ShareManager shareManager;
    private UDPInputThread _udp_inputThread = null;
    protected Vector<jDCBot> bots;
    protected ReentrantLock lock; //Used to synchronized some process like when initConnectToMe is called.

    /**
     * 
     * @param botname
     * @param botIP
     * @param listenPort
     * @param UDP_listenPort
     * @param password
     * @param description
     * @param conn_type
     * @param email
     * @param sharesize Set this to null if you want share size should be automatically calculated.
     * <b>Note:</b> This is computationally expensive.
     * @param uploadSlots
     * @param downloadSlots
     * @param passive
     * @param outputLog
     */
    public MultiHubsAdapter(String botname, String botIP, int listenPort, int UDP_listenPort, String password, String description,
	    String conn_type, String email, String sharesize, int uploadSlots, int downloadSlots, boolean passive, PrintStream outputLog) {
	_botname = botname;
	_password = password;
	// remove this and put
	// _description=description;
	// if you don't hub doesn't require standard description
	_description = description + "<++ V:0.668,M:" + (passive ? 'P' : 'A') + ",H:1/0/0,S:" + uploadSlots + ">";
	_conn_type = conn_type;
	_email = email;
	_sharesize = sharesize;
	_maxUploadSlots = uploadSlots;
	_maxDownloadSlots = downloadSlots;
	_botIP = botIP;
	_listenPort = listenPort;
	_passive = passive;
	log = outputLog;
	_udp_port = UDP_listenPort;

	GlobalObjects.log = log;
	lock = new ReentrantLock();
	bots = new Vector<jDCBot>();
	shareManager = new ShareManager();

	if (_sharesize == null)
	    _sharesize = String.valueOf(shareManager.getOwnShareSize(false));

	try {
	    socketServer = new ServerSocket(_listenPort);
	    socketServer.setSoTimeout(60000); // Wait for 60s before timing out.
	} catch (SocketException e) {
	    e.printStackTrace(log);
	} catch (IOException e) {
	    e.printStackTrace(log);
	}

	initiateUDPListening();
    }

    public Vector<jDCBot> getAllBots() {
	return new Vector<jDCBot>(bots);
    }

    public ShareManager getShareManager() {
	return shareManager;
    }

    public void terminate() {
	for (jDCBot bot : bots)
	    bot.terminate();
    }

    public void handleUDPCommand(String rawCommand, String ip, int port) {
	for (jDCBot bot : bots)
	    bot.handleUDPCommand(rawCommand, ip, port);
    }

    public void onUDPExceptionClose(IOException e) {
	_udp_inputThread = null;
	initiateUDPListening();
    }

    private void initiateUDPListening() {
	if (_udp_inputThread != null && !_udp_inputThread.isClosed())
	    return;

	try {
	    udpSocket = new DatagramSocket(_udp_port);
	} catch (SocketException e) {
	    log.println("Failed to listen for UDP packets.");
	    e.printStackTrace(log);
	}
	_udp_inputThread = new UDPInputThread(this, udpSocket);
	_udp_inputThread.start();
    }

    public void Search(SearchSet ss) throws IOException {
    // TODO Auto-generated method stub

    }

    public boolean UserExist(String user) {
	// TODO Auto-generated method stub
	return false;
    }

    public String getBotClientProtoSupports() {
	// TODO Auto-generated method stub
	return null;
    }

    public int getMaxDownloadSlots() {
	// TODO Auto-generated method stub
	return 0;
    }

    public User getUserByCID(String cid) {
	// TODO Auto-generated method stub
	return null;
    }

    public boolean isPassive() {
	// TODO Auto-generated method stub
	return false;
    }

    public String getBotName(jDCBot bot) {
	// TODO Based on bot return the customized name (if confiburede such).
	return _botname;
    }

    public String getPassword(jDCBot bot) {
	// TODO Based on bot return the customized name (if confiburede such).
	return _password;
    }
}
