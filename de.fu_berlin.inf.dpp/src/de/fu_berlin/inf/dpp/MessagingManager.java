/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universitaet Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.MessageEventManager;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;

import de.fu_berlin.inf.dpp.Saros.ConnectionState;
import de.fu_berlin.inf.dpp.net.IConnectionListener;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.net.internal.MultiUserChatManager;
import de.fu_berlin.inf.dpp.ui.MessagingWindow;

/**
 * MessagingManager handles all instant messaging related communications.
 * 
 * @author rdjemili
 */
public class MessagingManager implements PacketListener, MessageListener,
	IConnectionListener, InvitationListener {

    public class ChatLine {
	public Date date;

	public String packedID;

	public String sender;

	public String text;
    }

    /**
     * Sessions are one-to-one IM chat sessions. Sessions are responsible for *
     * Since all sessions already handle their incoming messages, this method is
     * only to handle incoming chat messages for which no sessions have been yet
     * created. If it finds a chat message that belongs to no current session,
     * it creates a new session. sending and receiving their messages. They also
     * handle their IM chat window and save their history, even when their chat
     * windows are disposed and reopened again.
     */
    public class ChatSession implements SessionProvider, PacketListener,
	    MessageListener {
	private final Chat chat;

	private final List<ChatLine> history = new ArrayList<ChatLine>();

	private final Logger logCH = Logger.getLogger(ChatSession.class
		.getName());

	private final String name;

	private final JID participant;

	private MessagingWindow window; // is null if disposed

	public ChatSession(Chat chat, String name) {
	    this.chat = chat;
	    this.name = name;
	    this.participant = new JID(chat.getParticipant());

	    // TODO: this method is not exists in the new API version.
	    chat.addMessageListener(this); // HACK
	    openWindow();
	}

	private void addChatLine(String sender, String text) {
	    ChatLine chatLine = new ChatLine();
	    chatLine.sender = sender;
	    chatLine.text = text;
	    chatLine.date = new Date();

	    this.history.add(chatLine);
	    this.window.addChatLine(chatLine);

	    for (IChatListener chatListener : MessagingManager.this.chatListeners) {
		chatListener.chatMessageAdded(sender, text);
	    }
	}

	public List<ChatLine> getHistory() {
	    return this.history;
	}

	public String getName() {
	    return this.name;
	}

	/**
	 * @return the participant associated to the chat object.
	 */
	public JID getParticipant() {
	    return this.participant;
	}

	/**
	 * Opens the chat window for this chat session. Refocuses the window if
	 * it is already opened.
	 */
	public void openWindow() {
	    if (this.window == null) {
		this.window = new MessagingWindow(this);
		this.window.open();
	    }

	    this.window.getShell().addDisposeListener(new DisposeListener() {
		public void widgetDisposed(DisposeEvent e) {
		    ChatSession.this.window = null;
		}
	    });
	}

	public void processMessage(Chat chat, Message message) {
	    // TODO: new Method for messagelistener

	    this.logCH.debug("processMessage called.");
	    processPacket(message);

	}

	public void processPacket(Packet packet) {
	    this.logCH.debug("processPacket called");

	    final Message message = (Message) packet;

	    if (message.getBody() == null) {
		return;
	    }

	    // openWindow();
	    // addChatLine(name,message.getBody());

	    Display.getDefault().syncExec(new Runnable() {
		public void run() {
		    openWindow();

		    addChatLine(ChatSession.this.name, message.getBody());
		}
	    });
	}

	/*
	 * @see de.fu_berlin.inf.dpp.MessagingManager.SessionProvider
	 */
	public void sendMessage(String text) {
	    try {
		// TODO: Änderung:
		// Message msg = chat.createMessage();

		// TODO: Check connection before sending.

		Message msg = new Message();
		msg.setBody(text);
		// send via muc process
		this.chat.sendMessage(msg);
		// Message msg = muc.createMessage();
		// msg.setBody(text);
		// //TODO: FÜR MUC
		// if(muc != null){
		// muc.sendMessage(msg);
		// }

		// for Testing
		addChatLine(Saros.getDefault().getMyJID().getName(), text);
		// Message msg = null;
		// msg = muc.nextMessage(2000);
		// if(msg != null && msg.getBody() != null){
		// System.out.println(msg.getBody());
		// }
	    } catch (XMPPException e1) {
		e1.printStackTrace();
		addChatLine("error", "Couldn't send message");
	    }
	}

    }

    /**
     * Listener for incoming chat messages.
     */
    public interface IChatListener {
	public void chatMessageAdded(String sender, String message);
    }

    /**
     * this class is only for testing with muc message listener.
     * 
     * @author rdjemili
     * 
     */
    class MUCListener implements MessageListener, PacketListener {

	public void processMessage(Chat chat, Message message) {
	    System.out.println("jetzt gehts los");

	}

	public void processPacket(Packet packet) {
	    if (packet instanceof Message) {
		Message msg = (Message) packet;
		System.out.println("from " + msg.getFrom() + " text: "
			+ msg.getBody());
	    }
	}

    }

    public class MultiChatSession implements SessionProvider, PacketListener,
	    MessageListener {
	private final List<ChatLine> history = new ArrayList<ChatLine>();

	private final Logger logCH = Logger.getLogger(ChatSession.class
		.getName());

	private final MultiUserChat muc;

	private final String name;

	private JID participant;

	private MessagingWindow window; // is null if disposed

	public MultiChatSession(MultiUserChat muc) {
	    this.muc = muc;
	    this.name = "Multi User Chat ("
		    + Saros.getDefault().getMyJID().getName() + ")";
	    muc.addMessageListener(this);
	}

	// public MultiChatSession(Chat chat, String name) {
	// this.chat = chat;
	// this.name = name;
	// this.participant = new JID(chat.getParticipant());
	//
	//
	//			
	// //TODO: this method is not exists in the new API version.
	// // chat.addMessageListener(this); // HACK
	// muc.addMessageListener(this);
	// // openWindow();
	// }

	private void addChatLine(String sender, String text) {
	    ChatLine chatLine = new ChatLine();
	    chatLine.sender = sender;
	    chatLine.text = text;
	    chatLine.date = new Date();

	    this.history.add(chatLine);
	    this.window.addChatLine(chatLine);

	    for (IChatListener chatListener : MessagingManager.this.chatListeners) {
		chatListener.chatMessageAdded(sender, text);
	    }
	}

	public List<ChatLine> getHistory() {
	    return this.history;
	}

	public String getName() {
	    return this.name;
	}

	/**
	 * @return the participant associated to the chat object.
	 */
	public JID getParticipant() {
	    return this.participant;
	}

	public void processMessage(Chat chat, Message message) {
	    // TODO: new Method for messagelistener

	    this.logCH.debug("processMessage called.");
	    processPacket(message);

	}

	public void processPacket(Packet packet) {
	    this.logCH.debug("processPacket called");

	    final Message message = (Message) packet;

	    if (message.getBody() == null) {
		return;
	    }

	    // openWindow();
	    // addChatLine(message.getFrom(),message.getBody());

	    /*
	     * TODO: Checken, warum er hier noch einmal diese angabe macht.
	     * diese Stelle könnte der grund sein, warum das Fenster nicht
	     * aufgeht.
	     */
	    // Display.getDefault().syncExec(new Runnable() {
	    // public void run() {
	    // openWindow();
	    // String from = message.getFrom().replace(
	    // multitrans.getRoomName() + "/", "").replace(
	    // "/Smack", "");
	    // addChatLine(from, message.getBody());
	    // }
	    // });
	    // notify chat listener
	    MessagingManager.log.debug("Notify Listener..");
	    for (IChatListener l : MessagingManager.this.chatListeners) {
		l.chatMessageAdded(message.getFrom(), message.getBody());
		MessagingManager.log.debug("Notified Listener");
	    }
	}

	/*
	 * @see de.fu_berlin.inf.dpp.MessagingManager.SessionProvider
	 */
	public void sendMessage(String text) {
	    try {
		// TODO: Änderung:
		// Message msg = chat.createMessage();

		// TODO: Check connection before sending.

		// Message msg = new Message();
		// msg.setBody(text);
		// send via muc process
		// chat.sendMessage(msg);
		Message msg = this.muc.createMessage();
		msg.setBody(text);
		// TODO: FÜR MUC
		if (this.muc != null) {
		    this.muc.sendMessage(msg);
		}

		// for Testing
		// addChatLine(Saros.getDefault().getMyJID().getName(), text);

		// Message msg = null;
		// msg = muc.nextMessage(2000);
		// if(msg != null && msg.getBody() != null){
		// System.out.println(msg.getBody());
		// }
	    } catch (XMPPException e1) {
		e1.printStackTrace();
		addChatLine("error", "Couldn't send message");
	    }
	}

    }

    /**
     * Encapsulates the interface that is needed by the MessagingWindow.
     */
    public interface SessionProvider {
	public List<ChatLine> getHistory();

	public String getName();

	public void sendMessage(String msg);
    }

    private static Logger log = Logger.getLogger(MessagingManager.class
	    .getName());

    private final String CHAT_ROOM = "saros";

    private final List<IChatListener> chatListeners = new ArrayList<IChatListener>();

    MessageEventManager messageEventManager;

    MUCListener mucl = new MUCListener();

    private MultiChatSession multiSession;

    MultiUserChatManager multitrans = null;

    private MultiChatSession session;

    private final List<ChatSession> sessions = new ArrayList<ChatSession>();

    public MessagingManager() {
	Saros.getDefault().addListener(this);

	this.multitrans = new MultiUserChatManager(this.CHAT_ROOM);
    }

    /**
     * Adds the chat listener.
     */
    public void addChatListener(IChatListener listener) {
	this.chatListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.listeners.IConnectionListener
     */
    public void connectionStateChanged(XMPPConnection connection,
	    ConnectionState newState) {
	if ((connection != null) && (newState == ConnectionState.NOT_CONNECTED)) {
	    // connection.removePacketListener(this);
	    System.out.println("unconnect");
	}

	if (newState == ConnectionState.CONNECTED) {
	    connection.addPacketListener(this, new MessageTypeFilter(
		    Message.Type.chat));
	    initMultiChatListener();
	}
    }

    public void connectMultiUserChat() throws XMPPException {
	if (!Saros.getDefault().isConnected()) {
	    throw new XMPPException("No connection ");
	}
	String user = Saros.getDefault().getConnection().getUser();
	if (this.session == null) {
	    MultiUserChat muc = this.multitrans.getMUC();
	    if (muc == null) {
		this.multitrans.initMUC(Saros.getDefault().getConnection(),
			user);
		muc = this.multitrans.getMUC();
	    }
	    MessagingManager.log.debug("Creating MUC session..");
	    this.session = new MultiChatSession(muc);
	} else {
	    this.multitrans.getMUC().join(user);
	}

    }

    public void disconnectMultiUserChat() throws XMPPException {
	MessagingManager.log.debug("Leaving MUC session..");
	this.multitrans.getMUC().leave();
	// session = null;
    }

    public MultiChatSession getSession() {
	return this.session;
    }

    /* MultiUserChat section */

    /**
     * invitation listener for multi chat invitations.
     */
    public void initMultiChatListener() {
	// listens for MUC invitations
	MultiUserChat.addInvitationListener(Saros.getDefault().getConnection(),
		this);
    }

    public void invitationReceived(XMPPConnection conn, String room,
	    String inviter, String reason, String password, Message message) {
	// TODO Auto-generated method stub
	try {
	    // System.out.println(conn.getUser());
	    if (this.multitrans.getMUC() == null) {
		// this.muc = XMPPMultiChatTransmitter.joinMuc(conn,
		// Saros.getDefault().getConnection().getUser(), room);
		this.multitrans.initMUC(conn, conn.getUser());
	    }
	    // muc.addMessageListener(mucl);
	    // showMultiChatMessagingWindow(new JID("Multi User Chat"), null);
	    // TODO: überprüfen, ob auch noch verbunden
	    if ((this.multiSession == null)
		    && (this.multitrans.getMUC() != null)) {
		// muc.removeMessageListener(mucl);
		MultiChatSession session = new MultiChatSession(this.multitrans
			.getMUC());
		this.multiSession = session;
	    }
	} catch (XMPPException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }

    public void processMessage(Chat chat, Message message) {
	// TODO new method for message notify
	MessagingManager.log.debug("processMessage called.");
	processPacket(message);

    }

    /**
     * Since all sessions already handle their incoming messages, this method is
     * only to handle incoming chat messages for which no sessions have been yet
     * created. If it finds a chat message that belongs to no current session,
     * it creates a new session.
     * 
     * @see org.jivesoftware.smack.PacketListener
     */
    public void processPacket(Packet packet) {
	MessagingManager.log.debug("messagePacket called");
	final Message message = (Message) packet;
	final JID jid = new JID(message.getFrom());

	if (message.getBody() == null) {
	    return;
	}

	/* check for multi or single chat. */
	if (message.getFrom().contains(this.multitrans.getRoomName())) {

	    if (this.multiSession == null) {
		this.multiSession = new MultiChatSession(this.multitrans
			.getMUC());
		this.multiSession.processPacket(message);
	    }
	} else {
	    /* old chat based message communication. */
	    for (ChatSession session : this.sessions) {
		// System.out.println(session.getParticipant());
		if (jid.equals(session.getParticipant())) {
		    return; // gets already handled by message handler in
		    // session
		}
	    }
	}
    }

    /**
     * @param thread
     *            ID of thread or <code>null</code> if chat should start a new
     *            thread.
     * @throws XMPPException
     */
    public ChatSession showMessagingWindow(JID remoteUser, String thread)
	    throws XMPPException {
	if (!Saros.getDefault().isConnected()) {
	    throw new XMPPException("No connection ");
	}

	/*
	 * testing for multichat if(muc == null){ this.muc =
	 * XMPPMultiChatTransmitter
	 * .initIndicateForm(Saros.getDefault().getConnection(),
	 * Saros.getDefault().getConnection().getUser(),
	 * XMPPMultiChatTransmitter.Room); muc.addMessageListener(mucl); } //
	 * try to invite user
	 * 
	 * muc.invite(remoteUser.toString(),"Testing");
	 */

	/*
	 * TODO: (23.12.) Er überprüft, ob es bereits eine Session gibt,
	 * überlegen, wie es im multichat realisiert werden soll!
	 */
	for (ChatSession session : this.sessions) {
	    // System.out.println(remoteUser);
	    if (remoteUser.equals(session.getParticipant())) {
		session.openWindow();
		return session;
	    }
	}

	// create chat and open window
	XMPPConnection connection = Saros.getDefault().getConnection();

	// Chat chat = (thread != null) ? new Chat(connection,
	// remoteUser.toString(), thread)
	// : new Chat(connection, remoteUser.toString());

	// TODO: Änderung
	ChatManager chatmanager = connection.getChatManager();
	Chat chat = null;
	if (thread != null) {
	    // chat = chatmanager.createChat(remoteUser.toString(), thread,
	    // this);
	    chat = chatmanager.getThreadChat(thread);
	    // chat = new Chat(connection, remoteUser.toString(), thread)
	} else {
	    chat = chatmanager.createChat(remoteUser.toString(), this);
	    // chat = new Chat(connection, remoteUser.toString());
	}

	// try to get name from roster
	RosterEntry rosterEntry = connection.getRoster().getEntry(
		remoteUser.getBase());

	String name;
	if (rosterEntry != null) {
	    name = rosterEntry.getName() != null ? rosterEntry.getName()
		    : rosterEntry.getUser();
	} else {
	    name = "unknown";
	}

	ChatSession session = new ChatSession(chat, name);
	// add this chat session to message listener of this chat instance.
	chat.addMessageListener(session);
	// chat.removeMessageListener(this);
	this.sessions.add(session);
	session.openWindow();
	return session;
    }

    /**
     * @param thread
     *            ID of thread or <code>null</code> if chat should start a new
     *            thread.
     * @throws XMPPException
     */
    public void showMultiChatMessagingWindow(JID remoteUser, String thread)
	    throws XMPPException {
	if (!Saros.getDefault().isConnected()) {
	    throw new XMPPException("No connection ");
	}

	// /*testing for multichat*/
	// if(muc == null){
	// this.muc =
	// XMPPMultiChatTransmitter.initIndicateForm(Saros.getDefault().getConnection(),
	// Saros.getDefault().getConnection().getUser(),
	// XMPPMultiChatTransmitter.Room);
	// muc.addMessageListener(mucl);
	// }

	MultiUserChat muc = this.multitrans.getMUC();
	if (muc == null) {
	    // muc = multitrans.getMUC();
	    // if(muc == null){
	    this.multitrans.initMUC(Saros.getDefault().getConnection(), Saros
		    .getDefault().getConnection().getUser());
	    muc = this.multitrans.getMUC();
	    // }
	}
	// else{
	// //for testing
	// multitrans.initMUC(Saros.getDefault().getConnection(),
	// Saros.getDefault().getConnection().getUser());
	// }

	/*
	 * try to invite user TODO: check if user has joined the room.
	 */

	Presence remoteUserPresence = muc.getOccupantPresence(this.CHAT_ROOM
		+ "/" + remoteUser.toString() + "/Smack");
	if (remoteUserPresence == null) {
	    muc.invite(remoteUser.toString(), "Testing");
	}

	/*
	 * TODO: (23.12.) Er überprüft, ob es bereits eine Session gibt,
	 * überlegen, wie es im multichat realisiert werden soll!
	 */
	if (this.multiSession == null) {
	    // TODO: für einzelchats muss der listener später weiter bestehen
	    muc.removeMessageListener(this.mucl);
	    try {
		/*
		 * TODO: es kann bei der Erzeugung des fenster zu einer
		 * exception kommen. es muss noch geklärt werden, warum !!!!
		 */
		this.multiSession = new MultiChatSession(muc);
	    } catch (Exception e) {
		e.printStackTrace();
		this.multiSession = null;
		muc.addMessageListener(this.mucl);
	    }
	}

	// multiSession.openWindow();

	// create chat and open window
	// XMPPConnection connection = Saros.getDefault().getConnection();

	// TODO: Änderung / An dieser Stelle muss später der Private Chat
	// aufgebaut werden
	// ChatManager chatmanager = connection.getChatManager();
	// Chat chat = null;
	// if(thread != null){
	// // chat = chatmanager.createChat(remoteUser.toString(), thread,
	// this);
	// chat = chatmanager.getThreadChat(thread);
	// // chat = new Chat(connection, remoteUser.toString(), thread)
	// }else{
	// chat = chatmanager.createChat(remoteUser.toString(), this);
	// // chat = new Chat(connection, remoteUser.toString());
	// }

	/*
	 * TODO: in der aktuellen Version kann der name aus der nachricht
	 * gelesen werden.
	 */
	//
	// // try to get name from roster
	// RosterEntry rosterEntry =
	// connection.getRoster().getEntry(remoteUser.getBase());
	//
	// String name;
	// if (rosterEntry != null) {
	// name = rosterEntry.getName() != null ? rosterEntry.getName() :
	// rosterEntry.getUser();
	// } else {
	// name = "unknown";
	// }
	//
	// ChatSession session = new ChatSession(chat, name);
	// add this chat session to message listener of this chat instance.
	// chat.addMessageListener(session);
	// // chat.removeMessageListener(this);
	// sessions.add(session);
	// session.openWindow();
	// return session;
    }

}
