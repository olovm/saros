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

import de.fu_berlin.inf.dpp.net.JID;

public class User {
    public enum UserConnectionState {
	OFFLINE, ONLINE, UNKNOWN
    };

    public enum UserRole {
	DRIVER, OBSERVER
    };

    private int colorid = 0;

    private final JID jid;
    private long offlineTime = 0;
    private UserConnectionState presence = UserConnectionState.UNKNOWN;
    private UserRole role = UserRole.OBSERVER;

    public User(JID jid) {
	this.jid = jid;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj instanceof User) {
	    User other = (User) obj;
	    return this.jid.equals(other.jid);
	}

	return false;
    }

    public int getColorID() {
	return this.colorid;
    }

    public JID getJid() {
	return this.jid;
    }

    public int getOfflineSecs() {
	return (int) (((new java.util.Date().getTime()) - this.offlineTime) / 1000);
    }

    public UserConnectionState getPresence() {
	return this.presence;
    }

    /**
     * Gets current project role of this user.
     * 
     * @return role (Driver, Observer)
     */
    public UserRole getUserRole() {
	return this.role;
    }

    public void setColorID(int c) {
	this.colorid = c;
    }

    public void setPresence(UserConnectionState p) {
	this.presence = p;
	if (this.presence == User.UserConnectionState.OFFLINE) {
	    this.offlineTime = (new java.util.Date().getTime());
	}
    }

    /**
     * set the current user role of this user inside the current project.
     * 
     * @param role
     *            (Driver, Observer)
     */
    public void setUserRole(UserRole role) {
	this.role = role;
    }

    @Override
    public String toString() {
	return this.jid.getName();
    }

}
