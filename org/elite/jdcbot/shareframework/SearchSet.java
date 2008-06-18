/*
 * SearchSet.java
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

/**
 * Created on 05-Jun-08<br>
 * This class allows you to specify
 * what you want to search
 * in the hub.
 *
 * @author AppleGrew
 * @since 1.0
 * @version 0.1
 */
public class SearchSet {

    /**
     * Criteria of comparision for size. 
     * @author AppleGrew
     * @since 1.0
     * @version 0.1
     */
    public static enum SizeCriteria {
	ATLEAST, ATMOST, NONE
    }

    /**
     * The unit of size. The value of size will
     * automatically be converted to the required
     * unit.
     * @author AppleGrew
     * @since 1.0
     * @version 0.1
     */
    public static enum SizeUnit {
	//NOTE Its is important for proper functioning of
	//jDCBot.Search() that the numbering for
	//size_unit start with 0 and serially increase
	//by 1.
	BYTE(0),
	KILOBYTE(1),
	MEGABYTE(2),
	GIGABYTE(3);

	int v;

	SizeUnit(int i) {
	    v = i;
	}

	public int getValue() {
	    return v;
	}
    }

    /**
     * Restricts the file type required. 
     * @author AppleGrew
     * @since 1.0
     * @version 0.1
     */
    public static enum DataType {
	//NOTE Its is important for proper functioning of
	//jDCBot.Search() that the numbering for
	//data_type should be as below.
	ANY(1),
	AUDIO(2),
	COMPRESSED(3),
	DOCUMENT(4),
	EXECUTABLE(5),
	PICTURE(6),
	VIDEO(7),
	DIRECTORY(8),
	TTH(9);

	int v;

	DataType(int i) {
	    v = i;
	}

	public int getValue() {
	    return v;
	}

	public static DataType getEnumForValue(int i) {
	    for (DataType e : DataType.values()) {
		if (e.getValue() == i)
		    return e;
	    }
	    return null;
	}
    }

    public String string;
    public long size = 0;
    public SizeCriteria size_criteria = SizeCriteria.NONE;
    public SizeUnit size_unit = SizeUnit.KILOBYTE;
    public DataType data_type = DataType.ANY;
}
