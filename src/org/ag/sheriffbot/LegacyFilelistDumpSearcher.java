/*
 * LegacyFilelistDumpSearcher.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author AppleGrew
 * @since 0.7.1
 * @version 0.1
 * @deprecated SheriffBot no longer creates specially parsed dumps of file lists. Use {@link org.ag.sheriffbot.FilelistDumpSearcher FilelistDumpSearcher} instead.
 * @see FilelistDumpSearcher
 */
public class LegacyFilelistDumpSearcher {

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

	LegacyFilelistDumpSearcher fds = new LegacyFilelistDumpSearcher();

	boolean showTTH = false;
	boolean phpSerialize = false;
	String hubname = null;
	int type = Content.ANY;
	long size = -1;
	int i = 0;
	String dumpLoc = null;
	for (i = 0; i < args.length; i++) {
	    if (args[i].equals("-t")) {
		if (args[i + 1].equals("f"))
		    type = Content.FILE;
		else if (args[i + 1].equals("d"))
		    type = Content.DIR;
		else {
		    System.err.println("Wrong type: " + args[i + 1]);
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
	    System.exit(1);
	}
	if (i > args.length - 1) {
	    System.err.println("No serach term given.");
	    System.exit(1);
	}

	File f = new File(dumpLoc);
	if (f.isFile()) {
	    Vector<String> r = fds.search(dumpLoc, args[i], type, size, showTTH, phpSerialize, hubname);
	    if (r != null)
		System.out.print(LegacyFilelistDumpSearcher.Vector2String(LegacyFilelistDumpSearcher.removeDuplicates(r)));
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
	    System.out.print(LegacyFilelistDumpSearcher.Vector2String(LegacyFilelistDumpSearcher.removeDuplicates(res)));
	} else {
	    System.err.println(dumpLoc + " is an invalid location.");
	    System.exit(1);
	}
	System.out.println();
    }

    public Vector<String> search(String dumpfile, String srfor, int type, long size, boolean showTTH, boolean phpSerialize, String hubname) {
	Vector<String> results = new Vector<String>();

	String currentUser = "";
	Vector<String> dirs = new Vector<String>();
	int currentlevel = -1;
	BufferedReader bin = null;

	try {
	    bin = new BufferedReader(new FileReader(dumpfile));
	    String line = null;
	    line = bin.readLine();
	    String dnt = line;
	    if (line == null) {
		dnt = "";
		return null;
	    }

	    line = bin.readLine();
	    if (line == null)
		return null;

	    if (!phpSerialize)
		results.add("Dump's Date and Time stamp: " + dnt + "\n==========================\n" + "hubname: " + line);
	    else
		results.add("$" + dnt + "\n" + "|" + line);

	    if (hubname != null && !line.trim().toLowerCase().contains(hubname.toLowerCase().subSequence(0, hubname.length()))) {
		results.add("No hits.");
		return results;
	    }

	    boolean anyHit = false;

	    while ((line = bin.readLine()) != null) {

		Content content = parse(line);
		if (content.type == Content.USER) {
		    currentUser = content.value;
		} else {
		    if (content.type == Content.DIR) {
			if (content.level <= currentlevel) {
			    while (content.level <= currentlevel) {
				dirs.remove(currentlevel--);
			    }
			}
			dirs.add(content.value);
			currentlevel++;
		    }

		    // Searching.
		    boolean found = false;
		    String result = null;
		    srfor = srfor.trim();
		    result = currentUser + ":" + (content.type == Content.FILE && showTTH ? content.TTH + ":" : "") + getPwd(dirs);
		    if (result.trim().toLowerCase().contains(srfor.toLowerCase().subSequence(0, srfor.length()))
			    || content.type == Content.FILE && content.TTH.equalsIgnoreCase(srfor)) {
			found = true;
		    }
		    if (type != Content.ANY && content.type != type)
			found = false;
		    if (size >= 0 && content.type == Content.FILE)
			if ((size == 0 && content.size != size) || (size != 0 && (((double) (Math.abs(content.size - size))) / size) > 0.1))
			    found = false;

		    if (found) {
			anyHit = true;
			if (!phpSerialize) {
			    //result = currentUser + ":" + (content.type == Content.FILE && showTTH ? content.TTH + ":" : "") + getPwd(dirs);
			    if (content.type == Content.FILE)
				result = result + "/" + content.value;
			} else {
			    int index = 0;
			    result = serializeEntity(index++, content.type == Content.FILE ? "f" : "d");
			    result = result + serializeEntity(index++, currentUser);
			    if (content.type == Content.FILE && showTTH)
				result = result + serializeEntity(index++, content.TTH);
			    result = result + serializeEntity(index++, getPwd(dirs));
			    if (content.type == Content.FILE)
				result = result + serializeEntity(index++, content.value);

			    result = "a:" + index + ":{" + result + "}";
			}
			results.add(result); // ADDING THE RESULT.
		    }
		}
	    }
	    if (!anyHit)
		results.add("No hits.");
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
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

    private static String serializeEntity(int index, String s) {
	return "i:" + index + ";s:" + s.length() + ":\"" + s + "\";";
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
		vec.add(res.get(i));
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

    private String getPwd(Vector<String> dirs) {
	String pwd = "";
	for (String dir : dirs) {
	    pwd = pwd + "/" + dir;
	}
	return pwd;
    }

    private static Content parse(String entry) {
	Content content = new Content();

	switch (entry.charAt(0)) {
	    case 'u':
		content.type = Content.USER;
		break;
	    case 'f':
		content.type = Content.FILE;
		break;
	    case 'd':
		content.type = Content.DIR;
		break;
	}

	int pos = 1;
	int secondSpcPos = entry.indexOf(' ', 2);
	if (content.type == Content.FILE || content.type == Content.DIR) {
	    content.level = Integer.parseInt(entry.substring(2, secondSpcPos));
	    pos = secondSpcPos;

	    if (content.type == Content.FILE) {
		int thirdpos = entry.indexOf(' ', secondSpcPos + 1);
		content.size = Long.parseLong(entry.substring(secondSpcPos + 1, thirdpos));
		pos = entry.indexOf(' ', thirdpos + 1);
		content.TTH = entry.substring(thirdpos + 1, pos);
	    }

	} else if (content.type == Content.USER) {
	    pos = secondSpcPos;
	    content.ip = entry.substring(2, secondSpcPos);
	}

	content.value = entry.substring(pos + 1);

	return content;
    }

    public static class Content {
	static final int USER = 0;

	static final int FILE = 1;

	static final int DIR = 2;

	static final int ANY = 3;

	String value;

	int type;

	int level = 0;

	long size = 0;

	String ip = "";

	String TTH = "";
    }
}
