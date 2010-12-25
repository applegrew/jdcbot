/*
 * MultiHubsAdapter.java
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
package org.elite.jdcbot.framework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.IIOException;

import org.elite.jdcbot.shareframework.SearchSet;
import org.elite.jdcbot.shareframework.ShareManager;
import org.elite.jdcbot.util.GlobalFunctions;
import org.slf4j.Logger;

/**
 * Created on 06-Jun-08<br>
 * This allows you to connect to multiple
 * hubs. This will handle the intricacies of creation of
 * different jDCBot instances for handling a hub and
 * synchronizing them.
 * <p>
 * <b>Note:</b> Whenever a method in this class has
 * a name similar to a method in jDCBot then always use the
 * method of this class, else proper synchronizations may
 * not possible. There are some similar named methods for which
 * this rule can be safely ignored, but they explicitly metion
 * this, hence look in their doc comment.
 * <p>
 * This class is thread safe.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1.2
 */
public class MultiHubsAdapter implements UDPInputThreadTarget, BotInterface {
	private static final Logger logger = GlobalObjects.getLogger(MultiHubsAdapter.class);
	private String _botname, _password;
	protected String _description, _conn_type, _email, _sharesize;
	protected boolean _passive;
	protected int _udp_port;
	protected String _botIP;
	protected int _listenPort;

	protected int _maxUploadSlots;
	protected int _maxDownloadSlots;

	protected BufferedServerSocket socketServer = null;
	protected DatagramSocket udpSocket = null;

	protected String miscDir;
	protected String incompleteDir;

	protected ShareManager shareManager;
	protected DownloadCentral downloadCentral = null;
	private UDPInputThread _udp_inputThread = null;

	protected List<jDCBot> bots;
	protected Map<String, Hub> hubMap = null;
	/**
	 * Used to synchronized some process like when initConnectToMe is called.
	 */
	protected ReentrantLock lock;
	
	/**
	 * Creates a new instance of MultiHubsAdapter. There should always be only instance of
	 * this class.
	 * @param config
	 * @throws IOException 
	 * @throws BotException 
	 */
	public MultiHubsAdapter(BotConfig config) throws IOException, BotException {
		this(
				config.getBotname(),
				config.getBotIP(),
				config.getListenPort(),
				config.getUDP_listenPort(),
				config.getPassword(),
				config.getDescription(),
				config.getConn_type(),
				config.getEmail(),
				config.getSharesize(),
				config.getUploadSlots(),
				config.getDownloadSlots(),
				config.isPassive()
				);
	}

	/**
	 * Creates a new instance of MultiHubsAdapter. There should always be only instance of
	 * this class. For explanation of the parameters see
	 * {@link jDCBot#jDCBot(String, String, int, int, String, String, String, String, String, int, int, boolean) jDCBot Constrcutor}.
	 * @param botname
	 * @param botIP
	 * @param listenPort
	 * @param UDP_listenPort
	 * @param password
	 * @param description
	 * @param conn_type
	 * @param email
	 * @param sharesize
	 * @param uploadSlots
	 * @param downloadSlots
	 * @param passive
	 * @throws IOException 
	 * @throws BotException 
	 */
	public MultiHubsAdapter(String botname, String botIP, int listenPort, int UDP_listenPort, String password, String description,
			String conn_type, String email, String sharesize, int uploadSlots, int downloadSlots, boolean passive)
	throws IOException, BotException {
		
		if(!GlobalFunctions.isUserNameValid(botname)) {
			throw new BotException(BotException.Error.INVALID_USERNAME);
		}
		
		_botname = botname;
		_password = password;
		_description = description;
		_conn_type = conn_type;
		_email = email;
		_sharesize = sharesize;
		_maxUploadSlots = uploadSlots;
		_maxDownloadSlots = downloadSlots;
		_botIP = botIP;
		_listenPort = listenPort;
		_passive = passive;
		_udp_port = UDP_listenPort;

		lock = new ReentrantLock();
		bots = Collections.synchronizedList(new ArrayList<jDCBot>(6));
		hubMap = Collections.synchronizedMap(new HashMap<String, Hub>(6));
		shareManager = null;

		if (_sharesize == null || _sharesize.isEmpty())
			_sharesize = "0";

		socketServer = new BufferedServerSocket(_listenPort);
		socketServer.setSoTimeout(60000); // Wait for 60s before timing out.

		initiateUDPListening();
	}

