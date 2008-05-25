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

package org.elite.jdcbot;

import java.net.*;
import java.util.StringTokenizer;
import java.io.*;

/**
 * jDCBot is a Java framework for writing DC (direct connect) bots easily.
 * <p>
 * It provides an event-driven architecture to handle common DC
 * events.
 *  <p>
 * Methods of the jDCBot class can be called to send events to the DC hub
 * that it connects to.  For example, calling the SendPublicMessage method will
 * send a public message.
 *  <p>
 * To perform an action when the jDCBot receives a normal message from the hub, 
 * you would override the onPubicMessage method defined in the jDCBot
 * class.  All on<i>XYZ</i> methods in the PircBot class are automatically called
 * when the event <i>XYZ</i> happens, so you would override these if you wish
 * to do something when it does happen.
 * 
 * @since 0.5
 * @author  Kokanovic Branko
 * @version    0.5
 */
public abstract class jDCBot{

    /**
     * The definitive version number of this release of jDCBot.
     */
    public static final String VERSION = "0.6";

    private InputThread _inputThread=null;
    Socket socket = null;
    
    InputStream input;
    OutputStream output;
    BufferedReader breader;
    UserMenager um;
    
    private String _botname,_password,_description,_conn_type,_email,_sharesize, _hubname;
    private int _slots;
	private InetAddress _ip;
	private int _port;
	
    /**
     * Constructs a jDCBot with your settings.
     *<p>
     * Most setting here depends on your hub. You might have to fake your share size and/or slots for hub to accept you...
     *For details, look at <a href=http://wiki.dcpp.net/index.php/%24MyINFO>DCPP wiki page of $MyINFO command</a>
     * 
     *
     * @param botname Name of the bot as it will appear in the list of users.
     * @param password Passsword if required, you could put anything if no password is needed.
     * @param description Description of your bot as it will appear in the list of users. On your description is appended standard description.
     * @param conn_type Your connection type, for details look <a href=http://wiki.dcpp.net/index.php/%24MyINFO>here</a>
     * @param email Your e-mail address as it will appear in the list of users.
     * @param sharesize Size of your share in bytes.
     * @param slots Number of slots for other user to connect to you.
     */
    public jDCBot(String botname,String password,String description,String conn_type,String email,String sharesize,int slots){
        _botname=botname;
        _password=password;
        //remove this and put
        //_description=description;
        //if you don't hub doesn't require standard description
        _description=description+"<++ V:0.668,M:A,H:1/0/0,S:"+slots+">";
        _conn_type=conn_type;
        _email=email;
        _sharesize=sharesize;
        _slots=slots;
    }
    
    /**
     * Constructs a jDCBot with the default settings.  Your own constructors
     * in classes which extend the jDCBot abstract class should be responsible
     * for changing the default settings if required.
     */
    public jDCBot(){
        _botname="jDCBot";
        _password="";
        _description="<++ V:0.668,M:A,H:1/0/0,S:0>";
        _conn_type="LAN(T1)1";
        _email="";
        _sharesize="0";
        _slots=0;
    }
    
    /**
     * @return Name of the bot
     */
	public final String botname() {
		return _botname;
	}
	
	/**
	 * @return Name of the hub we're connected on
	 */
	public final String hubname(){
		return _hubname;
	}
	
