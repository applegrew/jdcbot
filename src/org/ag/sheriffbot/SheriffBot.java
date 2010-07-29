/*
 * SheriffBot.java
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

package org.ag.sheriffbot;

import java.util.Calendar;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.HashMap;

import org.apache.tools.bzip2.CBZip2OutputStream;
import org.elite.jdcbot.framework.BotException;
import org.elite.jdcbot.util.TimerThread;
import org.elite.jdcbot.framework.DUEntity;
import org.elite.jdcbot.framework.GlobalObjects;
import org.elite.jdcbot.framework.User;
import org.elite.jdcbot.framework.jDCBot;
import org.slf4j.Logger;

/**
 * SheriffBot is a derived class from jDCBot apstract class overriding some methods.
 * <p>
 * This bot's task is to periodically download all the users' file list. It downloads all the
 * users' file list and packs them into a single xml file named in the format shown below.<br>
 * <br>
 * <code>Hub name-yyyy-MM-dd_HH.mm.ss.bz2</code><br><br>
 * where <i>yyyy-MM-dd_HH.mm.ss</i> is the date and time of creation of the file.<br>
 * This file is compressed using bzip2 algorithm. When any new user logs in for the first time
 * in-between the file list dumping cycle then that user's file list is downloaded and saved
 * separately and the file is named as<br>
 * <br>
 * <code>Hub name_User name-yyyy-MM-dd_HH.mm.ss.bz2</code><br>
 * <br>
 * The reason of creating this was that my college laid down the condition that DC++ will only
 * be allowed on college LAN network if the hub maintains a list of users' file list. So, that
 * later if required the originator of any illegal file can be traced back.
 * 
 * @since 0.7.1
 * @author AppleGrew
 * @version 1.2.1
 */
public class SheriffBot extends jDCBot {
	public final String version = "1.2.1";
	private static final Logger logger = GlobalObjects.getLogger(SheriffBot.class);

	private TimerThread tt;
	private int _updateInterval;
	private String _hubIP;
	private String _filelistDir;

	//Lists authorized users and the hub ips from where they are authorized to send command to SheriffBot.
	private HashMap<String, String> authorizedUsers;
	private UserListDownloadThread uth;
	private Vector<String> users2Index;

	private Vector<String> spareUsers; //The users' ips whose file lists shouldn't be downloaded.
	private Vector<String> indexedUsers;
	private Vector<String> downloadedUsers;

	private volatile boolean isShuttingDown = false;
	private volatile boolean listUpdatedAtleastOnce = false;
	private volatile boolean isUpdateInProgress = false;

	public SheriffBot() throws IOException {
		// constructs our bot with 0B share size and 3 slots, which updates
		// file list every 30mins.
		this("127.0.0.1", 1411, "127.0.0.1", 9000, 10000, "0", 3, 1000 * 60 * 30, "./", "", false);
	}

	public SheriffBot(String hubIP, int hubPort, String sharesizeInBytes, String hubpass, String botIP, int listenPort, int UDP_listenPort,
			String filelistDir, boolean active) throws IOException {
		// constructs our bot with 10GB share size and 3 slots, which updates
		// file list every 6hr.
		this(hubIP, hubPort, botIP, listenPort, UDP_listenPort, sharesizeInBytes, 3, 6 * 60 * 60 * 1000, filelistDir, hubpass, !active);
	}

	public SheriffBot(String hubIP, int hubPort, String botIP, int listenPort, int UDP_listenPort, String sharesizeInBytes, int noOfSlots,
			int updateInterval, String filelistDir, String hubPass, boolean passive) throws IOException {
		super("SheriffBot", botIP, listenPort, UDP_listenPort, hubPass, "Monitor Bot", "LAN(T1)1", "", sharesizeInBytes, noOfSlots, 4,
				passive);

		_updateInterval = updateInterval;
		_hubIP = hubIP;
		filelistDir = filelistDir.trim().replace('\\', '/');
		_filelistDir =
			filelistDir.equals("") ? "" : filelistDir.substring(0, filelistDir.endsWith("/") ? filelistDir.length() - 1 : filelistDir
					.length())
					+ "/";

		authorizedUsers = new HashMap<String, String>();
		authorizedUsers.put("applegrew", "127.0.0.1"); //applegrew when logged-in into hub with 127.0.0.1 is authorized to command SheriffBot.

		users2Index = new Vector<String>();

		downloadedUsers = new Vector<String>();

		spareUsers = new Vector<String>();
		spareUsers.add("127.0.0.1"); //The file list of the user with the ip 127.0.0.1 will be downloaded, but NOT saved. 

		indexedUsers = new Vector<String>();

		try {
			connect(hubIP, hubPort);
		} catch (BotException e) {
			logger.error("Error:" + e, e);
			terminate();
		} catch (Exception e) {
			logger.error("Exception in SheriffBot.", e);
			if (e instanceof IOException)
				terminate();
		}
	}

