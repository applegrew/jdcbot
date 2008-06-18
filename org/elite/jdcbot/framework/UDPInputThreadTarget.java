/*
 * UDPInputThreadTarget.java
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

/**
 * Created on 06-Jun-08<br>
 * This must be implemeneted by
 * classes that want to listen for UDP
 * connections.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public interface UDPInputThreadTarget {

    /**
     * Called when {@link UDPInputThread} is closed
     * due to an exception.
     */
    void onUDPExceptionClose(IOException e);

    /**
     * Called when a new UDP packet is read.
     * @param string The content of the packet.
     * @param hostName The host's IP from which the packet was sent.
     * @param port The port of the remote host that was used.
     */
    public void handleUDPCommand(String rawCommand, String ip, int port);

}