	/**
	 * <b>Note:</b> The given directories' must exist and should be empty, as
	 * already existing files in them will overwritten without warning.
	 * @param path2DirForMiscData In this directory own file list, hash data, etc. will be kept.
	 * @param path2IncompleteDir Where incomplete downloads will be kept.
	 * @throws FileNotFoundException If the directory paths are not found or 'fileListHash'
	 * doesn't exist.
	 * @throws IIOException If the given path are not directories.
	 * @throws InstantiationException The read object from 'fileListHash' is not instance of FLDir.
	 * @throws ClassNotFoundException Class of FLDir serialized object cannot be found.
	 * @throws IOException Error occured while reading from 'fileListHash'.
	 */
	public void setDirs(String path2DirForMiscData, String path2IncompleteDir) throws IIOException, FileNotFoundException, IOException {
		File miscDir = new File(path2DirForMiscData);
		File incompleteDir = new File(path2IncompleteDir);
		if (!miscDir.exists() || !incompleteDir.exists())
			throw new FileNotFoundException();
		if (!miscDir.isDirectory())
			throw new IIOException("Given path '" + path2DirForMiscData + "' is not a directory.");
		if (!incompleteDir.isDirectory())
			throw new IIOException("Given path '" + path2IncompleteDir + "' is not a directory.");

		this.miscDir = miscDir.getCanonicalPath();
		this.incompleteDir = incompleteDir.getCanonicalPath();

		for (jDCBot bot : bots) {
			bot.miscDir = this.miscDir;
			bot.incompleteDir = this.incompleteDir;
		}
	}
	
	void addBot(jDCBot bot) {
		bots.add(bot);
	}
	
	void removeBot(jDCBot bot) {
		bots.remove(bot);
	}

	public String getMiscDir() {
		return miscDir;
	}

	public String getIncompleteDir() {
		return incompleteDir;
	}

	public List<jDCBot> getAllBots() {
		synchronized (bots) {
			return new ArrayList<jDCBot>(bots);
		}
	}

	public jDCBot getBot(String hubSignature) {
		synchronized (bots) {
			for (jDCBot bot : bots)
				if (bot.getHubSignature().equals(hubSignature))
					return bot;
		}
		return null;
	}

	/**
	 * Sets the mapping of hub signature to
	 * Hub. This allows you to specify different
	 * settings for certain values (like user name,
	 * passowrd, active/passive mode, etc.) for
	 * different hubs.
	 * @param hubSettings List of Hub settings.
	 */
	public void setHubMaps(List<Hub> hubSettings) {
		for (Hub hub : hubSettings)
			hubMap.put(hub.getHubSignature(), hub);
	}

	public ShareManager getShareManager() {
		return shareManager;
	}

	/**
	 * <b>Note:</b> <u>Always</u> call {@link #setDirs(String, String)}
	 * before calling this method else you will get all sorts of nasty
	 * exceptions like NullPointerException, etc. and ShareManager
	 * will seem to be not working at all.
	 * <p>
	 * It is recommended that you set the shareManager at the
	 * initiation of application and donot call this method again,
	 * ever during the lifetime of the application.
	 * 
	 * @param sm
	 */
	public void setShareManager(ShareManager sm) {
		if (shareManager != null)
			shareManager.close();

		shareManager = sm;
		shareManager.setDirs(miscDir);
		shareManager.init();
		_sharesize = String.valueOf(shareManager.getOwnShareSize(false));
		synchronized (bots) {
			for (jDCBot bot : bots) {
				bot.shareManager = shareManager;
				bot._sharesize = _sharesize;
			}
		}
	}

