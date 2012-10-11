/*
 * FilelistDumpSearcher.java
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.elite.jdcbot.util.GlobalFunctions;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Use this to search dumps created by SheriffBot.<br>
 * <p>
 * Using this you can search for variety of information in the dumps. Just give it the directory name of the dump files
 * and give it the search query. Run it to learn about the options it supports.<br>
 * <p>
 * Please note though, that this uses seems to use too much processing power and is quite slow. I still don't know
 * where the bottleneck is. If you find out the bottleneck then do let me know.
 * 
 * @author AppleGrew
 * @since 0.7.1
 * @version 0.1.1
 * 
 */
public class FilelistDumpSearcher {

    public static void main(String args[]) {
	if (args.length == 0 || args[0].equals("-h")) {
	    System.out.println("Argument Syntax: [-T|-t|-s|-p|-H] -d pathToDumpFileOrFolder SearchTerm");
	    System.out.println("-t stands for types. It can take one of - f and d; for file and directory.");
	    System.out.println("-s stands for size of file in MBs intended. It can only be specified for files.");
	    System.out.println("-T instructs to show the TTH of files. By default it is not shown.");
	    System.out.println("-p instructs to display the result row in PHP serialized form. By default it is not shown like that.");
	    System.out
		    .println("-H stands for hub name. It takes the name of the hub. The results retured are from hubs with such similar names only.");
	}

	FilelistDumpSearcher fds = new FilelistDumpSearcher();

	boolean showTTH = false;
	boolean phpSerialize = false;
	String hubname = null;
	int type = FilelistHandler.ANY;
	long size = -1;
	int i = 0;
	String dumpLoc = null;
	for (i = 0; i < args.length; i++) {
	    if (args[i].equals("-t")) {
		if (args[i + 1].equals("f"))
		    type = FilelistHandler.FILE;
		else if (args[i + 1].equals("d"))
		    type = FilelistHandler.DIR;
		else {
		    System.err.println("Wrong type: " + args[i + 1]);
		    System.err.println("Arguments passed:\n" + arr2Str(args));
		    System.exit(1);
		}
		i++;
	    } else if (args[i].equals("-s")) {
		size = Long.parseLong(args[++i]) * 1024 * 1024;
	    } else if (args[i].equals("-d")) {
		dumpLoc = args[++i];
	    } else if (args[i].equals("-T")) {
		showTTH = true;
	    } else if (args[i].equals("-p")) {
		phpSerialize = true;
	    } else if (args[i].equals("-H")) {
		hubname = args[++i];
	    } else
		break;
	}
	if (dumpLoc == null) {
	    System.err.println("No dump file location given.");
	    System.err.println("Arguments passed:\n" + arr2Str(args));
	    System.exit(1);
	}
	if (i > args.length - 1) {
	    System.err.println("No serach term given.");
	    System.err.println("Arguments passed:\n" + arr2Str(args));
	    System.exit(1);
	}

	File f = new File(dumpLoc);
	if (f.isFile()) {
	    Vector<String> r = fds.search(dumpLoc, args[i], type, size, showTTH, phpSerialize, hubname);
	    if (r != null)
		System.out.print(FilelistDumpSearcher.Vector2String(FilelistDumpSearcher.removeDuplicates(r)));
	} else if (f.isDirectory()) {
	    Vector<String> res = new Vector<String>();
	    File dfs[] = f.listFiles();
	    Arrays.sort(dfs, new Comparator<File>() {
		public int compare(File o1, File o2) {
		    Pattern pattern = Pattern.compile("^.*-([0-9]{4}?)-([0-9]{2}?)-([0-9]{2}?)_([0-9]{2}?).([0-9]{2}?).([0-9]{2}?)$");
		    Matcher matcher1 = pattern.matcher(o1.getName());
		    Matcher matcher2 = pattern.matcher(o2.getName());
		    if (matcher1.matches() && matcher2.matches()) {
			for (int i = 1; i < matcher1.groupCount() && i < matcher2.groupCount(); i++)
			    if (Integer.parseInt(matcher1.group(i)) < Integer.parseInt(matcher2.group(i)))
				return 1;
			    else if (Integer.parseInt(matcher1.group(i)) > Integer.parseInt(matcher2.group(i)))
				return -1;
		    }
		    return 0;
		}
	    });
	    for (File df : dfs) {
		Vector<String> r = fds.search(df.getAbsolutePath(), args[i], type, size, showTTH, phpSerialize, hubname);
		if (r != null)
		    res.addAll(r);
	    }
	    System.out.print(FilelistDumpSearcher.Vector2String(FilelistDumpSearcher.removeDuplicates(res)));
	} else {
	    System.err.println(dumpLoc + " is an invalid location.");
	    System.err.println("Arguments passed:\n" + arr2Str(args));
	    System.exit(1);
	}
	System.out.println();
    }

