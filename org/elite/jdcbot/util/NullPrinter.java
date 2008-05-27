/*
 * NullPrinter.java
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Created on 26-May-08<br>
 * Use this class if you donot want framework to print to console or any other place. You want all the debug informations to be lost.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1
 * 
 */
public class NullPrinter extends PrintStream {
    public NullPrinter(){
	super(new ByteArrayOutputStream(1));
    }
    
    public void println(){}
    public void println(String s){}
    public void println(boolean b){}
    public void println(int i){}
    public void println(double d){}
    public void println(char c){}
    public void println(char c[]){}
}
