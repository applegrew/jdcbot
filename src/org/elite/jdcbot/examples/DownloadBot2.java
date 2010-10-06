/*
 * DownloadBot2.java
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
import java.util.List;
import java.util.Vector;

import org.elite.jdcbot.framework.BotException;
import org.elite.jdcbot.framework.DUEntity;
import org.elite.jdcbot.framework.GlobalObjects;
import org.elite.jdcbot.framework.JMethod;
import org.elite.jdcbot.framework.User;
import org.elite.jdcbot.framework.jDCBot;
import org.elite.jdcbot.util.OutputEntityStream;
import org.slf4j.Logger;

/**
 * Created on 02-Jun-08<br>
 * This demo bot will download any file from a user when that user
 * sends the magnet URI of the file in private message to this bot.
 * <p>
 * This is an extension of DownloadBot. It recognizes 4 commands and
 * as in Downloabot it reads the commands in the private message.
 * The commands are:-<br>
 * <ul>
 * <li><code><b>+quit or quit</b></code> This will make it terminate immediately.</li>
 * <li><code><b>download </b><i>magnet uri</i></code> This will download the file
 * specified by this magnet URI from the user who sent the command.</li>
 * <li><code><b>cancel </b><i>magnet uri</i></code> This will cancel downloads running/scheduled
 * specified by this URI and from the user who sent the command.</li>
 * <li><code><b>limit </b><i>rate in MBps</i></code> This will immediately set the download
 * rate to this. All future downloads too will run at this rate.</li>
 * </ul>
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1.3
 */
public class DownloadBot2 extends jDCBot {
	private static final Logger logger = GlobalObjects.getLogger(DownloadBot2.class);
	List<DUEntity> allDU;
	private long transferLimit = 0;

	public DownloadBot2() throws IOException {
		super("DownloadBot2", //Bot's name
				"127.0.0.1", //Bot's IP
				9020, //Bot's listen port
				10020, //Bot's listen port for UDP packets
				"", //Password
				"I Download U Really!", //Description
				"LAN(T1)1", //Connection type
				"", //Email
				"0", //Share size in bytes
				3, //No. of upload slots
				3, //No of download slots.
				false //Is passive
		);

		try {
			connect("127.0.0.1", 1411); //Connecting to hub
		} catch (BotException e) {
			logger.error("Exception in DownloadBot2()", e);
			terminate();
		} catch (IOException e) {
			logger.error("Exception in DownloadBot2()", e);
			terminate();
		}

		allDU = new Vector<DUEntity>();
	}

	private void pm(String user, String msg) {
		SendPrivateMessage(user, msg);
	}
	
	protected void onSendCommandFailed(String msg, Throwable e, JMethod m) {
		if(m.equals(JMethod.PRIVATE_MSG))
			logger.error("Exception in pm()", e);
	}

	protected void onPrivateMessage(String user, String msg) {
		msg = msg.trim();

		if (UserExist(user)) {
			if (msg.equals("+quit") || msg.equals("quit")) {
				terminate();
				return;
			} else if (msg.startsWith("download ") || msg.startsWith("cancel ")) {
				boolean download = msg.startsWith("download ");
				msg = msg.substring(msg.indexOf(' ')).trim();

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

				DUEntity due = new DUEntity(DUEntity.Type.FILE, tth, 0, size);
				due.setSetting(DUEntity.AUTO_PREFIX_TTH_SETTING);

				if (download) {//Start download
					File file = new File(name);
					if (file.exists()) {
						pm(user, "Cannot download. A file with this name already exists in download directory.");
						return;
					}

					try {
						due.os(new OutputEntityStream(new BufferedOutputStream(new FileOutputStream(file)), size, transferLimit));
					} catch (FileNotFoundException e) {
						logger.error("Exception in onPrivateMessage()", e);
						return;
					}

					allDU.add(due);

					try {
						getUser(user).download(due);
					} catch (BotException e) {
						logger.error("Exception in onPrivateMessage()", e);
					}
				} else { //Cancel download
					getUser(user).cancelDownload(due);
					allDU.remove(due);
				}

			} else if (msg.startsWith("limit ")) {
				msg = msg.substring(msg.indexOf(' ')).trim();
				transferLimit = (long) (Double.parseDouble(msg) * 1024 * 1024);
				pm(user, "New Transfer Limit is now " + msg + " MBps");

				for (DUEntity due : allDU) {
					if (due.os() instanceof OutputEntityStream) {
						OutputEntityStream des = (OutputEntityStream) due.os();
						des.setTransferLimit(transferLimit);
					}
				}
			} else
				pm(user, msg.substring(0, msg.indexOf(' ') == -1 ? msg.length() : msg.indexOf(' ')) + "? I know of no such command.");
		}
	}

	private Query[] getSegmentedQuery(String query) {
		Vector<Query> Q = new Vector<Query>();
		String qs[] = query.split("&");
		for (String q : qs) {
			String e[] = q.split("=");
			Q.add(new Query(e[0], e.length < 2 ? null : e[1]));
		}
		return Q.toArray(new Query[0]);
	}

	protected void onDownloadComplete(User user, DUEntity due, boolean success, BotException e) {
		pm(user.username(), "I just now "
				+ (success ? "successfully" : "unsuccessfully")
				+ " completed download of "
				+ due.file()
				+ " from you."
				+ "Average transfer rate was "
				+ (due.os() instanceof OutputEntityStream ? ((OutputEntityStream) due.os()).getTransferRate() / 1024 / 1024 + " MBps"
						: "N/A"));
		if (!success) {
			pm(user.username(), "I got this exception: " + e.getMessage());
		}
		allDU.remove(due);
	}

	private class Query {
		public Query(String q, String v) {
			query = q;
			value = v;
		}

		public String query;
		public String value;

		public String toString() {
			return query + " = " + value;
		}
	}

	public static void main(String[] args) {
		try {
			new DownloadBot2();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
