/*
 * InputThreadTarget.java
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

/**
 * Created on 26-May-08
 * @since 0.7
 * @author AppleGrew
 * @version 0.1
 * 
 */
public class InputThreadTarget extends DCIO {
    /**
     * Called by InputThread when a command is read from the socket input.
     * @param rawcmd The raw command is passed in this argument.
     */
    public void handleCommand(String rawcmd){}

    /**
     * Called by InputThread on socket disconnection. 
     *
     */
    protected void onDisconnect(){}
}
