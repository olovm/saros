package de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.contextMenu;

import java.rmi.RemoteException;

import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.sarosFinder.remoteComponents.EclipseComponent;

public interface RefactorC extends EclipseComponent {

    /**
     * Performs the action "move class to another package" which should be done
     * with the following steps:
     * 
     * <ol>
     * <li>selects menu "Refactor -> Move..."</li>
     * <li>choose the package specified by the passed parameter "targetPkg"</li>
     * <li>click "OK" to confirm the move</li>
     * </ol>
     * 
     * 
     * 
     * @param targetProject
     * @param targetPkg
     * @throws RemoteException
     */
    public void moveClassTo(String targetProject, String targetPkg)
        throws RemoteException;

    /**
     * Perform the action "rename class" which should be done with the following
     * steps:
     * <ol>
     * <li>click menu "Refactor" > "Rename..."</li>
     * <li>Enter the given new name to the text field with the title "New name:"
     * </li>
     * <li>click "OK" to confirm the rename</li>
     * <li>Waits until the shell "Rename Compilation Unit" is closed. It
     * guarantee that the "rename file" action is completely done.</li>
     * </ol>
     * <p>
     * <b>Attention:</b>
     * <ol>
     * <li>Makes sure, the package explorer view is open and active.</li>
     * <li>The function should treat all the recursive following actions, which
     * are activated or indirectly activated by clicking the sub menu
     * "rename..." . I mean, after clicking the sub menu you need to treat the
     * following popup window too.</li>
     * 
     * 
     * @param newName
     *            the new name of the given class.
     * 
     * @throws RemoteException
     */
    public void rename(String newName) throws RemoteException;

}
