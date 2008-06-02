/*
 * jDCBot.java
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

import java.net.*;
import java.util.StringTokenizer;
import java.io.*;

import org.elite.jdcbot.framework.BotException;
import org.elite.jdcbot.shareframework.ShareManager;

/**
 * jDCBot is a Java framework for writing DC (direct connect) bots easily.
 * <p>
 * It provides an event-driven architecture to handle common DC events.
 * <p>
 * Methods of the jDCBot class can be called to send events to the DC hub that it connects to. For example, calling the SendPublicMessage method will
 * send a public message.
 * <p>
 * To perform an action when the jDCBot receives a normal message from the hub, you would override the onPubicMessage method defined in the jDCBot
 * class. All on<i>XYZ</i> methods in the PircBot class are automatically called when the event <i>XYZ</i> happens, so you would override these if
 * you wish to do something when it does happen.
 * 
 * TODO: Make this handel multiple hubs????
 * 
 * @author Kokanovic Branko
 * @author AppleGrew
 * @since 0.5
 * @version 0.7.2
 */
public abstract class jDCBot extends InputThreadTarget {

    /**
     * The definitive version number of this release of jDCBot.
     **/
    public static final String VERSION = "0.7.1";
    private static final String _protoVersion = "1.0091"; //Version of DC++ protocol being used.

    private InputThread _inputThread = null;
    private BotEventDispatchThread dispatchThread = null;

    protected Socket socket = null;
    protected ServerSocket socketServer = null;
    protected PrintStream log = null;

    InputStream input;
    OutputStream output;

    //BufferedReader	breader;

    protected UserManager um;
    protected DownloadManager downloadManager;
    protected UploadManager uploadManager;
    protected ShareManager shareManager;

    protected String _botname, _password, _description, _conn_type, _email, _sharesize, _hubname;
    protected boolean _passive;
    protected InetAddress _ip;
    protected int _port;

    private final String _hubproto_supports = "NoGetINFO UserIP2 MiniSlots ";
    private final String _clientproto_supports = "MiniSlots ADCGet XmlBZList TTHF ZLIG ";

    private int _maxUploadSlots;
    private int _maxDownloadSlots;

    private String _hubSupports = "";
    private boolean _op = false;
    private String _botIP;
    private int _listenPort;

    //******Constructors******
    /**
     * Constructs a jDCBot with your settings.
     * <p>
     * Most setting here depends on your hub. You might have to fake your share size and/or slots for hub to accept you... For details, look at <a
     * href=http://wiki.dcpp.net/index.php/%24MyINFO>DCPP wiki page of $MyINFO command</a>
     * 
     * 
     * @param botname Name of the bot as it will appear in the list of users.
     * @param botIP Your IP.
     * @param listenPort The port on your computer where jdcbot should listen for incoming connections from clients.
     * @param password Passsword if required, you could put anything if no password is needed.
     * @param description Description of your bot as it will appear in the list of users. On your description is appended standard description.
     * @param conn_type Your connection type, for details look <a href=http://wiki.dcpp.net/index.php/%24MyINFO>here</a>
     * @param email Your e-mail address as it will appear in the list of users.
     * @param sharesize Size of your share in bytes.
     * @param uploadSlots Number of upload slots for other user to connect to you.
     * @param downloadSlots Number of download slots. This has nothing to do with DC++ protocol. This has been given
     * to put an upper cap on no. of simultaneous downloads.
     * @param passive Set this to fals if you are not behind a firewall.
     * @param outputLog <u>Almost</u> all debug messages will be printed in this.
     */
    public jDCBot(String botname, String botIP, int listenPort, String password, String description, String conn_type, String email,
	    String sharesize, int uploadSlots, int downloadSlots, boolean passive, PrintStream outputLog) {
	_botname = botname;
	_password = password;
	// remove this and put
	// _description=description;
	// if you don't hub doesn't require standard description
	_description = description + "<++ V:0.668,M:" + (passive ? 'P' : 'A') + ",H:1/0/0,S:" + uploadSlots + ">";
	_conn_type = conn_type;
	_email = email;
	_sharesize = sharesize;
	_maxUploadSlots = uploadSlots;
	_maxDownloadSlots = downloadSlots;
	_botIP = botIP;
	_listenPort = listenPort;
	_passive = passive;
	log = outputLog;

	downloadManager = new DownloadManager(this);
	uploadManager = new UploadManager(this);
	shareManager = new ShareManager();
	dispatchThread = new BotEventDispatchThread(this);
    }

    /**
     * Constructs a jDCBot with the default settings. Your own constructors in classes which extend the jDCBot abstract class should be
     * responsible for changing the default settings if required.
     */
    public jDCBot(String botIP) {
	this("jDCBot", botIP, 9000, "", "<++ V:0.668,M:A,H:1/0/0,S:0>", "LAN(T1)1", "", "0", 1, 3, false, System.out);
    }

