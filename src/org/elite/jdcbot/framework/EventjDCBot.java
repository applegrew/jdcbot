/*
 * EventjDCBot.java
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.elite.jdcbot.shareframework.SearchResultSet;
import org.elite.jdcbot.shareframework.SearchSet;

/**
 * Created on 20-Jun-08<br>
 * jDCBot class doesn't support event-listener
 * kind-of model at all. The sub-class of jDCBot
 * is the only one which can come to know about the
 * various events occurring in jDCBot.
 * <p>
 * For cases like when we need different objects to tap
 * the events of an instance of jDCBot, or, when many
 * instances of jDCBot need to notify a single instance of
 * a class of the events, the jDCBot's model is not much
 * suited.
 * <p>
 * To cater to such situation this class extends jDCBot and
 * it acts as beacon of events. Classes that implement
 * EventjDCBotListener can register themselves as listeners
 * in this class. Whenever an event occurs then jDCBot will
 * notify this class and this class will beacon that to all
 * the listeners.
 * <p>
 * To use this (event-listener) model you will need to extend
 * this class instead of jDCBot. The sub-class of this class
 * won't be able to override the event methods as is used to
 * be done in case of jDCBot, you will need to implement
 * EventjDCBotListener and register as listener here.
 * <p>
 * This class is thread safe.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.2.0
 * @see EventjDCBotAdapter
 */
abstract public class EventjDCBot extends jDCBot {
    private List<EventjDCBotListener> _listeners = Collections.synchronizedList(new ArrayList<EventjDCBotListener>());

    //******Constructors******/
    /**
     * Constructs a EventjDCBot with your settings.
     * <p>
     * Most setting here depends on your hub. You might have to fake your share size and/or slots for hub to accept you... For details, look at <a
     * href="http://www.teamfair.info/wiki/index.php?title=%24MyINFO">DC protocol wiki page of $MyINFO command</a>
     * 
     * 
     * @param botname Name of the bot as it will appear in the list of users.
     * @param botIP Your IP.
     * @param listenPort The port on your computer where jdcbot should listen for incoming connections from clients.
     * @param password Password if required, you could put anything if no password is needed.
     * @param description Description of your bot as it will appear in the list of users. On your description is appended standard description.
     * @param conn_type Your connection type, for details look <a href="http://www.teamfair.info/wiki/index.php?title=%24MyINFO">here</a>.
     * <b>Note</b> that this setting is just a mere imitation. It will not actually limit upload speed.
     * See {@link org.elite.jdcbot.shareframework.ShareManager#getUploadStreamManager() getUploadStreamManager()} for that.
     * @param email Your e-mail address as it will appear in the list of users.
     * @param sharesize Size of your share in bytes.
     * @param uploadSlots Number of upload slots for other user to connect to you.
     * @param downloadSlots Number of download slots. This has nothing to do with DC++ protocol. This has been given
     * to put an upper cap on no. of simultaneous downloads.
     * @param passive Set this to false if you are not behind a firewall.
     * @param outputLog <u>Almost</u> all debug messages will be printed in this.
     * @throws IOException 
     * @throws BotException 
     */
    public EventjDCBot(String botname, String botIP, int listenPort, int UDP_listenPort, String password, String description,
	    String conn_type, String email, String sharesize, int uploadSlots, int downloadSlots, boolean passive)
	    throws IOException, BotException {

	super(botname, botIP, listenPort, UDP_listenPort, password, description, conn_type, email, sharesize, uploadSlots, downloadSlots,
		passive);
    }

    /**
     * Constructs a EventjDCBot with the default settings. Your own constructors in classes which extend the EventjDCBot abstract class should be
     * responsible for changing the default settings if required.
     * @throws IOException 
     * @throws BotException 
     */
    public EventjDCBot(String botIP) throws IOException, BotException {
	super(botIP);
    }

    /**
     * Creates a new EventjDCBot instance which can coexist with other EventjDCBot instances, all
     * sharing the sharable resources like the server sockets, etc.
     * @param multiHubsAdapter An instance of MultiHubsAdapter.
     * @throws IOException 
     * @throws BotException 
     */
    public EventjDCBot(MultiHubsAdapter multiHubsAdapter) throws IOException, BotException {
	super(multiHubsAdapter);
    }

    //******Listeners handlers******/
    final public void addListener(EventjDCBotListener listener) {
	synchronized (_listeners) {
	    if (!_listeners.contains(listener))
		_listeners.add(listener);
	}
    }

    final public void removeListener(EventjDCBotListener listener) {
	_listeners.remove(listener);
    }

    //******Events******/
    @Override
    final protected void onSearchResult(String senderNick, String senderIP, int senderPort, SearchResultSet result, int free_slots,
	    int total_slots, String hubName) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_SearchResult(this, senderNick, senderIP, senderPort, result, free_slots, total_slots, hubName);
	}
    }

    @Override
    final protected void onConnect() {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_Connect(this);
	}
    }

    @Override
    final protected void onConnect2Client() {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_Connect2Client(this);
	}
    }

    @Override
    final protected void onBotQuit() {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_BotQuit(this);
	}
    }

    @Override
    final protected void onDisconnect() {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_Connect(this);
	}
    }

    @Override
    final protected void onPublicMessage(String user, String message) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_PublicMessage(this, user, message);
	}
    }

    @Override
    final protected void onJoin(String user) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_Join(this, user);
	}
    }

    @Override
    final protected void onQuit(String user) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_Quit(this, user);
	}
    }

    @Override
    final protected void onUpdateMyInfo(String user) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_UpdateMyInfo(this, user);
	}
    }

    @Override
    final protected void onPrivateMessage(String user, String message) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_PrivateMessage(this, user, message);
	}
    }

    @Override
    final protected void onChannelMessage(String user, String channel, String message) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_ChannelMessage(this, user, channel, message);
	}
    }

    @Override
    final protected void onPassiveSearch(String user, SearchSet search) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_PassiveSearch(this, user, search);
	}
    }

    @Override
    final protected void onActiveSearch(String ip, int port, SearchSet search) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_ActiveSearch(this, ip, port, search);
	}
    }

    @Override
    final protected void onDownloadComplete(User user, DUEntity due, boolean success, BotException e) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_DownloadComplete(this, user, due, success, e);
	}
    }

    @Override
    final protected void onDownloadStart(User user, DUEntity due) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_DownloadStart(this, user, due);
	}
    }

    @Override
    final protected void onUploadComplete(User user, DUEntity due, boolean success, BotException e) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_UploadComplete(this, user, due, success, e);
	}
    }

    @Override
    final protected void onUploadStart(User user, DUEntity due) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_UploadStart(this, user, due);
	}
    }
    
    @Override
    final protected void onHubName(String hubName) {
	synchronized (_listeners) {
	    for (EventjDCBotListener l : _listeners)
		l.on_HubName(this, hubName);
	}
    }
}
