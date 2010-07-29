/*
 * BotInterface.java
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

import org.elite.jdcbot.shareframework.SearchSet;
import org.elite.jdcbot.shareframework.ShareManager;

/**
 * Created on 09-Jun-08
 *
 * @author AppleGrew
 * 
 */
public interface BotInterface {

    public boolean UserExist(String user);

    public String getBotClientProtoSupports();

    /**
     * Searches in the hub.
     * @param what The term to search for as per constrains given.
     * @throws IOException When communication error occurs.
     */
    public void Search(SearchSet ss) throws IOException;

    public String getBotHubProtoSupports();

    public ShareManager getShareManager();

    public DownloadCentral getDownloadCentral();

    public String botname();

    /**
     * Checks if the bot's client-hub protocol implementation supports that protocol feature.
     * @param feature
     * @return
     */
    public boolean isBotHubProtoSupports(String feature);

    /**
     * Checks if the bot's client-client protocol implementation supports that protocol feature.
     * @param feature
     * @return
     */
    public boolean isBotClientProtoSupports(String feature);

    public int getMaxUploadSlots();

    public int getMaxDownloadSlots();

    public int getFreeUploadSlots();

    public int getFreeDownloadSlots();

    public User[] GetAllUsers();

    public void terminate();

    public void setMaxUploadSlots(int slots);

    public void setMaxDownloadSlots(int slots);

    public void setShareManager(ShareManager sm);

    public User getUser(String username);

    public User getUserByCID(String cid);

    public String getMiscDir();

    public String getIncompleteDir();

    public void updateShareSize();
}