    //******Methods to get informations or other misc getters******
    /**
     * @return Name of the bot
     */
    public final String botname() {
	return _botname;
    }

    /**
     * @return <b>true</b> if the bot/client is in Passive mode. <b>false</b> if it is in Active mode.
     */
    public final boolean isPassive() {
	return _passive;
    }

    /**
     * @return Returns if the bot is Operator or not.
     */
    public final boolean isOp() {
	return _op;
    }

    /**
     * @return Name of the hub we're connected on
     */
    public final String hubname() {
	return _hubname;
    }

    public String getBotHubProtoSupports() {
	return _hubproto_supports;
    }

    public String getBotClientProtoSupports() {
	return _clientproto_supports;
    }

    public ShareManager getShareManager() {
	return shareManager;
    }

    public BotEventDispatchThread getDispatchThread() {
	return dispatchThread;
    }

    public UserManager getUserManager() {
	return um;
    }

    /**
     * Returns whether or not the jDCBot is currently connected to a hub. The result of this method should only act as a rough guide, as the
     * result may not be valid by the time you act upon it.
     * 
     * @return True if and only if the jDCBot is currently connected to a hub.
     */
    public boolean isConnected() {
	return _inputThread != null;
    }

    /**
     * Checks if the hub supports that protocol feature.
     * @param feature
     * @return
     */
    public boolean isHubSupports(String feature) {
	return _hubSupports.toLowerCase().indexOf(feature.toLowerCase()) != -1;
    }

    /**
     * Checks if the bot's client-hub protocol implementation supports that protocol feature.
     * @param feature
     * @return
     */
    public boolean isBotHubProtoSupports(String feature) {
	return _hubproto_supports.toLowerCase().indexOf(feature.toLowerCase()) != -1;
    }

    /**
     * Checks if the bot's client-client protocol implementation supports that protocol feature.
     * @param feature
     * @return
     */
    public boolean isBotClientProtoSupports(String feature) {
	return _clientproto_supports.toLowerCase().indexOf(feature.toLowerCase()) != -1;
    }

    public int getMaxUploadSlots() {
	return _maxUploadSlots;
    }

    public int getMaxDownloadSlots() {
	return _maxDownloadSlots;
    }

    /**
     * Checks if user is present on hub
     * 
     * @param user
     *                Nick of a user
     * @return true if user exist on this hub, false otherwise
     */
    public boolean UserExist(String user) {
	return um.userExist(user);
    }

    /**
     * Gets all of user info
     * 
     * @param user
     *                Nick of the user
     * @return User class that holds everything about specified user if he exist, null otherwise
     */
    public User getUser(String user) {
	if (um.userExist(user) == false)
	    return null;
	else
	    return um.getUser(user);
    }

    /**
     * 
     * @return Random user from the hub
     */
    public User GetRandomUser() {
	return um.getRandomUser();
    }

    public final User[] GetAllUsers() {
	return um.getAllUsers();
    }

