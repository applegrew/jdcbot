/*
 * ResultSet.java
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
package org.elite.jdcbot.shareframework;

/**
 * Created on 06-Jun-08<br>
 * Results to searches on the hub
 * are returned enclosed in this.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class SearchResultSet {
    /**
     * The name or (virtual) path
     * to the file or directory
     * that matches your search. 
     */
    public String name = "";
    /**
     * This true when <i>name<i>
     * is a directory's name or path.
     */
    public boolean isDir = false;
    /**
     * The size of the file in
     * bytes. This is zero for
     * directories.
     */
    public long size = 0;
    /**
     * The TTH of the file
     * <i>name</i>. This
     * can be empty string if
     * hash for the file is
     * not available or it
     * is a directory.
     */
    public String TTH = "";
}
