/**
 *
 */
package de.fu_berlin.inf.dpp.net.internal.extensions;

import org.jivesoftware.smack.packet.DefaultPacketExtension;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;

import de.fu_berlin.inf.dpp.net.JID;

public abstract class JoinExtension extends SessionDefaultPacketExtension {

    public JoinExtension() {
        super("join");
    }

    public PacketExtension create(int colorID) {
        DefaultPacketExtension extension = create();

        extension.setValue(PacketExtensionUtils.COLOR_ID, String.valueOf(colorID));
        return extension;
    }

    /**
     * @return Returns a default implementation of this extension that does
     *         nothing in joinReceived(...)
     */
    public static JoinExtension getDefault() {
        return PacketExtensionUtils.getContainer()
            .getComponent(JoinExtension.class);
    }

    @Override
    public void processMessage(JID sender, Message message) {
        DefaultPacketExtension extension = JoinExtension.getDefault()
            .getExtension(message);

        int colorID = Integer.parseInt(extension
            .getValue(PacketExtensionUtils.COLOR_ID));

        joinReceived(sender, colorID);
    }

    public abstract void joinReceived(JID sender, int colorID);

}