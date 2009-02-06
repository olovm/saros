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
package de.fu_berlin.inf.dpp.net.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import de.fu_berlin.inf.dpp.FileList;
import de.fu_berlin.inf.dpp.PreferenceConstants;
import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.User;
import de.fu_berlin.inf.dpp.User.UserRole;
import de.fu_berlin.inf.dpp.activities.FileActivity;
import de.fu_berlin.inf.dpp.activities.IActivity;
import de.fu_berlin.inf.dpp.activities.TextEditActivity;
import de.fu_berlin.inf.dpp.concurrent.jupiter.Request;
import de.fu_berlin.inf.dpp.concurrent.management.DocumentChecksum;
import de.fu_berlin.inf.dpp.invitation.IInvitationProcess;
import de.fu_berlin.inf.dpp.invitation.IInvitationProcess.TransferMode;
import de.fu_berlin.inf.dpp.net.IFileTransferCallback;
import de.fu_berlin.inf.dpp.net.ITransmitter;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.net.TimedActivity;
import de.fu_berlin.inf.dpp.net.internal.extensions.CancelInviteExtension;
import de.fu_berlin.inf.dpp.net.internal.extensions.ChecksumErrorExtension;
import de.fu_berlin.inf.dpp.net.internal.extensions.ChecksumExtension;
import de.fu_berlin.inf.dpp.net.internal.extensions.DataTransferExtension;
import de.fu_berlin.inf.dpp.net.internal.extensions.InviteExtension;
import de.fu_berlin.inf.dpp.net.internal.extensions.JoinExtension;
import de.fu_berlin.inf.dpp.net.internal.extensions.JupiterErrorExtension;
import de.fu_berlin.inf.dpp.net.internal.extensions.LeaveExtension;
import de.fu_berlin.inf.dpp.net.internal.extensions.PacketExtensions;
import de.fu_berlin.inf.dpp.net.internal.extensions.RequestActivityExtension;
import de.fu_berlin.inf.dpp.net.internal.extensions.RequestForFileListExtension;
import de.fu_berlin.inf.dpp.net.internal.extensions.UserListExtension;
import de.fu_berlin.inf.dpp.net.jingle.IJingleFileTransferListener;
import de.fu_berlin.inf.dpp.net.jingle.JingleFileTransferData;
import de.fu_berlin.inf.dpp.net.jingle.JingleFileTransferManager;
import de.fu_berlin.inf.dpp.net.jingle.JingleSessionException;
import de.fu_berlin.inf.dpp.net.jingle.JingleFileTransferData.FileTransferType;
import de.fu_berlin.inf.dpp.project.ISessionManager;
import de.fu_berlin.inf.dpp.project.ISharedProject;
import de.fu_berlin.inf.dpp.project.SessionManager.ConnectionSessionListener;
import de.fu_berlin.inf.dpp.ui.WarningMessageDialog;

/**
 * The one ITransmitter implementation which uses Smack Chat objects.
 * 
 */