    public static String arr2Str(String arr[]) {
	String r = "";
	for (String a : arr)
	    r = r + a + "\n";
	return r;
    }

    public Vector<String> search(String dumpfile, String srfor, int type, long size, boolean showTTH, boolean phpSerialize, String hubname) {
	Vector<String> results = new Vector<String>();
	BufferedInputStream ubin = null;
	CBZip2InputStream bin = null;

	try {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		//XMLReader parser = XMLReaderFactory.createXMLReader();
		ubin = new BufferedInputStream(new FileInputStream(dumpfile));
		ubin.read(new byte[2]); //To discard the starting BZ flag.
		bin = new CBZip2InputStream(ubin);

		String line = null;
		int c = 0;
		line = "";
		while ((c = bin.read()) != '\n' && c != -1)
			line = line + (char) c;
		String dnt = line;
		if (c == -1) {
			dnt = "";
			return null;
		}

		line = "";
		while ((c = bin.read()) != '\n' && c != -1)
			line = line + (char) c;
		if (c == -1)
			return null;

		if (!phpSerialize)
			results.add("Dump's Date and Time stamp: " + dnt + "\n==========================\n" + "hubname: " + line);
		else
			results.add("$" + dnt + "\n" + "|" + line);

		if (hubname != null && !line.trim().toLowerCase().contains(hubname.toLowerCase().subSequence(0, hubname.length()))) {
			results.add("No hits.");
			return results;
		}

		while ((c = bin.read()) != '\n' && c != -1)
			;

		FilelistHandler handler = new FilelistHandler(srfor, type, size, showTTH, phpSerialize, results);
		//parser.setContentHandler(handler);
		try {
			parser.parse(bin, handler);
			//InputSource insrc = new InputSource(bin);
			//insrc.setEncoding("UTF-8");
			//parser.parse(insrc);
		} catch (org.xml.sax.SAXParseException saxe) {
			saxe.printStackTrace();
		}

		if (results.size() == 1) {
			results.add("No hits.");
		}

	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (ParserConfigurationException e) {
	    ///e.printStackTrace();
	} catch (SAXParseException e) {
	    e.printStackTrace();
	    System.err.println("Line: " + e.getLineNumber() + "; Col:" + e.getColumnNumber());
	} catch (SAXException e) {
	    e.printStackTrace();
	} finally {
		if (bin != null) {
			try {
				bin.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	return results;

    }

    public static Vector<String> removeDuplicates(Vector<String> res) {
	Vector<String> vec = new Vector<String>();

	for (int i = res.size() - 1; i >= 0; i--) {
	    boolean dup = false;
	    for (int j = i - 1; j >= 0; j--) {
		if (res.get(i).equals(res.get(j))) {
		    dup = true;
		    break;
		}
	    }
	    if (!dup)
		vec.insertElementAt(res.get(i), 0);
	    //vec.add(res.get(i));
	}
	return vec;
    }

    public static String Vector2String(Vector<String> vec) {
	String res = null;
	for (String l : vec) {
	    if (res != null)
		res = res + "\n";
	    else
		res = "";
	    res = res + l;
	}
	return res;
    }

    public static String getMiscStats(String dumpfile) {
	return null;
    }

    private class FilelistHandler extends DefaultHandler {
	static final int USER = 0;
	static final int FILE = 1;
	static final int DIR = 2;
	static final int ANY = 3;
	static final int UNKNOWN = 4;

	private Vector<String> results;
	Vector<String> dirs = new Vector<String>();

	private String srfor;
	private String srforArr[];
	private int type;
	private long size;
	private boolean showTTH;
	private boolean phpSerialize;

	private String currentUser;
	private String currentIP;
	private String pwd = "";

	public FilelistHandler(String Srfor, int Type, long Size, boolean ShowTTH, boolean PhpSerialize, Vector<String> res) {
	    srfor = Srfor.trim().toLowerCase();
	    srforArr = srfor.split(" ");
	    type = Type;
	    size = Size;
	    showTTH = ShowTTH;
	    phpSerialize = PhpSerialize;
	    results = res;
	}

	public void startElement(String uri, String lname, String qname, Attributes attrs) throws SAXException {
	    String result = null;
	    long currsize = 0L;
	    String TTH = "";
	    String value = "";
	    int currType = UNKNOWN;

	    if (qname.equalsIgnoreCase("Directory")) {
		value = attrs.getValue("Name");
		dirs.add(value);
		currType = DIR;

	    } else if (qname.equalsIgnoreCase("user")) {
		currentUser = attrs.getValue("username");
		currentIP = attrs.getValue("ip");
		currType = USER;

	    } else if (qname.equalsIgnoreCase("File")) {
		value = attrs.getValue("Name");
		currsize = Long.parseLong(attrs.getValue("Size"));
		TTH = attrs.getValue("TTH");
		currType = FILE;
	    }

	    if (currType == DIR)
		pwd = getPwd(dirs);

	    if (currType == FILE || currType == DIR) {
		// Searching.
		boolean found = false;
		result = currentUser + ":" + currentIP + ":" + (currType == FILE && showTTH ? TTH + ":" : "") + pwd;
		if (GlobalFunctions.matches(srforArr, result) || currType == FILE && TTH.equalsIgnoreCase(srfor)) {
		    found = true;
		}
		if (type != ANY && currType != type)
		    found = false;
		if (size >= 0 && currType == FILE)
		    if ((size == 0 && currsize != size) || (size != 0 && (((double) (Math.abs(currsize - size))) / size) > 0.1))
			found = false;

		if (found) {
		    if (!phpSerialize) {
			// result = currentUser + ":" + (content.type == Content.FILE && showTTH ? content.TTH + ":" : "") + getPwd(dirs);
			if (currType == FILE)
			    result = result + "/" + value;
		    } else {
			int index = 0;
			result = serializeEntity(index++, currType == FILE ? "f" : "d");
			result = result + serializeEntity(index++, currentUser);
			result = result + serializeEntity(index++, currentIP);
			if (currType == FILE && showTTH)
			    result = result + serializeEntity(index++, TTH);
			result = result + serializeEntity(index++, getPwd(dirs));
			if (currType == FILE)
			    result = result + serializeEntity(index++, value);

			result = "a:" + index + ":{" + result + "}";
		    }
		    results.add(result); // ADDING THE RESULT.
		}
	    }
	    //Thread.yield();
	}

	public void endElement(String uri, String lname, String qname) throws SAXException {
	    if (qname.equalsIgnoreCase("Directory")) {
		dirs.remove(dirs.size() - 1);
	    }
	}

	private String getPwd(Vector<String> dirs) {
	    String pwd = "";
	    for (String dir : dirs) {
		pwd = pwd + "/" + dir;
	    }
	    return pwd;
	}

	/**
	 * Helper function to convert to PHP serialized form, so that
	 * the output of this program can be automatically converted to
	 * PHP array. I infact prepared a website for it. ;-)
	 * @param index
	 * @param s
	 * @return
	 */
	private String serializeEntity(int index, String s) {
	    return "i:" + index + ";s:" + s.length() + ":\"" + s + "\";";
	}
    }
}
