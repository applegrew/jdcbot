/*
 * FilelistConverter.java
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
package org.elite.jdcbot.shareframework;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Created on 11-Jun-08<br>
 * Parses the XML filelist and converts
 * that to FLDir and FLFile tree.
 * 
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class FilelistConverter extends ByteArrayInputStream {
    private FLDir fl = new FLDir("Root", true, null);

    public FilelistConverter(byte in[]) {
	super(in);
    }

    public FLDir parse() throws ParserConfigurationException, SAXException, IOException {
	SAXParserFactory factory = SAXParserFactory.newInstance();
	SAXParser parser = factory.newSAXParser();

	FilelistHandler handler = new FilelistHandler();

	parser.parse(this, handler);

	return fl;
    }

    private class FilelistHandler extends DefaultHandler {
	private FLDir pwd = fl;

	public void startElement(String uri, String lname, String qname, Attributes attrs) throws SAXException {

	    if (qname.equalsIgnoreCase("Directory")) {
		FLDir curr = new FLDir(attrs.getValue("Name"), false, pwd);
		pwd.addSubDir(curr);
		pwd = curr;

	    } else if (qname.equalsIgnoreCase("FileListing")) {
		fl.setCID(attrs.getValue("CID"));
		fl.setJDCBotGenerated(attrs.getValue("Generator").toLowerCase().contains("jdcbot"));

	    } else if (qname.equalsIgnoreCase("File")) {
		FLFile f = new FLFile();
		f.hash = attrs.getValue("TTH");
		f.name = attrs.getValue("Name");
		f.shared = true;
		f.size = Long.parseLong(attrs.getValue("Size"));
		pwd.addFile(f);

	    }

	    //Thread.yield();
	}

	public void endElement(String uri, String lname, String qname) throws SAXException {
	    if (qname.equalsIgnoreCase("Directory")) {
		pwd = pwd.getParent();
	    }
	}
    }
}