	public DownloadCentral getDownloadCentral() {
		return downloadCentral;
	}

	/**
	 * <b>Note:</b> <u>Always</u> call {@link #setDirs(String, String)}
	 * before calling this method else you will get all sorts of nasty
	 * exceptions like NullPointerException, etc. and DownloadCentral
	 * will seem to be not working at all.
	 * <p>
	 * It is recommended that you set the downloadManager at the
	 * initiation of application and donot call this method again,
	 * ever during the lifetime of the application.
	 * 
	 * @param dc 
	 */
	public void setDownloadCentral(DownloadCentral dc) {
		if (downloadCentral != null)
			downloadCentral.close();
		downloadCentral = dc;
		downloadCentral.setDirs(incompleteDir);
		downloadCentral.init();
		downloadCentral.startNewQueueProcessThread();
		synchronized (bots) {
			for (jDCBot bot : bots)
				bot.downloadCentral = downloadCentral;
		}
	}

	public void updateShareSize() {
		String sharesize = String.valueOf(shareManager.getOwnShareSize(false));
		if (sharesize.equals(_sharesize))
			return;

		_sharesize = sharesize;
		synchronized (bots) {
			for (jDCBot bot : bots)
				try {
					bot.sendMyINFO();
				} catch (IOException e) {
					logger.error("Exception in setDownloadCentral()", e);
				}
		}
	}

	public void terminate() {
		for (jDCBot bot : bots)
			bot.terminate();
		if (_udp_inputThread != null)
			_udp_inputThread.stop();
		if (shareManager != null)
			shareManager.close();
		if (downloadCentral != null)
			downloadCentral.close();
	}

	public void handleUDPCommand(String rawCommand, String ip, int port) {
		synchronized (bots) {
			for (jDCBot bot : bots)
				bot.handleUDPCommand(rawCommand, ip, port);
		}
	}

	public void onUDPExceptionClose(IOException e) {
		_udp_inputThread = null;
		try {
			initiateUDPListening();
		} catch (SocketException e1) {
			logger.warn("Failed to reopen UDP port. Searching may not work.");
			logger.error("Exception in onUDPExceptionClose()", e);
		}
	}

	synchronized private void initiateUDPListening() throws SocketException {
		if (_udp_inputThread != null && !_udp_inputThread.isClosed())
			return;

		udpSocket = new DatagramSocket(_udp_port);
		_udp_inputThread = new UDPInputThread(this, udpSocket);
		_udp_inputThread.start();
	}

	/**
	 * Searches all the hubs for the given term.
	 * <p>
	 * If you want to search in only some specific
	 * hubs the you can safely call the Search()
	 * methods of appropriate jDCBot.
	 * @param ss
	 * @throws IOException
	 */
	public void Search(SearchSet ss) throws IOException {
		synchronized (bots) {
			for (jDCBot bot : bots)
				bot.Search(ss);
		}
	}

	/**
	 * You can safely use jDCBot's
	 * UserExist() if you need.
	 */
	public boolean UserExist(String user) {
		synchronized (bots) {
			for (jDCBot bot : bots)
				if (bot.UserExist(user))
					return true;
		}
		return false;
	}

	/**
	 * You can safely use jDCBot's
	 * getBotClientProtoSupports() if you need.
	 */
	public String getBotClientProtoSupports() {
		return jDCBot._clientproto_supports;
	}

	/**
	 * You can safely use jDCBot's
	 * getBotHubProtoSupports() if you need.
	 */
	public String getBotHubProtoSupports() {
		return jDCBot._hubproto_supports;
	}

	public int getMaxDownloadSlots() {
		return _maxDownloadSlots;
	}

	public void setMaxDownloadSlots(int slots) {
		_maxDownloadSlots = slots;
		synchronized (bots) {
			for (jDCBot bot : bots)
				bot.setMaxDownloadSlots(slots);
		}
	}