    //******Methods that perform some tasks******
    /**
     * Attempt to connect to the specified DC hub. The OnConnect method is called upon success.
     * 
     * @param hostname The hostname of the server to connect to. 
     * 
     * @throws IOException
     *                 if it was not possible to connect to the server.
     * @throws BotException
     *                 if the server would not let us join it because of bad password or if there exist user with the same name.
     */
    public final void connect(String hostname, int port) throws IOException, BotException {

	String buffer;

	_port = port;

	if (this.isConnected()) {
	    throw new IOException("Already connected");
	}
	// connect to server
	socket = new Socket(hostname, port);
	input = socket.getInputStream();
	output = socket.getOutputStream();
	//breader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

	_ip = socket.getInetAddress();

	buffer = ReadCommand();
	log.println(buffer);
	String lock = parseRawCmd(buffer)[1];

	if (lock.startsWith("EXTENDEDPROTOCOL")) {
	    buffer = "$Supports " + _hubproto_supports + "|";
	    log.println("From bot: " + buffer);
	    SendCommand(buffer);
	}

	String key = lock2key(lock);
	buffer = "$Key " + key + "|";
	log.println("From bot: " + buffer);
	SendCommand(buffer);

	buffer = "$ValidateNick " + _botname + "|";
	log.println("From bot: " + buffer);
	SendCommand(buffer);

	buffer = ReadCommand();
	log.println(buffer);

	while (buffer.startsWith("$Hello") != true) {
	    if (buffer.startsWith("$ValidateDenide"))
		throw new BotException(BotException.VALIDATE_DENIED);
	    if (buffer.startsWith("$BadPass"))
		throw new BotException(BotException.BAD_PASSWORD);

	    if (buffer.startsWith("$GetPass")) {
		buffer = "$MyPass " + _password + "|";
		log.println("From bot: $MyPass xxxx|");
		SendCommand(buffer);
	    }
	    if (buffer.startsWith("$HubName ")) {
		_hubname = buffer.substring(9, buffer.length() - 1);
	    }
	    if (buffer.startsWith("$Supports ")) {
		_hubSupports = parseCmdArgs(buffer);
	    }

	    try {
		buffer = ReadCommand();
	    } catch (IOException e) {
		throw new BotException(buffer, BotException.IO_ERROR); //Sends the last read command (this will usually be a message from the hub).
	    }
	    log.println(buffer);
	}

	buffer = "$Version " + _protoVersion + "|";
	log.println("From bot: " + buffer);
	SendCommand(buffer);

	buffer = "$GetNickList|";
	log.println("From bot: " + buffer);
	SendCommand(buffer);

	buffer = "$MyINFO $ALL " + _botname + " " + _description + "$ $" + _conn_type + "$" + _email + "$" + _sharesize + "$|";
	log.println("From bot: " + buffer);
	SendCommand(buffer);

	um = new UserManager(this);

	while (!(buffer = ReadCommand()).startsWith("$NickList ")) {
	    log.println(buffer);
	    if (!buffer.startsWith("$NickList ")) {
		log.println("Expected $NickList but got something else. Reading on...");
	    }
	}
	log.println(buffer);
	um.addUsers(buffer.substring(10, buffer.length() - 1));

	/*
	 * do { buffer = ReadCommand(); log.println(buffer); } while (buffer.startsWith("$NickList ") == false); buffer = ReadCommand();
	 */

	// Creating Listen port for clients to contact this.
	socketServer = new ServerSocket(_listenPort);
	socketServer.setSoTimeout(60000); // Wait for 60s before timing out.

	_inputThread = new InputThread(this, input);
	_inputThread.start();

	onConnect();
    }

    public final Socket initConnectToMe(String user, String direction) throws BotException, IOException {
	if (!isConnected()) {
	    throw new BotException(BotException.NOT_CONNECTED_TO_HUB);
	}
	if (!UserExist(user)) {
	    throw new BotException(BotException.USRNAME_NOT_FOUND);
	}

	direction = direction.toLowerCase();
	direction = direction.substring(0, 1).toUpperCase() + direction.substring(1);

	Socket newsocket = null;
	User u = um.getUser(user);

	//connect to client
	String buffer = "$ConnectToMe " + user + " " + _botIP + ":" + _listenPort + "|";
	log.println("From bot: " + buffer);
	SendCommand(buffer);
	try {
	    newsocket = socketServer.accept();
	} catch (SocketTimeoutException soce) {
	    soce.printStackTrace(log);
	    throw new SocketTimeoutException("Connection to client " + user + " timed out.");
	}

	String remoteClientIP = newsocket.getInetAddress().getHostAddress();
	log.println("00>>Connected to remote Client:: " + remoteClientIP);
	u.setUserIP(remoteClientIP);

	buffer = ReadCommand(newsocket); //Reading $MyNick remote_nick| OR $MaxedOut //remote_user == user
	log.println(buffer);
	if (buffer.equals("$MaxedOut|")) {
	    throw new BotException(BotException.NO_FREE_SLOTS);
	}
	String remote_nick = parseRawCmd(buffer)[1];
	if (!remote_nick.equalsIgnoreCase(user)) {
	    log.println("Remote client wrong username. Expected: " + user + ", but got: " + remote_nick);
	    throw new BotException(BotException.REMOTE_CLIENT_SENT_WRONG_USRNAME);
	}

	buffer = ReadCommand(newsocket); //Reading $Lock EXTENDEDPROTOCOLABCABCABCABCABCABC Pk=DCPLUSPLUS0.698ABCABC|
	log.println(buffer);
	String lock = parseRawCmd(buffer)[1];
	String key = lock2key(lock);

	if (!lock.startsWith("EXTENDEDPROTOCOL")) {
	    log.println("Using non-Extended protocol. What kind of old client is this. You can expect errors now.");

	    buffer = "$Key " + key + "|";
	    log.println("From bot: " + buffer);
	    SendCommand(buffer, newsocket);

	    // We are now required to send a lock to the remote client. I am
	    // simply resending the same lock it returned, back to it.
	    buffer = lock;
	    log.println("From bot: " + buffer);
	    SendCommand(buffer, newsocket);

	    buffer = ReadCommand(newsocket); // Read the key sent by the
	    // remote client. I am not
	    // verifying the key,
	    // hence I am now simply moving on to the next step without
	    // parsing
	    // buffer.
	    log.println(buffer);

	    buffer = "$MyNick " + _botname + "|";
	    log.println("From bot: " + buffer);
	    SendCommand(buffer, newsocket);

	    buffer = ReadCommand(newsocket); // Reading $Direction.
	    log.println(buffer);
	    String read_direction = buffer.substring(buffer.indexOf(' ') + 1, buffer.indexOf(' ', buffer.indexOf(' ') + 1));
	    if (read_direction.equalsIgnoreCase(direction)) {
		// In this case the remote client to wants to do the same thing as this client, i.e.
		// both wants to download or upload.
		log.println("WARNING! Remote client for " + user + " too wants to " + direction + " from me. This situation is not handled.");
	    }
	} else {
	    log.println("Using Extended protocol.");

	    int N1 = 0x7FFF - 2;
	    buffer = "$MyNick " + _botname + "|$Lock EXTENDEDPROTOCOLABCABCABCABCABCABC Pk=DCPLUSPLUS0.698ABCABC|$Supports " + _clientproto_supports
		    + "|$Direction " + direction + " " + N1 + "|$Key " + key + "|";
	    log.println("From bot: " + buffer);
	    SendCommand(buffer, newsocket);

	    buffer = ReadCommand(newsocket);// Reading $Supports S|
	    log.println(buffer);
	    String remote_supports = parseCmdArgs(buffer);
	    u.setSupports(remote_supports);

	    buffer = ReadCommand(newsocket);// Reading $Direction Upload N2|
	    log.println(buffer);
	    String params[] = parseRawCmd(buffer);
	    int N2 = Integer.parseInt(params[2]);
	    if (params[1].equalsIgnoreCase(direction)) {
		log.println("Huh! Remote client for  " + user + " too wants to " + direction + " from me. This situation is not handled.");
	    }

	    buffer = ReadCommand(newsocket);// Reading $Key ........A .....0.0. 0. 0. 0. 0. 0.|
	    log.println(buffer);

	    if (N1 < N2)
		log.println("N1 is < N2 dunno what to do now. Anyway continuing as if it never happened.");
	}

	onConnect2Client();
	return newsocket;
    }

