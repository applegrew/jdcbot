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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

import javax.imageio.IIOException;

import org.elite.jdcbot.framework.BotException;
import org.elite.jdcbot.framework.User;
import org.elite.jdcbot.framework.jDCBot;
import org.elite.jdcbot.shareframework.FLDir;
import org.elite.jdcbot.shareframework.FLFile;
import org.elite.jdcbot.shareframework.FLInterface;
import org.elite.jdcbot.shareframework.FileListManager;
import org.elite.jdcbot.shareframework.HashException;
import org.elite.jdcbot.shareframework.SearchResultSet;
import org.elite.jdcbot.shareframework.SearchSet;
import org.elite.jdcbot.shareframework.ShareManagerListener;
import org.elite.jdcbot.util.GlobalFunctions;

/**
 * Created on 11-Jun-08
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class DemoBot extends jDCBot implements ShareManagerListener {
    private String ownerNick;//User with this nick is authorized to command DemoBot.

    private String hubIP;

    private String pBuf;
    private int maxLines = 23;
    private PmPrinter pmlog = new PmPrinter();
    private boolean isShuttingDown = false;
    private boolean mute = false;
    private Vector<File> includes = new Vector<File>();
    private Vector<File> excludes = new Vector<File>();
    private Vector<String> removes = new Vector<String>();

    public DemoBot(String mastersNick, String hubsIP) {
	super("DemoBot", //Bot's name
		"127.0.0.1", //Bot's IP
		9000, //Bot's listen port
		10000, //Bot's listen port for UDP packets
		"", //Password
		"A jDCBot Demo Bot", //Description
		"LAN(T1)1", //Connection type
		"", //Email
		"0", //Share size in bytes
		3, //No. of upload slots
		3, //No of download slots.
		false, //Is passive
		System.out //PrintStream where debug messages will go
	);

	ownerNick = mastersNick;
	hubIP = hubsIP;

	try {
	    shareManager.setDirs("settings", "settings/downloadedFileLists");
	} catch (IIOException e1) {
	    e1.printStackTrace(pmlog);
	} catch (FileNotFoundException e1) {
	    e1.printStackTrace(pmlog);
	}
	try {
	    shareManager.init();
	} catch (ClassNotFoundException e1) {
	    e1.printStackTrace(pmlog);
	} catch (InstantiationException e1) {
	    e1.printStackTrace(pmlog);
	} catch (IOException e1) {
	    e1.printStackTrace(pmlog);
	}
	shareManager.addListener(this);

	this._sharesize = String.valueOf(shareManager.getOwnShareSize(false));

	try {
	    connect(hubIP, 1411); //Connecting to hub
	} catch (BotException e) {
	    e.printStackTrace(pmlog);
	    terminate();
	} catch (IOException e) {
	    e.printStackTrace(pmlog);
	    terminate();
	}
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
		msg = o + "\nThere are more line(s). Use +more to view more.";
	    } else {
		pBuf = "";
		msg = o + msg;
	    }
	}

	final int maxLen = 20000; //YnHub has a restriction of allowing maximum 32768 bytes in PM messages, but in reality it allows even less.
	try {
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
	} catch (IOException e) {
	    e.printStackTrace(log);
	}
    }

    private void pmMaster(String msg) {
	if (UserExist(ownerNick))
	    pm(ownerNick, msg);
    }

    @Override
    protected void onPrivateMessage(String user, String msg) {
	msg = msg.trim();
	if (authorized(user)) {
	    try {
		if (msg.toLowerCase().startsWith("+quit")) {
		    terminate();

		} else if (msg.toLowerCase().startsWith("+getfl ")) {
		    String usr = msg.substring(msg.indexOf(' ')).trim();
		    if (!UserExist(usr)) {
			pmMaster(usr + " doesn't exist.");
			return;
		    }
		    try {
			shareManager.downloadOthersFileList(getUser(usr));
		    } catch (BotException e) {
			e.printStackTrace(pmlog);
		    }

		} else if (msg.toLowerCase().startsWith("+search ")) {
		    String term = msg.substring(msg.indexOf(' ')).trim();
		    SearchSet s = new SearchSet();
		    s.string = term;
		    try {
			Search(s);
		    } catch (IOException e) {
			e.printStackTrace(pmlog);
			pmMaster("Got IOException: " + e.getMessage());
		    }

		} else if (msg.toLowerCase().startsWith("+more")) {
		    showMore();

		} else if (msg.toLowerCase().startsWith("+setscrnlen ")) {
		    maxLines = Integer.parseInt(msg.substring(msg.indexOf(' ')).trim());

		} else if (msg.toLowerCase().startsWith("+rebuild")) {
		    try {
			shareManager.rebuildFileList();
		    } catch (FileNotFoundException e1) {
			e1.printStackTrace(pmlog);
		    } catch (IOException e1) {
			e1.printStackTrace(pmlog);
		    }

		} else if (msg.toLowerCase().startsWith("+addshare ")) {
		    String m = msg.substring(msg.indexOf(' ')).trim();
		    if (m.equalsIgnoreCase("clean")) {
			includes.removeAllElements();
			pmMaster("includes cleaned.");
		    } else {
			File f = new File(m);
			includes.add(f);
		    }

		} else if (msg.toLowerCase().startsWith("+excludeshare ")) {
		    String m = msg.substring(msg.indexOf(' ')).trim();
		    if (m.equalsIgnoreCase("clean")) {
			excludes.removeAllElements();
			pmMaster("excludes cleaned.");
		    } else {
			File f = new File(m);
			excludes.add(f);
		    }

		} else if (msg.toLowerCase().startsWith("+commitshare")) {
		    shareManager.addShare(includes, excludes, new FilenameFilter() {
			public boolean accept(File dir, String name) {
			    if ((GlobalFunctions.isWindowsOS() && new File(dir + File.separator + name).isHidden())
				    || (!GlobalFunctions.isWindowsOS() && name.startsWith("."))) //Not sharing hidden files.
				return false;
			    else
				return true;
			}
		    });

		    shareManager.removeShare(removes);

		} else if (msg.toLowerCase().startsWith("+removeshare ")) {
		    String m = msg.substring(msg.indexOf(' ')).trim();
		    if (m.equalsIgnoreCase("clean")) {
			removes.removeAllElements();
			pmMaster("removes cleaned.");
		    } else {
			removes.add(m);
		    }

		} else if (msg.toLowerCase().startsWith("+hashstat")) {
		    pmMaster("\nHashing: " + shareManager.getCurrentlyHashedFileName() + "\n%Completion: "
			    + shareManager.getPercentageHashCompletion() + "%\nHashing speed: "
			    + GlobalFunctions.trimDecimals(shareManager.getHashingSpeed() / 1024 / 1024, 2) + " MBps");

		} else if (msg.toLowerCase().startsWith("+printtree ")) {
		    String usr = msg.substring(msg.indexOf(' ')).trim();
		    if (usr.equalsIgnoreCase("$own"))
			pmMaster(shareManager.getOwnFileListManager().getFilelist().printTree());
		    else {
			FileListManager flm = shareManager.getOthersFileListManager(getUser(user));
			if (flm == null)
			    pmMaster("The file list is not available. Use '+getfl " + usr + "' to download it first.");
			else
			    pmMaster(flm.getFilelist().printTree());
		    }

		} else if (msg.toLowerCase().startsWith("+limit ")) {
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
			    User u = getUser(msg.substring(spc).trim());
			    if (u == null)
				pmMaster("User " + msg.substring(spc).trim() + " not found.");
			    else
				shareManager.getUploadStreamManager().setUploadTransferLimit(u, speed);
			}
		    }

		} else if (msg.toLowerCase().startsWith("+mute")) {
		    if (mute) {
			mute = false;
			pmMaster("UNMuted.");
		    } else {
			pmMaster("Muted.");
			mute = true;
		    }

		} else if (msg.toLowerCase().startsWith("+getflnode ")) {
		    int spc1 = msg.indexOf(' ');
		    int spc2 = msg.substring(spc1).trim().indexOf(' ');
		    String usr = msg.substring(spc1, spc2).trim();
		    String path = msg.substring(spc2).trim();
		    boolean bailOut = false;
		    FLInterface flf = null;
		    if (usr.equalsIgnoreCase("$own"))
			flf = shareManager.getOwnFileListManager().getFilelist().getChildInTree(FLDir.getDirNamesFromPath(path), false);
		    else {
			User u = getUser(usr);
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
			    pmMaster("\nInformation about FLFile\nName: " + f.name + "\nHash: " + f.hash + "\nTimestamp: " + f.lastModified
				    + "\nActual Path: " + f.path + "\nSize: " + f.size + "\nIs Shared: " + f.shared);
			} else {
			    FLDir d = (FLDir) flf;
			    pmMaster("\nInformation about FLDir\nName: " + d.getName() + "\nSize: " + d.getSize(false));
			}
		    }

		} else if (msg.toLowerCase().startsWith("+savefl ")) {
		    String usr = msg.substring(msg.indexOf(' ')).trim();
		    if (usr.equalsIgnoreCase("$own"))
			shareManager.saveOwnFL();
		    else
			shareManager.saveOthersFL(getUser(usr));

		} else if (msg.toLowerCase().startsWith("+searchfl ")) {
		    int spc1 = msg.indexOf(' ');
		    int spc2 = msg.substring(spc1).trim().indexOf(' ');
		    String usr = msg.substring(spc1, spc2).trim();
		    String term = msg.substring(spc2).trim();
		    SearchSet s = new SearchSet();
		    s.string = term;

		    boolean bailOut = false;

		    Vector<SearchResultSet> res = null;
		    if (usr.equalsIgnoreCase("$own"))
			res = shareManager.getOwnFileListManager().search(s, 0, false);
		    else {
			User u = getUser(usr);
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

		} else if (msg.toLowerCase().startsWith("+help")) {
		    showHelp();
		} else
		    pmMaster("Unknown command. Use +help for commands and their syntaxes.");
	    } catch (Exception e) {
		e.printStackTrace(pmlog);
	    }
	} else
	    pm(user, user + " you are not authorized to command me.");
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
		+ "\n"
		+ "The commands:-\n"
		+ "+quit - Asks bot to quit the hub and terminate.\n"
		+ "+getfl <username> - Asks the bot the download the filelist of <username>. This file list will then be printed as tree.\n"
		+ "+search <term> - Searches for <term> in the hub.\n"
		+ "+setscrnlen <number> - Sets the number of lines your DC clien't PM window can show at a time. Default is 23.\n"
		+ "+more - Displays more lines that hasn't been displayed so that your DC client's PM window do not overflow with PM messages.\n"
		+ "+rebuild - Rebuilds the bot's file list. After adding a share this is automatically triggere and hence is not usually required.\n"
		+ "+addshare <path> - Schedules the path to file or directory to be added to bot's share list. It actually adds this to a 'includes' list and doesn't actually share it, yet.\n"
		+ "+excludeshare <path> - Schedules the path to file or directory to be excluded from 'includes' list.\n"
		+ "+removeshare <path> - Schedules the path to be removed from share. It simply adds the share to 'removes' list and doesn't actually commit it.\n"
		+ "+commitshare - This is the command which actually adds the files/directories in resultant 'includes' list to the bot' share. Just after this command hashing is started. Also all share in 'removes' list are now actually removed.\n"
		+ "+hashstat  - Gives the current hashing statistics.\n"
		+ "+printtree ($own|<username>) - This command will print the file list in 'tree' form. To print bot's own file list use '$own' else give the other user's username.\n"
		+ "+limit (hash|upload|uploadUser) <speed> [<username>] - This allows you to limit hashing speed or upload transfer rate or upload transfer to a particular user (using uploadUser option). uploadUser option requires the <username> of the user. All speeds in MBps.\n"
		+ "+mute - Sometimes the bot generates a lot of messages. You can mute it using this command. Issue this command again to unmute.\n"
		+ "+getflnode ($own|<username>) <path> - Displays information about any directory or file in the filelist of bot's (if $own is given) or other user's.\n"
		+ "+savefl ($own|<username>) - Saves the file list of bot's ($own) or other users to hard disk. When a file list is downloaded then it is kept in RAM only. It is lost on exit. Only bot's own file list is automatically saved on exit.\n"
		+ "+searchfl ($own|<username>) <term> - Searches for term in the bot's own file list or downloaded file lists of other users.\n");
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
    public void onSearchResult(String senderNick, String senderIP, int senderPort, SearchResultSet result, int free_slots, int total_slots,
	    String hubName) {
	String out =
		"Received search result from " + senderNick + " (" + senderIP + ":" + senderPort + ")\n" + "Slots: " + free_slots + "/"
			+ total_slots + "\nResult: " + result.name + (result.isDir ? "" : " (TTH: " + result.TTH + ") ")
			+ (!result.isDir ? "\nSize: " + GlobalFunctions.trimDecimals(result.size / 1024 / 1024, 3) + "MB" : "")
			+ "\nAnd it is a " + (result.isDir ? "directory" : "file" + ".");
	pmMaster(out);
    }

    @Override
    public void onDisconnect() {
	if (!isShuttingDown)
	    log.println("Disconnected from hub");
	while (!isShuttingDown && !isConnected()) {
	    // Try to reconnect to the hub after waiting for sometime.
	    try {
		Thread.sleep(6000); // Waiting for 6s
		log.println("Reconnecting to hub...");
		connect(hubIP, _port);

	    } catch (BotException e) {
		log.println("Error:" + e);
		e.printStackTrace();
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
    public void hashingOfFileSkipped(String f, String reason) {}

    @Override
    public void onMiscMsg(String msg) {}

    private class PmPrinter extends PrintStream {
	public PmPrinter() {
	    super(log);
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

    public static void main(String args[]) {
	if (args.length < 2)
	    new DemoBot("agApple", "127.0.0.1");
	else
	    new DemoBot(args[0], args[1]);
    }

}
