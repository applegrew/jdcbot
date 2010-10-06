/*
 * DemoBot.java
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.IIOException;

import org.apache.tools.bzip2.CBZip2OutputStream;
import org.elite.jdcbot.framework.BotException;
import org.elite.jdcbot.framework.DUEntity;
import org.elite.jdcbot.framework.DownloadCentral;
import org.elite.jdcbot.framework.DownloadQEntry;
import org.elite.jdcbot.framework.EventjDCBotAdapter;
import org.elite.jdcbot.framework.GlobalObjects;
import org.elite.jdcbot.framework.JMethod;
import org.elite.jdcbot.framework.MultiHubsAdapter;
import org.elite.jdcbot.framework.User;
import org.elite.jdcbot.framework.jDCBot;
import org.elite.jdcbot.shareframework.FLDir;
import org.elite.jdcbot.shareframework.FLFile;
import org.elite.jdcbot.shareframework.FLInterface;
import org.elite.jdcbot.shareframework.FileListManager;
import org.elite.jdcbot.shareframework.HashException;
import org.elite.jdcbot.shareframework.SearchResultSet;
import org.elite.jdcbot.shareframework.SearchSet;
import org.elite.jdcbot.shareframework.ShareManager;
import org.elite.jdcbot.shareframework.ShareManagerListener;
import org.elite.jdcbot.util.GlobalFunctions;
import org.elite.jdcbot.util.OutputEntityStream;
import org.slf4j.Logger;

/**
 * Created on 11-Jun-08<br>
 * This class acts as demo to show you how to
 * use jDCBot framework by implementing a near
 * full-fledged client. It doesn't have
 * user interface. The only way to interact
 * with it is sending commands to it by
 * private message using another DC client.
 * It will accept commands from only one
 * authorized user. Send help command as
 * private message to the DemoBot to get
 * a list of commands.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1.2
 */
public class DemoBot extends EventjDCBotAdapter implements ShareManagerListener {
	private static final Logger logger = GlobalObjects.getLogger(DemoBot.class);
	private String ownerNick = null; //User with this nick is authorized to command DemoBot.

	/**
	 * The first instance launched by
	 * void main() acts as mater, i.e.
	 * it is the only one which will respond
	 * to user's commands. Slaves do not respond to
	 * user's commands (atleast directly). 
	 */
	private boolean isSlave = true;

	private MultiHubsAdapter multiHubsAdapter = null;
	private String pBuf;
	private int maxLines = 23;
	private PmPrinter pmlog = new PmPrinter();
	private boolean isShuttingDown = false;
	private boolean mute = false;
	private List<File> includes = new ArrayList<File>();
	private List<File> excludes = new ArrayList<File>();
	private List<String> removes = new ArrayList<String>();

	public DemoBot(String mastersNick, MultiHubsAdapter mha) throws IOException {
		super(mha);

		ownerNick = mastersNick;
		addListener(this); //All instances listen for themselves, but some events will be handled by master instance only.
	}

	private boolean authorized(String user) {
		if (user.equalsIgnoreCase(ownerNick))
			return true;
		else
			return false;
	}

	private void showMore() {
		pm(ownerNick, pBuf);
	}

	private void pm(String user, String msg) {
		if (mute)
			return;
		msg = "\n" + msg;
		if (maxLines > 0) {
			pBuf = msg;
			String o = "";
			int lc = 0;
			do {
				int i = msg.indexOf('\n');
				if (i == -1)
					i = msg.indexOf('\r');
				if (i == -1) {
					break;
				}
				o = o + msg.substring(0, i + 1);
				lc++;
				if (i + 1 < msg.length())
					msg = msg.substring(i + 1);
				else
					msg = "";
			} while (lc < maxLines && !msg.isEmpty());
			if (lc >= maxLines) {
				pBuf = pBuf.substring(o.length());
				msg = o + "\nThere are more line(s). PM me 'more' (without quotes ofcourse) to view more.";
			} else {
				pBuf = "";
				msg = o + msg;
			}
		}

		final int maxLen = 20000; //YnHub has a restriction of allowing maximum 32768 bytes in PM messages, but in reality it allows even less.
		int len;
		do {
			len = msg.getBytes().length - maxLen;
			String m;
			if (len <= 0)
				m = msg;
			else {
				byte[] b = new byte[maxLen];
				System.arraycopy(msg.getBytes(), 0, b, 0, b.length);
				m = new String(b);
				b = new byte[len];
				System.arraycopy(msg.getBytes(), maxLen, b, 0, b.length);
				msg = msg.substring(maxLen);
			}
			SendPrivateMessage(user, m);
		} while (len > 0);
	}
	
