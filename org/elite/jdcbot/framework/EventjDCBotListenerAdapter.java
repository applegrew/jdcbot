/*
 * EventjDCBotListenerAdapter.java
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
 * This abstract class implements
 * all methods of EventjDCBotListener,
 * allowing you to override only the required
 * methods.
 * 
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 * @see EventjDCBotAdapter
 */
abstract public class EventjDCBotListenerAdapter implements EventjDCBotListener {

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
}
