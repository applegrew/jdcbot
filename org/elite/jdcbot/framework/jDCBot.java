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
import java.util.Vector;
import java.io.*;

import org.elite.jdcbot.framework.BotException;
import org.elite.jdcbot.shareframework.SearchResultSet;
import org.elite.jdcbot.shareframework.SearchSet;
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
 * @author Kokanovic Branko
 * @author AppleGrew
 * @since 0.5
 * @version 1.0
 */
public abstract class jDCBot extends InputThreadTarget implements UDPInputThreadTarget, BotInterface {
    protected int MAX_RESULTS_ACTIVE = 20;
    protected int MAX_RESULTS_PASSIVE = 10;

    /**
     * The definitive version number of this release of jDCBot.
     **/
    public static final String VERSION = "1.0";
    protected static final String _protoVersion = "1.0091"; //Version of DC++ protocol being used.

    protected InputThread _inputThread = null;
    protected UDPInputThread _udp_inputThread = null; //In multiHubsMode only the MultiHubsAdapter does the UDP listening.
    private BotEventDispatchThread dispatchThread = null;

    protected Socket socket = null;
    protected ServerSocket socketServer = null;
    protected DatagramSocket udpSocket = null;
    protected PrintStream log = null;

    InputStream input;
    OutputStream output;

    //BufferedReader	breader;

    protected UserManager um;
    protected DownloadManager downloadManager;
    protected UploadManager uploadManager;
    protected ShareManager shareManager;
    protected MultiHubsAdapter multiHubsAdapter = null;
    protected DownloadCentral downloadCentral = null;

    protected String _botname, _password, _description, _conn_type, _email, _sharesize, _hubname;
    protected boolean _passive;
    protected InetAddress _ip;
    protected int _port;
    protected int _udp_port;
    protected String _botIP;
    protected int _listenPort;

    protected int _maxUploadSlots;
    protected int _maxDownloadSlots;

    protected final String _hubproto_supports = "NoGetINFO UserIP2 MiniSlots TTH ";
    protected final String _clientproto_supports = "MiniSlots ADCGet XmlBZList TTHF ZLIG ";

    private String _hubSupports = "";
    protected boolean _op = false;

    //******Constructors******/
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
    public jDCBot(String botname, String botIP, int listenPort, int UDP_listenPort, String password, String description, String conn_type,
	    String email, String sharesize, int uploadSlots, int downloadSlots, boolean passive, PrintStream outputLog) {

	init(botname, botIP, listenPort, UDP_listenPort, password, description, conn_type, email, sharesize, uploadSlots, downloadSlots,
		passive, outputLog, new ShareManager());
    }

    /**
     * Constructs a jDCBot with the default settings. Your own constructors in classes which extend the jDCBot abstract class should be
     * responsible for changing the default settings if required.
     */
    public jDCBot(String botIP) {
	init("jDCBot", botIP, 9000, 10000, "", "<++ V:0.668,M:A,H:1/0/0,S:0>", "LAN(T1)1", "", "0", 1, 3, false, System.out,
		new ShareManager());
    }

    /**
     * Creates a new jDCBot instance which can co-exist with other jDCBot instances, all
     * sharing the shareable resources like the server sockets, etc.
     * @param multiHubsAdapter
     */
    public jDCBot(MultiHubsAdapter multiHubsAdapter) {
	this.multiHubsAdapter = multiHubsAdapter;

	init(multiHubsAdapter.getBotName(this), multiHubsAdapter._botIP, multiHubsAdapter._listenPort, multiHubsAdapter._udp_port,
		multiHubsAdapter.getPassword(this), multiHubsAdapter._description, multiHubsAdapter._conn_type, multiHubsAdapter._email,
		multiHubsAdapter._sharesize, multiHubsAdapter._maxUploadSlots, multiHubsAdapter._maxDownloadSlots,
		multiHubsAdapter._passive, multiHubsAdapter.log, multiHubsAdapter.shareManager);
    }

