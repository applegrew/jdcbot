/*
 * User.java
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

package org.elite.jdcbot.framework;

/**
 * User class.
 * <p>
 * Holds everything about user (description, e-mail, sharesize...)
 * 
 * @since 0.6
 * @author  Kokanovic Branko
 * @version    0.6
 */
public class User {
	
	private String _username,_desc,_conn,_mail,_share,_tag;
	private boolean _hasInfo;
	
	public User(String username){
		_username=username;
		_hasInfo=false;
	}
	
	public User(String username,String desc,String conn,String mail,String share){
		_username=username;
		_desc=desc;
		_conn=conn;
		_mail=mail;
		_share=share;
		_hasInfo=true;
		int index=_desc.indexOf('<');
		if (index==-1)
			_tag=new String();
		else
			_tag=_desc.substring(_desc.indexOf('<')+1,_desc.length()-1);
	}
	
	public boolean hasInfo(){return _hasInfo;}
	
	
	/**
	 * @return User nick
	 */
	public String username(){return _username;}
	/**
	 * @return User description (along with the client tag). If you want real description,
	 * use method with that name 
	 */
	public String description(){return _desc;}
	/**
	 * @return Type of user's connection
	 */
	public String connection_type(){return _conn;}
	/**
	 * @return User mail he specified
	 */
	public String mail(){return _mail;}
	/**
	 * @return Share size of the user in bytes
	 */
	public String sharesize(){return _share;}
	/**
	 * @return true if user has client tag, false otherwise
	 */
	public boolean hasTag(){
		return (_tag.length()!=0);
	}
	/**
	 * @return Client tag if it exist
	 */
	public String tag(){return _tag;}
	
	/**
	 * Try to get real description, not just tag
	 * @return Description (also known as comment) if it exist, "" otherwise
	 */
	public String real_description(){
		String buffer=new String();
		int index=0;
		while((_desc.length()>index) && (_desc.charAt(index)!='<')){
			buffer=buffer+_desc.charAt(index);
			index++;
		}
		return buffer;
	}
	
	/**
	 * Tries to get client from the tag
	 * @return Client that user use if it exist, "" otherwise
	 */
	public String client(){
		if (_tag.length()<1) return "";
		if (_tag.indexOf(' ')==-1) return "";
		return _tag.substring(0,_tag.indexOf(' '));
	}
	
	/**
	 * Tries to get client version form user
	 * @return Version of the client user use if it exist in tag, "" otherwise
	 */
	public String version(){
		if (_tag.contains(" V:")){
			int index1=_tag.indexOf(" V:")+2;
			int index2=_tag.indexOf(',',index1);
			if (index2==-1) return "";
			return _tag.substring(index1,index2+1);
		}
		else return "";
	}
	
	/**
	 * 
	 * @return true if user is active, false if it is in passive mode, or tag does not exist
	 */
	public boolean isActive(){
		if (_tag.contains(",M:A")) return true;
		else return false;
	}
	
	/**
	 * 
	 * @return Number of slots user have if exists in tag, 0 otherwise 
	 */
	public int slots(){
		if (_tag.contains(",S:")){
			int index1=_tag.indexOf(",S:")+3;
			String buffer=new String();
			while ((_tag.charAt(index1)!='>') && (_tag.charAt(index1)!=',') && (_tag.charAt(index1)!=' '))
				buffer+=_tag.charAt(index1++);
			return Integer.parseInt(buffer);
		}
		else return 0;
	}
}
