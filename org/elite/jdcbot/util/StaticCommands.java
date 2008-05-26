/*
 * StaticCommands.java
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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Simple MySQLWork extended class that gets static command (command with static output)
 * from MySQL database. For creating this table, find command.sql in downloaded framework and
 * run it. Also, you should change name of database, username and password in constructor
 * 
 * @since 0.6
 * @author Kokanovic Branko
 * @version 0.6
 *
 */
public class StaticCommands extends MySQLWork{
	
	public StaticCommands(){
		super("jdcbot","jdcbot","secret");
	}
	
	/**
	 * Tries to get command output from database
	 * @param cmd Command that was on main chat for which we search output (e.g. +help)
	 * @return Empty string ("") if command was not found in database, command output otherwise
	 * (e.g. "Output some help")
	 */
	public String parseCommand(String cmd){
		try{
			String query = "SELECT cmd_output FROM static_cmds WHERE cmd_name='"+cmd+"'";
			//System.out.println("Upit:"+query);
			ResultSet rs=stmt.executeQuery(query);
			if (rs.first()==false) return ""; //nije komanda uopste nadjena
			String desc=rs.getString("cmd_output");
			return desc;
		}catch(SQLException e){
			displaySQLErrors(e);
			return "";
		}
	}
}