	public int getMaxUploadSlots() {
		return _maxUploadSlots;
	}

	public void setMaxUploadSlots(int slots) {
		_maxUploadSlots = slots;
		synchronized (bots) {
			for (jDCBot bot : bots)
				bot.setMaxUploadSlots(slots);
		}
	}

	/**
	 * @return User with the matching client ID. If none found then it is null.
	 */
	public User getUserByCID(String cid) {
		synchronized (bots) {
			for (jDCBot bot : bots) {
				User u = bot.getUserByCID(cid);
				if (u != null)
					return u;
			}
		}
		return null;
	}

	/**
	 * This is a powerful method of locating users from different hubs
	 * who are actually the same inspite of having different usernames.
	 * Note that having same IP doesn't mean that two users are the
	 * same. Also note that user may run multiple clients in which
	 * case the CID may very well be different. Also to know the
	 * CID you need to download client's file list first.
	 * 
	 * @param cid
	 * @return null is never returned.
	 */
	public List<User> getUsersByCID(String cid) {
		List<User> users = new ArrayList<User>();
		synchronized (bots) {
			for (jDCBot bot : bots) {
				User u = bot.getUserByCID(cid);
				if (u != null)
					users.add(u);
			}
		}
		return users;
	}

	/**
	 * You can safely use jDCBot's
	 * botname() if you need.
	 * @return
	 */
	public String botname() {
		return _botname;
	}

	protected String botname(jDCBot bot) {
		Hub h;
		synchronized (hubMap) {
			if (hubMap != null && (h = hubMap.get(bot.getHubSignature())) != null) {
				h.username = h.username.trim();
				return h.username == null || h.username.isEmpty() ? _botname : h.username;
			}
		}
		return _botname;
	}

	protected String getPassword(jDCBot bot) {
		Hub h;
		synchronized (hubMap) {
			if (hubMap != null && (h = hubMap.get(bot.getHubSignature())) != null) {
				return h.password;
			}
		}
		return _password;
	}

	protected String getDescription(jDCBot bot) {
		Hub h;
		synchronized (hubMap) {
			if (hubMap != null && (h = hubMap.get(bot.getHubSignature())) != null) {
				return h.description;
			}
		}
		return _description;
	}

	protected String getConnType(jDCBot bot) {
		Hub h;
		synchronized (hubMap) {
			if (hubMap != null && (h = hubMap.get(bot.getHubSignature())) != null) {
				return h.conn_type;
			}
		}
		return _conn_type;
	}

	protected String getEmail(jDCBot bot) {
		Hub h;
		synchronized (hubMap) {
			if (hubMap != null && (h = hubMap.get(bot.getHubSignature())) != null) {
				return h.email;
			}
		}
		return _email;
	}

	protected boolean isPassive(jDCBot bot) {
		Hub h;
		synchronized (hubMap) {
			if (hubMap != null && (h = hubMap.get(bot.getHubSignature())) != null) {
				return h.isPassive;
			}
		}
		return _passive;
	}

	/**
	 * This will return all users
	 * from all the hubs.
	 * <p>
	 * You can safely use jDCBot's
	 * GetAllUsers() if you need.
	 * @return Array of Users.
	 */
	public User[] GetAllUsers() {
		List<User> users = new ArrayList<User>();
		synchronized (bots) {
			for (jDCBot bot : bots)
				for (User u : bot.GetAllUsers())
					users.add(u);
		}
		return users.toArray(new User[0]);
	}

	/**
	 * Connects to a hub.
	 * @param hostname
	 * @param port
	 * @param newbot A new instance of jDCBot's sub-class. <b>Note</b>, that
	 * this sub-class must have called jDCBot's {@link jDCBot#jDCBot(MultiHubsAdapter)}
	 * constructor with this class instance being passed as argument.
	 * @throws IOException
	 * @throws BotException Various exceptions are thrown when error occurs during connecting.
	 * If we are already connected to this hub then BotException.Error.ALREADY_CONNECTED is
	 * thrown.
	 */
	public void connect(String hostname, int port, jDCBot newbot) throws IOException, BotException {
		synchronized (bots) {
			for (jDCBot bot : bots)
				if (Hub.prepareHubSignature(hostname, port).equals(bot.getHubSignature()))
					throw new BotException(BotException.Error.ALREADY_CONNECTED);

			bots.add(newbot);
		}
		newbot.connect(hostname, port);
	}

