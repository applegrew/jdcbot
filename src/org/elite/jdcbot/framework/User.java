/*
 * User.java
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

import java.io.OutputStream;

import org.slf4j.Logger;

/**
 * User class.
 * <p>
 * Holds everything about user (description, e-mail, sharesize...)
 * <p>
 * This class is thread safe.
 * 
 * @since 0.6
 * @author Kokanovic Branko
 * @author AppleGrew
 * @version 0.8.2
 */
public class User {
	private static final Logger logger = GlobalObjects.getLogger(User.class);
    private static final int HASH_CONST = 51;

    public static final int NORMAL_FLAG = 1;
    public static final int AWAY_FLAG = 2;
    public static final int SERVER_FLAG = 4;
    public static final int SERVER_AWAY_FLAG = 6;
    public static final int FIREBALL_FLAG = 8;
    public static final int FIREBALL_AWAY_FLAG = 10;

    private String _username, _desc, _conn, _mail, _share, _tag, _supports = "", _ip = "";
    private int _flag;
    private volatile boolean _hasInfo, _op = false, extraSlotsGranted = false, blockUploadToUser = false;
    private jDCBot _bot;
    private String _CID = "";
    private volatile boolean hasQuit = false;

    private int hashCode = -1;

    User(String username, jDCBot bot) {
	if(username == null || bot == null) {
		logger.error("Username or bot is null: user:" + username + ", bot:" + bot);
		throw new NullPointerException("username or bot is null!!!");
	}
	_username = username;
	_bot = bot;
	setInfo("", "", "", "0");
	_hasInfo = false;
    }

    synchronized void setInfo(String desc, String conn, String mail, String share) {
	_hasInfo = true;

	_desc = desc == null? "": desc;
	_conn = conn == null? "": conn;
	_mail = mail == null? "": mail;
	_share = share == null? "0": share;

	int index = _desc.indexOf('<');
	if (index == -1)
	    _tag = new String();
	else
		try {
	    _tag = _desc.substring(_desc.indexOf('<') + 1, _desc.length() - 1);
		} catch (IndexOutOfBoundsException iobe) {
		logger.warn("Malformed tag in user description: " + _desc);
		_tag = "";
		}

	String flag;
	if (_conn.length() == 0)
	    flag = "1";
	else {
	    flag = _conn.substring(_conn.length() - 1, _conn.length());
	    try {
		Integer.parseInt(flag);
		if ((_conn.length() - 2) >= 0 && _conn.charAt(_conn.length() - 2) == '1')
		    flag = "1" + flag;
	    } catch (NumberFormatException nfe) {
		flag = "1";
	    }
	}
	_flag = Integer.parseInt(flag);
	if (_flag == 3)
	    _flag = 2;
	else if (_flag == 5)
	    _flag = 4;
	else if (_flag == 7)
	    _flag = 6;
	else if (_flag == 9)
	    _flag = 8;
	else if (_flag == 11)
	    _flag = 10;

	init();
	logger.info("User info set: " + toString());
    }

    private void init() {
	hashCode = 1;
	hashCode = hashCode * HASH_CONST + _username.hashCode();
	hashCode = hashCode * HASH_CONST + (_bot == null ? 0 : _bot.getHubSignature().hashCode());
    }

    public boolean hasQuit() {
	return hasQuit;
    }

    void setHasQuit() {
	hasQuit = true;
    }

    public String getClientID() {
	synchronized (_CID) {
	    return _CID;
	}
    }

    public void setClientID(String CID) {
	synchronized (_CID) {
	    _CID = CID;
	}
    }

    public String getHubSignature() {
	return _bot.getHubSignature();
    }

    public boolean hasInfo() {
	return _hasInfo;
    }

    public jDCBot getBot() {
	return _bot;
    }

    public void setSupports(String supports) {
	synchronized (_supports) {
	    _supports = supports.toLowerCase();
	}
	_bot.getDispatchThread().callOnUpdateMyInfo(_username);
    }

    /**
     * Sets if the user is operator or not.
     * @param flag
     */
    public void setOp(boolean flag) {
	_op = flag;
	_bot.getDispatchThread().callOnUpdateMyInfo(_username);
    }

    public boolean isOp() {
	return _op;
    }

    /**
     * Checks if the User supports that protocol feature.
     * @param feature
     * @return
     */
    public boolean isSupports(String feature) {
	synchronized (_supports) {
	    return _supports.indexOf(feature.toLowerCase()) != -1;
	}
    }

    public void setUserIP(String ip) {
	synchronized (_ip) {
	    _ip = ip;
	}
	_bot.getDispatchThread().callOnUpdateMyInfo(_username);
    }

    public String getUserIP() {
	synchronized (_ip) {
	    return _ip;
	}
    }

    /**
     * @return User nick
     */
    public String username() {
	return _username;
    }

    /**
     * @return User description (along with the client tag). If you want real description,
     * use method with that name 
     */
    public String description() {
	synchronized (_desc) {
	    return _desc;
	}
    }

    /**
     * @return Type of user's connection
     */
    public String connection_type() {
	return _conn;
    }

    /**
     * @return User mail he specified
     */
    public String mail() {
	return _mail;
    }

    /**
     * @return Share size of the user in bytes
     */
    public String sharesize() {
	return _share;
    }

