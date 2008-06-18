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

package org.elite.jdcbot.framework;

import java.util.*;

/**
 * An user manager class.
 * <p>
 * This class holds all data about all users present on the hub like description, e-mail...
 * You shoudn't use this class directly, 
 * idea is to have only ine instance and that one is in jDCBot class. 
 * You should rather add methods in jDCBot (example is GetRandomUser in jDCBot class)
 * 
 * @since 0.6
 * @author Kokanovic Branko
 * @author AppleGrew
 * @version 0.8
 */
public class UserManager {
    private Vector<User> users;

    private jDCBot _bot;

    public UserManager(jDCBot bot) {
	_bot = bot;
	users = new Vector<User>();
    }

    /**
     * Add all users from the user list (user nick are delimited with '$$')<br>
     * <b>Note:</b> When this methos is called then all pre-existing users will be lost.
     * 
     * @param user_list
     *                List of all users delimited with '$$'
     */
    public void addUsers(String user_list) {
	users.clear();
	ArrayList<String> userList = parseDoubleDollarList(user_list);
	for (int i = 0; i < userList.size(); i++) {
	    String user = userList.get(i);
	    if (!user.equals(_bot.botname())) {
		users.add(new User(user, _bot));
		//_bot.getDispatchThread().callOnUpdateMyInfo(user);
		try {
		    if (!_bot.isHubSupports("NoGetINFO")) {
			String cmd = "$GetINFO $" + user + " $" + _bot.botname() + "|";
			_bot.log.println(cmd);
			_bot.SendCommand(cmd);
		    }
		} catch (Exception e) {}
	    }
	}
    }

    public void updateUserIPs(String list) {
	ArrayList<String> userList = parseDoubleDollarList(list);
	for (int i = 0; i < userList.size(); i++) {
	    String userNip = userList.get(i);
	    int spcpos = userNip.indexOf(' ');
	    String user = userNip.substring(0, spcpos);
	    String ip = userNip.trim().substring(spcpos);
	    User u = getUser(user);
	    u.setUserIP(ip);
	    _bot.getDispatchThread().callOnUpdateMyInfo(user);
	}
    }

    /**
     * Sets the users (including the bot) in the list as operators. If the user is not in the internal data structure then it is automatically added.
     * @param user_list
     */
    public void addOps(String user_list) {
	ArrayList<String> userList = parseDoubleDollarList(user_list);
	for (int i = 0; i < userList.size(); i++) {
	    String user = userList.get(i);
	    if (user.equals(_bot.botname()))
		_bot.setOp(true);
	    else {
		if (!userExist(user))
		    users.add(new User(user, _bot));
		User u = getUser(user);
		u.setOp(true);
		_bot.getDispatchThread().callOnUpdateMyInfo(user);
	    }
	}
    }

    /**
     * Parses double dollar delimited list. e.g. username1$$username2$$username3...
     * @param list The list (only).
     * @return
     */
    private ArrayList<String> parseDoubleDollarList(String list) {
	ArrayList<String> alist = new ArrayList<String>();

	StringTokenizer st = new StringTokenizer(list, "$$");
	while (st.hasMoreTokens()) {
	    String token = st.nextToken();
	    alist.add(token);
	}
	return alist;
    }

    /**
     * Throw out user from our list since he quited
     * 
     * @param user
     *                Nick of the user who quited
     */
    public void userQuit(String user) {
	Iterator i = users.iterator();
	while (i.hasNext()) {
	    User u = (User) i.next();
	    if (u.username().equals(user))
		i.remove();
	}
    }

    /**
     * Add user to our list since user joined hub
     * 
     * @param user
     *                Nick of the user who joined
     */
    public void userJoin(String user) {
	Iterator i = users.iterator();
	boolean contains = false;
	while (i.hasNext()) {
	    User u = (User) i.next();
	    if (u.username().equals(user))
		contains = true;
	}
	if (contains == false)
	    users.add(new User(user, _bot));
	try {
	    if (!_bot.isHubSupports("NoGetINFO")) {
		String cmd = "$GetINFO $" + user + " $" + _bot.botname() + "|";
		_bot.log.println(cmd);
		_bot.SendCommand(cmd);
	    }
	} catch (Exception e) {}
    }

