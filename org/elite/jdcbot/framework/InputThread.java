/*
 * InputThead.java
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

package org.elite.jdcbot.framework;

import java.io.*;

/**
 * Threads that reads raw commands from hub and passes them to jDCBot.
 *
 * @since 0.5
 * @author  Kokanovic Branko
 * @version    0.6
 */
public class InputThread extends Thread{
    
    private BufferedReader _breader;
    private jDCBot _bot;
    
    /** Constructs thread that will read raw commands from hub
     *
     * @param bot jDCBot instance
     * @param breader BufferedReader class from which we will read.
     */
    public InputThread(jDCBot bot,BufferedReader breader) {
        _bot=bot;
        _breader=breader;
    }
    
    public void run() {
        try {
            boolean running = true;
            while (running) {
                String rawCommand=null;
                rawCommand=this.ReadCommand();
                if ((rawCommand==null) || (rawCommand.length()==0)){
                    running=false;
                    _bot.onDisconnect();
                }
                _bot.handleCommand(rawCommand);
            }
        }catch(Exception e){
            _bot.onDisconnect();
        }
    }
    
    /**
     * Reads raw command sent from hub
     */
    private String ReadCommand() throws Exception{
        int c;
        String buffer=new String();
        do{
            try{
                c=_breader.read();
                if (c==-1){
                    throw new IOException("disconnected");
                }
                buffer+=(char)c;
            }catch(IOException e){c='|';}
        }while(c!='|');
        return buffer;
    }
}
