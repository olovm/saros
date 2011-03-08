package de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.finder.remoteWidgets;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IRemoteBotToolbarToggleButton extends Remote {
    /**********************************************
     * 
     * finders
     * 
     **********************************************/
    public IRemoteBotMenu contextMenu(String text) throws RemoteException;

    /**********************************************
     * 
     * actions
     * 
     **********************************************/
    public void click() throws RemoteException;

    public void deselect() throws RemoteException;

    public IRemoteBotToolbarToggleButton toggle() throws RemoteException;

    public void select() throws RemoteException;

    public void clickAndWait() throws RemoteException;

    public void setFocus() throws RemoteException;

    /**********************************************
     * 
     * states
     * 
     **********************************************/
    public boolean isEnabled() throws RemoteException;

    public boolean isVisible() throws RemoteException;

    public boolean isActive() throws RemoteException;

    public String getText() throws RemoteException;

    public String getToolTipText() throws RemoteException;

    public boolean isChecked() throws RemoteException;

    /**********************************************
     * 
     * waits until
     * 
     **********************************************/
    public void waitUntilIsEnabled() throws RemoteException;
}
