/*
 * WebPageFetcher.java
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

package org.elite.jdcbot.util;

/*
 * From http://www.warnertechnology.com/Computers/Articles/JavaMOSXS/javacode1.shtml and
 * http://www.javapractices.com/Topic147.cjp
 * Slighty changed for our needs
 */
import java.io.*;
import java.net.*;

/**
* Fetches the HTML content of a web page as a String.
* Idea is to use derived class, example can be found in GoogleCalculation class
* This class just fetches the page, to parse things you want from web page, use derived class
* 
* @since 0.6
* @author  Kokanovic Branko
* @version    0.6
*/
public abstract class WebPageFetcher {

	private URL fURL;
	private static final String fHTTP = "http";
	private static final String fNEWLINE = System.getProperty("line.separator");
	
	public WebPageFetcher( URL aURL ){
		if ( !aURL.getProtocol().equals(fHTTP) ) {
			throw new IllegalArgumentException("URL is not for HTTP Protocol: " + aURL);
		}
		fURL = aURL;
	}

	public WebPageFetcher( String aUrlName ) throws MalformedURLException {
		this ( new URL(aUrlName) );
	}
 
	public WebPageFetcher(){}

  
	protected void SetURL(URL aURL){
		if ( !aURL.getProtocol().equals(fHTTP) ) {
			throw new IllegalArgumentException("URL is not for HTTP Protocol: " + aURL);
		}
		fURL = aURL;
	}
 
	protected void SetURL(String aUrlName ) throws MalformedURLException {
		SetURL(new URL(aUrlName));
	}
 
	/**
	* Fetch the HTML content of the page as simple text.
	*/
	protected String getPageContent() throws UnknownHostException, IOException {
		int result=0, fetched=0;
		char[] cbuf=new char[65000];
		String theInfo;
		try{
			URLConnection conn = fURL.openConnection();
			//google won't let us in without this property
			conn.setRequestProperty("User-Agent", "Mozilla/4.x");
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			// due to the nature of sockets, you probably wont get all the text back with one call
			// so we continue to call read until we get -1, which means were done,
			// or until we run out of space in our array of characters
			while ((fetched!=-1) && (result<65000)) {
				fetched=in.read(cbuf,result,65000-result);
				result+=fetched;
			}
			in.close();
		}catch (UnknownHostException e) {
			System.err.println("Don't know about host: "+e);
		}catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection: "+e);
		}catch (Exception e) {
			System.err.println(e);
		}finally {
			// convert the array of characters to a String
			// being sure to convert only the characters that have
			// data, not the entire 65,000 character array
			theInfo=new String(cbuf,0,result);
		}
		return theInfo;
	}

	/**
	* Fetch the HTML headers as simple text.
	*/
	public String getPageHeader(){
		StringBuffer result = new StringBuffer();
		URLConnection connection = null;
		try {
			connection = fURL.openConnection();
		}catch (IOException ex) {
			System.err.println("Cannot open connection to URL: " + fURL);
		}

		//not all headers come in key-value pairs - sometimes the key is
		//null or an empty String
		int headerIdx = 0;
		String headerKey = null;
		String headerValue = null;
		while ( (headerValue = connection.getHeaderField(headerIdx)) != null ) {
			headerKey = connection.getHeaderFieldKey(headerIdx);
			if ( headerKey != null && headerKey.length()>0 ) {
				result.append( headerKey );
				result.append(" : ");
			}
			result.append( headerValue );
			result.append(fNEWLINE);
			headerIdx++;
		}
		return result.toString();
	}
} 