    private void init(String botname, String botIP, int listenPort, int UDP_listenPort, String password, String description,
	    String conn_type, String email, String sharesize, int uploadSlots, int downloadSlots, boolean passive, PrintStream outputLog,
	    ShareManager share_manager) {
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
	_udp_port = UDP_listenPort;

	GlobalObjects.log = log;

	downloadManager = new DownloadManager(this);
	uploadManager = new UploadManager(this);
	shareManager = share_manager;
	dispatchThread = new BotEventDispatchThread(this);

	//Creating Listen port for clients to contact this.
	try {
	    if (isInMultiHubsMode())
		socketServer = multiHubsAdapter.socketServer;
	    if (socketServer == null || socketServer.isClosed()) {
		socketServer = new ServerSocket(_listenPort);
		socketServer.setSoTimeout(60000); // Wait for 60s before timing out.
	    }
	} catch (SocketException e) {
	    e.printStackTrace(log);
	} catch (IOException e) {
	    e.printStackTrace(log);
	}

	initiateUDPListening();
    }

    //******Methods to get informations or other misc getters******/
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

    public DownloadCentral getDownloadCentral() {
	return downloadCentral;
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

    public int getFreeUploadSlots() {
	int free = _maxUploadSlots - uploadManager.getAllUHCount();
	return free >= 0 ? free : 0;
    }

    public int getFreeDownloadSlots() {
	int free = _maxDownloadSlots - downloadManager.getAllDHCount();
	return free >= 0 ? free : 0;
    }

    public boolean isInMultiHubsMode() {
	return multiHubsAdapter != null;
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
     * @return User with the matching client ID. If none found then it is null.
     */
    public User getUserByCID(String CID) {
	return um.getUserByCID(CID);
    }

    /**
     * This uniquely identifies a hub. This
     * is nothing more than hub's ip
     * concatenated with its port.<br>
     * If hub ip is (say) 127.0.0.1 and
     * port is 411 then output is<br>
     * <code>127.0.0.1:411</code>
     * @return The unique signature of the hub.
     */
    public String getHubSignature() {
	return _ip + ":" + _port;
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

    //******Methods that perform some tasks******/
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
		throw new BotException(BotException.Error.VALIDATE_DENIED);
	    if (buffer.startsWith("$BadPass"))
		throw new BotException(BotException.Error.BAD_PASSWORD);

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
		//Sends the last read command (this will usually be a message from the hub).
		throw new BotException(e.getMessage() + ": " + buffer, BotException.Error.IO_ERROR);
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

	_inputThread = new InputThread(this, input);
	_inputThread.start();

	onConnect();

	if (downloadCentral != null)
	    downloadCentral.triggerProcessQ();
    }

    public final Socket initConnectToMe(String user, String direction) throws BotException, IOException {
	if (!isConnected()) {
	    throw new BotException(BotException.Error.NOT_CONNECTED_TO_HUB);
	}
	if (!UserExist(user)) {
	    throw new BotException(BotException.Error.USERNAME_NOT_FOUND);
	}

	direction = direction.toLowerCase();
	direction = direction.substring(0, 1).toUpperCase() + direction.substring(1);

	Socket newsocket = null;
	String buffer = null;
	User u = um.getUser(user);

	//connect to client
	if (isInMultiHubsMode())
	    /*
	     * The below is needed, so that multiple bots' simultaneous request for connection can be synchronised
	     * because ServerSocket though allows multiple threads to simutaneously to listen but the packet received
	     * will be sent to any one of the threads, myabe the one that requested to listen first gets the first
	     * packet, but suppose botA sends issues CTM to client A and now it starts listening for the connection,
	     * and at the same time botB also issues CTM to client B and it too waits for incoming. If due to heavy
	     * network traffic on client A's route its packet is delayed and hence client B's response is received
	     * earlier then botB will end up with connection to client A and botA with client B. While verifying
	     * remote nicks they both find it wrong and hence both the connections will be dropped.
	     * 
	     * To fix that we synchronized the threads using locks.
	     */
	    multiHubsAdapter.lock.lock();
	try {
	    buffer = "$ConnectToMe " + user + " " + _botIP + ":" + _listenPort + "|";
	    log.println("From bot: " + buffer);
	    SendCommand(buffer);

	    newsocket = socketServer.accept();

	} catch (SocketTimeoutException soce) {
	    soce.printStackTrace(log);
	    throw new SocketTimeoutException("Connection to client " + user + " timed out.");
	} finally {
	    if (isInMultiHubsMode())
		multiHubsAdapter.lock.unlock(); //We can now safely unlock as connection has been made.
	}

	String remoteClientIP = newsocket.getInetAddress().getHostAddress();
	log.println("00>>Connected to remote Client:: " + remoteClientIP);
	u.setUserIP(remoteClientIP);

	buffer = ReadCommand(newsocket); //Reading $MyNick remote_nick| OR $MaxedOut //remote_user == user
	log.println(buffer);
	if (buffer.equals("$MaxedOut|")) {
	    throw new BotException(BotException.Error.NO_FREE_SLOTS);
	}
	String remote_nick = parseRawCmd(buffer)[1];
	if (!remote_nick.equalsIgnoreCase(user)) {
	    log.println("Remote client wrong username. Expected: " + user + ", but got: " + remote_nick);
	    throw new BotException(BotException.Error.REMOTE_CLIENT_SENT_WRONG_USERNAME);
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
		log.println("WARNING! Remote client for " + user + " too wants to " + direction
			+ " from me. This situation is not handled.");
	    }
	} else {
	    log.println("Using Extended protocol.");

	    int N1 = 0x7FFF - 2;
	    buffer =
		    "$MyNick " + _botname + "|$Lock EXTENDEDPROTOCOLABCABCABCABCABCABC Pk=DCPLUSPLUS0.698ABCABC|$Supports "
			    + _clientproto_supports + "|$Direction " + direction + " " + N1 + "|$Key " + key + "|";
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
	    throw new BotException(BotException.Error.NOT_CONNECTED_TO_HUB);
	}
	if (!user.equalsIgnoreCase(_botname)) {
	    throw new BotException(BotException.Error.UNEXPECTED_RESPONSE);
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
	    throw new BotException(BotException.Error.NO_FREE_SLOTS);
	}
	String remote_nick = parseRawCmd(buffer)[1];
	if (!um.userExist(remote_nick)) {
	    throw new BotException(BotException.Error.USERNAME_NOT_FOUND);
	}

	User u = um.getUser(remote_nick);
	u.setUserIP(ip);

	buffer = ReadCommand(newsocket); //Reading $Lock EXTENDEDPROTOCOLABCABCABCABCABCABC Pk=DCPLUSPLUS0.698ABCABC|
	log.println(buffer);
	String lock = parseRawCmd(buffer)[1];
	String key = lock2key(lock);
	if (!lock.startsWith("EXTENDEDPROTOCOL")) {
	    log.println("Remote client doesn't support Exdtended protocol. Don't know how to continue now. Baling out.");
	    throw new BotException(BotException.Error.PROTOCOL_UNSUPPORTED);
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
		if (_inputThread != null)
		    _inputThread.stop();
		if (socket != null)
		    socket.close();
		socket = null;
		_inputThread = null;
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
	if (!isInMultiHubsMode())
	    _udp_inputThread.stop();
	if (shareManager != null)
	    shareManager.close();
	if (downloadCentral != null && !isInMultiHubsMode())
	    downloadCentral.stopQueueProcessThread();
    }

    @Override
    public void disconnected() {
	_inputThread = null;
	onDisconnect();
    }

    /**
     * Converts special characters like
     * |, $, etc. to a form acceptable by
     * DC protocol.
     * @param msg
     * @param whitespace If true then replaces white spaces by +.
     * @return
     */
    public String escapeSpecial(String msg, boolean whitespace) {
	msg = msg.replace("&", "&amp;").replace("$", "&#36;").replace("|", "&#124;");
	if (whitespace)
	    msg = msg.replace(' ', '$');
	return msg;
    }

    public String unescapeSpecial(String msg) {
	return msg.replace('$', ' ').replace("&#36;", "$").replace("&#124;", "|").replace("&amp;", "&");
    }

    /**
     * This will convert forward slashes to backward slashes (as required by
     * DC protocol). <b>Note:</b> It <u>won't</u> escape special characters
     * automatically. You will need to manually call {@link #escapeSpecial(String, boolean)}.
     * @param path
     * @return
     */
    public String sanitizePath(String path) {
	path = path.trim().replace('/', '\\');
	if (path.startsWith("\\") && path.length() > 1)
	    path = path.substring(1);
	return path;
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

    public void setDownloadCentral(DownloadCentral dc) {
	downloadCentral = dc;
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
     * @param rawCommand Raw command sent from hub
     */
    public void handleCommand(String rawCommand) {
	log.println("From hub: " + rawCommand);

	if (rawCommand.startsWith("<")) {
	    String user, message;
	    user = rawCommand.substring(1, rawCommand.indexOf('>'));
	    message = rawCommand.substring(rawCommand.indexOf('>'));
	    message = message.substring(2, message.length() - 1);
	    this.onPublicMessage(user, unescapeSpecial(message));
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
	    message = unescapeSpecial(rawCommand.substring(index2 + 2, rawCommand.length() - 1));
	    if (user.equals(from))
		onPrivateMessage(user, message);
	    else
		onChannelMessage(user, from, message);
	} else if (rawCommand.startsWith("$Search ")) {
	    int space = rawCommand.indexOf(' ', 9);
	    String firstPart = rawCommand.substring(8, space).trim();
	    String secondPart = rawCommand.substring(space + 1, rawCommand.length() - 1);
	    StringTokenizer st = new StringTokenizer(secondPart, "?");
	    if (st.countTokens() != 5)
		return;
	    boolean isSizeRestricted, isMinimumSize;
	    long size;
	    SearchSet.DataType dataType;
	    String searchPattern;
	    isSizeRestricted = (st.nextToken() == "T");
	    isMinimumSize = (st.nextToken() == "T");
	    size = Long.parseLong(st.nextToken());
	    dataType = SearchSet.DataType.getEnumForValue(Integer.parseInt(st.nextToken()));
	    searchPattern = st.nextToken();

	    SearchSet search = new SearchSet();
	    search.string = dataType == SearchSet.DataType.TTH ? searchPattern : unescapeSpecial(searchPattern);
	    search.size = size;
	    search.size_unit = SearchSet.SizeUnit.BYTE;
	    search.size_criteria =
		    isSizeRestricted ? (isMinimumSize ? SearchSet.SizeCriteria.ATMOST : SearchSet.SizeCriteria.ATLEAST)
			    : SearchSet.SizeCriteria.NONE;
	    search.data_type = dataType;

	    // send trigger to passive/active search
	    if (firstPart.toLowerCase().startsWith("hub:")) {
		String user = firstPart.substring(4);
		onSearch(user, search);
		onPassiveSearch(user, search);
		onPassiveSearch(user, isSizeRestricted, isMinimumSize, size, dataType.getValue(), searchPattern);
	    } else {
		int dotdot = firstPart.indexOf(':');
		String ip = firstPart.substring(0, dotdot);
		int port = Integer.parseInt(firstPart.substring(dotdot + 1));
		onSearch(ip, port, search);
		onActiveSearch(ip, port, search);
		onActiveSearch(ip, port, isSizeRestricted, isMinimumSize, size, dataType.getValue(), searchPattern);
	    }
	} else if (rawCommand.startsWith("$NickList")) {
	    um.addUsers(rawCommand.substring(10, rawCommand.length() - 1));
	} else if (rawCommand.startsWith("$OpList")) {
	    um.addOps(rawCommand.substring(8, rawCommand.length() - 1));
	} else if (rawCommand.startsWith("$MyINFO $ALL")) {
	    um.SetInfo(rawCommand.substring(13, rawCommand.length() - 1));
	    if (downloadCentral != null)
		downloadCentral.triggerProcessQ();
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
	} else if (rawCommand.startsWith("$SR ")) {
	    processSRcommand(rawCommand, null, 0);
	} else
	    log.println("The command above is not handled.");

    }

    public void handleUDPCommand(String rawCommand, String ip, int port) {
	log.println("From user(" + ip + ":" + port + "): " + rawCommand);

	if (rawCommand.startsWith("$SR ")) {
	    processSRcommand(rawCommand, ip, port);
	} else
	    log.println("The command above is not handled.");
    }

    private void processSRcommand(String rawCommand, String ip, int port) {
	int delil = rawCommand.indexOf(' ');
	int delir = rawCommand.indexOf(' ', delil + 1);
	String senderNick = rawCommand.substring(delil + 1, delir);

	delil = delir;
	delir = rawCommand.lastIndexOf(' ', rawCommand.lastIndexOf(5));
	String result = rawCommand.substring(delil + 1, delir);

	delil = delir;
	delir = rawCommand.indexOf('/', delil + 1);
	int free_slots = Integer.parseInt(rawCommand.substring(delil + 1, delir));

	delil = delir;
	delir = rawCommand.indexOf(5, delil + 1);
	int total_slots = Integer.parseInt(rawCommand.substring(delil + 1, delir));

	delil = delir;
	delir = rawCommand.indexOf(' ', delil + 1);
	String hubORTTH = rawCommand.substring(delil + 1, delir);
	boolean isTTH = hubORTTH.startsWith("TTH:");

	delil = rawCommand.indexOf('(', delir + 1);
	delir = rawCommand.indexOf(')', delil + 1);
	String hubIPANDPort = rawCommand.substring(delil + 1, delir);
	String hubIP;
	int hubPort = 411;
	int colonPos = hubIPANDPort.indexOf(':');
	if (colonPos != -1) {
	    hubIP = hubIPANDPort.substring(0, colonPos);
	    hubPort = Integer.parseInt(hubIPANDPort.substring(colonPos + 1));
	} else
	    hubIP = hubIPANDPort;

	if (!hubIP.equals(_ip.getHostAddress()) || hubPort != _port) {
	    log.println("Search result meant for other hub has been ignored.");
	    return;
	}

	int res5Pos = -1;
	boolean isDir = (res5Pos = result.indexOf(5)) == -1;
	String resName = result;
	long resSize = 0;
	if (!isDir) {
	    resName = result.substring(0, res5Pos);
	    resSize = Long.parseLong(result.substring(res5Pos + 1));
	}

	SearchResultSet res = new SearchResultSet();
	res.name = unescapeSpecial(resName);
	res.size = resSize;
	res.isDir = isDir;
	res.TTH = isTTH ? hubORTTH.substring(4) : "";

	if (ip != null)
	    getUser(senderNick).setUserIP(ip);

	if (downloadCentral != null && isTTH)
	    downloadCentral.searchResult(hubORTTH, getUser(senderNick));
	onSearchResult(senderNick, ip, port, res, free_slots, total_slots, isTTH ? "" : hubORTTH);
    }

    private void initiateUDPListening() {
	if (isInMultiHubsMode())
	    return;

	if (_udp_inputThread != null && !_udp_inputThread.isClosed())
	    return;

	try {
	    udpSocket = new DatagramSocket(_udp_port);
	} catch (SocketException e) {
	    log.println("Failed to listen for UDP packets.");
	    e.printStackTrace(log);
	}
	_udp_inputThread = new UDPInputThread(this, udpSocket);
	_udp_inputThread.start();
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

    /**
     * Damn Java won't let me make it package only visible.
     * Anyway, don't call this method ever. It is <u>not for
     * you</u>. So once again, <u><b>don't call this method
     * ever</b></u>.
     */
    public final void onUDPExceptionClose(IOException e) {
	_udp_inputThread = null;
	initiateUDPListening();
    }

    //********Methods to send commands to the hub***********/
    /**
     * Sends public message on main chat.
     * 
     * @param message Message to be sent. It shouldn't end with "|".
     * @throws IOException On error while sending data into the socket.
     */
    public final void SendPublicMessage(String message) throws IOException {
	SendCommand("<" + _botname + "> " + escapeSpecial(message, false) + "|");
    }

    /**
     * Sends private message to specified user.
     * 
     * @param user User who will get message.
     * @param message Message to be sent. It shouldn't end with "|".
     * @throws IOException On error while sending data into the socket.
     */
    public final void SendPrivateMessage(String user, String message) throws IOException {
	SendCommand("$To: " + user + " From: " + _botname + " $<" + _botname + "> " + escapeSpecial(message, false) + "|");
    }

    /**
     * Kicks specified user. note that bot has to have permission to do this
     * 
     * @param user User to be kicked
     */
    public final void KickUser(User user) {
	try {
	    SendCommand("$Kick " + user.username() + "|");
	} catch (Exception e) {}
    }

    /**
     * Kicks specified user. note that bot has to have permission to do this
     * 
     * @param user User to be kicked
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
     * @param message Message to be send to all users
     * @param timeout Timeout interval in milliseconds between sending to two consecutive user
     */
    public final void SendAll(String message, long timeout) {
	um.SendAll(message, timeout);
    }

    /**
     * Searches in the hub.
     * @param what The term to search for as per constrains given.
     * @throws IOException When communication error occurs.
     */
    public final void Search(SearchSet what) throws IOException {
	long size =
		what.size_criteria == SearchSet.SizeCriteria.NONE ? 0 : (what.size_unit == SearchSet.SizeUnit.BYTE ? what.size
			: what.size_unit.getValue() * 1024 * what.size);

	String cmd = "$Search ";
	if (_passive) {
	    cmd = cmd + "Hub:" + _botname + " ";
	} else {
	    cmd = cmd + _botIP + ":" + _udp_port + " ";
	}
	String search = (what.size_criteria == SearchSet.SizeCriteria.NONE ? "F" : "T") + "?";
	search =
		search
			+ (what.size_criteria == SearchSet.SizeCriteria.NONE || what.size_criteria == SearchSet.SizeCriteria.ATLEAST ? "F"
				: "T") + "?";
	search = search + (what.size_criteria == SearchSet.SizeCriteria.NONE ? "0" : size) + "?";
	search = search + what.data_type.getValue() + "?";
	search = search + (what.data_type == SearchSet.DataType.TTH ? "TTH:" : "") + escapeSpecial(what.string, true);

	cmd = cmd + search + "|";

	log.println("from bot: " + cmd);

	SendCommand(cmd);
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
    public final void SendActiveSearchReturn(String IP, int port, boolean isDir, String name, String hash, long size, int free_slots) {
	name = sanitizePath(escapeSpecial(name, false));

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
	    buffer.append((hash == null ? _hubname : "TTH:" + hash) + " (" + hub_ip + ":" + _port + ")|");
	}

	log.println("from bot: " + buffer);

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
    public final void SendPassiveSearchReturn(String user, boolean isDir, String name, String hash, long size, int free_slots) {
	name = sanitizePath(escapeSpecial(name, false));

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
	    buffer.append((hash == null ? _hubname : "TTH:" + hash) + " (" + hub_ip + ":" + _port + ")");
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

	log.println("from bot: " + buffer);

	try {
	    SendCommand(buffer.toString());
	} catch (Exception e) {
	    log.println(e);
	}
    }

    //******Methods that are called to notify an event******/
    /**
     * Called when a <u>active</u> user is searching  for something.<br>
     * You need not override this method as it has the code that automcatically searches
     * in the file list and returns the result to the user.<br>
     * If you need to implement a feature e.g. a search spy, etc. override
     * {@link #onActiveSearch(String, int, SearchSet)}.
     * @param ip The IP of the user who made the search.
     * @param port The port to which the result data datagram should be sent.
     * @param search The search that was made.
     */
    protected void onSearch(String ip, int port, SearchSet search) {
	if (shareManager == null)
	    return;

	Vector<SearchResultSet> res = shareManager.searchOwnFileList(search, MAX_RESULTS_ACTIVE);
	if (res != null) {
	    for (SearchResultSet r : res)
		SendActiveSearchReturn(ip, port, r.isDir, r.name, r.TTH.isEmpty() || r.TTH == null ? null : r.TTH, r.size,
			getFreeUploadSlots());
	}
    }

    /**
     * Called when a <u>passive</u> user is searching  for something.<br>
     * You need not override this method as it has the code that automcatically searches
     * in the file list and returns the result to the user.<br>
     * If you need to implement a feature e.g. a search spy, etc. override
     * {@link #onPassiveSearch(String, SearchSet)}.
     * @param user The user who made the search.
     * @param search The search that was made.
     */
    protected void onSearch(String user, SearchSet search) {
	if (shareManager == null)
	    return;

	Vector<SearchResultSet> res = shareManager.searchOwnFileList(search, MAX_RESULTS_PASSIVE);
	if (res != null) {
	    for (SearchResultSet r : res)
		SendPassiveSearchReturn(user, r.isDir, r.name, r.TTH.isEmpty() || r.TTH == null ? null : r.TTH, r.size,
			getFreeUploadSlots());
	}
    }

    /**
     * Called when receiving a search result from any user or the hub (in case you are passive).
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * 
     * @param senderNick The user's nick who returned the result.
     * @param senderIP This can be null if search response is received
     * from the hub, i.e. you are passive.
     * @param senderPort This is zero when you are pasive.
     * @param result The search response.
     * @param free_slots The number of free slots <i>senderNick</i> user has.
     * @param total_slots The total number of upload slots <i>senderNick</i> user has.
     * @param hubName This is empty when TTH in <i>result</i> is set.
     */
    protected void onSearchResult(String senderNick, String senderIP, int senderPort, SearchResultSet result, int free_slots,
	    int total_slots, String hubName) {}

    /**
     * Called upon succesfully connecting to hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     */
    protected void onConnect() {}

    /**
     * Called just when a new connection has been established with another client in Active mode. 
     *
     */
    protected void onConnect2Client() {}

    /**
     * It is called when the bot quits. Just after it quits, as a side-effect of closing the socket, the onDisconnect() too is called.
     * 
     */
    protected void onBotQuit() {}

    /**
     * Called upon disconnecting from hub.
     * 
     * @since 1.0 The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required..
     */
    protected void onDisconnect() {}

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
     * @deprecated Use {@link #onPassiveSearch(String, SearchSet)} instead.
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
    protected void onPassiveSearch(String user, boolean isSizeRestricted, boolean isMinimumSize, long size, int dataType,
	    String searchPattern) {}

    /**
     * Called when user in passive mode is searching for something.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * @param user The passive user who made the search.
     * @param search Contains all the details abot the search made.
     */
    protected void onPassiveSearch(String user, SearchSet search) {}

    /**
     * Called when user in passive mode is searching for something. For specific details, (like meaning of dataType field and syntax of
     * searchPattern) you should consult direct connect protocol documentation like:
     * http://dc.selwerd.nl/doc/Command_Types_(client_to_server).html
     * 
     * @deprecated Use {@link #onActiveSearch(String, int, SearchSet)} instead.
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
    protected void onActiveSearch(String IP, int port, boolean isSizeRestricted, boolean isMinimumSize, long size, int dataType,
	    String searchPattern) {}

    /**
     * Called when user in active mode is searching for something.
     * <p>
     * The implementation of this method in the jDCBot abstract class performs no actions and may be overridden as required.
     * @param ip The IP of the user who made the search.
     * @param port The port to which the search result should be sent.
     * @param search Contains all the details abot the search made.
     */
    protected void onActiveSearch(String ip, int port, SearchSet search) {}

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
     * 
     * @param user
     * @param due
     */
    protected void onDownloadStart(User user, DUEntity due) {}

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