    /**
     * Attempt to connect to the specified DC hub.
     * The OnConnect method is called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     * 
     * @throws Exception if it was not possible to connect to the server.
     * @throws BotException if the server would not let us join it because of bad password or if there
     * exist user with the same name.
     */
    public final void connect(String hostname,int port) throws Exception{
    	
    	String buffer;
    	
    	_port=port;
        
        if (this.isConnected()){
            throw new IOException("Already connected");
        }
	// connect to server
        socket = new Socket(hostname, port);
        input = socket.getInputStream();
        output = socket.getOutputStream();
        breader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        _ip=socket.getInetAddress();
        
        buffer=ReadCommand();
        System.out.println(buffer);
        String lock=buffer.substring(buffer.indexOf(' ')+1,buffer.indexOf(' ', buffer.indexOf(' ')+1));
        System.out.println(lock);
        String key=lock2key(lock);
        buffer="$Key "+key+"|";
        System.out.println(buffer);
        SendCommand(buffer);
        
        buffer="$ValidateNick "+_botname+"|";
        SendCommand(buffer);
        
        buffer=ReadCommand();

        
        while(buffer.startsWith("$Hello")!=true){
            if (buffer.startsWith("$ValidateDenide")) throw new BotException("Validate Denied");
            if (buffer.startsWith("$BadPass")) throw new BotException("Bad password");
            
            if (buffer.startsWith("$GetPass")){
                SendCommand("$MyPass "+_password+"|");
            }
			if (buffer.startsWith("$HubName ")){
				_hubname=buffer.substring(9,buffer.length()-1);
			}
            System.out.println(buffer);
            buffer=ReadCommand();
        }
        
        buffer="$Version 1.0091|";
        SendCommand(buffer);
        
        SendCommand("$GetNickList|");
        
        buffer="$MyINFO $ALL "+_botname+" "+_description+"$ $"+_conn_type+"$"+_email+"$"+_sharesize+"$|";
        System.out.println(buffer);
        SendCommand(buffer);
        
		um = new UserMenager(this);
		do {
			buffer = ReadCommand();

		} while (buffer.startsWith("$NickList ") == false);
		um.addUsers(buffer.substring(10, buffer.length() - 1));
		
        buffer=ReadCommand();
        
        _inputThread=new InputThread(this, breader);
        _inputThread.start();

        onConnect();
    }
    
    /**
     * Attemps to nicely close connection.
     */
    public final void quit(){
	try {
            SendCommand("$Quit "+_botname+"|");
	}
	catch (Exception e) {}
        finally{
            try{
                socket.close();
                socket=null;
            }catch(IOException e){}
        }
    }
    
    /**
     * Returns whether or not the jDCBot is currently connected to a hub.
     * The result of this method should only act as a rough guide,
     * as the result may not be valid by the time you act upon it.
     *
     * @return True if and only if the jDCBot is currently connected to a hub.
     */
    public final boolean isConnected(){
        return _inputThread!=null;
    }
    
    /**
     * Handles all commands from InputThread all passes it to different methods.
     *
     * @param rawCommand Raw command sent from hub
     */
    public final void handleCommand(String rawCommand){
        //System.out.println(rawCommand);
        if (rawCommand.startsWith("<")){
            String user,message;
            user=rawCommand.substring(1, rawCommand.indexOf('>'));
            message=rawCommand.substring(rawCommand.indexOf('>'));
			message = message.substring(2, message.length() - 1);
            this.onPublicMessage(user,message);
        }
        else if (rawCommand.startsWith("$Quit")){
            String user=rawCommand.substring(6);
            user=user.substring(0,user.length()-1);
            um.userQuit(user);
            onQuit(user);
        }
        else if (rawCommand.startsWith("$Hello") && (rawCommand!="$Hello "+_botname+"|")){
            String user=rawCommand.substring(7);
            user=user.substring(0,user.length()-1);
            um.userJoin(user);
            onJoin(user);
        }
        else if (rawCommand.startsWith("$To:")){
            String user,from,message;
            int index1=rawCommand.indexOf('$', 2);
            int index2=rawCommand.indexOf('>', index1);
            
            from=rawCommand.substring(rawCommand.indexOf(':', 4)+2, rawCommand.indexOf('$',2)-1);
            user=rawCommand.substring(index1+2,index2);
            message=rawCommand.substring(index2+2, rawCommand.length()-1);
            if (user.equals(from))
                onPrivateMessage(user,message);
            else
                onChannelMessage(user,from,message);
        }
		else if (rawCommand.startsWith("$Search ")){
			int space=rawCommand.indexOf(' ',9);
			String firstPart=rawCommand.substring(8,space);
			String secondPart=rawCommand.substring(space+1,rawCommand.length()-1);
			StringTokenizer st=new StringTokenizer(secondPart,"?");
			if (st.countTokens()!=5) return;
			boolean isSizeRestricted,isMinimumSize;
			long size;
			int dataType;
			String searchPattern;
			isSizeRestricted=(st.nextToken()=="T");
			isMinimumSize=(st.nextToken()=="T");
			size=Long.parseLong(st.nextToken());
			dataType=Integer.parseInt(st.nextToken());
			searchPattern=st.nextToken();
			//send trigger to passive/active search
			if (firstPart.toLowerCase().startsWith("hub:")){
				String user=firstPart.substring(4);
				onPassiveSearch(user,isSizeRestricted,isMinimumSize,size,dataType,searchPattern);
			}
			else{
				int dotdot=firstPart.indexOf(':');
				String ip=firstPart.substring(0,dotdot);
				int port=Integer.parseInt(firstPart.substring(dotdot+1));
				onActiveSearch(ip,port,isSizeRestricted,isMinimumSize,size,dataType,searchPattern);
			}
		}
		else if (rawCommand.startsWith("$NickList")) {
			um.addUsers(rawCommand.substring(10, rawCommand.length() - 1));
		}
		else if (rawCommand.startsWith("$MyINFO $ALL ")) {
			um.SetInfo(rawCommand.substring(13, rawCommand.length() - 1));
		}
    }
    
