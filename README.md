**Project::** jDCBot
**Website::** http://jdcbot.sourceforge.net
**Version::** 1.0
**Javadoc::** http://jdcbot.sourceforge.net/javadoc
**License::** GNU Public License (See enclosed License.txt)
**Must see Tutorials::** http://jdcbot.sourceforge.net/ (See Tutorial Section)
**Requirements::** JDK 1.5 or higher is required to compile jDCBot (external dependencies have been included in the lib folder).

To compile
----------

Open a console and cd to this directory and type th following command.

    javac -d bin -cp lib/bzip2.jar:lib/jacksum.jar org/ag/sheriffbot/*.java org/elite/jdcbot/examples/*.java org/elite/jdcbot/framework/*.java org/elite/jdcbot/shareframework/*.java org/elite/jdcbot/util/*.java

--------
**Note for Windows users:** You may need to change forward slashes (/) to backward slashes (\) and replce : by ; for the -cp option.

This should finish the compilation without any errors. If you get any error then probably you _don't_ have JDK 1.5 or higher installed. Type

    javac -version

to find the version of java compiler you have. If it says that - No such command found or something like that, then you probably don't have JDK installed or it is not properly set up.
--------
**Note:** JRE (Java Runtime Environment) provides the programs that can _ONLY_ a java programs _NOT_ compile it.
You need JDK (Java Developer Kit), which provides _both_ runtime and the compiler. (You can safely uninstall JRE if you are installing the JDK).

Introduction
------------

jDCBot is a framework for writing your own bot/client. It is a good idea to understand classes here, since you will be working with them while writing your bot. We have provided simple ExampleBot and DownloadBot classes that extends jDCBot abstract class. That should be the start location to look at. Currently, there are several triggers you could override:


	protected void onConnect(){}
    protected void onDisconnect(){}
	protected void onPublicMessage(String user,String message){}
	protected void onJoin(String user){}
	protected void onQuit(String user){}
	protected void onPrivateMessage(String user,String message){}
	protected void onChannelMessage(String user,String channel,String message){}
	protected void onActiveSearch(String IP,int port,boolean isSizeRestricted,boolean isMinimumSize,long size,int dataType,String searchPattern){}
	protected void onPassiveSearch(String user,boolean isSizeRestricted,boolean isMinimumSize,long size,int dataType,String searchPattern){}
	and more...

which should be enough to start with.

	List of methods you could use (including one using some other classes):
	public final void connect(String hostname,int port)
	public final void quit()
	public final boolean UserExist(String user)
	public final User GetUserInfo(String user)
	public final void SendPublicMessage(String message)
	public final void SendPrivateMessage(String user, String message)
	public final void KickUser(String user)
	public final void SendAll(String message,long timeout)
	public final void SendActiveSearchReturn(String IP,int port,boolean isDir,String name,long size,int free_slots)
	public final void SendPassiveSearchReturn(String user,boolean isDir,String name,long size,int free_slots)

but this list isn't complete (there are some more methods, but look at a source or the Javadoc ).

jDCBot has a TimerThread abstract class that generates onTimer() event and you should extend it (example is provided through simple FloodMessageThread class) every time you need new and different onTimer() event.

Also, there is WebPageFetcher abstract class that is extended by GoogleCalculation (when you run your bot and type e.g. '+calc 1+2*3' on main chat, it should react and output result) to show how to use it.
There is MySQLWork abstract class (very simple one) that is extended by StaticCommands class that use one table in database to search for commands with static output in it and displays its output on main chat (you could test it with +help, but first read SQL).
jDCBot is tested with several hubs and is working nicely (ExampleBot could have problem with Verlihub which use same starting of the command - plus sign). If you have any problems, try debugging connect() method of jDCBot class (currently there is no debugging printing on standard output). If you find any bugs, we appriciate for letting us know.

We hope you will have nice time developing with jDCBot. Kokanovic Branko, AppleGrew and Milos Grbic