    private void replyConnectToMe(String user, String ip, int port) throws BotException, IOException { //Called in response to $ConnectToMe command from hub.
	if (!isConnected()) {
	    throw new BotException(BotException.NOT_CONNECTED_TO_HUB);
	}
	if (!user.equalsIgnoreCase(_botname)) {
	    throw new BotException(BotException.UNEXPECTED_RESPONSE);
	}

	Socket newsocket = new Socket();
	newsocket.connect(new InetSocketAddress(ip, port), 60000);
	log.println("00>>Connected to remote Client:: " + ip + ":" + port);

	String buffer = "$MyNick " + _botname + "|$Lock EXTENDEDPROTOCOLABCABCABCABCABCABC Pk=DCPLUSPLUS0.698ABCABC|"; //user == remote_nick
	log.println("From bot: " + buffer);
	SendCommand(buffer, newsocket);

	buffer = ReadCommand(newsocket); //Reading $MyNick remote_nick| OR $MaxedOut
	log.println(buffer);
	if (buffer.equals("$MaxedOut|")) {
	    throw new BotException(BotException.NO_FREE_SLOTS);
	}
	String remote_nick = parseRawCmd(buffer)[1];
	if (!um.userExist(remote_nick)) {
	    throw new BotException(BotException.USRNAME_NOT_FOUND);
	}

	User u = um.getUser(remote_nick);
	u.setUserIP(ip);

	buffer = ReadCommand(newsocket); //Reading $Lock EXTENDEDPROTOCOLABCABCABCABCABCABC Pk=DCPLUSPLUS0.698ABCABC|
	log.println(buffer);
	String lock = parseRawCmd(buffer)[1];
	String key = lock2key(lock);
	if (!lock.startsWith("EXTENDEDPROTOCOL")) {
	    log.println("Remote client doesn't support Exdtended protocol. Don't know how to continue now. Baling out.");
	    throw new BotException(BotException.PROTOCOL_UNSUPPORTED);
	}

	buffer = ReadCommand(newsocket); //Reading $Supports S|
	log.println(buffer);
	String remote_supports = parseCmdArgs(buffer);
	u.setSupports(remote_supports);

	buffer = ReadCommand(newsocket); //Reading $Direction D N|
	log.println(buffer);
	String params[] = parseRawCmd(buffer);
	String direction = params[1];
	int N = Integer.parseInt(params[2]);

	buffer = ReadCommand(newsocket); //Reading $Key ........A .....0.0. 0. 0. 0. 0. 0.|
	log.println(buffer);

	if (direction.equalsIgnoreCase("Upload")) {
	    try {
		downloadManager.download(remote_nick, newsocket, N, key);
	    } catch (BotException be) {
		socket.close();
	    }
	} else
	    uploadManager.upload(remote_nick, newsocket, N, key);
    }