	/**
	 * Prints on main chat that we are here and starts flood thread.
	 */
	public void onConnect() {
		tt = new TimerThread(this, _updateInterval, "UpdateFileListThread", 5 * 60 * 1000) {// The first list update will start after 5mins to
			// allow collection of user infos.
			protected void onTimer() {
				((SheriffBot) _bot).updateFilelists();
				((SheriffBot) _bot).clearIndexedUsersList();
			}

		};
		tt.start();

		uth = new UserListDownloadThread();
		uth.start();

	}

	protected void clearIndexedUsersList() {
		indexedUsers.removeAllElements();
	}

	/**
	 * Sends user who wants to talk to us a feedback that we're (still) stupid.
	 */
	public void onPrivateMessage(String user, String message) {
		if (authorized(user) && message.startsWith("+raw ")) {
			try {
				String cmd = message.substring(message.indexOf(' ') + 1).replaceAll("&#36;", "\\$").replaceAll("&#124;", "|");
				logger.debug("Sending command: " + cmd);
				SendCommand(cmd);
			} catch (Exception e) {}
		} else if (message.equals("+version")) {
			try {
				SendPrivateMessage(user, "Version: " + version);
			} catch (Exception e) {}
		} else if (authorized(user) && message.equals("+quit")) {
			try {
				SendPrivateMessage(user, "Disabled.");
			} catch (Exception e) {}
			//quit();
		} else {
			String msg = null;
			// msg = "Go away! I have nothing to talk about. I am on duty right now.";
			if (message.equals("+quit"))
				msg = "You are not authorized to command me. Go away!";
			try {
				if (msg != null)
					SendPrivateMessage(user, msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void onBotQuit() {
		isShuttingDown = true;
		tt.stopIt();
		uth.stopIt();
	}

	public void onDisconnect() {
		if (!isShuttingDown)
			logger.debug("Disconnected from hub");
		while (!isShuttingDown && !isConnected()) {
			// Try to reconnect to the hub after waiting for sometime.
			try {
				Thread.sleep(6000); // Waiting for 6s
				connect(_hubIP, _port);

			} catch (BotException e) {
				logger.error("Error:" + e, e);
			} catch (Exception e) {
				logger.error("Exception in SheriffBot.", e);
				if (e instanceof IOException)
					quit();
			}
		}
	}

	private void updateFilelists() {
		if (!isConnected() || isUpdateInProgress)
			return;

		if (uth.getState() == Thread.State.RUNNABLE)
			try {
				uth.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			isUpdateInProgress = true;
			listUpdatedAtleastOnce = true;
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

			String filename = _hubname + "-" + sdf.format(cal.getTime());
			logger.debug("Filelist Refresh Started.");
			try {
				BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(_filelistDir + filename + ".bz2"));
				bout.write(("BZ").getBytes("UTF-8"));
				CBZip2OutputStream bos = new CBZip2OutputStream(bout);
				bos.write((sdf.format(cal.getTime()) + "\n").getBytes("UTF-8"));
				bos.write((_hubname + "\n").getBytes("UTF-8"));
				bos.write(("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n").getBytes("UTF-8"));

				User[] users = GetAllUsers();
				if (users != null) {
					bos.write(("<DumpListing>\n").getBytes("UTF-8"));
					for (User user : users) {
						String reason = null;
						boolean skipuser = false;
						if (user.sharesize().equals("0")) {
							reason = "share is 0 (" + user.sharesize() + ")";
							skipuser = true;
						} else if (user.username().equalsIgnoreCase(_botname)) {
							reason = "is me";
							skipuser = true;
						}

						if (skipuser) {
							logger.info("Skipping user " + user.username() + ". Reason: " + reason);
							continue;
						}
						byte b[] = getFileListBytes(user, DUEntity.NO_SETTING);
						if (spareUsers.isEmpty() || spareUsers.indexOf(user.getUserIP()) == -1) {
							if (b != null) {
								bos.write(b, 0, b.length);
								b = null;
							}
						}
						b = null;
						System.gc();
						Thread.yield();
					}
					bos.write(("\n</DumpListing>\n").getBytes("UTF-8"));
				}
				bos.close();
			} catch (FileNotFoundException fne) {
				logger.error("File not found.", fne);
			} catch (IOException ioe) {
				logger.error("IOException", ioe);
			} finally {
				isUpdateInProgress = false;
				logger.debug("Filelist Refresh Finished.");
			}
	}

	private boolean authorized(String user) {
		String ip = authorizedUsers.get(user.toLowerCase());
		if (ip != null && ip.equals(_ip.getHostAddress()))
			return true;
		else
			return false;
	}

	public void onJoin(String user) {
		if (indexedUsers.indexOf(user) == -1) {
			users2Index.add(user);
			uth.process();
		}
	}

	public byte[] getFileListBytes(User user, int settings) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			user.downloadFileList(bos, settings);
		} catch (BotException e1) {
			logger.error(e1.getMessage(), e1);
			return null;
		}

		int in;
		logger.info("Waiting for file list to finish downloading of user " + user.username());
		while ((in = downloadedUsers.indexOf(user.username())) == -1 && UserExist(user.username()) && !isShuttingDown) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (in != -1) {
			downloadedUsers.remove(in);
			byte b[] = bos.toByteArray();
			/*
			 * Trimming the first line of the file list which is
			 * <?xml version="1.0" encoding="utf-8" standalone="yes"?>\n
			 */
			byte b2[] = new byte[b.length - 57];
			System.arraycopy(b, 57, b2, 0, b2.length);
			b = null;
			bos.reset();
			return b2;
		} else
			return null;
	}

	protected void onDownloadComplete(User user, DUEntity due, boolean sucess, BotException e) {
		logger.info("File list downloaded of user " + user.username() + ". Download was sucessful = " + sucess);
		downloadedUsers.add(user.username());
	}

	private class UserListDownloadThread extends Thread {
		private volatile boolean halt = false;

		public UserListDownloadThread() {
			super("UserListDownloadThread");
		}

		public void run() {
			while (!halt) {
				while (!users2Index.isEmpty() && !halt && !(!listUpdatedAtleastOnce || !isConnected() || isUpdateInProgress)) {
					String user = users2Index.get(0);
					if (indexedUsers.indexOf(user) == -1 && !user.equals(_botname)) {
						updateIndivisualUserList(user);
						indexedUsers.add(user);
					}
					users2Index.remove(0);
				}
				if (halt)
					return;
				try {
					Thread.sleep(1 * 60 * 60 * 1000);
				} catch (InterruptedException e) {}
			}
		}

		private void updateIndivisualUserList(String user) {
			logger.debug("Indivisual User FileList update Started.");

			try {
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
				String filename = _hubname + "_" + user + "-" + sdf.format(cal.getTime());

				byte b[] = getFileListBytes(getUser(user), DUEntity.NO_SETTING);
				if (spareUsers.isEmpty() || spareUsers.indexOf(getUser(user).getUserIP()) == -1) {
					BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(_filelistDir + filename + ".bz2"));
					bout.write(("BZ").getBytes("UTF-8"));
					CBZip2OutputStream bos = new CBZip2OutputStream(bout);
					bos.write((sdf.format(cal.getTime()) + "\n").getBytes("UTF-8"));
					bos.write((_hubname + "\n").getBytes("UTF-8"));
					bos.write(("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n").getBytes("UTF-8"));
					if (b != null) {
						bos.write(b, 0, b.length);
						b = null;
					}
					b = null;
					bos.close();
				}
				System.gc();
				Thread.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
			logger.debug("Indivisual User FileList update Finished.");
		}

		public void stopIt() {
			halt = true;
			this.interrupt();
		}

		public void process() {
			this.interrupt();
		}
	}

	public static void main(String[] args) {
		System.setErr(System.out);
		try {
			if (args.length == 0)
				new SheriffBot();
			else if (args.length == 9) {
				new SheriffBot(args[0], Integer.parseInt(args[1]), args[7], args[6], args[2], Integer.parseInt(args[3]), Integer
						.parseInt(args[4]), args[5], Boolean.parseBoolean(args[8]));
			} else
				System.out
				.println("Wrong number of arguments.\n"
						+ "Accepted arguments are:\n"
						+ "none or\n"
						+ "HubIP HubPort TheIPOnWhichTheBotIsRunning ThePortOnWhichTheBotShouldListen "
						+ "ThePortToListenForUDPPackets TheDirectoryWhereFileListsShouldBeSaved BotPass ShareSize IsActive(give true/false)");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
