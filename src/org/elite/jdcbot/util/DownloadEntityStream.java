/*
 * DownloadEntityStream.java
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
package org.elite.jdcbot.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created on 16-Jun-08
 *
 * @author AppleGrew
 * @deprecated <b>This class has been renamed.</b>
 * @see OutputEntityStream
 * @since 0.7.2
 * @version 0.1
 */
public class DownloadEntityStream extends InputStream {

    @Override
    public int read() throws IOException {
	return 0;
    }

}
