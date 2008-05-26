/*
 * MySQLWork.java
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


import java.sql.*;

/**
 * Simple mysql class that you can extend to do your work. 
 * Example class would be StaticCommands. It just connects to your database in constructor,
 * and, then you can use protected Statement stmt to execute queries.
 * You should have MySQLConnector class for this to work,. You can get it at
 * http://www.mysql.com/products/connector/j/
 * 
 * @since 0.6
 * @author Milos Grbic
 * @version 0.6
 *
 */
public abstract class MySQLWork {

	private Connection conn;
	protected Statement stmt;
	
	public MySQLWork(String database,String username){
		this(database,username,"");
	}
	
	public MySQLWork(String database,String username,String password){
		try{
			Class.forName("com.mysql.jdbc.Driver");
			String s=new String();
			s="jdbc:mysql://localhost/";
			s+=database;
			s+="?user="+username;
			if (password.length()!=0)
				s+="&password="+password;
			
			conn=DriverManager.getConnection(s);
			stmt=conn.createStatement();
		}
		catch (SQLException e) {
			displaySQLErrors(e);
		}
		catch(ClassNotFoundException e){
			System.out.println("You don't have MySQLConnector class!");
			e.printStackTrace();
		}
	}

	public void finalize(){
		try{
			stmt.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	protected void displaySQLErrors(SQLException e){
		System.out.println("SQLException: "+e.getMessage());
		System.out.println("SQLState: "+e.getSQLState());
		System.out.println("Vendor error: "+e.getErrorCode());
	}
}
