/*
 * DownloadQEntry.java
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

import java.util.List;
import java.util.Vector;

import org.elite.jdcbot.framework.DownloadCentral.Src;

/**
 * Created on 19-Jun-08
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class DownloadQEntry {
    public String file;
    public DownloadCentral.State state;
    public String saveTo;
    public Vector<User> srcsByUser = new Vector<User>();
    /**
     * When user has gone offilne then only CID maybe
     * available then these entries are stored here.
     */
    public Vector<String> srcsByCID = new Vector<String>();

    public DownloadQEntry(DownloadCentral.Download d) {
	file = d.due.file();
	state = d.state;
	saveTo = d.saveto.getPath();
	List<DownloadCentral.Src> srcs = d.getAllSrcs();
	for (Src s : srcs) {
	    User u = s.getUser();
	    if (u != null)
		srcsByUser.add(u);
	    else {
		String CID = s.getCID();
		if (CID != null && !CID.isEmpty())
		    srcsByCID.add(CID);
	    }
	}
    }
}