    /**
     * @return true if user has client tag, false otherwise
     */
    public boolean hasTag() {
	synchronized (_tag) {
	    return (_tag.length() != 0);
	}
    }

    /**
     * @return Client tag if it exist
     */
    public String tag() {
	synchronized (_tag) {
	    return _tag;
	}
    }

    /**
     * Try to get real description, not just tag
     * @return Description (also known as comment) if it exist, "" otherwise
     */
    public String real_description() {
	String buffer = new String();
	int index = 0;
	while ((_desc.length() > index) && (_desc.charAt(index) != '<')) {
	    buffer = buffer + _desc.charAt(index);
	    index++;
	}
	return buffer;
    }

    /**
     * Tries to get client from the tag
     * @return Client that user use if it exist, "" otherwise
     */
    public String client() {
	synchronized (_tag) {
	    if (_tag.length() < 1)
		return "";
	    if (_tag.indexOf(' ') == -1)
		return "";
	    return _tag.substring(0, _tag.indexOf(' '));
	}
    }

    /**
     * Tries to get client version form user
     * @return Version of the client user use if it exist in tag, "" otherwise
     */
    public String version() {
	synchronized (_tag) {
	    if (_tag.contains(" V:")) {
		int index1 = _tag.indexOf(" V:") + 2;
		int index2 = _tag.indexOf(',', index1);
		if (index2 == -1)
		    return "";
		return _tag.substring(index1, index2 + 1);
	    } else
		return "";
	}
    }

    /**
     * 
     * @return true if user is active, false if it is in passive mode, or tag does not exist
     */
    public boolean isActive() {
	synchronized (_tag) {
	    if (_tag.contains(",M:A"))
		return true;
	    else
		return false;
	}
    }

    /**
     * 
     * @return Number of slots user have if exists in tag, 0 otherwise 
     */
    public int slots() {
	synchronized (_tag) {
	    if (_tag.contains(",S:")) {
		int index1 = _tag.indexOf(",S:") + 3;
		String buffer = new String();
		while ((_tag.charAt(index1) != '>') && (_tag.charAt(index1) != ',') && (_tag.charAt(index1) != ' '))
		    buffer += _tag.charAt(index1++);
		return Integer.parseInt(buffer);
	    } else
		return 0;
	}
    }

    public boolean isGrantedExtraSlot() {
	return extraSlotsGranted;
    }

    /**
     * Grants an extra slot to the user.
     * <p>
     * Note that the grant won't be revoked automatically
     * after any time period.
     * @param flag Set this to false to revoke
     * extra slot grant.
     */
    public void setGratedExtraSlotFlag(boolean flag) {
	extraSlotsGranted = flag;
    }

    /**
     * To block this user from downloading any
     * files from you.
     * @param flag Set this to ture to enable this, else false.
     * @see org.elite.jdcbot.shareframework.ShareManager#canUpload(User,String)
     */
    public void setBlockUploadToUser(boolean flag) {
	blockUploadToUser = flag;
    }

    public boolean isUploadToUserBlocked() {
	return blockUploadToUser;
    }

    public int getFlag() {
	return _flag;
    }

    /**
     * This is the function users of the framework are expected to use to download files.
     * @param de
     * @throws BotException 
     */
    public void download(DUEntity de) throws BotException {
	_bot.downloadManager.download(de, this);
    }

    /**
     * This is the function users of the framework are expected to use to download file list.
     * @param os The OutputStream where the file list will be saved. <b>Note:</b> It will be <u>decompressed</u>
     * by default, unless you set the <i>settings</i> that it shouldn't.
     * @param settings See {@link DUEntity#settingFlags settingFlags}
     * @throws BotException 
     */
    public void downloadFileList(OutputStream os, int settings) throws BotException {
	DUEntity de = new DUEntity(DUEntity.Type.FILELIST, "", 0, -1, os, settings);
	_bot.downloadManager.download(de, this);
    }

    /**
     * Cancels download of a file. The DUEntity must have file, fileType, start and len set to proper values to
     * identify the download entity fully, others fields like os, in and settings are not checked, and hence can
     * have any value.
     * @param de The entity that should be canceled.
     */
    public void cancelDownload(DUEntity de) {
	_bot.downloadManager.cancelDownload(de, this);
    }

    public void cancelFileListDownload() {
	DUEntity de = new DUEntity(DUEntity.Type.FILELIST, "");
	_bot.downloadManager.cancelDownload(de, this);
    }

    public void cancelUpload() {
	_bot.uploadManager.cancelUpoad(this);
    }

    @Override
    public boolean equals(Object o) {
	if (this == o)
	    return true;

	if (o instanceof User) {
	    User u = (User) o;
	    if (u.username().equalsIgnoreCase(username()) && u.getHubSignature().equals(getHubSignature()))
		return true;
	}
	return false;
    }

    @Override
    public int hashCode() {
	return hashCode;
    }

    @Override
    public String toString() {
	return new StringBuffer(_username).append("@").append(getHubSignature())
	.append(", CID:").append(_CID.isEmpty() ? "unknown" : _CID)
	.append(", Has Info:").append(_hasInfo)
	.append(", isOp:").append(_op)
	.append(", IP:").append(_ip)
	.append(", Tag:").append(_tag)
	.toString();
    }

}
