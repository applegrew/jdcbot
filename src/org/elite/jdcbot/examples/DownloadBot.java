/*
 * DownloadBot.java
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
package org.elite.jdcbot.examples;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import org.elite.jdcbot.framework.BotException;
import org.elite.jdcbot.framework.DUEntity;
import org.elite.jdcbot.framework.GlobalObjects;
import org.elite.jdcbot.framework.User;
import org.elite.jdcbot.framework.jDCBot;
import org.slf4j.Logger;

/**
 * Created on 31-May-08<br>
 * This exmple bot will download any file from a user when that user
 * sends the magnet URI of the file in private message to this bot.
 * <p>
 * The bot will immediately quit if anybody sends <code>+quit</code>
 * as private message to this bot.
 *
 * @author AppleGrew
 * @since 0.7.1
 * @version 0.1.1
 */
public class DownloadBot extends jDCBot {
	private static final Logger logger = GlobalObjects.getLogger(DownloadBot.class);

	public DownloadBot() throws IOException {
		super("DownloadBot", //Bot's name
				"127.0.0.1", //Bot's IP
				9020, //Bot's listen port
				10020, //Bot's listen port for UDP packets
				"", //Password
				"I Download U", //Description
				"LAN(T1)1", //Connection type
				"", //Email
				"0", //Share size in bytes
				3, //No. of upload slots
				3, //No of download slots.
				false //Is passive
		);

		try {
			connect("127.0.0.1", 1411); //Connecting to the hub.
		} catch (BotException e) {
			logger.error("Exception in DownloadBot()", e);
			terminate();
		} catch (IOException e) {
			logger.error("Exception in DownloadBot()", e);
			terminate();
		}
	}

	private void pm(String user, String msg) {
		try {
			SendPrivateMessage(user, msg);
		} catch (Exception e) {
			logger.error("Exception in pm()", e);
		}
	}

	protected void onPrivateMessage(String user, String msg) {
		if (UserExist(user)) {
			if (msg.equals("+quit")) {
				terminate();
				return;
			}
			msg = msg.trim();

			Query Q[] = getSegmentedQuery(msg.substring(msg.indexOf('?') + 1));
			if (Q == null) {
				pm(user, "Error! Maybe the URI is not in proper format");
				return;
			}

			String tth = null, name = null;
			long size = 0;
			for (Query q : Q) {
				if (q.query.equalsIgnoreCase("xt")) {
					tth = q.value.substring(q.value.lastIndexOf(':') + 1);
				} else if (q.query.equalsIgnoreCase("xl")) {
					try {
						size = Long.parseLong(q.value);
					} catch (NumberFormatException e) {
						pm(user, "Please enter a valid magnet uri. Error occured while trying to parse file size");
						return;
					}
				} else if (q.query.equalsIgnoreCase("dn")) {
					name = q.value.replace('+', ' ');
				}
			}

			if (size <= 0) {
				pm(user, "Invalid value of file size: " + size + ". Make sure you have entered a valid magnet uri.");
				return;
			}
			if (tth == null || name == null) {
				pm(user, "Error occured during parsing the magnet uri. Make sure this is valid.");
				return;
			}

			File file = new File(name);
			if (file.exists()) {
				pm(user, "Cannot download. A file with this name already exists in download directory.");
				return;
			}

			BufferedOutputStream os;
			try {
				os = new BufferedOutputStream(new FileOutputStream(file));
			} catch (FileNotFoundException e) {
				logger.error("Exception in onPrivateMessage()", e);
				return;
			}

			//due.file = "TTH/" + tth; No need to prefix to 'TTH/' manually anymore, just set the following flag.
			DUEntity due = new DUEntity(DUEntity.Type.FILE, tth, 0, size, os, DUEntity.AUTO_PREFIX_TTH_SETTING);

			try {
				getUser(user).download(due);
			} catch (BotException e) {
				logger.error("Exception in onPrivateMessage()", e);
			}
		}
	}

	public Query[] getSegmentedQuery(String query) {
		Vector<Query> Q = new Vector<Query>();
		String qs[] = query.split("&");
		for (String q : qs) {
			String e[] = q.split("=");
			Q.add(new Query(e[0], e.length < 2 ? null : e[1]));
		}
		return Q.toArray(new Query[0]);
	}

	protected void onDownloadComplete(User user, DUEntity due, boolean success, BotException e) {
		pm(user.username(), "I just now " + (success ? "successfully" : "unsuccessfully") + " completed download of " + due.file()
				+ " from you.");
		if (!success) {
			pm(user.username(), "I got this exception: " + e.getMessage());
		}
	}

	private class Query {
		public Query(String q, String v) {
			query = q;
			value = v;
		}

		public String query;
		public String value;
	}

	public static void main(String[] args) {
		try {
			new DownloadBot();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
