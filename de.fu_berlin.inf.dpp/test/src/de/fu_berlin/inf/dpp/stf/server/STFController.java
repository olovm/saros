package de.fu_berlin.inf.dpp.stf.server;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.accountManagement.XMPPAccountStore;
import de.fu_berlin.inf.dpp.editor.EditorManager;
import de.fu_berlin.inf.dpp.feedback.FeedbackManager;
import de.fu_berlin.inf.dpp.net.internal.DataTransferManager;
import de.fu_berlin.inf.dpp.project.SarosSessionManager;
import de.fu_berlin.inf.dpp.stf.STF;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.RemoteBot;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.RemoteWorkbenchBot;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotButton;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotCCombo;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotCheckBox;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotCombo;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotEditor;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotLabel;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotList;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotMenu;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotPerspective;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotRadio;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotShell;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotStyledText;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotTable;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotTableItem;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotText;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotToolbarButton;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotToolbarDropDownButton;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotToolbarPushButton;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotToolbarRadioButton;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotToolbarToggleButton;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotTree;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotTreeItem;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets.RemoteBotViewMenu;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.SuperBot;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.Component;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.ContextMenuWrapper;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.NewC;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.RefactorC;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.SarosContextMenuWrapper;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.ShareWithC;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.contextMenu.TeamC;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.menuBar.MenuBar;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.menuBar.SarosM;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.menuBar.SarosPreferences;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.menuBar.WindowM;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.ConsoleView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.PEView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.ProgressView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.Views;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.sarosViews.BuddiesView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.sarosViews.ChatView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.sarosViews.RSView;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.superFinder.remoteComponents.views.sarosViews.SessionView;

/**
 * STFController is responsible to register all exported objects.
 * 
 */
public class STFController {

    private static final transient Logger log = Logger
        .getLogger(STFController.class);

    public static int sleepTime = 750;

    /** The RMI registry used, is not exported */
    private static transient Registry registry;

    /*
     * sometimes when connecting to a server i'm getting error:
     * java.rmi.NoSuchObjectException:no Such object in table. This happens when
     * the remote object the stub refers to has been DGC'd and GC's locally. My
     * solution is keeping a static references "classVariable" to the object in
     * the object in the server JVM.
     */
    public static void exportedObjects(int port, Saros saros,
        SarosSessionManager sessionManager,
        DataTransferManager dataTransferManager, EditorManager editorManager,
        XMPPAccountStore xmppAccountStore, FeedbackManager feedbackManager)
        throws RemoteException {

        STF.saros = saros;
        Component.sessionManager = sessionManager;
        Component.dataTransferManager = dataTransferManager;
        Component.editorManager = editorManager;
        Component.xmppAccountStore = xmppAccountStore;
        Component.feedbackManager = feedbackManager;

        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry(port);
        }

        /*
         * bots' family
         */
        exportObject(RemoteBot.getInstance(), "bot");
        exportObject(RemoteWorkbenchBot.getInstance(), "workbenchBot");
        exportObject(SuperBot.getInstance(), "superBot");

        /*
         * export remoteWidgets
         */
        exportObject(RemoteBotButton.getInstance(), "button");
        exportObject(RemoteBotCCombo.getInstance(), "ccombo");
        exportObject(RemoteBotCheckBox.getInstance(), "checkBox");
        exportObject(RemoteBotCombo.getInstance(), "combo");
        exportObject(RemoteBotEditor.getInstance(), "eclipseEditor");
        exportObject(RemoteBotLabel.getInstance(), "label");
        exportObject(RemoteBotList.getInstance(), "list");
        exportObject(RemoteBotMenu.getInstance(), "menu");
        exportObject(RemoteBotPerspective.getInstance(), "perspective");
        exportObject(RemoteBotRadio.getInstance(), "radio");
        exportObject(RemoteBotShell.getInstance(), "shell");
        exportObject(RemoteBotStyledText.getInstance(), "styledText");
        exportObject(RemoteBotTable.getInstance(), "table");
        exportObject(RemoteBotTableItem.getInstance(), "tableItem");
        exportObject(RemoteBotText.getInstance(), "text");
        exportObject(RemoteBotToolbarButton.getInstance(), "toggleButton");
        exportObject(RemoteBotToolbarButton.getInstance(), "toolbarButton");
        exportObject(RemoteBotToolbarDropDownButton.getInstance(),
            "toolbarDropDownButton");
        exportObject(RemoteBotToolbarPushButton.getInstance(),
            "toolbarPushButon");
        exportObject(RemoteBotToolbarRadioButton.getInstance(),
            "toolbarRadioButton");
        exportObject(RemoteBotToolbarToggleButton.getInstance(),
            "toolbarToggleButton");
        exportObject(RemoteBotTree.getInstance(), "tree");
        exportObject(RemoteBotTreeItem.getInstance(), "treeItem");
        exportObject(RemoteBotView.getInstance(), "view");
        exportObject(RemoteBotViewMenu.getInstance(), "viewMenu");

        /*
         * remote eclipse components
         */
        exportObject(PEView.getInstance(), "packageExplorerView");
        exportObject(ProgressView.getInstance(), "progressView");
        exportObject(BuddiesView.getInstance(), "rosterView");
        exportObject(SessionView.getInstance(), "sessionView");
        exportObject(RSView.getInstance(), "remoteScreenView");
        exportObject(ChatView.getInstance(), "chatView");
        exportObject(ConsoleView.getInstance(), "consoleView");

        exportObject(NewC.getInstance(), "fileM");
        exportObject(RefactorC.getInstance(), "refactorM");
        exportObject(WindowM.getInstance(), "windowM");
        exportObject(SarosM.getInstance(), "sarosM");

        exportObject(TeamC.getInstance(), "teamC");
        exportObject(ShareWithC.getInstance(), "shareWithC");
        exportObject(ContextMenuWrapper.getInstance(), "contextMenu");
        exportObject(SarosContextMenuWrapper.getInstance(),
            "sarosContextMenu");

        exportObject(SarosPreferences.getInstance(), "sarosPreferences");

        exportObject(Views.getInstance(), "views");
        exportObject(MenuBar.getInstance(), "menuBar");
    }

    /**
     * Add a shutdown hook to unbind exported Object from registry.
     */
    private static void addShutdownHook(final String name) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    if (registry != null && name != null)
                        registry.unbind(name);
                } catch (RemoteException e) {
                    log.warn("Failed to unbind: " + name, e);
                } catch (NotBoundException e) {
                    log.warn("Failed to unbind: " + name, e);
                }
            }
        });
    }

    /**
     * Export object by given name on our local RMI Registry.
     */
    private static Remote exportObject(Remote exportedObject, String exportName) {
        try {
            Remote remoteObject = UnicastRemoteObject.exportObject(
                exportedObject, 0);
            addShutdownHook(exportName);
            registry.bind(exportName, remoteObject);
            return remoteObject;
        } catch (RemoteException e) {
            log.error("Could not export the object " + exportName, e);
        } catch (AlreadyBoundException e) {
            log.error("Could not bind the object " + exportName
                + ", because it is bound already.", e);
        }
        return null;
    }

    public static void listRmiObjects() {
        try {
            for (String s : registry.list())
                log.debug("registered Object: " + s);
        } catch (AccessException e) {
            log.error("Failed on access", e);
        } catch (RemoteException e) {
            log.error("Failed", e);
        }
    }
}