    /**
     * Attemps to nicely close connection with the hub. You
     * can call {@link #connect(String, int) connect} again to connect to the hub.
     */
    public void quit() {
	try {
	    SendCommand("$Quit " + _botname + "|");
	} catch (Exception e) {} finally {
	    onBotQuit();
	    try {
		socket.close();
		socket = null;
	    } catch (IOException e) {}
	}
    }

    /**
     * Call this when you want to shut down framework completely. Unlike {@link #quit() quit},
     * {@link #connect(String, int) connect} is not supposed to be called after calling this method. 
     *
     */
    public void terminate() {
	quit();
	uploadManager.close();
	downloadManager.close();
	if (_inputThread != null)
	    _inputThread.stop();
	dispatchThread.stopIt();
    }

    /**
     * Sets if the bot is operator or not.
     * @param flag
     */
    void setOp(boolean flag) {
	_op = flag;
    }

    public void setShareManager(ShareManager sm) {
	shareManager = sm;
    }

    public void setMaxUploadSlots(int slots) {
	_maxUploadSlots = slots;
    }

    public void setMaxDownloadSlots(int slots) {
	_maxDownloadSlots = slots;
    }

    /**
     * Handles all commands from InputThread all passes it to different methods.
     * 
     * @param rawCommand
     *                Raw command sent from hub
     */
    public void handleCommand(String rawCommand) {
	log.println("From hub: " + rawCommand);

	if (rawCommand.startsWith("<")) {
	    String user, message;
	    user = rawCommand.substring(1, rawCommand.indexOf('>'));
	    message = rawCommand.substring(rawCommand.indexOf('>'));
	    message = message.substring(2, message.length() - 1);
	    this.onPublicMessage(user, message);
	} else if (rawCommand.startsWith("$Quit")) {
	    String user = rawCommand.substring(6);
	    user = user.substring(0, user.length() - 1);
	    um.userQuit(user);
	    onQuit(user);
	} else if (rawCommand.startsWith("$Hello") && (rawCommand != "$Hello " + _botname + "|")) {
	    String user = rawCommand.substring(7);
	    user = user.substring(0, user.length() - 1);
	    um.userJoin(user);
	    onJoin(user);
	} else if (rawCommand.startsWith("$To:")) {
	    String user, from, message;
	    int index1 = rawCommand.indexOf('$', 2);
	    int index2 = rawCommand.indexOf('>', index1);

	    from = rawCommand.substring(rawCommand.indexOf(':', 4) + 2, rawCommand.indexOf('$', 2) - 1);
	    user = rawCommand.substring(index1 + 2, index2);
	    message = rawCommand.substring(index2 + 2, rawCommand.length() - 1);
	    if (user.equals(from))
		onPrivateMessage(user, message);
	    else
		onChannelMessage(user, from, message);
	} else if (rawCommand.startsWith("$Search ")) {
	    int space = rawCommand.indexOf(' ', 9);
	    String firstPart = rawCommand.substring(8, space);
	    String secondPart = rawCommand.substring(space + 1, rawCommand.length() - 1);
	    StringTokenizer st = new StringTokenizer(secondPart, "?");
	    if (st.countTokens() != 5)
		return;
	    boolean isSizeRestricted, isMinimumSize;
	    long size;
	    int dataType;
	    String searchPattern;
	    isSizeRestricted = (st.nextToken() == "T");
	    isMinimumSize = (st.nextToken() == "T");
	    size = Long.parseLong(st.nextToken());
	    dataType = Integer.parseInt(st.nextToken());
	    searchPattern = st.nextToken();
	    // send trigger to passive/active search
	    if (firstPart.toLowerCase().startsWith("hub:")) {
		String user = firstPart.substring(4);
		onPassiveSearch(user, isSizeRestricted, isMinimumSize, size, dataType, searchPattern);
	    } else {
		int dotdot = firstPart.indexOf(':');
		String ip = firstPart.substring(0, dotdot);
		int port = Integer.parseInt(firstPart.substring(dotdot + 1));
		onActiveSearch(ip, port, isSizeRestricted, isMinimumSize, size, dataType, searchPattern);
	    }
	} else if (rawCommand.startsWith("$NickList")) {
	    um.addUsers(rawCommand.substring(10, rawCommand.length() - 1));
	} else if (rawCommand.startsWith("$OpList")) {
	    um.addOps(rawCommand.substring(8, rawCommand.length() - 1));
	} else if (rawCommand.startsWith("$MyINFO $ALL")) {
	    um.SetInfo(rawCommand.substring(13, rawCommand.length() - 1));
	} else if (rawCommand.startsWith("$UserIP")) {
	    um.updateUserIPs(rawCommand.substring(8, rawCommand.length() - 1));
	} else if (rawCommand.startsWith("$RevConnectToMe")) {
	    String params[] = parseRawCmd(rawCommand);
	    String me = params[2];
	    String remote_user = params[1];
	    if (me.equalsIgnoreCase(_botname)) {
		try {
		    uploadManager.uploadPassive(remote_user);
		} catch (BotException e) {
		    log.println("BotException from uploadManager.uploadPassive(): " + e.getMessage());
		    e.printStackTrace(log);
		}
	    }
	} else if (rawCommand.startsWith("$ConnectToMe")) {
	    String params[] = parseRawCmd(rawCommand);
	    String user = params[1];
	    params = params[2].split(":");
	    String ip = params[0];
	    int port = Integer.parseInt(params[1]);
	    try {
		replyConnectToMe(user, ip, port);
	    } catch (Exception e) {
		log.println("Exception by replyConnectToMe in handleCommand: " + e.getMessage());
		e.printStackTrace(log);
	    }
	} else
	    log.println("The command above is not handled.");

    }

