/*
 * FloodMessageThread.java
 *
 * Copyright (C) 2005 Kokanovic Branko
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

package org.elite.jdcbot.util;

import org.elite.jdcbot.framework.jDCBot;


/**
 * Simple example class that extends TimerThread showing how to handle OnTimer triggers.
 *
 * Every 10 min. prints some flood message on main chat.
 *
 * @since 0.5
 * @author  Kokanovic Branko
 * @author AppleGrew
 * @version    0.7
 */
public class FloodMessageThread extends TimerThread{
    
    /**
     * Constructs FloodMessageThread.
     *
     * @param bot Instance of jDCBot.
     * @param waittime Time between OnTimer events (in ms).
     */
    public FloodMessageThread(jDCBot bot,long waittime) {
        super(bot,waittime,"FloodThread");
    }
    
    /** 
     * Overriden onTimer events and prints some stupid flood message on main chat.
     */
    public void onTimer(){
        try{
            _bot.SendPublicMessage("Some example flood message");
        }catch(Exception e){}
    }
    
}