	public void onSendCommandFailed(String msg, Throwable e, JMethod m) {
		if(m.equals(JMethod.PRIVATE_MSG))
			logger.error("Exception in pm()", e);
	}

	private void pmMaster(String msg) {
		if (UserExist(ownerNick))
			pm(ownerNick, msg);
	}

	public User getUserFromAnyHub(String usr) {
		if (!multiHubsAdapter.UserExist(usr)) {
			pmMaster(usr + " doesn't exist.");
			return null;
		}
		List<User> u = multiHubsAdapter.getUsers(usr);
		if (u.size() > 1)
			pmMaster("Multiple hubs have user with the this name. Getting user from " + u.get(0).getHubSignature() + ".");
		return u.get(0);
	}

	@Override
	public void on_PrivateMessage(jDCBot src, String user, String msg) {
		if (isSlave)
			pm(user, "This bot is a slave instance and hence is not answerable to commands.");
		else {
			msg = msg.trim();
			if (authorized(user)) {
				try {
					if (msg.toLowerCase().equals("quit")) {
						multiHubsAdapter.terminate();

					} else if (msg.toLowerCase().startsWith("getfl ")) {
						String usr = msg.substring(msg.indexOf(' ')).trim();
						try {
							User u = getUserFromAnyHub(usr);
							if (u != null)
								shareManager.downloadOthersFileList(u);
						} catch (BotException e) {
							e.printStackTrace(pmlog);
						}

					} else if (msg.toLowerCase().startsWith("search ")) {
						String term = msg.substring(msg.indexOf(' ')).trim();
						SearchSet s = new SearchSet();
						s.string = term;
						try {
							multiHubsAdapter.Search(s);
						} catch (IOException e) {
							e.printStackTrace(pmlog);
							pmMaster("Got IOException: " + e.getMessage());
						}

					} else if (msg.toLowerCase().equals("more")) {
						showMore();

					} else if (msg.toLowerCase().startsWith("setscrnlen ")) {
						maxLines = Integer.parseInt(msg.substring(msg.indexOf(' ')).trim());

					} else if (msg.toLowerCase().equals("rebuild")) {
						try {
							shareManager.rebuildFileList();
						} catch (FileNotFoundException e1) {
							e1.printStackTrace(pmlog);
						} catch (IOException e1) {
							e1.printStackTrace(pmlog);
						}

					} else if (msg.toLowerCase().startsWith("addshare ")) {
						String m = msg.substring(msg.indexOf(' ')).trim();
						if (m.equalsIgnoreCase("$clean")) {
							includes.clear();
							pmMaster("includes cleaned.");
						} else {
							File f = new File(m);
							includes.add(f);
							pmMaster(m + " added to includes list.");
						}

					} else if (msg.toLowerCase().startsWith("excludeshare ")) {
						String m = msg.substring(msg.indexOf(' ')).trim();
						if (m.equalsIgnoreCase("$clean")) {
							excludes.clear();
							pmMaster("excludes cleaned.");
						} else {
							File f = new File(m);
							excludes.add(f);
							pmMaster(m + " added to excludes list.");
						}

					} else if (msg.toLowerCase().equals("commitshare")) {
						shareManager.addShare(new ArrayList<File>(includes), new ArrayList<File>(excludes), new FilenameFilter() {
							public boolean accept(File dir, String name) {
								if ((GlobalFunctions.isWindowsOS() && new File(dir + File.separator + name).isHidden())
										|| (!GlobalFunctions.isWindowsOS() && name.startsWith("."))) //Not sharing hidden files.
									return false;
								else
									return true;
							}
						}, null);

						shareManager.removeShare(new ArrayList<String>(removes));

						includes.clear();
						excludes.clear();
						removes.clear();

					} else if (msg.toLowerCase().startsWith("removeshare ")) {
						String m = msg.substring(msg.indexOf(' ')).trim();
						if (m.equalsIgnoreCase("$clean")) {
							removes.clear();
							pmMaster("removes cleaned.");
						} else {
							removes.add(m);
							pmMaster(m + " added to removes list.");
						}

					} else if (msg.toLowerCase().equals("hashstat")) {
						pmMaster("\nHashing: " + shareManager.getCurrentlyHashedFileName() + "\n%Completion: "
								+ shareManager.getPercentageHashCompletion() + "%\nHashing speed: "
								+ GlobalFunctions.trimDecimals(shareManager.getHashingSpeed() / 1024 / 1024, 2) + " MBps\nTime Remaining: "
								+ GlobalFunctions.trimDecimals(shareManager.getTimeLeft2CompleteHashing() / 60, 2) + " min(s)");

					} else if (msg.toLowerCase().startsWith("printtree ")) {
						String usr = msg.substring(msg.indexOf(' ')).trim();
						if (usr.equalsIgnoreCase("$own"))
							pmMaster(shareManager.getOwnFileListManager().getFilelist().printTree());
						else {
							FileListManager flm = shareManager.getOthersFileListManager(getUserFromAnyHub(usr));
							if (flm == null)
								pmMaster("The file list is not available. Use '+getfl " + usr + "' to download it first.");
							else
								pmMaster(flm.getFilelist().printTree());
						}

					} else if (msg.toLowerCase().startsWith("limit ")) {
						int spc = msg.indexOf(' ');
						msg = msg.substring(spc).trim();
						spc = msg.indexOf(' ');
						String what = msg.substring(0, spc).trim().toLowerCase();
						msg = msg.substring(spc).trim();
						spc = msg.indexOf(' ');
						double speed = Double.parseDouble(msg.substring(0, spc == -1 ? msg.length() : spc).trim()) * 1024 * 1024;
						if (what.equals("hash"))
							shareManager.setMaxHashingSpeed(speed);
						else if (what.equals("upload"))
							shareManager.getUploadStreamManager().setUploadTransferLimit(speed);
						else if (what.equals("uploaduser")) {
							if (spc == -1)
								pmMaster("No username given.");
							else {
								User u = getUserFromAnyHub(msg.substring(spc).trim());
								if (u == null)
									pmMaster("User " + msg.substring(spc).trim() + " not found.");
								else
									shareManager.getUploadStreamManager().setUploadTransferLimit(u, speed);
							}
						} else if (what.equals("download"))
							downloadCentral.setTransferRate(speed);

					} else if (msg.toLowerCase().equals("mute")) {
						if (mute) {
							mute = false;
							pmMaster("UNMuted.");
						} else {
							pmMaster("Muted.");
							mute = true;
						}

					} else if (msg.toLowerCase().startsWith("getflnode ")) {
						int spc1 = msg.indexOf(' ');
						int spc2 = msg.substring(spc1).trim().indexOf(' ');
						String usr = msg.substring(spc1, spc2).trim();
						String path = msg.substring(spc2).trim();
						boolean bailOut = false;
						FLInterface flf = null;
						if (usr.equalsIgnoreCase("$own"))
							flf = shareManager.getOwnFileListManager().getFilelist().getChildInTree(FLDir.getDirNamesFromPath(path), false);
						else {
							User u = getUserFromAnyHub(usr);
							if (u == null) {
								pmMaster("User " + usr + " not found.");
								bailOut = true;
							} else {
								FileListManager flm = shareManager.getOthersFileListManager(u);
								if (flm == null) {
									pmMaster("Users file list has not been downloaded yet use '+getfl " + usr + "' command.");
									bailOut = true;
								} else
									flf = flm.getFilelist().getChildInTree(FLDir.getDirNamesFromPath(path), false);
							}
						}
						if (!bailOut) {
							if (flf == null)
								pmMaster("No such path exists.");
							else if (flf instanceof FLFile) {
								FLFile f = (FLFile) flf;
								pmMaster("\nInformation about FLFile\nName: " + f.name + "\nHash: " + f.hash + "\nTimestamp: "
										+ f.lastModified + "\nActual Path: " + f.path + "\nSize: " + f.size + "\nIs Shared: " + f.shared);
							} else {
								FLDir d = (FLDir) flf;
								pmMaster("\nInformation about FLDir\nName: " + d.getName() + "\nSize: " + d.getSize(false));
							}
						}

					} else if (msg.toLowerCase().startsWith("searchfl ")) {
						int spc1 = msg.indexOf(' ');
						int spc2 = msg.substring(spc1).trim().indexOf(' ');
						String usr = msg.substring(spc1, spc2).trim();
						String term = msg.substring(spc2).trim();
						SearchSet s = new SearchSet();
						s.string = term;

						boolean bailOut = false;

						List<SearchResultSet> res = null;
						if (usr.equalsIgnoreCase("$own"))
							res = shareManager.getOwnFileListManager().search(s, 0, false);
						else {
							User u = getUserFromAnyHub(usr);
							if (u == null) {
								pmMaster("The user doesn't exist.");
								bailOut = true;
							} else {
								FileListManager flm = shareManager.getOthersFileListManager(u);
								if (flm == null) {
									pmMaster("The file of this user has yet not been downloaded.");
									bailOut = true;
								} else
									res = flm.search(s, 0, false);
							}
						}
						if (!bailOut) {
							String out = "Search Results:-\n";
							for (SearchResultSet result : res) {
								out = out + "\n";
								out =
									out
									+ "Result: "
									+ result.name
									+ (result.isDir ? "" : " (TTH: " + result.TTH + ") ")
									+ (!result.isDir ? "\nSize: " + GlobalFunctions.trimDecimals(result.size / 1024 / 1024, 3)
											+ "MB" : "") + "\nAnd it is a " + (result.isDir ? "directory" : "file" + ".\n");
							}
							pmMaster(out);
						}

					} else if (msg.toLowerCase().equals("help")) {
						showHelp();

					} else if (msg.toLowerCase().startsWith("connect ")) {
						String hubAdd = msg.substring(msg.indexOf(' ')).trim();
						String hubIP = hubAdd;
						int hubPort = 411;
						int cp = hubAdd.indexOf(':');
						if (cp != -1 && cp != hubAdd.length() - 1)
							hubPort = Integer.parseInt(hubAdd.substring(cp + 1));
						if (cp != -1)
							hubIP = hubAdd.substring(0, cp);
						DemoBot newBot = new DemoBot(null, multiHubsAdapter);
						newBot.addListener(this); //Now all events generated there will be routed to this instance too, i.e. the master instance.
						multiHubsAdapter.connect(hubIP, hubPort, newBot);

					} else if (msg.toLowerCase().startsWith("download ") || msg.toLowerCase().startsWith("dstat ")
							|| (msg.toLowerCase().startsWith("cancel ")) && msg.toLowerCase().substring(6).trim().startsWith("download ")) {

						boolean download = msg.toLowerCase().startsWith("download ");
						boolean dstat = msg.toLowerCase().startsWith("dstat ");
						msg = msg.substring(msg.indexOf(' ')).trim();

						Query Q[] = getSegmentedQuery(msg.substring(msg.indexOf('?') + 1));
						if (Q == null) {
							pmMaster("Error! Maybe the URI is not in proper format");
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
									pmMaster("Please enter a valid magnet uri. Error occured while trying to parse file size");
									return;
								}
							} else if (q.query.equalsIgnoreCase("dn")) {
								name = q.value.replace('+', ' ');
							}
						}

						if (size <= 0) {
							pmMaster("Invalid value of file size: " + size + ". Make sure you have entered a valid magnet uri.");
							return;
						}
						if (tth == null || name == null) {
							pmMaster("Error occured during parsing the magnet uri. Make sure this is valid.");
							return;
						}

						if (download) {//Start download
							File file = new File(name);
							if (file.exists()) {
								pmMaster("Cannot download. A file with this name already exists in download directory.");
								return;
							}

							try {
								downloadCentral.download(tth, true, size, file, null);
							} catch (BotException e) {
								e.printStackTrace(pmlog);
							}

						} else if (dstat) { //Show download stat
							DownloadQEntry dqe = downloadCentral.getStats(tth);
							if (dqe == null)
								pmMaster("No stats for " + tth + " found. It means that it is not scheduled for download.");
							else {
								String out = "Download Stats:-\n" + "File: " + tth + "\nDownload State: " + dqe.state + "\nSources:";
								for (User u : dqe.srcsByUser)
									out = out + "\n\t" + u.username();
								for (String cid : dqe.srcsByCID)
									out = out + "\n\t" + cid + " (Client ID, Maybe User is Offline)";

								double dspeed = downloadCentral.getDownloadSpeed(tth);
								if (dspeed != -1)
									dspeed = dspeed / 1024 / 1024;
								double tr = downloadCentral.getDownloadTimeRemaining(tth);
								if (tr != -1)
									tr = tr / 60;
								double pc = downloadCentral.getDownloadPercentageCompletion(tth);

								out =
									out + "\nDownload Speed: "
									+ (dspeed == -1 ? "NA" : GlobalFunctions.trimDecimals(dspeed, 2) + " MBps")
									+ "\nPercentage Completion: "
									+ (pc == -1 ? "NA" : GlobalFunctions.trimDecimals(pc, 2) + "%") + "\nTime Remaining: "
									+ (tr == -1 ? "NA" : GlobalFunctions.trimDecimals(tr, 2) + " mins");
								pmMaster(out);
							}

						} else { //Cancel download
							downloadCentral.cancelDownload(tth);
						}

					} else if (msg.toLowerCase().startsWith("cancel ")) {
						msg = msg.substring(msg.indexOf(' ')).trim().toLowerCase();
						if (msg.equals("upload"))
							for (User u : multiHubsAdapter.GetAllUsers())
								u.cancelUpload();
						else if (msg.equals("hash"))
							shareManager.cancelHashing();

					} else if (msg.toLowerCase().startsWith("prune")) {
						shareManager.pruneUnsharedShares();
						pmMaster("Done");

					} else if (msg.toLowerCase().startsWith("hi"))
						pmMaster("Hello, but use help commands to learn about some useful commands to work with.");
					else
						pmMaster("Unknown command. Use help for commands and their syntaxes.");

				} catch (Exception e) {
					e.printStackTrace(pmlog);
				}

			} else
				pm(user, user + " you are not authorized to command me.");
		}
	}

	private Query[] getSegmentedQuery(String query) {
		List<Query> Q = new ArrayList<Query>();
		String qs[] = query.split("&");
		for (String q : qs) {
			String e[] = q.split("=");
			Q.add(new Query(e[0], e.length < 2 ? null : e[1]));
		}
		return Q.toArray(new Query[0]);
	}

	private void showHelp() {
		pmMaster("Commands:-\n"
				+ "\n"
				+ "Syntax notations:\n"
				+ "1) Replace things enclosed in <> by appropriate values and DO replace everything including the < and >.\n"
				+ "\te.g. '<username>' when replaced will given 'Apple' if the username is Apple.\n"
				+ "2) Anything enclosed within [] (square brakets) is optional.\n"
				+ "3) Anything enclosed with () or not enclosed at all are not optional.\n"
				+ "4) Alternatives of a group are speparated by | (pipe). A group is somthing enclosed within () or [], where the later forms an optional group.\n"
				+ "\te.g. (hash|upload|uploadUser) means that either hash or upload or uploadUser must be used and only any one must be used.\n"
				+ "5) The command names are not case sensitive but their arguments could be.\n"
				+ "\n"
				+ "The commands:-\n"
				+ "> quit - Asks bot to quit the hub and terminate.\n"
				+ "> getfl <username> - Asks the bot the download the filelist of <username>. This file list will then be printed as tree.\n"
				+ "> search <term> - Searches for <term> in the hub.\n"
				+ "> setscrnlen <number> - Sets the number of lines your DC clien't PM window can show at a time. Default is 23.\n"
				+ "> more - Displays more lines that hasn't been displayed so that your DC client's PM window do not overflow with PM messages.\n"
				+ "> rebuild - Rebuilds the bot's file list. After adding a share this is automatically triggere and hence is not usually required.\n"
				+ "> addshare ($clean|<path>) - Schedules the path to file or directory to be added to bot's share list. It actually adds this to a 'includes' list and doesn't actually share it, yet. If $clean is used then includes list is cleaned.\n"
				+ "> excludeshare ($clean|<path>) - Schedules the path to file or directory to be excluded from 'includes' list. If $clean is used then excludes list is cleaned.\n"
				+ "> removeshare ($clean|<path>) - Schedules the path to be removed from share. It simply adds the share to 'removes' list and doesn't actually commit it.  If $clean is used then removes list is cleaned.\n"
				+ "> commitshare - This is the command which actually adds the files/directories in resultant 'includes' list to the bot' share. Just after this command hashing is started. Also all share in 'removes' list are now actually removed.\n"
				+ "> hashstat  - Gives the current hashing statistics.\n"
				+ "> printtree ($own|<username>) - This command will print the file list in 'tree' form. To print bot's own file list use '$own' else give the other user's username.\n"
				+ "> limit (hash|upload|uploadUser) <speed> [<username>] - This allows you to limit hashing speed or upload transfer rate or upload transfer to a particular user (using uploadUser option). uploadUser option requires the <username> of the user. All speeds in MBps.\n"
				+ "> mute - Sometimes the bot generates a lot of messages. You can mute it using this command. Issue this command again to unmute.\n"
				+ "> getflnode ($own|<username>) <path> - Displays information about any directory or file in the filelist of bot's (if $own is given) or other user's.\n"
				+ "> searchfl ($own|<username>) <term> - Searches for term in the bot's own file list or downloaded file lists of other users.\n"
				+ "> connect <hub ip>[:[<hub port>]] - This will spawn a new instance of DemoBot which will connect to the other hub. If no port is given the 411 is assumed.\n"
				+ "> download <magnet uri> - It will automatically search for files mathcing this magnet URI and download it.\n"
				+ "> dstat [<magnet uri>] - This simple command simply show the various stats realting the download of file represented by the given magnet URI.\n"
				+ "> cancel (download|upload|hash) [<magnet uri>] - If any file is being downloaded with this magnet URI then it will be cancelled. Arguemtn options 'upload' and 'hash' doesn't requir magnet URI argument. 'upload' will cancel all running uploads and 'hash' will cancel the hashing.\n"
				+ "> prune - When shares are removed then they are actually just hidden, ready to be showed again when they are added again. Use this command to permanently delete them.\n");
	}

	@Override
	public void on_DownloadComplete(jDCBot src, User user, DUEntity due, boolean success, BotException e) {
		if (isSlave)
			return;

		String out =
			"I just now "
			+ (success ? "successfully" : "unsuccessfully")
			+ " completed download of "
			+ due.file()
			+ " from "
			+ user.username()
			+ ". Average transfer rate was "
			+ (due.os() instanceof OutputEntityStream ? GlobalFunctions.trimDecimals(((OutputEntityStream) due.os())
					.getTransferRate() / 1024 / 1024, 2)
					+ " MBps" : "N/A");
		if (!success) {
			out = out + "\nI got this exception: " + e.getMessage();
		}
		pmMaster(out);
	}

	@Override
	public void onFilelistDownloadFinished(User u, boolean success, Exception e) {
		if (success) {
			FileListManager flm = shareManager.getOthersFileListManager(u);
			String out =
				"File list of " + u.username() + "\n" + flm.getFilelist().printTree() + "\nActual Total Share Size = "
				+ GlobalFunctions.trimDecimals(flm.getFilelist().getSize(false) / 1024 / 1024 / 1024, 2) + " GB"
				+ "\nClient ID = " + flm.getFilelist().getCID();
			pmMaster(out);
			//log.println(out);
		} else {
			pmMaster("Download of file list from " + u.username() + " failed. Got the exception: " + e.getMessage());
		}
	}

	@Override
	public void terminate() {
		isShuttingDown = true;
		super.terminate();
	}

	@Override
	public void on_SearchResult(jDCBot src, String senderNick, String senderIP, int senderPort, SearchResultSet result, int free_slots,
			int total_slots, String hubName) {
		if (isSlave)
			return;

		String out =
			"Received search result from " + senderNick + " (" + senderIP + ":" + senderPort + ")\n" + "Slots: " + free_slots + "/"
			+ total_slots + "\nResult: " + result.name + (result.isDir ? "" : " (TTH: " + result.TTH + ") ")
			+ (!result.isDir ? "\nSize: " + GlobalFunctions.trimDecimals(result.size / 1024 / 1024, 3) + "MB" : "")
			+ "\nAnd it is a " + (result.isDir ? "directory" : "file" + ".");
		pmMaster(out);
	}

	@Override
	public void on_Disconnect(jDCBot src) {
		if (src != this) //Handling of on_Disconnect() is instance's own resposibility.
			return;

		if (!isShuttingDown)
			logger.info("Disconnected from hub");
		while (!isShuttingDown && !isConnected()) {
			// Try to reconnect to the hub after waiting for sometime.
			try {
				Thread.sleep(6000); // Waiting for 6s
				logger.info("Reconnecting to hub...");
				connect(_ip, _port);

			} catch (BotException e) {
				logger.error("Error:" + e, e);
			} catch (Exception e) {
				e.printStackTrace();
				if (e instanceof IOException)
					quit();
			}
		}
	}

	@Override
	public void hashingJobFinished() {
		pmMaster("Hashing of all files complete.");
	}

	@Override
	public void hashingOfFileComplete(String f, boolean success, HashException e) {
		pmMaster("Hashing of " + f + " " + (success ? "is complete." : " failed due to exception - " + e.getMessage()));
	}

	@Override
	public void hashingOfFileStarting(String file) {
		pmMaster("Hashing of " + file + " has just started.");
	}

	@Override
	public void hashingOfFileSkipped(String f, String reason) {
		pmMaster("Hashing of " + f + " skipped because - " + reason + ".");
	}

	@Override
	public void onMiscMsg(String msg) {
		pmMaster("Misc error msg: " + msg);
	}

	private class PmPrinter extends PrintStream {
		public PmPrinter() {
			super(System.out);
		}

		public void println() {
			super.println();
			pmMaster("\n");
		}

		public void println(String s) {
			super.println(s);
			pmMaster(s);
		}

		public void println(boolean b) {
			super.println(b);
			pmMaster(String.valueOf(b));
		}

		public void println(int i) {
			super.println(i);
			pmMaster(String.valueOf(i));
		}

		public void println(double d) {
			super.println(d);
			pmMaster(String.valueOf(d));
		}

		public void println(char c) {
			super.println(c);
			pmMaster(String.valueOf(c));
		}

		public void println(char c[]) {
			super.println(c);
			pmMaster(new String(c));
		}
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

	private static class CustomShareManager extends ShareManager {
		private final String banIP = "172.16.";

		private FLDir customFL;
		private String jDCBotFile =
			"\n\n\n\n\n" + "\t\tThis is jDCBot version " + GlobalObjects.VERSION + "\n"
			+ "\t\tGet it at http://jdcbot.sourceforge.net\n\n" + "\t\tBy the way since your IP is like " + banIP + "\n"
			+ "\t\thence you see only this file in the file list.\n" + "\t\tThis is not the real file list.\n"
			+ "\t\tSeeing is not believing. ;-)\n" + "\n\n\n\n\n";
		private byte[] customFLData;

		public CustomShareManager(MultiHubsAdapter mha) {
			super(mha);
			prepareCustomFL();
			prepareCustomFL_XML_BZ2();
		}

		private void prepareCustomFL() {
			customFL = new FLDir("Root", true, null);
			FLDir subDir = new FLDir("jDCBot", false, customFL);
			customFL.addSubDir(subDir);
			FLFile jdc = new FLFile("jDCBot ReadMe.txt", jDCBotFile.getBytes().length, "cache://jDCBot.txt", 1, true, subDir);
			jdc.hash = hashMan.getHash(jDCBotFile);
			subDir.addFile(jdc);
		}

		private void prepareCustomFL_XML_BZ2() {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			try {
				bos.write("BZ".getBytes());
				CBZip2OutputStream cbos = new CBZip2OutputStream(bos);

				writeFL(cbos, customFL);

				cbos.close();
			} catch (IOException e) {
				logger.error("Exception in prepareCustomFL_XML_BZ2()", e);
			}
			customFLData = bos.toByteArray();
		}

		private boolean isBanned(User u) {
			if (u.getUserIP().startsWith(banIP)) //Banning ips like 172.16.*.*
				return true;
			else
				return false;
		}

		@Override
		protected byte[] getCacheFileData(String uri) {
			if (uri.equalsIgnoreCase("jDCBot.txt"))
				return jDCBotFile.getBytes();
			else
				return null;
		}

		@Override
		protected byte[] getVirtualFLData(User u) {
			if (isBanned(u))
				return customFLData;
			else
				return null;
		}

		@Override
		protected FLDir getOwnFL(User u, double certainity) {
			if ((u != null && certainity >= 0.9 && isBanned(u)) || u == null) {
				//Bans uncoditionally if u==null, hence if bot is not Op then no search result will be returned
				//if the searching user had not download file list from this bot.
				return customFL;
			}
			return ownFL.getFilelist();
		}
	}

	public static void main(String args[]) {
		//If insufficient number of command-line arguements are provided then
		//the following values are used. The command-line arguments expected are
		//	owner's_username hubIP hubPort
		String owner = "agApple";
		String hubIP = "127.0.0.1";
		int hubPort = 1411;

		MultiHubsAdapter mha;
		try {
			mha = new MultiHubsAdapter("DemoBot", //Bot's name
					"127.0.0.1", //Bot's IP
					9000, //Bot's listen port
					10000, //Bot's listen port for UDP packets
					"", //Password
					"A jDCBot Demo Bot", //Description
					"LAN(T1)" + User.NORMAL_FLAG, //Connection type
					"", //Email
					"0", //Share size in bytes
					3, //No. of upload slots
					3, //No of download slots.
					false //Is passive
			);

			boolean settingDirsSuccess = false;
			try {
				mha.setDirs("settings", "Incomplete");
				settingDirsSuccess = true;
			} catch (IIOException e1) {
				e1.printStackTrace();
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (!settingDirsSuccess) {
				System.out.println("Setting of directories was not successfull. Aborting.");
				mha.terminate();
				return;
			}

			CustomShareManager shareManager = new DemoBot.CustomShareManager(mha);
			DownloadCentral dc = new DownloadCentral(mha);

			mha.setShareManager(shareManager); //CORRECT
			mha.setDownloadCentral(dc); //CORRECT

			DemoBot db;
			if (args.length < 1)
				db = new DemoBot(owner, mha);
			else
				db = new DemoBot(args[0], mha);
			db.isSlave = false;
			db.multiHubsAdapter = mha;

			shareManager.addListener(db);
			/*
		     * Below two lines placements are WRONG. The ones marked
		     * CORRECT are correct. You need to set ShareManager and
		     * DownloadCentral in mha before 'connect' is called.
		     * This is because DemoBot has already been created and
		     * since mha didn't have this ShareManager or DownloadCentral
		     * associated with it hence DemoBot gets null and not them.
		     * Calling setXXX() method is supposed to assign the said
		     * objects with all the jDCBot instances it has, but
		     * we haven't yet called mha's connect() method so
		     * the above DemoBot instance is still not known to mha,
		     * so effectively db will have null for ShareManager and
		     * DownloadCentral. 
		     */
			//mha.setShareManager(shareManager); WRONG
			//mha.setDownloadCentral(dc); WRONG
			try {
				if (args.length < 3)
					mha.connect(hubIP, hubPort, db); //Connecting to hub with default settings.
				else
					mha.connect(args[1], Integer.parseInt(args[2]), db); //Connecting to hub using command-line parameters.

			} catch (BotException e) {
				e.printStackTrace();
				mha.terminate();
			} catch (IOException e) {
				e.printStackTrace();
				mha.terminate();
			}

		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}

}