    /**
     * Generates key from lock needed to connect to hub.
     * 
     * @param lock
     *                Lock sent from hub
     * 
     * @return Key which is sent back to hub to validate we know algorithm:-/
     */
    private final String lock2key(String lock) {
	String key_return;
	int len = lock.length();
	char[] key = new char[len];
	for (int i = 1; i < len; i++)
	    key[i] = (char) (lock.charAt(i) ^ lock.charAt(i - 1));
	key[0] = (char) (lock.charAt(0) ^ lock.charAt(len - 1) ^ lock.charAt(len - 2) ^ 5);
	for (int i = 0; i < len; i++)
	    key[i] = (char) (((key[i] << 4) & 240) | ((key[i] >> 4) & 15));

	key_return = new String();
	for (int i = 0; i < len; i++) {
	    if (key[i] == 0) {
		key_return += "/%DCN000%/";
	    } else if (key[i] == 5) {
		key_return += "/%DCN005%/";
	    } else if (key[i] == 36) {
		key_return += "/%DCN036%/";
	    } else if (key[i] == 96) {
		key_return += "/%DCN096%/";
	    } else if (key[i] == 124) {
		key_return += "/%DCN124%/";
	    } else if (key[i] == 126) {
		key_return += "/%DCN126%/";
	    } else {
		key_return += key[i];
	    }
	}

	log.println(key_return);
	return key_return;
    }

    /**
     * Sends raw command to hub.
     * 
     * @param buffer
     *                Line which needs to be send. This method won't append "|" on the end on the string if it doesn't exist, so it is up to make
     *                sure buffer ends with "|" if you calling this method.
     * @throws IOException On error while sending data into the socket.
     */
    public final void SendCommand(String buffer) throws IOException {
	SendCommand(buffer, output);
    }

    /**
     * Reading command before InputThread is started (only for connecting).
     * @throws IOException On error while sending data into the socket.
     * @return Command from hub
     */
    private final String ReadCommand() throws IOException {
	return ReadCommand(input);
    }

    //********Methods to send commands to the hub***********
    /**
     * Sends public message on main chat.
     * 
     * @param message Message to be sent. It shouldn't end with "|".
     * @throws IOException On error while sending data into the socket.
     */
    public final void SendPublicMessage(String message) throws IOException {
	SendCommand("<" + _botname + "> " + message + "|");
    }

    /**
     * Sends private message to specified user.
     * 
     * @param user
     *                User who will get message.
     * @param message
     *                Message to be sent. It shouldn't end with "|".
     * @throws IOException On error while sending data into the socket.
     */
    public final void SendPrivateMessage(String user, String message) throws IOException {
	SendCommand("$To: " + user + " From: " + _botname + " $<" + _botname + "> " + message + "|");
    }

    /**
     * Kicks specified user. note that bot has to have permission to do this
     * 
     * @param user
     *                User to be kicked
     */
    public final void KickUser(User user) {
	try {
	    SendCommand("$Kick " + user.username() + "|");
	} catch (Exception e) {}
    }

    /**
     * Kicks specified user. note that bot has to have permission to do this
     * 
     * @param user
     *                User to be kicked
     */
    public final void KickUser(String user) {
	try {
	    SendCommand("$Kick " + user + "|");
	} catch (Exception e) {}
    }

    /**
     * This method serves to send message to all users on the hub. Note that most of the hubs have a flood detection system, so you will want to
     * set timeout interval between two message sendings, or we will get warn and/or kicked!
     * 
     * @param message
     *                Message to be send to all users
     * @param timeout
     *                Timeout interval in milliseconds between sending to two consecutive user
     */
    public final void SendAll(String message, long timeout) {
	um.SendAll(message, timeout);
    }

