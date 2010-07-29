/*
 * UploadHandler.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.zip.DeflaterOutputStream;

import org.elite.jdcbot.shareframework.ShareManager;
import org.slf4j.Logger;

/**
 * Created on 26-May-08<br>
 * Handles all the uploads to a single user for a session.
 * <p>
 * The framework manages this hence you need not bother
 * about this.
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1.3
 */
public class UploadHandler extends InputThreadTarget {
	private static final Logger logger = GlobalObjects.getLogger(UploadHandler.class);
	private final int BUFF_SIZE = 64 * 1024; //64 KB

	private Socket socket;
	private UploadManager um;
	private jDCBot jdcbot;
	private ShareManager sm;
	private TimeoutInputThread inputThread = null;
	/**
	 * This variable is important so fair upload
	 * policy can be enforced. It would be better
	 * to explain using an example.
	 * <p>
	 * If suppose <b>S</b> is a special user who has
	 * been granted an extra slot, and <b>N</b> is a normal
	 * user who has no extra slot granted to him.
	 * <p>
	 * If we have only one upload slot open and <b>N</b> starts
	 * a download first and now <b>S</b> later tries to download;
	 * if we donot use this variable then running upload of
	 * <b>N</b> will be interrupted by <b>S</b> and <b>N</b> will suddenly get
	 * the no free slots error. This is unfair to <b>N</b>.
	 * <p>
	 * Another possible implementation without using this
	 * variable too <i>seems</i> good. It is that we instead
	 * query from UploadManager about number of running uploads
	 * to normal users only. But, this too has a problem of
	 * being too liberal about uploading. Suppose if we have
	 * only one upload slot open and <b>S</b> is already
	 * downloading from us, if at that moment if <b>N</b>
	 * tries to download then he too would be allowed to
	 * download. This time refusing to user <b>N</b> wouldn't
	 * have been rude, yet we are alowing two uploads instead
	 * of one.
	 */
	private boolean isfirstUpload = true;
	private User user;
	private volatile boolean close;
	private volatile boolean cancelUpload;

	UploadHandler(User usr, Socket socket, jDCBot jdcbot, UploadManager um) {
		this.um = um;
		this.socket = socket;
		this.jdcbot = jdcbot;
		sm = jdcbot.getShareManager();
		user = usr;
		close = false;
		cancelUpload = false;
	}

	public void startUploads() {
		if (inputThread == null) {
			try {
				inputThread = new TimeoutInputThread(this, socket.getInputStream());
			} catch (IOException e) {
				logger.error("IOException by socket.getInputStream() in startUploads(): " + e.getMessage(), e);
			}
			cancelUpload = false;
			isfirstUpload = true;
			inputThread.start();
		}
	}

	public String getUserName() {
		return user.username();
	}

	public void close() throws IOException {
		inputThread.stop();
		close = true;
		if (socket != null & !socket.isClosed())
			socket.close();
		socket = null;
	}

	/**
	 * Cancels the currently running upload.
	 * It won't affect the next upload to the user.
	 * Infact if the remote client retries to get
	 * that file again then it won't affect that
	 * attempt. You will need to invoke this
	 * mathod again then.
	 */
	public void cancelUpload() {
		cancelUpload = true;
	}

	@Override
	public void handleCommand(String cmd) {
		upload(cmd);
	}

