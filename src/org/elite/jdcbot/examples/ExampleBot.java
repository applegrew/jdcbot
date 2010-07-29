/*
 * ExampleBot.java
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

package org.elite.jdcbot.examples;

import java.io.IOException;

import org.elite.jdcbot.framework.BotException;
import org.elite.jdcbot.framework.GlobalObjects;
import org.elite.jdcbot.framework.jDCBot;
import org.elite.jdcbot.util.FloodMessageThread;
import org.elite.jdcbot.util.GoogleCalculation;
import org.elite.jdcbot.util.StaticCommands;
import org.elite.jdcbot.util.TimerThread;
import org.slf4j.Logger;

/**
 * ExampleBot is simple derived class from jDCBot apstract class overriding some methods.
 *
 * ExampleBot is example and start point for developing your bot. It shows how to overrides jDCBot methods 
 * and implements simple TimerThread which adds onTimer method.
 * <p>
 * Upon connecting, it will announce everyone on hub that it is present
 * On private message, it will tell that it doesn't understand a word.
 * Has +calc command on main chat that shows how to use WebPageFetcher and GoogleCalculation
 * And will flood main chat every 10 min. showing how to use TimerThread class
 *
 * @since 0.5
 * @author Kokanovic Branko
 * @author Milos Grbic
 * @author AppleGrew
 * @version 0.7.1
 */
public class ExampleBot extends jDCBot {
	private static final Logger logger = GlobalObjects.getLogger(ExampleBot.class);

	//abstract TimerThread class for flood message
	private TimerThread tt;

	private StaticCommands static_cmds = new StaticCommands();

	public ExampleBot() throws IOException {
		//constructs our bot with 100GB share size and 3 slots
		super("ExampleBot", "127.0.0.1", 9006, 10006, "", "This example bot", "LAN(T1)8", "", "107374182400", 3, 6, false);
		try {
			connect("127.0.0.1", 1411); //my local hub, change it to whatever yours is/
		} catch (BotException e) {
			logger.error("Exception in ExampleBot()", e);
		} catch (Exception e) {
			logger.error("Exception in ExampleBot()", e);
		}
	}

	/**
	 * Prints on main chat that we are here and starts flood thread.
	 */
	public void onConnect() {
		tt = new FloodMessageThread(this, 1000 * 60 * 10);
		tt.start();
		try {
			SendPublicMessage("Hi, I'm example ExampleBot. I have been created using jDCBot ( http://jdcbot.sourceforget.net )");
		} catch (Exception e) {}
	}

	/**
	 * Simple example how to use GoogleCalculation and static commands from database.
	 * Bot will only detect two commands on main chat:
	 * +help (from database, StaticCommands class) and 
	 * +calc (in form of e.g. '+calc 1+2*3', GoogleCalculation class)
	 */
	public void onPublicMessage(String user, String message) {
		//examine if command starts with +calc (you could put more commands here
		//in 'else if' construction
		if (message.toLowerCase().startsWith("+calc ")) {
			String eq = message.substring(message.indexOf(" ") + 1, message.length());
			try {
				Thread t = new Thread(new GoogleCalculation(this, eq));
				t.start();
			} catch (Exception e) {}
		} else
			//if there were no command recognized above, try to get command from database
			if (message.startsWith("+")) { //we are calling sql queries only if it starts with '+' for optimization
				//get only command (not including space if it exist nor anything after it)
				String only_cmd = ((message.indexOf(' ')) != -1) ? (message.substring(0, message.indexOf(' '))) : message;
				String output = static_cmds.parseCommand(only_cmd);
				if (output.length() != 0) { //if command exist in database
					try {
						SendPublicMessage(output);
					} catch (Exception e) {}
				}
			}
	}

	/**
	 * Sends user who wants to talk to us a feedback that we're (still) stupid.
	 */
	public void onPrivateMessage(String user, String message) {
		try {
			SendPrivateMessage(user, "I don't know how to talk anything...yet. ;)");
		} catch (Exception e) {}
	}

	public void onDisconnect() {
		super.onDisconnect();
		logger.info("Disconnected from the hub");
		tt.stopIt();
	}

	public static void main(String[] args) {
		try {
			new ExampleBot();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