public class XMPPChatTransmitter implements ITransmitter,
    ConnectionSessionListener {

    private static Logger log = Logger.getLogger(XMPPChatTransmitter.class
        .getName());

    private static final int MAX_PARALLEL_SENDS = 10;
    private static final int MAX_TRANSFER_RETRIES = 5;
    private static final int FORCEDPART_OFFLINEUSER_AFTERSECS = 60;

    /*
     * the following string descriptions are used to differentiate between
     * transfers that are for invitations and transfers that are an activity for
     * the current project.
     */
    private static final String RESOURCE_TRANSFER_DESCRIPTION = "resourceAddActivity";

    private static final String FILELIST_TRANSFER_DESCRIPTION = "filelist";

    private static final String PROJECT_ARCHIVE_DESCRIPTION = "projectArchiveFile";

    private XMPPConnection connection;

    private ChatManager chatmanager;

    private final Map<JID, Chat> chats = new HashMap<JID, Chat>();

    private FileTransferManager fileTransferManager;

    private final List<IInvitationProcess> processes = new CopyOnWriteArrayList<IInvitationProcess>();

    private final ConcurrentLinkedQueue<TransferData> fileTransferQueue = new ConcurrentLinkedQueue<TransferData>();
    private final List<MessageTransfer> messageTransferQueue = Collections
        .synchronizedList(new LinkedList<MessageTransfer>());
    private final Map<String, IncomingFile> incomingFiles = new HashMap<String, IncomingFile>();

    private JingleFileTransferManager jingleManager;

    public JingleFileTransferManager getJingleManager() {
        try {
            if (startingJingleThread == null)
                return null;

            startingJingleThread.join();
        } catch (InterruptedException e) {
            // do nothing
        }
        return jingleManager;
    }

    private Thread startingJingleThread;

    protected long lastReceivedActivityTime;

    protected JingleDiscoveryManager jingleDiscovery;

    /**
     * TODO What to do if already in a Session?
     */
    private final class InvitePacketListener implements PacketListener {
        public void processPacket(Packet packet) {
            Message message = (Message) packet;

            JID fromJID = new JID(message.getFrom());

            if (InviteExtension.getDefault().hasExtension(message)) {
                processInviteExtension(message, fromJID);
                return;
            }
        }

        private void processInviteExtension(final Message message, JID fromJID) {
            DefaultPacketExtension inviteExtension = InviteExtension
                .getDefault().getExtension(message);
            String desc = inviteExtension
                .getValue(PacketExtensions.DESCRIPTION);
            String pName = inviteExtension
                .getValue(PacketExtensions.PROJECTNAME);
            String sessionID = inviteExtension
                .getValue(PacketExtensions.SESSION_ID);
            int colorID = Integer.parseInt(inviteExtension
                .getValue(PacketExtensions.COLOR_ID));

            ISessionManager sm = Saros.getDefault().getSessionManager();
            if (sm.getSessionID().equals(ISessionManager.NOT_IN_SESSION)) {
                log.debug("Received invitation with session id " + sessionID);
                log.debug("and ColorID: " + colorID + ", i'm "
                    + Saros.getDefault().getMyJID());
                sm.invitationReceived(fromJID, sessionID, pName, desc, colorID);
            } else {
                sendMessage(
                    fromJID,
                    CancelInviteExtension
                        .getDefault()
                        .create(sessionID,
                            "I am already in a Saros-Session, try to contact me by chat first"));
            }
        }
    }

    private final class JingleTransferListener implements
        IJingleFileTransferListener {
        /**
         * File received with jingle.
         */
        public void incomingResourceFile(JingleFileTransferData data,
            InputStream input) {
            log.info("incoming resource " + data.file_project_path);

            JID from = data.sender;
            Path path = new Path(data.file_project_path);
            int time = data.timestamp;

            // TODO CJ: move this to business logic
            boolean handledByInvitation = false;
            for (IInvitationProcess process : processes) {
                if (process.getPeer().equals(from)) {
                    process.resourceReceived(from, path, input);
                    handledByInvitation = true;
                    break;
                }
            }

            if (!handledByInvitation) {
                FileActivity activity = new FileActivity(
                    FileActivity.Type.Created, path, input);

                TimedActivity timedActivity = new TimedActivity(activity, time);

                ISessionManager sm = Saros.getDefault().getSessionManager();
                sm.getSharedProject().getSequencer().exec(timedActivity);
            }
        }

        public void incomingFileList(String fileList_content, JID recipient) {
            FileList fileList = null;
            log.info("incoming file list");
            try {
                fileList = new FileList(fileList_content);
            } catch (Exception e) {

                for (IInvitationProcess process : processes) {
                    if (process.getPeer().equals(recipient))
                        process.cancel("Could not parse your FileList", false);
                }
                log.error("Could not parse FileList", e);
            }
            for (IInvitationProcess process : processes) {
                if (process.getPeer().equals(recipient))
                    process.fileListReceived(recipient, fileList);
            }
        }

        public void connected(String protocol, String remote) {
            // ignore, because we only need to know when a file arrived
        }
    }

    private class IBBTransferListener implements FileTransferListener {

        /*
         * (non-Javadoc)
         * 
         * @see org.jivesoftware.smackx.filetransfer.FileTransferListener
         */
        public void fileTransferRequest(FileTransferRequest incommingRequest) {

            final FileTransferRequest request = incommingRequest;

            new Thread(new Runnable() {

                public void run() {
                    try {
                        String fileDescription = request.getDescription();
                        XMPPChatTransmitter.log
                            .debug("1. incomming file transfer "
                                + request.getFileName());
                        if (fileDescription
                            .equals(XMPPChatTransmitter.PROJECT_ARCHIVE_DESCRIPTION)) {
                            XMPPChatTransmitter.log
                                .debug(" incoming project archive file.");
                            receiveArchiveFile(request);
                        }
                        if (fileDescription
                            .equals(XMPPChatTransmitter.FILELIST_TRANSFER_DESCRIPTION)) {
                            FileList fileList = receiveFileListBufferByteArray(request);
                            JID fromJID = new JID(request.getRequestor());

                            XMPPChatTransmitter.log
                                .debug("2. inform invitation process...");
                            for (IInvitationProcess process : XMPPChatTransmitter.this.processes) {
                                if (process.getPeer().equals(fromJID)) {
                                    process.fileListReceived(fromJID, fileList);
                                    /*
                                     * incoming IBB transfer. cancel jingle
                                     * transfer mode.
                                     */
                                    process.setTransferMode(TransferMode.IBB);
                                }
                            }

                        } else if (fileDescription.startsWith(
                            XMPPChatTransmitter.RESOURCE_TRANSFER_DESCRIPTION,
                            0)) {
                            receiveResource(request);
                        }
                    } catch (Exception e) {
                        XMPPChatTransmitter.log.error(
                            "Incoming File Transfer Thread: ", e);
                        for (IInvitationProcess process : XMPPChatTransmitter.this.processes) {
                            if (process.getPeer().equals(
                                new JID(request.getRequestor()))) {
                                process.cancel(e.getMessage(), false);
                            }
                        }
                    }

                }

            }).start();

        }

        /**
         * Receive file and save temporary.
         * 
         * @param request
         *            transfer request of incoming file.
         * @return File object of received file
         */
        private File receiveFile(FileTransferRequest request) {
            File archiveFile = new File("./incoming_archive.zip");
            XMPPChatTransmitter.log.debug("Archive file: "
                + archiveFile.getAbsolutePath());
            try {
                final IncomingFileTransfer transfer = request.accept();

                IFileTransferCallback callback = null;

                /* get IInvitationprocess for monitoring. */
                JID fromJID = new JID(request.getRequestor());
                for (IInvitationProcess process : processes) {
                    if (process.getPeer().equals(fromJID)) {
                        /* set callback. */
                        callback = process;
                    }
                }

                /* monitoring of transfer process */
                FileTransferProcessMonitor monitor = new FileTransferProcessMonitor(
                    transfer, callback);

                /* receive file. */
                transfer.recieveFile(archiveFile);

                /* wait for complete transfer. */
                while (monitor.isAlive() && monitor.isRunning()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                monitor.closeMonitor(true);

            } catch (Exception e) {
                XMPPChatTransmitter.log.error("Error in Incoming File: ", e);
                return null;
            }

            return archiveFile;
        }

        /**
         * read incoming file and open inputstream to IInvitationProcess.
         * 
         * @param request
         * @throws Exception
         */
        private void receiveArchiveFile(FileTransferRequest request)
            throws Exception {
            // try{
            File archive = receiveFile(request);

            ZipFile zip = new ZipFile(archive);
            @SuppressWarnings("unchecked")
            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zip
                .entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                XMPPChatTransmitter.log.debug(entry.getName());
                JID fromJID = new JID(request.getRequestor());

                XMPPChatTransmitter.log
                    .debug("2. inform invitation process...");
                for (IInvitationProcess process : processes) {
                    if (process.getPeer().equals(fromJID)) {
                        process.resourceReceived(fromJID, new Path(entry
                            .getName()), zip.getInputStream(entry));
                    }
                }
            }
            archive.delete();
        }

        /**
         * receive resource with file transfer.
         * 
         * @param request
         */
        private void receiveResource(FileTransferRequest request) {
            try {

                JID from = new JID(request.getRequestor());

                /* file path exists in description. */
                Path path = new Path(request.getDescription()
                    .substring(
                        XMPPChatTransmitter.RESOURCE_TRANSFER_DESCRIPTION
                            .length() + 1));

                XMPPChatTransmitter.log.debug("Receiving resource from"
                    + from.toString() + ": " + request.getFileName());

                IncomingFileTransfer transfer = request.accept();

                InputStream in = transfer.recieveFile();

                // TODO CJ: move this to business logic
                boolean handledByInvitation = false;
                for (IInvitationProcess process : processes) {
                    if (process.getPeer().equals(from)) {
                        process.resourceReceived(from, path, in);
                        handledByInvitation = true;
                        break;
                    }
                }

                if (!handledByInvitation) {
                    FileActivity activity = new FileActivity(
                        FileActivity.Type.Created, path, in);

                    int time;
                    String description = request.getDescription();
                    try {
                        time = Integer
                            .parseInt(description
                                .substring(XMPPChatTransmitter.RESOURCE_TRANSFER_DESCRIPTION
                                    .length() + 1));
                    } catch (NumberFormatException e) {
                        Saros.log("Could not parse time from description: "
                            + description, e);
                        time = 0; // HACK
                    }

                    TimedActivity timedActivity = new TimedActivity(activity,
                        time);

                    ISessionManager sm = Saros.getDefault().getSessionManager();
                    sm.getSharedProject().getSequencer().exec(timedActivity);
                }

                // /* wait for complete transfer. */
                // while (monitor.isAlive() && monitor.isRunning()) {
                // Thread.sleep(500);
                // }
                // monitor.closeMonitor(true);

                XMPPChatTransmitter.log.info("Received resource "
                    + request.getFileName());

            } catch (Exception e) {
                XMPPChatTransmitter.log.warn("Failed to receive "
                    + request.getFileName(), e);
            }
        }

        public FileList receiveFileListBufferByteArray(
            FileTransferRequest request) {
            FileList fileList = null;
            try {
                final IncomingFileTransfer transfer = request.accept();

                InputStream in = transfer.recieveFile();

                byte[] buffer = new byte[1024];
                int bytesRead;
                String sb = new String();
                while ((bytesRead = in.read(buffer, 0, 1024)) != -1) {
                    sb += new String(buffer, 0, bytesRead).toString();
                }
                in.close();
                XMPPChatTransmitter.log.debug("Close input stream");
                fileList = new FileList(sb.toString());

            } catch (Exception e) {
                XMPPChatTransmitter.log.error("Error in Incoming File List: ",
                    e);
                return null;
            }

            return fileList;
        }
    }

    /**
     * TODO break this up into many individually registered Listeners
     */
    private final class GodPacketListener implements PacketListener {
        public void processPacket(Packet packet) {

            try {
                Message message = (Message) packet;

                JID fromJID = new JID(message.getFrom());

                final ISharedProject project = Saros.getDefault()
                    .getSessionManager().getSharedProject();

                // Change the input method to get the right chats
                putIncomingChat(fromJID, message.getThread());

                if (PacketExtensions.getActvitiesExtension(message) != null) {
                    processActivitiesExtension(message, fromJID, project);
                }

                if (JoinExtension.getDefault().hasExtension(message)) {
                    processJoinExtension(message, fromJID, project);
                }

                // TODO CJ: Leave Project Message must be handled better
                if (LeaveExtension.getDefault().hasExtension(message)) {
                    processLeaveExtension(fromJID, project);
                }

                if (RequestActivityExtension.getDefault().hasExtension(message)) {
                    processRequestActivityExtension(message, project, fromJID);
                }

                if (DataTransferExtension.getDefault().hasExtension(message)) {
                    receiveChatTransfer(message);
                }

                if (RequestForFileListExtension.getDefault().hasExtension(
                    message)) {
                    processRequestForFileListExtension(fromJID);
                }

                if (UserListExtension.getDefault().hasExtension(message)) {
                    processUserListExtension(message, fromJID, project);
                }

                if (CancelInviteExtension.getDefault().hasExtension(message)) {
                    processCancelInviteExtension(message, fromJID);
                }
            } catch (Exception e) {
                XMPPChatTransmitter.log.error(
                    "An internal error occurred while processing packets", e);
            }
        }

        private void processActivitiesExtension(final Message message,
            JID fromJID, final ISharedProject project) {
            ActivitiesPacketExtension activitiesPacket = PacketExtensions
                .getActvitiesExtension(message);

            List<TimedActivity> timedActivities = activitiesPacket
                .getActivities();

            String source = fromJID.toString();
            XMPPChatTransmitter.log.debug("Received activities from " + source
                + ": " + timedActivities);

            if ((project == null) || (project.getParticipant(fromJID) == null)) {
                XMPPChatTransmitter.log.info("user not member!");
                return;
            }

            for (TimedActivity timedActivity : timedActivities) {

                IActivity activity = timedActivity.getActivity();
                activity.setSource(source);

                /*
                 * incoming fileActivities that add files are only used as
                 * placeholder to bump the timestamp. the real fileActivity will
                 * be processed by using a file transfer.
                 */
                if (!(activity instanceof FileActivity)
                    || !((FileActivity) activity).getType().equals(
                        FileActivity.Type.Created)) {

                    project.getSequencer().exec(timedActivity);

                }
            }
        }

        private void processRequestActivityExtension(final Message message,
            ISharedProject project, JID fromJID) {

            if (project == null || project.getParticipant(fromJID) == null) {
                return;
            }

            DefaultPacketExtension rae = RequestActivityExtension.getDefault()
                .getExtension(message);

            String sID = rae.getValue("ID");
            String sIDandup = rae.getValue("ANDUP");

            int ts = -1;
            if (sID != null) {
                ts = (new Integer(sID)).intValue();
                // get that activity from history (if it was mine) and send it
                boolean sent = resendActivity(fromJID, ts, (sIDandup != null));

                String info = "Received Activity request for timestamp=" + ts
                    + ".";
                if (sIDandup != null) {
                    info += " (andup) ";
                }
                if (sent) {
                    info += " I sent response.";
                } else {
                    info += " (not for me)";
                }

                XMPPChatTransmitter.log.info(info);
            }
        }

        private void processCancelInviteExtension(final Message message,
            JID fromJID) {
            DefaultPacketExtension cancelInviteExtension = CancelInviteExtension
                .getDefault().getExtension(message);

            String errorMsg = cancelInviteExtension
                .getValue(PacketExtensions.ERROR);

            for (IInvitationProcess process : processes) {
                if (process.getPeer().equals(fromJID)) {
                    process.cancel(errorMsg, true);
                }
            }
        }

        private void processJoinExtension(final Message message, JID fromJID,
            final ISharedProject project) {

            DefaultPacketExtension extension = JoinExtension.getDefault()
                .getExtension(message);

            int colorID = Integer.parseInt(extension.getValue("ColorID"));

            log.debug("Join: ColorID: " + colorID);

            for (IInvitationProcess process : processes) {
                if (process.getPeer().equals(fromJID)) {
                    process.joinReceived(fromJID);
                    return;
                }
            }
            if (project != null) {
                project.addUser(new User(fromJID, colorID));
                // a new user
                // joined this
                // session
            }
        }

        private void processLeaveExtension(JID fromJID,
            final ISharedProject project) {
            if (project != null) {
                if (project.getHost().getJID().equals(fromJID)) {
                    // Host
                    Saros.getDefault().getSessionManager().stopSharedProject();

                    WarningMessageDialog.showWarningMessage(
                        "Closing the Session",
                        "Closing the session because the host left.");
                } else {// Client
                    project.removeUser(project.getParticipant(fromJID));
                }
            }
        }

        /**
         * Invitee request for project file list (state.INVITATION_SEND)
         */
        private void processRequestForFileListExtension(final JID fromJID) {

            XMPPChatTransmitter.log
                .debug("Received Request for FileList from from " + fromJID);

            new Thread(new Runnable() {
                public void run() {
                    for (IInvitationProcess process : processes) {
                        if (process.getPeer().equals(fromJID)) {
                            process.invitationAccepted(fromJID);
                        }
                    }
                }
            }).start();
        }

        private void processUserListExtension(final Message message,
            JID fromJID, final ISharedProject project) {
            DefaultPacketExtension userlistExtension = UserListExtension
                .getDefault().getExtension(message);

            // My inviter sent a list of all session participants
            // I need to adapt the order for later case of driver leaving the
            // session
            XMPPChatTransmitter.log.debug("Received user list from " + fromJID);

            int count = 0;
            while (true) {
                String jidS = userlistExtension.getValue("User" + count);
                if (jidS == null) {
                    break;
                }
                JID jid = new JID(jidS);
                XMPPChatTransmitter.log.debug("   *:" + jidS);
                int colorID = Integer.parseInt(userlistExtension
                    .getValue("UserColor" + count));
                XMPPChatTransmitter.log.debug("   color: " + colorID);

                User user = project.getParticipant(jid);

                if (user == null) {
                    // This user is new, we have to send him a message later
                    // and add him to the project
                    user = new User(jid, colorID);
                }

                if (user.getColorID() != colorID) {
                    log.warn("Received color id doesn't match known color id");
                }

                String userRole = userlistExtension
                    .getValue("UserRole" + count);
                user.setUserRole(UserRole.valueOf(userRole));

                if (project.getParticipant(jid) == null) {
                    project.addUser(user);

                    sendMessage(jid, JoinExtension.getDefault().create(
                        Saros.getDefault().getLocalUser().getColorID()));
                }

                count++;
            }
        }

        /**
         * Receives a data buffer sent by a chat message. The data will be
         * decoded from base64 encoding. Splitted transfer will be buffered
         * until all chunks are received. Then the file will be reconstructed
         * and processed as a whole.
         * 
         * @param message
         *            Message containing the data as extension.
         * 
         * @return <code>true</code> if the message was handled successfully.
         */
        boolean receiveChatTransfer(Message message) {
            DefaultPacketExtension dt = DataTransferExtension.getDefault()
                .getExtension(message);
            String sName = dt.getValue(PacketExtensions.DT_NAME);
            String sData = dt.getValue(PacketExtensions.DT_DATA);

            String sSplit = dt.getValue(PacketExtensions.DT_SPLIT);
            try {
                // is this a multipart transfer?
                if ((sSplit != null) && (sSplit.equals("1/1") == false)) {
                    // parse split information (index and chunk count)
                    int i = sSplit.indexOf('/');
                    int cur = Integer.parseInt(sSplit.substring(0, i));
                    int max = Integer.parseInt(sSplit.substring(i + 1));

                    XMPPChatTransmitter.log.debug("Received chunk " + cur
                        + " of " + max + " of file " + sName);

                    // check for previous chunks
                    IncomingFile ifile = incomingFiles.get(sName);
                    if (ifile == null) {
                        // this is the first received chunk->create incoming
                        // file
                        // object
                        ifile = new IncomingFile();
                        ifile.receivedChunks++;
                        ifile.chunkCount = max;
                        ifile.name = sName;
                        for (i = 0; i < max; i++) {
                            ifile.messageBuffer.add(null);
                        }
                        ifile.messageBuffer.set(cur - 1, sData);
                        incomingFiles.put(sName, ifile);
                        return true;
                    } else {
                        // this is a following chunk
                        ifile.receivedChunks++;
                        ifile.messageBuffer.set(cur - 1, sData);

                        if (ifile.isComplete() == false) {
                            return true;
                        }

                        // join the buffers to restore the file from chunks
                        sData = "";
                        for (i = 0; i < max; i++) {
                            sData += ifile.messageBuffer.get(i);
                        }
                        incomingFiles.remove(ifile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;

            }

            byte[] dataOrg = Base64.decodeBase64(sData.getBytes());
            if (dataOrg == null)
                return false;

            // File list received
            if (sName.equals(XMPPChatTransmitter.FILELIST_TRANSFER_DESCRIPTION)) {
                FileList fileList = null;
                IInvitationProcess myProcess = null;
                try {
                    JID fromJID = new JID(message.getFrom());
                    for (IInvitationProcess process : processes) {
                        if (process.getPeer().equals(fromJID)) {
                            myProcess = process;
                            fileList = new FileList(new String(dataOrg));
                            process.fileListReceived(fromJID, fileList);
                        }
                    }
                    XMPPChatTransmitter.log
                        .info("Received file list via ChatTransfer");
                } catch (Exception e) {
                    if (myProcess != null) {
                        myProcess.cancel("Error receiving file list", false);
                    }
                }

            } else {
                // receiving file (resource)

                try {

                    JID from = new JID(message.getFrom());
                    Path path = new Path(sName);

                    ByteArrayInputStream in = new ByteArrayInputStream(dataOrg);

                    XMPPChatTransmitter.log.debug("Receiving resource from "
                        + from.toString() + ": " + sName + " (ChatTransfer)");

                    boolean handledByInvitation = false;
                    for (IInvitationProcess process : processes) {
                        if (process.getPeer().equals(from)) {
                            process.resourceReceived(from, path, in);
                            handledByInvitation = true;
                        }
                    }

                    if (!handledByInvitation) {

                        if (Saros.getDefault().getSessionManager()
                            .getSharedProject() == null) {
                            // receiving resource without a running session? not
                            // accepted
                            return false;
                        }

                        FileActivity activity = new FileActivity(
                            FileActivity.Type.Created, path, in);

                        int time;
                        String description = dt
                            .getValue(PacketExtensions.DT_DESC);
                        try {
                            time = Integer
                                .parseInt(description
                                    .substring(XMPPChatTransmitter.RESOURCE_TRANSFER_DESCRIPTION
                                        .length() + 1));
                        } catch (Exception e) {
                            Saros.log("Could not parse time from description: "
                                + description, e);
                            time = 0; // HACK
                        }

                        TimedActivity timedActivity = new TimedActivity(
                            activity, time);

                        ISessionManager sm = Saros.getDefault()
                            .getSessionManager();
                        sm.getSharedProject().getSequencer()
                            .exec(timedActivity);
                    }

                    XMPPChatTransmitter.log.info("Received resource " + sName);

                } catch (Exception e) {
                    XMPPChatTransmitter.log.warn("Failed to receive " + sName,
                        e);
                }
            }

            return true;
        }
    }

    /**
     * A simple struct that is used to queue file transfers.
     */
    public class FileTransferData {
        public JID recipient;
        public IPath path;
        public int timestamp;
        public int retries = 0;
        public byte[] content;
        public long filesize;
        public IProject project;
    }

    /**
     * A simple struct that is used to manage incoming chunked files via
     * chat-file transfer
     */
    private class IncomingFile {
        String name;
        int receivedChunks;
        int chunkCount;
        List<String> messageBuffer;

        IncomingFile() {
            this.messageBuffer = new LinkedList<String>();
        }

        boolean isComplete() {
            return (this.receivedChunks == this.chunkCount);
        }
    }

    /**
     * A simple struct that is used to queue message transfers.
     */
    private class MessageTransfer {
        public JID receipient;
        public PacketExtension packetextension;
    }

    public void setXMPPConnection(final XMPPConnection connection) {

        this.connection = connection;
        this.chatmanager = connection.getChatManager();

        this.fileTransferManager = new FileTransferManager(connection);
        this.fileTransferManager
            .addFileTransferListener(new IBBTransferListener());

        this.chats.clear();

        // Create JingleDiscoveryManager
        jingleDiscovery = new JingleDiscoveryManager(connection);

        // Register PacketListeners
        this.connection.addPacketListener(new InvitePacketListener(),
            new AndFilter(new MessageTypeFilter(Message.Type.chat),
                InviteExtension.getDefault().getFilter()));

        this.connection.addPacketListener(new GodPacketListener(),
            PacketExtensions.getSessionIDPacketFilter());

        if (!getFileTransferModeViaChat()) {
            // Start Jingle Manager asynchronous
            this.startingJingleThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        jingleManager = new JingleFileTransferManager(
                            connection, new JingleTransferListener());
                        log.debug("Jingle Manager started");
                    } catch (Exception e) {
                        log.error("Jingle Manager could not be started", e);
                        jingleManager = null;
                    }
                }
            });
            this.startingJingleThread.start();
        }
    }

    public void addInvitationProcess(IInvitationProcess process) {
        this.processes.add(process);
    }

    public void removeInvitationProcess(IInvitationProcess process) {
        this.processes.remove(process);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.net.ITransmitter
     */
    public void sendCancelInvitationMessage(JID user, String errorMsg) {
        sendMessage(user, CancelInviteExtension.getDefault().create(
            Saros.getDefault().getSessionManager().getSessionID(), errorMsg));
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.ITransmitter
     */
    public void sendRequestForFileListMessage(JID toJID) {

        XMPPChatTransmitter.log.debug("Send request for FileList to " + toJID);

        // Make sure JingleManager has started
        getJingleManager();

        sendMessage(toJID, RequestForFileListExtension.getDefault().create());

    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.ITransmitter
     */
    public void sendRequestForActivity(ISharedProject sharedProject,
        int timestamp, boolean andup) {

        // log.info("Requesting old activity (timestamp=" + timestamp + ", "
        // + andup + ") from all...");
        //
        // sendMessageToAll(sharedProject, PacketExtensions
        // .createRequestForActivityExtension(timestamp, andup));

    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.ITransmitter
     */
    public void sendInviteMessage(ISharedProject sharedProject, JID guest,
        String description, int colorID) {
        sendMessage(guest, InviteExtension.getDefault().create(
            sharedProject.getProject().getName(), description, colorID));
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.ITransmitter
     */
    public void sendJoinMessage(ISharedProject sharedProject) {
        try {
            /* sleep process for 500 millis to ensure invitation state process. */
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        sendMessageToAll(sharedProject, JoinExtension.getDefault().create(
            Saros.getDefault().getLocalUser().getColorID()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.ITransmitter
     */
    public void sendLeaveMessage(ISharedProject sharedProject) {
        sendMessageToAll(sharedProject, LeaveExtension.getDefault().create());
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.fu_berlin.inf.dpp.ITransmitter
     */
    public void sendActivities(ISharedProject sharedProject,
        List<TimedActivity> timedActivities) {

        for (TimedActivity timedActivity : timedActivities) {
            IActivity activity = timedActivity.getActivity();

            if (activity instanceof TextEditActivity) {
                log.debug("sendActivities: " + activity);
                TextEditActivity textEditActivity = (TextEditActivity) activity;
                textEditActivity.setSource(Saros.getDefault().getMyJID()
                    .toString());
            }

            if (activity instanceof FileActivity) {
                FileActivity fileAdd = (FileActivity) activity;

                if (fileAdd.getType().equals(FileActivity.Type.Created)) {
                    JID myJID = Saros.getDefault().getMyJID();

                    for (User participant : sharedProject.getParticipants()) {
                        JID jid = participant.getJID();
                        if (jid.equals(myJID)) {
                            continue;
                        }

                        // TODO use callback
                        int time = timedActivity.getTimestamp();
                        try {
                            IFileTransferCallback callback = new AbstractFileTransferCallback() {
                                @Override
                                public void fileTransferFailed(IPath path,
                                    Exception e) {
                                    log.error("File could not be send:", e);
                                }
                            };
                            sendFileAsync(jid, sharedProject.getProject(),
                                fileAdd.getPath(), time, callback);
                        } catch (IOException e) {
                            log.error("File could not be send:", e);
                            // TODO This means we were really unable to send
                            // this file. No more falling back.
                        }
                    }
                }
            } else {
                sharedProject.getSequencer().getActivityHistory().add(
                    timedActivity);

                // TODO: removed very old entries
            }
        }

        XMPPChatTransmitter.log.info("Sent activities: " + timedActivities);

        if (timedActivities != null) {
            sendMessageToAll(sharedProject, new ActivitiesPacketExtension(Saros
                .getDefault().getSessionManager().getSessionID(),
                timedActivities));
        }

    }

    protected void sendData(TransferData data) throws IOException {

        // TODO Buffer correctly when not connected....
        // this.fileTransferQueue.offer(transfer);
        // sendNextFile();

        /*
         * TODO This is not a safe way to determine whether the user really
         * supports Jingle at this point in time - He might have left and
         * reconnected and changed his Jingle settings in between
         */
        if (getFileTransferModeViaChat()
            || !jingleDiscovery.getCachedJingleSupport(data.getRecipient())) {

            ibb.send(data);

        } else {

            try {
                jingle.send(data);
            } catch (Exception e) {
                // TODO Catch only IOException and RuntimeException
                log
                    .info("Failed to send file with jingle, fall back to IBB",
                        e);
                ibb.send(data);

                // Fall back to ChatTransfer:
                // handmade.send(data)
            }
        }
    }

    public interface Transmitter {

        /**
         * Send the given data as a blocking operation.
         * 
         * If this call returns the data has been send successfully, otherwise
         * an IOException is thrown with the reason why the transfer failed.
         * 
         * @param data
         *            The data to be sent.
         * @throws IOException
         *             if the send failed
         */
        public void send(TransferData data) throws IOException;

    }

    /**
     * Sends a data buffer to a recipient using chat messages. The buffer is
     * transmitted Base64 encoded and split into blocks of size MAX_MSG_LENGTH.
     * 
     * This is not IBB (XEP-96)!!
     * 
     */
    Transmitter handmade = new Transmitter() {

        public void send(TransferData data) throws IOException {

            IBBData ibbData = data.getForIBB();

            final int maxMsgLen = Saros.getDefault().getPreferenceStore()
                .getInt(PreferenceConstants.CHATFILETRANSFER_CHUNKSIZE);

            // Convert byte array to base64 string
            byte[] bytes64 = Base64.encodeBase64(IOUtils
                .toByteArray(ibbData.data));

            String data64;
            try {
                data64 = new String(bytes64, "UTF-8");
            } catch (UnsupportedCharsetException e1) {
                data64 = new String(bytes64);
            }

            // send large data sets in several messages
            int tosend = data64.length();
            int pcount = (tosend / maxMsgLen)
                + ((tosend % maxMsgLen == 0) ? 0 : 1);
            int start = 0;
            try {
                for (int i = 1; i <= pcount; i++) {
                    int psize = Math.min(tosend, maxMsgLen);
                    int end = start + psize;

                    PacketExtension extension = DataTransferExtension
                        .getDefault().create(ibbData.filename,
                            ibbData.description, i, pcount,
                            data64.substring(start, end));

                    sendMessage(data.getRecipient(), extension);

                    start = end;
                    tosend -= psize;

                }
            } catch (Exception e) {
                throw new IOException("Sending failed", e);
            }
        }
    };

    Transmitter ibb = new Transmitter() {

        public void send(TransferData data) throws IOException {

            OutgoingFileTransfer
                .setResponseTimeout(XMPPChatTransmitter.MAX_TRANSFER_RETRIES * 1000);
            OutgoingFileTransfer transfer = fileTransferManager
                .createOutgoingFileTransfer(data.getRecipient().toString());

            FileTransferProcessMonitor monitor = new FileTransferProcessMonitor(
                transfer);

            IBBData ibbData = data.getForIBB();

            transfer.sendStream(ibbData.data, ibbData.filename,
                ibbData.filesize, ibbData.description);

            /* wait for complete transfer. */
            while (monitor.isAlive() && monitor.isRunning()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            monitor.closeMonitor(true);
        }

    };

    Transmitter jingle = new Transmitter() {

        public void send(TransferData data) throws IOException {
            try {
                JingleFileTransferManager jftm = getJingleManager();
                if (jftm == null)
                    throw new IOException("Jingle is disabled");

                jftm.send(data.getRecipient(), data.getForJingle());
            } catch (JingleSessionException e) {
                throw new IOException(e);
            }
        }
    };

    public void sendFileList(JID recipient, FileList fileList)
        throws IOException {

        sendData(toTransferData(recipient, fileList.toXML()));
    }

    public void sendFile(JID to, IProject project, IPath path, int timestamp)
        throws IOException {

        FileTransferData transfer = new FileTransferData();
        transfer.recipient = to;
        transfer.path = path;
        transfer.timestamp = timestamp;
        transfer.project = project;
        transfer.filesize = project.getFile(path).getLocation().toFile()
            .length();

        TransferData data = toTransferData(transfer);

        sendData(data);
    }

    public void sendProjectArchive(JID recipient, IProject project,
        File archive, IFileTransferCallback callback) {
        OutgoingFileTransfer
            .setResponseTimeout(XMPPChatTransmitter.MAX_TRANSFER_RETRIES * 1000);
        OutgoingFileTransfer transfer = this.fileTransferManager
            .createOutgoingFileTransfer(recipient.toString());

        try {
            transfer.sendFile(archive,
                XMPPChatTransmitter.PROJECT_ARCHIVE_DESCRIPTION);

            FileTransferProcessMonitor monitor = new FileTransferProcessMonitor(
                transfer, callback);
            /* wait for complete transfer. */
            while (monitor.isAlive() && monitor.isRunning()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            monitor.closeMonitor(true);

            if (transfer
                .getStatus()
                .equals(
                    org.jivesoftware.smackx.filetransfer.FileTransfer.Status.complete)) {
                XMPPChatTransmitter.log.debug("transfer complete");
                callback.fileSent(new Path(archive.getName()));
            }

            /* delete temp archive file. */
            archive.delete();

        } catch (Exception e) {

            XMPPChatTransmitter.log.warn("Failed to send archive file", e);
            if (callback != null) {
                callback.fileTransferFailed(null, e);
            }
        }
    }

    //
    // private void sendNextFile() {
    //
    // final TransferData transfer = this.fileTransferQueue.poll();
    //
    // if (transfer == null) {
    // XMPPChatTransmitter.log.debug("No file to send in queue.");
    // return;
    // }
    //
    // executor.execute(new Runnable() {
    // public void run() {
    // try {
    // sendData(transfer);
    // } catch (Exception e) {
    // transfer.callback.fileTransferFailed(transfer.path, e);
    // } finally {
    // sendNextFile();
    // }
    // }
    // });
    // }

    public void sendUserListTo(JID to, Collection<User> participants) {
        XMPPChatTransmitter.log.debug("Sending user list to " + to.toString());

        sendMessage(to, UserListExtension.getDefault().create(participants));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.fu_berlin.inf.dpp.net.ITransmitter#sendFileChecksumErrorMessage(org
     * .eclipse .core.runtime.IPath)
     */
    public void sendFileChecksumErrorMessage(IPath path, boolean resolved) {

        Collection<User> participants = Saros.getDefault().getSessionManager()
            .getSharedProject().getParticipants();

        XMPPChatTransmitter.log.debug("Sending checksum error message of file "
            + path.lastSegment() + " to all");
        for (User user : participants) {
            sendMessage(user.getJID(), ChecksumErrorExtension.getDefault()
                .create(path, resolved));
        }
    }

    /**
     * 
     * @see de.fu_berlin.inf.dpp.net.ITransmitter
     */
    public void sendDocChecksumsToClients(Collection<DocumentChecksum> checksums) {
        // send checksums to all clients
        ISharedProject project = Saros.getDefault().getSessionManager()
            .getSharedProject();

        if (project == null) {
            return;
        }

        assert project.isHost() : "This message should only be called from the host";

        Collection<User> participants = project.getParticipants();
        if (participants == null) {
            return;
        }

        for (User participant : participants) {
            if (project.getHost().getJID().equals(participant.getJID())) {
                continue;
            }

            JID jid = participant.getJID();
            XMPPChatTransmitter.log.debug("Sending checksums to " + jid);

            try {
                sendMessageWithoutQueueing(jid, ChecksumExtension.getDefault()
                    .create(checksums));
            } catch (IOException e) {
                // If checksums are failed to be sent, this is not a big problem
                log.warn("Sending Checksum to " + jid + " failed: ", e);
            }
        }
    }

    public void sendJupiterTransformationError(JID to, IPath path) {
        XMPPChatTransmitter.log
            .debug("Sending jupiter transformation error message to " + to
                + " of file " + path.lastSegment());
        sendMessage(to, JupiterErrorExtension.getDefault().create(path));
    }

    public void sendRemainingFiles() {

        if (this.fileTransferQueue.size() > 0) {
            // sendNextFile();
        }
    }

    public void sendRemainingMessages() {

        List<MessageTransfer> toTransfer = null;

        synchronized (messageTransferQueue) {
            toTransfer = new ArrayList<MessageTransfer>(messageTransferQueue);
            messageTransferQueue.clear();
        }

        for (MessageTransfer pex : toTransfer) {
            sendMessage(pex.receipient, pex.packetextension);
        }
    }

    public boolean resendActivity(JID jid, int timestamp, boolean andup) {

        boolean sent = false;

        ISharedProject project = Saros.getDefault().getSessionManager()
            .getSharedProject();

        try {
            List<TimedActivity> tempActivities = new LinkedList<TimedActivity>();
            for (TimedActivity tact : project.getSequencer()
                .getActivityHistory()) {

                if (((andup == false) && (tact.getTimestamp() != timestamp))
                    || ((andup == true) && (tact.getTimestamp() < timestamp))) {
                    continue;
                }

                tempActivities.add(tact);
                sent = true;

                if (andup == false) {
                    break;
                }
            }

            if (sent) {
                PacketExtension extension = new ActivitiesPacketExtension(Saros
                    .getDefault().getSessionManager().getSessionID(),
                    tempActivities);
                sendMessage(jid, extension);
            }

        } catch (Exception e) {
            Saros.getDefault().getLog().log(
                new Status(IStatus.ERROR, Saros.SAROS, IStatus.ERROR,
                    "Could not resend message", e));
        }

        return sent;
    }

    public void sendJupiterRequest(ISharedProject sharedProject,
        Request request, JID jid) {
        XMPPChatTransmitter.log.info("send request to : " + jid + " request: "
            + request);
        sendMessage(jid, new RequestPacketExtension(Saros.getDefault()
            .getSessionManager().getSessionID(), request));
    }

    /**
     * TODO use sendMessage
     * 
     * @param sharedProject
     * @param extension
     */
    protected void sendMessageToAll(ISharedProject sharedProject,
        PacketExtension extension) { // HACK

        JID myJID = Saros.getDefault().getMyJID();

        for (User participant : sharedProject.getParticipants()) {
            if (participant.getJID().equals(myJID)) {
                continue;
            }

            // if user is known to be offline, dont send but queue
            if (sharedProject != null) {

                User user = sharedProject.getParticipant(participant.getJID());
                if ((user != null)
                    && (user.getPresence() == User.UserConnectionState.OFFLINE)) {

                    // offline for too long
                    if (user.getOfflineSeconds() > XMPPChatTransmitter.FORCEDPART_OFFLINEUSER_AFTERSECS) {
                        XMPPChatTransmitter.log
                            .info("Removing offline user from session...");
                        sharedProject.removeUser(user);
                    } else {
                        queueMessage(participant.getJID(), extension);
                        XMPPChatTransmitter.log
                            .info("User known as offline - Message queued!");
                    }

                    continue;
                }
            }

            sendMessage(participant.getJID(), extension);
        }
    }

    private void queueMessage(JID jid, PacketExtension extension) {
        MessageTransfer msg = new MessageTransfer();
        msg.receipient = jid;
        msg.packetextension = extension;
        this.messageTransferQueue.add(msg);
    }

    protected void sendMessage(JID jid, PacketExtension extension) {

        if (!this.connection.isConnected()) {
            queueMessage(jid, extension);
            return;
        }

        try {
            sendMessageWithoutQueueing(jid, extension);
        } catch (IOException e) {
            queueMessage(jid, extension);

            Saros.getDefault().getLog().log(
                new Status(IStatus.ERROR, Saros.SAROS, IStatus.ERROR,
                    "Could not send message, message queued", e));
        }
    }

    /**
     * Send the given packet to the given user.
     * 
     * If no connection is set or sending fails, this method fails by throwing
     * an IOException
     * 
     * @param jid
     * @param extension
     */
    protected void sendMessageWithoutQueueing(JID jid, PacketExtension extension)
        throws IOException {

        if (!this.connection.isConnected()) {
            throw new IOException("Connection is not open");
        }

        try {
            Chat chat = getChat(jid);
            Message message = new Message();
            message.addExtension(extension);
            chat.sendMessage(message);
        } catch (XMPPException e) {
            throw new IOException("Failed to send message", e);
        }
    }

    public TransferData toTransferData(final JID recipient, final String xml) {

        return new TransferData() {

            public IBBData getForIBB() {

                IBBData result = new IBBData();

                byte[] content;
                try {
                    content = xml.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    content = xml.getBytes();
                }

                result.data = new ByteArrayInputStream(content);
                result.description = XMPPChatTransmitter.FILELIST_TRANSFER_DESCRIPTION;
                result.filesize = content.length;
                result.filename = XMPPChatTransmitter.FILELIST_TRANSFER_DESCRIPTION
                    + "." + new JID(connection.getUser()).getName();

                return result;
            }

            public JingleFileTransferData getForJingle() {

                JingleFileTransferData data = new JingleFileTransferData();
                data.file_list_content = xml;
                data.type = FileTransferType.FILELIST_TRANSFER;
                data.recipient = recipient;
                data.sender = new JID(connection.getUser());
                data.file_project_path = FileTransferType.FILELIST_TRANSFER
                    .toString();
                return data;
            }

            public JID getRecipient() {
                return recipient;
            }

        };
    }

    /**
     * Will return a TransferData object ready to pass to sendData(...).
     * 
     * This method will read all data from the given file into memory and fail
     * immediately if an error occurs.
     * 
     * If this method returns, then the data has been cached from the file.
     */
    public TransferData toTransferData(final FileTransferData transferData)
        throws IOException {

        // Read content data
        File f = new File(transferData.project.getFile(transferData.path)
            .getLocation().toOSString());
        final long filesize = f.length();
        final String filename = f.getName();
        final byte[] content = new byte[(int) filesize];

        try {
            InputStream in = transferData.project.getFile(transferData.path)
                .getContents();
            in.read(content, 0, (int) filesize);
        } catch (Exception e) {
            log.error("Error during read file content for transfer!", e);
            throw new IOException(e);
        }

        return new TransferData() {

            public IBBData getForIBB() {

                IBBData result = new IBBData();
                result.data = new ByteArrayInputStream(content);
                result.description = XMPPChatTransmitter.RESOURCE_TRANSFER_DESCRIPTION
                    + ":" + transferData.path.toString();
                result.filesize = filesize;
                result.filename = filename;

                return result;
            }

            public JingleFileTransferData getForJingle() {
                /* create file transfer. */
                JingleFileTransferData data = new JingleFileTransferData();

                data.file_project_path = transferData.path.toString();
                data.project_name = transferData.project.getName();
                data.type = FileTransferType.RESOURCE_TRANSFER;
                data.recipient = transferData.recipient;
                data.sender = new JID(connection.getUser());
                data.content = content;

                return data;
            }

            public JID getRecipient() {
                return transferData.recipient;
            }

        };
    }

    public interface TransferData {

        public JID getRecipient();

        public IBBData getForIBB();

        public JingleFileTransferData getForJingle();
    }

    public static class IBBData {

        String description;

        InputStream data;

        String filename;

        long filesize;
    }

    private void putIncomingChat(JID jid, String thread) {
        if (!this.chats.containsKey(jid)) {
            Chat chat = this.chatmanager.getThreadChat(thread);
            this.chats.put(jid, chat);
        }

    }

    private Chat getChat(JID jid) {
        if (this.connection == null) {
            throw new NullPointerException("Connection can't be null.");
        }

        Chat chat = this.chats.get(jid);

        if (chat == null) {
            chat = this.chatmanager.createChat(jid.toString(),
                new MessageListener() {
                    public void processMessage(Chat arg0, Message arg1) {
                        // We don't care about the messages here, because we
                        // are registered as a PacketListener
                    }
                });
            this.chats.put(jid, chat);
        }

        return chat;
    }

    public static boolean getFileTransferModeViaChat() {
        return Saros.getDefault().getPreferenceStore().getBoolean(
            PreferenceConstants.FORCE_FILETRANSFER_BY_CHAT);

    }

    ExecutorService executor = Executors.newFixedThreadPool(MAX_PARALLEL_SENDS);

    public void sendFileAsync(JID recipient, IProject project, IPath path,
        int timestamp, final IFileTransferCallback callback) throws IOException {

        if (callback == null)
            throw new IllegalArgumentException();

        final FileTransferData transfer = new FileTransferData();
        transfer.recipient = recipient;
        transfer.path = path;
        transfer.timestamp = timestamp;
        transfer.project = project;
        transfer.filesize = project.getFile(path).getLocation().toFile()
            .length();

        final TransferData data = toTransferData(transfer);

        executor.execute(new Runnable() {
            public void run() {
                try {
                    sendData(data);
                    callback.fileSent(transfer.path);
                } catch (Exception e) {
                    callback.fileTransferFailed(transfer.path, e);
                }
            }
        });
    }

    public void dispose() {
        executor.shutdownNow();
    }

    public void prepare(XMPPConnection connection) {
        setXMPPConnection(connection);
    }

    public void start() {
        // TODO start sending only now, queue otherwise
    }

    public void stop() {
        // TODO stop sending, but queue rather
    }
}