    /**
     * Generates key from lock needed to connect to hub.
     *
     * @param lock Lock sent from hub
     *
     * @return Key which is sent back to hub to validate we know algorithm:-/
     */
    private final String lock2key(String lock){
        String key_return;
        int len=lock.length();
        char[] key=new char[len];
        for (int i = 1; i < len; i++)
            key[i] = (char)(lock.charAt(i) ^ lock.charAt(i-1));
        key[0] = (char)(lock.charAt(0) ^ lock.charAt(len-1) ^ lock.charAt(len-2) ^ 5);
        for (int i = 0; i < len; i++)
            key[i] = (char)(((key[i]<<4) & 240) | ((key[i]>>4) & 15));
        
        key_return=new String();
        for (int i = 0; i < len; i++){
           if (key[i]==0){
                key_return+="/%DCN000%/";
           }
           else if (key[i]==5){
               key_return+="/%DCN005%/";
           }
           else if (key[i]==36){
               key_return+="/%DCN036%/";
           }
           else if (key[i]==96){
               key_return+="/%DCN096%/";
           }           
           else if (key[i]==124){
               key_return+="/%DCN124%/";
           }
           else if (key[i]==126){
               key_return+="/%DCN126%/";
           }
           else{
               key_return+=key[i];
           }
        }
        
        System.out.println(key_return);
        return key_return;
    }
    
    /**
     * Checks if user is present on hub
     * 
     * @param user Nick of a user
     * @return true if user exist on this hub, false otherwise
     */
	public final boolean UserExist(String user) {
		return um.UserExist(user);
	}
	
	/**
	 * Gets all of user info
	 * @param user Nick of the user
	 * @return User class that holds everything about specified user if he exist, null otherwise
	 */
	public final User GetUserInfo(String user) {
		if (um.UserExist(user) == false)
			return null;
		else
			return um.getUser(user);
	}

	/**
	 * 
	 * @return Random user from the hub
	 */
	public final User GetRandomUser() {
		return um.getRandomUser();
	}
	
    /**
     * Sends raw command to hub.
     *
     * @param buffer Line which needs to be send. This method won't append "|" on the end on the string
     * if it doesn't exist, so it is up to make sure buffer ends with "|" if you calling this method.
     */ 
    public final void SendCommand(String buffer) throws Exception{
        byte[] bytes=new byte[buffer.length()];
        for (int i=0;i<buffer.length();i++) bytes[i]=(byte)buffer.charAt(i);
        output.write(bytes);
    }
    
    /**
     * Sends public message on main chat.
     *
     * @param message Message to be sent. It shouldn't end with "|".
     */
    public final void SendPublicMessage(String message) throws Exception{
        SendCommand("<"+_botname+"> "+message+"|");
    }
    
    /**
     * Sends private message to specified user.
     *
     * @param user User who will get message.
     * @param message Message to be sent. It shouldn't end with "|".
     */
    public final void SendPrivateMessage(String user, String message) throws Exception{
        SendCommand("$To: "+user+" From: "+_botname+" $<"+_botname+"> "+message+"|");
    }
    
    /**
     * Kicks specified user. note that bot has to have permission to do this
     * @param user User to be kicked
     */
    public final void KickUser(User user){
    	try{
    		SendCommand("$Kick "+user.username()+"|");
    	}catch(Exception e){}
    }
    
