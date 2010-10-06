/*
 * EventjDCBotAdapter.java
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

import org.elite.jdcbot.shareframework.SearchResultSet;
import org.elite.jdcbot.shareframework.SearchSet;

/**
 * Created on 20-Jun-08<br>
 * This is class exists just for
 * your convenience. It implements
 * all the methods of EventjDCBotListener
 * and extends EventjDCBot too. This
 * allows you to create a class that
 * can sub-class EventjDCBot and listen
 * for event generated by it too without
 * needing to implement all the methods of
 * EventjDCBotListener.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1.2
 */
abstract public class EventjDCBotAdapter extends EventjDCBot implements EventjDCBotListener {

    //******Constructors******/
    /**
     * Constructs a EventjDCBotAdapter with your settings.
     * <p>
     * Most setting here depends on your hub. You might have to fake your share size and/or slots for hub to accept you... For details, look at <a
     * href="http://www.teamfair.info/wiki/index.php?title=%24MyINFO">DC protocol wiki page of $MyINFO command</a>
     * 
     * 
     * @param botname Name of the bot as it will appear in the list of users.
     * @param botIP Your IP.
     * @param listenPort The port on your computer where jdcbot should listen for incoming connections from clients.
     * @param password Passsword if required, you could put anything if no password is needed.
     * @param description Description of your bot as it will appear in the list of users. On your description is appended standard description.
     * @param conn_type Your connection type, for details look <a href="http://www.teamfair.info/wiki/index.php?title=%24MyINFO">here</a>.
     * <b>Note</b> that this setting is just a mere imitation. It will not actually limit upload speed.
     * See {@link org.elite.jdcbot.shareframework.ShareManager#getUploadStreamManager() getUploadStreamManager()} for that.
     * @param email Your e-mail address as it will appear in the list of users.
     * @param sharesize Size of your share in bytes.
     * @param uploadSlots Number of upload slots for other user to connect to you.
     * @param downloadSlots Number of download slots. This has nothing to do with DC++ protocol. This has been given
     * to put an upper cap on no. of simultaneous downloads.
     * @param passive Set this to fals if you are not behind a firewall.
     * @param outputLog <u>Almost</u> all debug messages will be printed in this.
     * @throws IOException 
     */
    public EventjDCBotAdapter(String botname, String botIP, int listenPort, int UDP_listenPort, String password, String description,
	    String conn_type, String email, String sharesize, int uploadSlots, int downloadSlots, boolean passive)
	    throws IOException {

	super(botname, botIP, listenPort, UDP_listenPort, password, description, conn_type, email, sharesize, uploadSlots, downloadSlots,
		passive);
    }

    /**
     * Constructs a EventjDCBotAdapter with the default settings. Your own constructors
     * in classes which extend the EventjDCBotAdapter abstract class should be
     * responsible for changing the default settings if required.
     * @throws IOException 
     */
    public EventjDCBotAdapter(String botIP) throws IOException {
	super(botIP);
    }

    /**
     * Creates a new EventjDCBotAdapter instance which can co-exist with other EventjDCBotAdapter instances, all
     * sharing the shareable resources like the server sockets, etc.
     * @param multiHubsAdapter An instance of MultiHubsAdapter.
     * @throws IOException 
     */
    public EventjDCBotAdapter(MultiHubsAdapter multiHubsAdapter) throws IOException {
	super(multiHubsAdapter);
    }

    //******Events******/
    @Override
    public void on_ActiveSearch(jDCBot src, String ip, int port, SearchSet search) {}

    @Override
    public void on_BotQuit(jDCBot src) {}

    @Override
    public void on_ChannelMessage(jDCBot src, String user, String channel, String message) {}

    @Override
    public void on_Connect(jDCBot src) {}

    @Override
    public void on_Connect2Client(jDCBot src) {}

    @Override
    public void on_Disconnect(jDCBot src) {}

    @Override
    public void on_DownloadComplete(jDCBot src, User user, DUEntity due, boolean success, BotException e) {}

    @Override
    public void on_DownloadStart(jDCBot src, User user, DUEntity due) {}

    @Override
    public void on_Join(jDCBot src, String user) {}

    @Override
    public void on_PassiveSearch(jDCBot src, String user, SearchSet search) {}

    @Override
    public void on_PrivateMessage(jDCBot src, String user, String message) {}

    @Override
    public void on_PublicMessage(jDCBot src, String user, String message) {}

    @Override
    public void on_Quit(jDCBot src, String user) {}

    @Override
    public void on_SearchResult(jDCBot src, String senderNick, String senderIP, int senderPort, SearchResultSet result, int free_slots,
	    int total_slots, String hubName) {}

    @Override
    public void on_UpdateMyInfo(jDCBot src, String user) {}

    @Override
    public void on_UploadComplete(jDCBot src, User user, DUEntity due, boolean success, BotException e) {}

    @Override
    public void on_UploadStart(jDCBot src, User user, DUEntity due) {}
    
    @Override
    public void on_SendCommandFailed(String msg, Throwable e, JMethod src) {}
}