    /**
     * 
     * @param user
     *                Nick of the user to find out is he on the hub
     * @return true if user is n the hub, false otherwise
     */
    public boolean userExist(String user) {
	Iterator i = users.iterator();
	while (i.hasNext()) {
	    User u = (User) i.next();
	    if (u.username().equals(user))
		return true;
	}
	return false;
    }

    /**
     * Returns users with matching IPs.<br>
     * <b>Note:</b> All users' ip addresses may not be available.
     * @param ip The IP of the user or a regular expression for the IP.
     * @param isRegx If you are using a regular expression for
     * to (for example) find users in a range of IP addresses, then
     * set this to true.
     * @return The list of users with matching IPs.
     */
    public Vector<User> getUserByIP(String ip, boolean isRegx) {
	Vector<User> ru = new Vector<User>();
	for (User u : users) {
	    if (isRegx ? u.getUserIP().matches(ip) : u.getUserIP().equals(ip))
		ru.add(u);
	}
	return ru;
    }

    /**
     * Gets everything about user.
     * 
     * @param user
     *                Nick of the user
     * @return User class if user exist, null otherwise
     */
    public User getUser(String user) {
	Iterator i = users.iterator();
	while (i.hasNext()) {
	    User u = (User) i.next();
	    if (u.username().equalsIgnoreCase(user)) {
		/*if (u.hasInfo() == false)
		 return null;
		 else*/
		return u;
	    }
	}
	return null;
    }

    public User getUserByCID(String CID) {
	for (User u : users) {
	    if (u.getClientID() != null && u.getClientID().equalsIgnoreCase(CID))
		return u;
	}
	return null;
    }

    /**
     * Gets random user from the hub
     * 
     * @return Random user
     */
    public User getRandomUser() {
	if (users.isEmpty())
	    return null;
	int i = users.size();
	int a = new Random().nextInt(i);
	return (User) users.get(a);
    }

    public User[] getAllUsers() {
	if (users.isEmpty())
	    return null;
	User u[] = new User[1];
	return users.toArray(u);
    }

    /**
     * Sets user info (description, e-mail...)
     * 
     * @param info
     *                Info from the user that will be parsed
     */
    public void SetInfo(String info) {
	String user, desc, conn, mail, share;
	int index = 0;
	user = new String();
	while (info.charAt(index) != ' ') {
	    user = user + info.charAt(index);
	    index++;
	}
	if (user.equals(_bot.botname()))
	    return;

	index++;
	desc = new String();
	while (info.charAt(index) != '$') {
	    desc = desc + info.charAt(index);
	    index++;
	}
	index = index + 3;
	conn = new String();
	while (info.charAt(index) != '$') {
	    conn = conn + info.charAt(index);
	    index++;
	}
	index++;
	mail = new String();
	while (info.charAt(index) != '$') {
	    mail = mail + info.charAt(index);
	    index++;
	}
	index++;
	share = new String();
	while (info.charAt(index) != '$') {
	    share = share + info.charAt(index);
	    index++;
	}
	Iterator i = users.iterator();
	while (i.hasNext()) {
	    User u = (User) i.next();
	    if (u.username().equals(user)) {
		i.remove();
		users.add(new User(user, desc, conn, mail, share, _bot));
		return;
	    }
	}
	_bot.getDispatchThread().callOnUpdateMyInfo(user);
    }

    /**
     * Sends message to all user on the hub
     * 
     * @param pm Message to be sent
     * @param timeout Timeout inteval in milliseconds between to private
     * messages
     */
    public void SendAll(String pm, long timeout) {
	SendingAll sa = new SendingAll(pm, timeout);
	sa.start();
    }

    private class SendingAll extends Thread {
	String _pm;

	long _timeout;

	Vector<User> useri = new Vector<User>(users);

	public SendingAll(String pm, long timeout) {
	    _pm = pm;
	    _timeout = timeout;
	}

	public void run() {
	    Iterator i = useri.iterator();
	    while (i.hasNext()) {
		User u = (User) i.next();
		try {
		    if (!(_bot.botname().equals(u.username())))
			_bot.SendPrivateMessage(u.username(), _pm);
		    sleep(_timeout);
		} catch (Exception e) {}
	    }
	    return;
	}
    }
}