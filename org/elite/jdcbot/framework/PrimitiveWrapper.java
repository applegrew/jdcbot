/*
 * PrimitiveWrapper.java
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

/**
 * Created on 31-May-08
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1
 * @deprecated
 */
public class PrimitiveWrapper<T> {
    private T _wrapperObject;

    public PrimitiveWrapper(T wrapperObject) {
	_wrapperObject = wrapperObject;
    }

    public T getObject() {
	return _wrapperObject;
    }

    public Class getObjectClass() {
	if (_wrapperObject instanceof Boolean)
	    return boolean.class;
	else if (_wrapperObject instanceof Integer)
	    return int.class;
	else if (_wrapperObject instanceof Float)
	    return float.class;
	else if (_wrapperObject instanceof Double)
	    return double.class;
	else if (_wrapperObject instanceof Long)
	    return long.class;
	else if (_wrapperObject instanceof Character)
	    return char.class;
	else if (_wrapperObject instanceof Byte)
	    return byte.class;
	else if (_wrapperObject instanceof Short)
	    return short.class;
	else
	    return null;
    }
}
