/*
 * EventDispatchThread.java
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

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created on 27-May-08<br>
 * The task of this class is to run a thread (jDCBot-EventDispatchThread), which can be used to call
 * methods of various methods, making the code of those methods run in this thread, asynchornously from
 * the calling method. 
 *
 * @author AppleGrew
 * @since 0.7
 * @version 0.1.1
 * @deprecated This type of generalized class was not needed, but my hand was itching to code using reflection, i guess.
 * Sorry.
 */
public class EventDispatchThread extends Thread {
    private List<DispatchEntity> dispatch;
    private volatile boolean running;

    public EventDispatchThread() {
	super("jDCBot-EventDispatchThread");
	dispatch = Collections.synchronizedList(new ArrayList<DispatchEntity>());
	running = true;
	start();
    }

    public void run() {
	while (running) {
	    while (!dispatch.isEmpty()) {
		DispatchEntity de = dispatch.get(0);
		dispatch.remove(0);
		if (de.method != null) {
		    try {
			de.method.invoke(de.owner, de.params);
		    } catch (IllegalArgumentException e) {
			e.printStackTrace();
		    } catch (IllegalAccessException e) {
			e.printStackTrace();
		    } catch (InvocationTargetException e) {
			e.printStackTrace();
		    }
		}
	    }
	    try {
		sleep(60000L);
	    } catch (InterruptedException e) {}
	}
    }

    /**
     * Use this to invoke a call to a method via this (jDCBot-EventDispatchThread) thread.
     * @param owner The object of whom <i>method</i> is memeber.
     * @param method The case-sensitive name of the method. This can be public <b>only</b>.
     * @param args All the arguments of the <i>method</i>.<br>
     * <b>Note:</b><br>
     * <ul>
     * <li>If <i>method</i> has no arguments then no need to pass
     * any any argument to this parameter, it is a varag and will automatically set to null.<br></li>
     * <li>If you need to supply any primitive data types e.g. int then wrap it in its corresponding wrapper class
     * then wrap that class inside {@link PrimitiveWrapper PrimitiveWrapper}. e.g. If you need to pass<br>
     * <code>int i;</code><br>
     * then wrap <i>i</i> as below.<br>
     * <code>
     * new PrimitiveWrapper&lt;Integer&gt;(new Integer(i))
     * </code><br></li>
     * <li>Also note that passing sub-class of a class won't match, i.e. if the method has argument type Exception then
     * passing IOException won't match. If need to do so then use {@link #call(Object, String, Class[], Object[]) call}</li>
     * <li>You cannot use this method if any of the passed arguments needs to be set to null. For such a call you need to use
     * {@link #call(Object, String, Class[], Object[]) call}.</li>
     * </ul>
     */
    /*public void call(Object owner, String method, Object... args) {
     Class params[] = null;
     if (args != null)
     params = new Class[args.length];

     for (int i = 0; i < args.length; i++) {
     if (args[i] instanceof PrimitiveWrapper) {
     params[i] = ((PrimitiveWrapper) args[i]).getObjectClass();
     if (params[i] == null)
     params[i] = ((PrimitiveWrapper) args[i]).getObject().getClass();
     args[i] = ((PrimitiveWrapper) args[i]).getObject();
     } else
     params[i] = args[i].getClass();
     }

     call(owner, method, params, args);
     }*/

    /**
     * Use this to invoke a call to a method via this (jDCBot-EventDispatchThread) thread, but you will also need to
     * specify arguments' types in <i>param_types</i> argument.
     * @param owner The object of whom <i>method</i> is memeber.
     * @param method The case-sensitive name of the method. This can be public <b>only</b>.
     * @param param_types The array of arguments' types.
     * <b>Note:</b><br>
     * <ul>
     * <li>If you need to supply any primitive data types e.g. int then pass its primitive class type. e.g. If you need to pass<br>
     * <code>int i;</code><br>
     * then pass as its class type as shown below.<br>
     * <code>
     * new Class[]{int.class}
     * </code><br>
     * There is no need for using {@link PrimitiveWrapper PrimitiveWrapper}.</li>
     * </ul>
     * @param args All the arguments of the <i>method</i>.<br>
     * <b>Note:</b><br>
     * <ul>
     * <li>If <i>method</i> has no arguments then no need to pass
     * any any argument to this parameter, it is a varag and will automatically set to null.<br></li>
     * </ul>
     */
    public void call(Object owner, String method, Class param_types[], Object... args) {
	Method m = null;
	try {
	    m = owner.getClass().getMethod(method, param_types);
	} catch (SecurityException e) {
	    e.printStackTrace();
	    return;
	} catch (NoSuchMethodException e) {
	    e.printStackTrace();
	    return;
	}
	DispatchEntity de = new DispatchEntity();
	de.method = m;
	de.owner = owner;
	de.params = args;
	dispatch.add(de);

	this.interrupt();
    }

    public void stopIt() {
	running = false;
	this.interrupt();
    }

    private class DispatchEntity {
	public Method method;
	public Object owner;
	public Object params[];
    }
}