    /**
     * Method for returning search results to active clients. You hould use it carefully if you're not owner/super user of the hub 'cause this can
     * gets you banned/kicked. Search result you will return here are imaginary (same as your sharesize).
     * 
     * @param IP
     *                IP address that gave us user who was searching for returning results
     * @param port
     *                Port that gave us user who was searching for returning results
     * @param isDir
     *                Set true if you're returning directory, false if it is a file
     * @param name
     *                Name of the file/dir you're returning. Note that some clients reject names that are note like the one they were searching
     *                for. This means that if someone were searching for 'firefox', and we're returned 'opera', his client won't display our
     *                result.
     * @param size
     *                Size of the file in bytes we're returning
     * @param free_slots
     *                How many slots we have opened/unused
     */
    public final void SendActiveSearchReturn(String IP, int port, boolean isDir, String name, long size, int free_slots) {
	StringBuffer buffer = new StringBuffer();
	String hub_ip = _ip.toString();
	if (hub_ip.contains("/")) {
	    hub_ip = hub_ip.substring(hub_ip.indexOf('/') + 1);
	}
	char c = 5;
	if (isDir == true) {
	    buffer.append("$SR " + _botname + " " + name);
	    buffer.append(" " + free_slots + "/" + _maxUploadSlots);
	    buffer.append(c);
	    buffer.append(_hubname + " (" + hub_ip + ":" + _port + ")|");
	} else {
	    buffer.append("$SR " + _botname + " " + name);
	    buffer.append(c);
	    buffer.append(size + " " + free_slots + "/" + _maxUploadSlots);
	    buffer.append(c);
	    buffer.append(_hubname + " (" + hub_ip + ":" + _port + ")|");
	}

	try {
	    DatagramSocket ds = new DatagramSocket();
	    byte[] bytes = new byte[buffer.length()];
	    for (int i = 0; i < buffer.length(); i++)
		bytes[i] = (byte) buffer.charAt(i);
	    InetAddress address = InetAddress.getByName(IP);
	    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
	    ds.send(packet);
	} catch (Exception e) {
	    log.println(e);
	}
    }

    /**
     * Method for returning search results to passive clients. You hould use it carefully if you're not owner/super user of the hub 'cause this
     * can gets you banned/kicked. Search result you will return here are imaginary (same as your sharesize).
     * 
     * @param user
     *                User who was searching. Since he is in passive mode, we return result to hub
     * @param isDir
     *                Set true if you're returning directory, false if it is a file
     * @param name
     *                Name of the file/dir you're returning. Note that some clients reject names that are note like the one they were searching
     *                for. This means that if someone were searching for 'firefox', and we're returned 'opera', his client won't display our
     *                result.
     * @param size
     *                Size of the file in bytes we're returning
     * @param free_slots
     *                How many slots we have opened/unused
     */
    public final void SendPassiveSearchReturn(String user, boolean isDir, String name, long size, int free_slots) {

	StringBuffer buffer = new StringBuffer();
	String hub_ip = _ip.toString();
	if (hub_ip.contains("/")) {
	    hub_ip = hub_ip.substring(hub_ip.indexOf('/') + 1);
	}
	char c = 5;
	if (isDir == true) {
	    buffer.append("$SR " + _botname + " " + name);
	    buffer.append(" " + free_slots + "/" + _maxUploadSlots);
	    buffer.append(c);
	    buffer.append(_hubname + " (" + hub_ip + ":" + _port + ")");
	    buffer.append(c);
	    buffer.append(user + "|");
	} else {
	    buffer.append("$SR " + _botname + " " + name);
	    buffer.append(c);
	    buffer.append(size + " " + free_slots + "/" + _maxUploadSlots);
	    buffer.append(c);
	    buffer.append(_hubname + " (" + hub_ip + ":" + _port + ")");
	    buffer.append(c);
	    buffer.append(user + "|");

	}
	try {
	    SendCommand(buffer.toString());
	} catch (Exception e) {
	    log.println(e);
	}
    }

    //******Methods that are called to notify an event******
    /**
     * Called upon succesfully connecting to hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     */
    protected void onConnect() {}

    protected void onConnect2Client() {}

    /**
     * It is called when the bot quits. Just after it quits, as a side-effect of closing the socket, the onDisconnect() too is called.
     * 
     */
    protected void onBotQuit() {}

    /**
     * Called upon disconnecting from hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class resets the _inputThread to null hence when overridden super.onDisconnect()
     * must be called first.
     */
    protected void onDisconnect() {
	_inputThread = null;
    }

