/*
 * EventjDCBotListener.java
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

import org.elite.jdcbot.shareframework.SearchResultSet;
import org.elite.jdcbot.shareframework.SearchSet;

/**
 * Created on 20-Jun-08<br>
 * Classes that want to listen for events generated
 * by EventjDCBot should implement this.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.2.0
 * @see EventjDCBotListenerAdapter
 * @see EventjDCBotAdapter
 */
public interface EventjDCBotListener {
    /**
     * Called when receiving a search result from any user or the hub (in case you are passive).
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param senderNick The user's nick who returned the result.
     * @param senderIP This can be null if search response is received
     * from the hub, i.e. you are passive.
     * @param senderPort This is zero when you are passive.
     * @param result The search response.
     * @param free_slots The number of free slots <i>senderNick</i> user has.
     * @param total_slots The total number of upload slots <i>senderNick</i> user has.
     * @param hubName This is empty when TTH in <i>result</i> is set.
     */
    public abstract void on_SearchResult(jDCBot src, String senderNick, String senderIP, int senderPort, SearchResultSet result,
	    int free_slots, int total_slots, String hubName);

    /**
     * Called upon successfully connecting to hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     */
    public abstract void on_Connect(jDCBot src);

    /**
     * Called just when a new connection has been established with another client in Active mode. 
     *
     */
    public abstract void on_Connect2Client(jDCBot src);

    /**
     * It is called when the bot quits. Just after it quits, as a side-effect of closing the socket, the onDisconnect() too is called.
     * 
     */
    public abstract void on_BotQuit(jDCBot src);

    /**
     * Called upon disconnecting from hub.
     * 
     * @since 1.0 The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     */
    public abstract void on_Disconnect(jDCBot src);

    /**
     * Called when public message is received.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param user
     *                User who sent message.
     * @param message
     *                Contents of the message.
     */
    public abstract void on_PublicMessage(jDCBot src, String user, String message);

    /**
     * Called when user enter the hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param user
     *                Name of the user who entered hub.
     */
    public abstract void on_Join(jDCBot src, String user);

    /**
     * Called when user quits hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param user
     *                of user quited hub.
     */
    public abstract void on_Quit(jDCBot src, String user);

    /**
     * Called when some new info about the user is found. Like his IP, Passive/Active state, etc.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.<br>
     * <b>Note:</b> This method is called by <b>User</b> and <b>UserManager</b> using <i>jDCBot-EventDispatchThread</i> thread.
     * @param user
     *                The user from the hub.
     */
    public abstract void on_UpdateMyInfo(jDCBot src, String user);

    /**
     * Called when bot receives private message.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param user
     *                Name of user who sent us private message.
     * @param message
     *                Contents of private message.
     */
    public abstract void on_PrivateMessage(jDCBot src, String user, String message);

    /**
     * Called when channel message in channel where bot is present is received.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param user
     *                Name of the user who sent message.
     * @param channel
     *                Channel on which message is sent.
     * @param message
     *                Contents of the channel message.
     */
    public abstract void on_ChannelMessage(jDCBot src, String user, String channel, String message);

    /**
     * Called when user in passive mode is searching for something.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * @param user The passive user who made the search.
     * @param search Contains all the details about the search made.
     */
    public abstract void on_PassiveSearch(jDCBot src, String user, SearchSet search);

    /**
     * Called when user in active mode is searching for something.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * @param ip The IP of the user who made the search.
     * @param port The port to which the search result should be sent.
     * @param search Contains all the details about the search made.
     */
    public abstract void on_ActiveSearch(jDCBot src, String ip, int port, SearchSet search);

    /**
     * Called when download is complete.<br>
     * <b>Note:</b> This method is called by <b>DownloadHandler</b> using <i>jDCBot-EventDispatchThread</i> thread.
     * @param user The user from whom the file was downloaded.
     * @param due The informations about the file downloaded is in this.
     * @param success It is true if download was successful else false.
     * @param e The exception that occurred when success is false else it is null.
     */
    public abstract void on_DownloadComplete(jDCBot src, User user, DUEntity due, boolean success, BotException e);

    /**
     * 
     * @param user
     * @param due
     */
    public abstract void on_DownloadStart(jDCBot src, User user, DUEntity due);

    /**
     * Called when upload is complete.<br>
     * <b>Note:</b> This method is called by <b>DownloadHandler</b> using <i>jDCBot-EventDispatchThread</i> thread.
     * @param user The user to whom the file was uploaded.
     * @param due The informations about the file uploaded is in this.
     * @param success It is true if upload was successful else false.
     * @param e The exception that occured when sucess is false else it is null.
     */
    public abstract void on_UploadComplete(jDCBot src, User user, DUEntity due, boolean success, BotException e);

    /**
     * Called when upload is starting.<br>
     * <b>Note:</b> This method is called by <b>DownloadHandler</b> using <i>jDCBot-EventDispatchThread</i> thread.
     * @param user The user to whom the file is being uploaded.
     * @param due The informations about the file downloaded is in this.
     */
    public abstract void on_UploadStart(jDCBot src, User user, DUEntity due);
    
	/**
	 * Called when async call to communicate with remote system fails.
	 * The methods which may result in the invocation of this will
	 * mention this in its comment.
	 * @param msg
	 * @param exception
	 * @param srcMethod The source method
	 */
    public abstract void on_SendCommandFailed(jDCBot src, String msg, Throwable e, JMethod srcMethod);
    
    /**
	 * Called when hub announces its name. Note
	 * that for different users the same hub may
	 * send different hub name, also the hub name
	 * may change several times during a single
	 * session, so do not use this to uniquely
	 * identify a hub. Instead use {@link User#getHubSignature()}. 
	 * @param hubName Hub's name.
	 */
	public abstract void on_HubName(jDCBot src, String hubName);
}