    /**
     * Kicks specified user. note that bot has to have permission to do this
     * @param user User to be kicked
     */
    public final void KickUser(String user){
    	try{
    		SendCommand("$Kick "+user+"|");
    	}catch(Exception e){}
    }
    /**
     * Reading command before InputThread is started (only for connecting).
     *
     * @return Command from hub
     */
    private final String ReadCommand() throws Exception{
        int c;
        String buffer=new String();
        do{
            c=input.read();
            if (c==-1) throw new IOException();
            buffer+=(char)c;
        }while(c!='|');
        return buffer;
    }
    
    /**
     * This method serves to send message to all users on the hub. Note that most of the hubs
     * have a flood detection system, so you will want to set timeout interval between two
     * message sendings, or we will get warn and/or kicked!
     * @param message Message to be send to all users
     * @param timeout Timeout interval in milliseconds between sending to two consecutive user
     */
	public final void SendAll(String message,long timeout){
		um.SendAll(message,timeout);
	}
	
	/**
	 * Method for returning search results to active clients. You hould use it carefully if you're not owner/super user of the hub
	 * 'cause this can gets you banned/kicked. Search result you will return here are imaginary
	 * (same as your sharesize).
	 * @param IP IP address that gave us user who was searching for returning results 
	 * @param port Port that gave us user who was searching for returning results
	 * @param isDir Set true if you're returning directory, false if it is a file
	 * @param name Name of the file/dir you're returning. Note that some clients reject names that are note like the one they were searching for.
	 * This means that if someone were searching for 'firefox', and we're returned 'opera', his client won't display our result.
	 * @param size Size of the file in bytes we're returning
	 * @param free_slots How many slots we have opened/unused 
	 */
	public final void SendActiveSearchReturn(String IP,int port,boolean isDir,String name,long size,int free_slots){
		StringBuffer buffer=new StringBuffer();
		String hub_ip=_ip.toString();
		if (hub_ip.contains("/")){
			hub_ip=hub_ip.substring(hub_ip.indexOf('/')+1);
		}
		char c=5;
		if (isDir==true){
			buffer.append("$SR "+_botname+" "+name);
			buffer.append(" "+free_slots+"/"+_slots);
			buffer.append(c);
			buffer.append(_hubname+" (" +hub_ip+":"+_port+")|");
		}
		else{
			buffer.append("$SR "+_botname+" "+name);
			buffer.append(c);
			buffer.append(size+" "+free_slots+"/"+_slots);
			buffer.append(c);
			buffer.append(_hubname+" (" +hub_ip+":"+_port+")|");
		}
		
		try{
			DatagramSocket ds = new DatagramSocket();
			byte[] bytes = new byte[buffer.length()];
			for (int i = 0; i < buffer.length(); i++)
				bytes[i] = (byte) buffer.charAt(i);
			InetAddress address = InetAddress.getByName(IP);
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length, 
			                                           address, port);
			ds.send(packet);
		}catch(Exception e){System.out.println(e);}
	}
	
	/**
	 * Method for returning search results to passive clients. You hould use it carefully if you're not owner/super user of the hub
	 * 'cause this can gets you banned/kicked. Search result you will return here are imaginary
	 * (same as your sharesize).
	 * @param user User who was searching. Since he is in passive mode, we return result to hub
	 * @param isDir Set true if you're returning directory, false if it is a file
	 * @param name Name of the file/dir you're returning. Note that some clients reject names that are note like the one they were searching for.
	 * This means that if someone were searching for 'firefox', and we're returned 'opera', his client won't display our result.
	 * @param size Size of the file in bytes we're returning
	 * @param free_slots How many slots we have opened/unused 
	 */
	public final void SendPassiveSearchReturn(String user,boolean isDir,String name,long size,int free_slots){

		StringBuffer buffer=new StringBuffer();
		String hub_ip=_ip.toString();
		if (hub_ip.contains("/")){
			hub_ip=hub_ip.substring(hub_ip.indexOf('/')+1);
		}
		char c=5;
		if (isDir==true){
			buffer.append("$SR "+_botname+" "+name);
			buffer.append(" "+free_slots+"/"+_slots);
			buffer.append(c);
			buffer.append(_hubname+" (" +hub_ip+":"+_port+")");
			buffer.append(c);
			buffer.append(user+"|");
		}
		else{			
			buffer.append("$SR "+_botname+" "+name);
			buffer.append(c);
			buffer.append(size+" "+free_slots+"/"+_slots);
			buffer.append(c);
			buffer.append(_hubname+" (" +hub_ip+":"+_port+")");
			buffer.append(c);
			buffer.append(user+"|");

		}
		try{
			SendCommand(buffer.toString());
		}catch(Exception e){System.out.println(e);}
	}
	
    /**
     * Called upon succesfully connecting to hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class
     * performs no actions and may be overridden as required.
     */
    protected void onConnect(){}
    
    /**
     * Called upon disconnecting from hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class
     * performs no actions and may be overridden as required.
     */
    protected void onDisconnect(){}
    
    /**
     * Called when public message is received.
     * <p>
     * The implementation of this method in the jDCBot abstract class
     * performs no actions and may be overridden as required.
     *
     * @param user User who sent message.
     * @param message Contents of the message.
     */
    protected void onPublicMessage(String user,String message){}
    
    /**
     * Called when user enter the hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class
     * performs no actions and may be overridden as required.
     *
     * @param user Nema of the user who entered hub.
     */
    protected void onJoin(String user){}
    
    /**
     * Called when user wuits hub.
     * <p>
     * The implementation of this method in the jDCBot abstract class
     * performs no actions and may be overridden as required.
     *
     * @param user of user quited hub.
     */
    protected void onQuit(String user){}
    
    /**
     * Called when bot receives private message.
     * <p>
     * The implementation of this method in the jDCBot abstract class
     * performs no actions and may be overridden as required.
     *
     * @param user Name of user who sent us private message.
     * @param message Contents of private message.
     */
    protected void onPrivateMessage(String user,String message){}
    
    /**
     * Called when channel message in channel where bot is present is received.
     * <p>
     * The implementation of this method in the jDCBot abstract class
     * performs no actions and may be overridden as required.
     *
     * @param user Name of the user who sent message.
     * @param channel Channel on which message is sent.
     * @param message Contents of the channel message.
     */
    protected void onChannelMessage(String user,String channel,String message){}
    
    /**
     * Called when user in passive mode is searching for something. For specific details,
     * (like meaning of dataType field and syntax of searchPattern) you should consult
     * direct connect protocol documentation like:
     * http://dc.selwerd.nl/doc/Command_Types_(client_to_server).html
     * @param user User who is searching
     * @param isSizeRestricted true if user restricted search result for minimum/maximum file size.
     * If false, isMinimumSize and size should not be used and has no meaning
     * @param isMinimumSize true if user restricted his search to file that has minimum size, 
     * false if user restricted search result to maximum size. Used only if isSizeRestricted=true
     * @param size Size that user restricted his search. Is it minimum od maximum size 
     * is contained in isMimimumSizeUsed only if isSizeRestricted=true
     * @param dataType Type of the data user is searching for.
     * @param searchPattern Pattern user is searching for.
     */
	protected void onPassiveSearch(String user,boolean isSizeRestricted,boolean isMinimumSize,long size,int dataType,String searchPattern){
	}

	/**
     * Called when user in passive mode is searching for something. For specific details,
     * (like meaning of dataType field and syntax of searchPattern) you should consult
     * direct connect protocol documentation like:
     * http://dc.selwerd.nl/doc/Command_Types_(client_to_server).html
     * @param IP IP address user who was searching gave to deliver search results
	 * @param port Port user who was searching gave to deliver search results
     * @param isSizeRestricted true if user restricted search result for minimum/maximum file size.
     * If false, isMinimumSize and size should not be used and has no meaning
     * @param isMinimumSize true if user restricted his search to file that has minimum size, 
     * false if user restricted search result to maximum size. Used only if isSizeRestricted=true
     * @param size Size that user restricted his search. Is it minimum od maximum size 
     * is contained in isMimimumSizeUsed only if isSizeRestricted=true
     * @param dataType Type of the data user is searching for.
     * @param searchPattern Pattern user is searching for.
	 */
	protected void onActiveSearch(String IP,int port,boolean isSizeRestricted,boolean isMinimumSize,long size,int dataType,String searchPattern){
	}
}