	public void upload(String cmd) { //Called by inputThread thread only.
		if (socket == null) {
			cancelUpload = false;
			return;
		}

		logger.debug("From remote client:" + cmd);

		boolean ZLIG = false;
		InputStream in = null;
		OutputStream os = null;
		long fileLen = 0;
		String buffer;
		DUEntity due = null;

		try {
			if (cmd.startsWith("$ADCGET")) {
				String params[] = parseRawCmd(cmd); //Parsing $ADCGET F S LZ then //Z is ' ZL1' or nothing
				String fileType = params[1].toLowerCase().trim();
				String file = params[2];
				long start = Long.parseLong(params[3]);
				fileLen = Long.parseLong(params[4]);
				String Z = params[params.length - 1]; //Using this weird method because Z may or may not be present.

				if (Z.equalsIgnoreCase("ZL1"))
					ZLIG = true;

				DUEntity.Type fType = DUEntity.Type.FILE;
				if (fileType.equals("file")) {
					if (file.equals("files.xml.bz2") || file.equals("MyList.bz2"))
						fType = DUEntity.Type.FILELIST;
					else
						fType = DUEntity.Type.FILE;
				} else if (fileType.equals("tthl"))
					fType = DUEntity.Type.TTHL;

				if (isfirstUpload && fType != DUEntity.Type.FILELIST && !user.isGrantedExtraSlot() && jdcbot.getFreeUploadSlots() <= 0) {
					buffer = "$MaxedOut|";
					try {
						SendCommand(buffer, socket);
						logger.debug("From bot: " + buffer);
					} catch (Exception e) {
						logger.error("Exception by SendCommand in upload(): " + e.getMessage(), e);
					} finally {
						try {
							socket.close();
						} catch (IOException e) {
							logger.error("Exception by socket.close() in upload(): " + e.getMessage(), e);
						}
					}
					cancelUpload = false;
					return;
				}

				if (fType != DUEntity.Type.FILELIST)
					isfirstUpload = false;

				try {
					if (fType == DUEntity.Type.FILELIST)
						due = sm.getFileList(user);
					else
						due = sm.getFile(user, file, fType, start, fileLen);
				} catch (FileNotFoundException e1) {
					buffer = "$Error " + e1.getMessage() + "|";
					try {
						SendCommand(buffer, socket);
					} catch (Exception e) {
						logger.error("Exception by SendCommand in upload(): " + e.getMessage(), e);
					}
					logger.debug("From bot: " + buffer);
					cancelUpload = false;
					return;
				}

				try {
					in = due.in();
					fileLen = due.len();
					if (ZLIG) {
						buffer = "$ADCSND " + due.getFileType() + " " + file + " " + due.start() + " " + due.len() + " ZL1|";
						SendCommand(buffer, socket);
						logger.debug("From bot: " + buffer);
					} else {
						buffer = "$ADCSND " + due.getFileType() + " " + file + " " + due.start() + " " + due.len() + "|";
						SendCommand(buffer, socket);
						logger.debug("From bot: " + buffer);
					}
				} catch (Exception e) {
					logger.error("Exception by SendCommand in upload(): " + e.getMessage(), e);
					cancelUpload = false;
					return;
				}
			} else {
				logger.warn("Unsupported protocol requested by remote client. Command sent was: " + cmd);
				cancelUpload = false;
				return;
			}

			try {
				if (ZLIG)
					os = new DeflaterOutputStream(socket.getOutputStream());
				else
					os = socket.getOutputStream();
			} catch (IOException e) {
				logger.error("IOException while getting OutputStream of the socket in upload(): " + e.getMessage(), e);
				cancelUpload = false;
				return;
			}

			if (in != null && os != null) {
				jdcbot.getDispatchThread().callOnUploadStart(user, due);
				int c;
				byte buff[] = new byte[BUFF_SIZE];
				try {
					while ((c = in.read(buff)) != -1 && !close) {
						if (cancelUpload) {
							throw new BotException(BotException.Error.TRANSFER_CANCELLED);
						}
						os.write(buff, 0, c);
					}
					in.close();
					if (os instanceof DeflaterOutputStream)
						((DeflaterOutputStream) os).finish();
					else
						os.flush();
					jdcbot.getDispatchThread().callOnUploadComplete(user, due, true, null);
				} catch (IOException ioe) {
					logger.error("IOException in startUploads(): " + ioe.getMessage(), ioe);
					jdcbot.getDispatchThread().callOnUploadComplete(user, due, false,
							new BotException(ioe.getMessage(), BotException.Error.IO_ERROR));
				} catch (BotException e) {
					jdcbot.getDispatchThread().callOnUploadComplete(user, due, false, e);
				}
				cancelUpload = false;

			} else {
				logger.info("InputStream or OutputStream was null hence no data was sent. IN==" + in + ", OUT==" + os);
			}
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					logger.error("Exception in upload()", e);
				}
				cancelUpload = false;
		}
	}

	@Override
	public void disconnected() {
		um.tasksComplete(this);
	}
}
