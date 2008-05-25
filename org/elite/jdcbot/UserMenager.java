/*
 * UserMenager.java
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

import java.util.*;

/**
 * An user menager class.
 * <p>
 * This class holds all data about all users present on the hub like description, e-mail...
 * You shoudn't use this class directly, 
 * idea is to have only ine instance and that one is in jDCBot class. 
 * You should rather add methods in jDCBot (example is GetRandomUser in jDCBot class)
 * 
 * @since 0.6
 * @author  Kokanovic Branko
 * @version    0.6
 */
public class UserMenager {
	private Vector users;
	private jDCBot _bot;
	
	public UserMenager(jDCBot bot){
		_bot=bot;
		users=new Vector();
	}
	
	/**
	 * Add all users from the user list (user nick are delimited with '$$')
	 * @param user_list List of all users delimited with '$$'
	 */
	public void addUsers(String user_list){
        users.clear();
        StringTokenizer st=new StringTokenizer(user_list,"$$");
        while (st.hasMoreTokens()) {
        	String user=st.nextToken();
            users.add(new User(user));
    		try{
    			_bot.SendCommand("$GetINFO $"+user+" $"+_bot.botname()+"|");
    		}catch(Exception e){}
        }
	}
	
	/**
	 * Throw out user from our list since he quited
	 * @param user Nick of the user who quited
	 */
	public void userQuit(String user){
		Iterator i=users.iterator();
		while (i.hasNext()){
			User u=(User)i.next();
			if (u.username().equals(user)) i.remove();
		}
	}
	
	/**
	 * Add user to our list since user joined hub
	 * @param user Nick of the user who joined
	 */
	public void userJoin(String user){
		Iterator i=users.iterator();
		boolean contains=false;
		while (i.hasNext()){
			User u=(User)i.next();
			if (u.username().equals(user)) contains=true;
		}
		if (contains==false)
            users.add(new User(user));
		try{
			_bot.SendCommand("$GetINFO $"+user+" $"+_bot.botname()+"|");
		}catch(Exception e){}
	}
	
	/**
	 * 
	 * @param user Nick of the user to find out is he on the hub
	 * @return true if user is n the hub, false otherwise
	 */
	public boolean UserExist(String user){
		Iterator i=users.iterator();
		while (i.hasNext()){
			User u=(User)i.next();
			if (u.username().equals(user)) return true;
		}
		return false;
	}
	
	/**
	 * Gets everything about user if we have user description
	 * @param user Nick of the user
	 * @return User class if user exist and we managed to have description, null otherwise 
	 */
	public User getUser(String user){
		Iterator i=users.iterator();
		while (i.hasNext()){
			User u=(User)i.next();
			if (u.username().equals(user)){
				if (u.hasInfo()==false)
					return null;
				else
					return u;
			}
		}
		return (User)i.next();
	}
	
	/**
	 * Gets random user from the hub
	 * @return Random user
	 */
	public User getRandomUser(){
		if (users.isEmpty()) return null;
		int i=users.size();
		int a=new Random().nextInt(i);
		return (User)users.get(a);
	}
	
	/**
	 * Sets user info (description, e-mail...)
	 * @param info Info from the user that will be parsed
	 */
	public void SetInfo(String info){
		String user,desc,conn,mail,share;
		int index=0;
		user=new String();
		while (info.charAt(index)!=' '){
			user=user+info.charAt(index);
			index++;
		}
		index++;
		desc=new String();
		while (info.charAt(index)!='$'){
			desc=desc+info.charAt(index);
			index++;
		}
		index=index+3;
		conn=new String();
		while (info.charAt(index)!='$'){
			conn=conn+info.charAt(index);
			index++;
		}
		index++;
		mail=new String();
		while (info.charAt(index)!='$'){
			mail=mail+info.charAt(index);
			index++;
		}
		index++;
		share=new String();
		while (info.charAt(index)!='$'){
			share=share+info.charAt(index);
			index++;
		}
		Iterator i=users.iterator();
		while (i.hasNext()){
			User u=(User)i.next();
			if (u.username().equals(user)){
				i.remove();
				users.add(new User(user,desc,conn,mail,share));
				return;
			}
		}
	}
	
	/**
	 * Sends message to all user on the hub
	 * @param pm Message to be sent
	 * @param timeout Timeout inteval in milliseconds between to private messages
	 */
	public void SendAll(String pm,long timeout){
		SendingAll sa=new SendingAll(pm,timeout);
		sa.start();
	}
	
	private class SendingAll extends Thread{
		String _pm;
		long _timeout;
		Vector useri=new Vector(users);
		public SendingAll(String pm, long timeout){
			_pm=pm;
			_timeout=timeout;
		}
		public void run(){
			Iterator i=useri.iterator();
			while (i.hasNext()){
				User u=(User)i.next();
				try{
					if (!(_bot.botname().equals(u.username())))
						_bot.SendPrivateMessage(u.username(),_pm);
					sleep(_timeout);
				}catch(Exception e){}
			}
			return;
		}
	}
}