    /**
     * Called when public message is received.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param user
     *                User who sent message.
     * @param message
     *                Contents of the message.
     */
    protected void onPublicMessage(String user, String message) {}

    /**
     * Called when user enter the hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param user
     *                Nema of the user who entered hub.
     */
    protected void onJoin(String user) {}

    /**
     * Called when user quits hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param user
     *                of user quited hub.
     */
    protected void onQuit(String user) {}

    /**
     * Called when some new info about the user is found. Like his IP, Passive/Active state, etc.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.<br>
     * <b>Note:</b> This method is called by <b>User</b> and <b>UserManager</b> using <i>jDCBot-EventDispatchThread</i> thread.
     * @param user
     *                The user from the hub.
     */
    protected void onUpdateMyInfo(String user) {}

    /**
     * Called when bot receives private message.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param user
     *                Name of user who sent us private message.
     * @param message
     *                Contents of private message.
     */
    protected void onPrivateMessage(String user, String message) {}

    /**
     * Called when channel message in channel where bot is present is received.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param user
     *                Name of the user who sent message.
     * @param channel
     *                Channel on which message is sent.
     * @param message
     *                Contents of the channel message.
     */
    protected void onChannelMessage(String user, String channel, String message) {}

    /**
     * Called when user in passive mode is searching for something. For specific details, (like meaning of dataType field and syntax of
     * searchPattern) you should consult direct connect protocol documentation like:
     * http://dc.selwerd.nl/doc/Command_Types_(client_to_server).html
     * 
     * @param user
     *                User who is searching
     * @param isSizeRestricted
     *                true if user restricted search result for minimum/maximum file size. If false, isMinimumSize and size should not be used and
     *                has no meaning
     * @param isMinimumSize
     *                true if user restricted his search to file that has minimum size, false if user restricted search result to maximum size.
     *                Used only if isSizeRestricted=true
     * @param size
     *                Size that user restricted his search. Is it minimum od maximum size is contained in isMimimumSizeUsed only if
     *                isSizeRestricted=true
     * @param dataType
     *                Type of the data user is searching for.
     * @param searchPattern
     *                Pattern user is searching for.
     */
    protected void onPassiveSearch(String user, boolean isSizeRestricted, boolean isMinimumSize, long size, int dataType, String searchPattern) {}

    /**
     * Called when user in passive mode is searching for something. For specific details, (like meaning of dataType field and syntax of
     * searchPattern) you should consult direct connect protocol documentation like:
     * http://dc.selwerd.nl/doc/Command_Types_(client_to_server).html
     * 
     * @param IP
     *                IP address user who was searching gave to deliver search results
     * @param port
     *                Port user who was searching gave to deliver search results
     * @param isSizeRestricted
     *                true if user restricted search result for minimum/maximum file size. If false, isMinimumSize and size should not be used and
     *                has no meaning
     * @param isMinimumSize
     *                true if user restricted his search to file that has minimum size, false if user restricted search result to maximum size.
     *                Used only if isSizeRestricted=true
     * @param size
     *                Size that user restricted his search. Is it minimum od maximum size is contained in isMimimumSizeUsed only if
     *                isSizeRestricted=true
     * @param dataType
     *                Type of the data user is searching for.
     * @param searchPattern
     *                Pattern user is searching for.
     */
    protected void onActiveSearch(String IP, int port, boolean isSizeRestricted, boolean isMinimumSize, long size, int dataType, String searchPattern) {}

    /**
     * Called when download is complete.<br>
     * <b>Note:</b> This method is called by <b>DownloadHandler</b> using <i>jDCBot-EventDispatchThread</i> thread.
     * @param user The user from whom the file was downloaded.
     * @param due The informations about the file downloaded is in this.
     * @param success It is true if download was successful else false.
     * @param e The exception that occured when sucess is false else it is null.
     */
    protected void onDownloadComplete(User user, DUEntity due, boolean success, BotException e) {}

    /**
     * Called when upload is complete.<br>
     * <b>Note:</b> This method is called by <b>DownloadHandler</b> using <i>jDCBot-EventDispatchThread</i> thread.
     * @param user The user to whom the file was uploaded.
     * @param due The informations about the file uploaded is in this.
     * @param success It is true if upload was successful else false.
     * @param e The exception that occured when sucess is false else it is null.
     */
    protected void onUploadComplete(User user, DUEntity due, boolean success, BotException e) {}

    /**
     * Called when upload is starting.<br>
     * <b>Note:</b> This method is called by <b>DownloadHandler</b> using <i>jDCBot-EventDispatchThread</i> thread.
     * @param user The user to whom the file is being uploaded.
     * @param due The informations about the file downloaded is in this.
     */
    protected void onUploadStart(User user, DUEntity due) {}
}