	public int getFreeDownloadSlots() {
		int dhCount = 0;
		synchronized (bots) {
			for (jDCBot bot : bots)
				dhCount += bot.downloadManager.getAllDHCount();
		}
		int free = _maxDownloadSlots - dhCount;
		return free < 0 ? 0 : free;
	}

	public int getFreeUploadSlots() {
		int uhCount = 0;
		synchronized (bots) {
			for (jDCBot bot : bots)
				uhCount += bot.uploadManager.getAllUHCount();
		}
		int free = _maxUploadSlots - uhCount;
		return free < 0 ? 0 : free;
	}

	/**
	 * @param user
	 * @return Users with matching
	 * user name as <i>user</i>. Multiple hubs
	 * may contain the same user
	 * name hence an ArrayList is
	 * returned. It will never be null.
	 */
	public List<User> getUsers(String user) {
		List<User> usrs = new ArrayList<User>();
		synchronized (bots) {
			for (jDCBot bot : bots) {
				User u = bot.getUser(user);
				if (u != null)
					usrs.add(u);
			}
		}
		return usrs;
	}

	/**
	 * You can safely use jDCBot's
	 * getUser() if you need. When
	 * multiple users with same name
	 * is found (in different hubs)
	 * then arbitraly any one is
	 * returned.
	 * @param user
	 * @return null if
	 * no user with this user name is
	 * found in any of the hubs.
	 */
	public User getUser(String user) {
		synchronized (bots) {
			for (jDCBot bot : bots) {
				User u = bot.getUser(user);
				if (u != null)
					return u;
			}
		}
		return null;
	}

	/**
	 * You can safely use jDCBot's
	 * isBotClientProtoSupports() if you need.
	 */
	public boolean isBotClientProtoSupports(String feature) {
		return jDCBot._clientproto_supports.toLowerCase().indexOf(feature.toLowerCase()) != -1;
	}

	/**
	 * You can safely use jDCBot's
	 * isBotHubProtoSupports() if you need.
	 */
	public boolean isBotHubProtoSupports(String feature) {
		return jDCBot._hubproto_supports.toLowerCase().indexOf(feature.toLowerCase()) != -1;
	}

	public int getTotalHubsConnectedToCount() {
		return bots.size();
	}
	
	public int getTotalHubsConnectedToAsOps() {
		try {
			int count = 0;
			for(jDCBot bot: bots) {
				if(bot.isOp())
					count++;
			}
			return count;
		} catch(ConcurrentModificationException cme) {
			logger.warn("ConcurrentModificationException so sending TotalHubsConnectedToCount" +
					"instead of TotalHubsConnectedToAsOps");
			return getTotalHubsConnectedToCount();
		}
	}
	
	public int getTotalHubsConnectedToAsRegistered() {
		try {
			int count = 0;
			for(jDCBot bot: bots) {
				if(bot.isRegistered() && !bot.isOp())
					count++;
			}
			return count;
		} catch(ConcurrentModificationException cme) {
			logger.warn("ConcurrentModificationException so sending TotalHubsConnectedToRegistered" +
					"instead of TotalHubsConnectedToAsOps");
			return getTotalHubsConnectedToCount();
		}
	}
	
	public int getTotalHubsConnectedToAsNormalUser() {
		int normalCount = getTotalHubsConnectedToCount() - getTotalHubsConnectedToAsOps() - getTotalHubsConnectedToAsRegistered();
		return normalCount < 0? getTotalHubsConnectedToCount(): normalCount;
	}
}
