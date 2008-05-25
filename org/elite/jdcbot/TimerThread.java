/*
 * TimerThread.java
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

package org.elite.jdcbot;

/**
 * Simple abstract class for generating OnTimer event.
 *
 * You are encouraged to extends this thread with your own class (example is FloodMessageThread)
 * since it enables you to have more than one onTimer event. So you would have one class which prints
 * users joined hub on every hour on main chat and another that prints current weather on every two hours.
 *
 * @since 0.5
 * @author  Kokanovic Branko
 * @version    0.6
 */
public abstract class TimerThread extends Thread{
    
    private long _wait_time;
    protected jDCBot _bot;
    private boolean running=true;
    /**
     * Constructs new thread that triggers onTimer event.
     *
     * @param bot jDCBot instance needed to send messages...
     * @param wait_time Time (in ms) between triggers.
     */
    public TimerThread(jDCBot bot,long wait_time) {
        _bot=bot;
        _wait_time=wait_time;
    }
    
    public void run(){
        while(running){
            try{
                sleep(_wait_time);
            }catch(InterruptedException e){}
            onTimer();
        }
    }
    
    /**
     * Called every wait_time
     * <p>
     * The implementation of this method in the TimerThread abstract class
     * performs no actions and may be overridden as required.
     */
    protected void onTimer(){}
    
    /**
     * Stops the thread.
     */
    public synchronized void stopIt(){
        running=false;
        notify();
    }
    
}
