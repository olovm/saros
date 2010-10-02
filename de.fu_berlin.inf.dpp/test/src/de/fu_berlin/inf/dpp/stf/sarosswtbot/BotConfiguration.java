package de.fu_berlin.inf.dpp.stf.sarosswtbot;

import de.fu_berlin.inf.dpp.Saros;

/**
 * BotConfigration is the central configuration class, so that you can switch
 * from local tests to VM related tests without pain.
 * 
 * Example VM arguments of the Runconfigration for user alice1_fu running on
 * localhost:
 * 
 * <pre>
 * -Xmx512m -ea -XX:MaxPermSize=128m -Djava.rmi.server.codebase=file:$WORKSPACE/Saros/bin -Djava.security.manager -Djava.security.policy=file:/Users/sszuecs/Documents/eclipse_workspace_35/Saros_rmiswtbot/bin/all.policy -Dde.fu_berlin.inf.dpp.testmode=12345 -Djava.rmi.server.hostname=localhost
 * </pre>
 * 
 * On the bottom you will find the used configurations which is used by STF.
 */
public class BotConfiguration {
    public final static boolean DEVELOPMODE = true;

    /**
     * Directory for temporary files for saving screen captures.
     */
    // public final static transient String TEMPDIR = System
    // .getProperty("java.io.tmpdir");

    // public final static transient String TEMPDIR =
    // "D:\\Lin\\Space\\Saros_10.07.30\\screenshot";

    public final static transient String SCREENSHOTDIR = "screenshot";
    /** A small Java Project */
    public final static String PROJECTNAME_SVN = "examples";
    public final static String PROJECTNAME_SVN_TRUNK = "examples [trunk/examples]";
    public final static String PROJECTNAME_SVN_TAG = "examples [tags/eclipsecon2009]";
    public final static String PROJECTNAME = "Foo_Saros";
    public final static String PROJECTNAME2 = "Foo_Saros2";
    public final static String PROJECTNAME3 = "Foo_Saros3";
    public final static String CLASSNAME = "MyClass";
    public final static String CLASSNAME2 = "MyClass2";
    public final static String CLASSNAME3 = "MyClass3";
    public final static String PACKAGENAME = "my.pkg";
    public final static String PACKAGENAME2 = "my.pkg2";
    public final static String PACKAGENAME3 = "my.pkg3";
    public final static String CONTENTPATH = "test/STF/" + CLASSNAME + ".java";
    public final static String CONTENTPATH2 = "test/STF/" + CLASSNAME2
        + ".java";
    public final static String CONTENTPATH3 = "test/STF/" + CLASSNAME3
        + ".java";
    public final static String NAME_ROSTER_VIEW = "ROSTER";
    public final static String NAME_SESSION_VIEW = "Shared Project Session";
    public final static String NAME_CHAT_VIEW = "Chat View";

    public final static String SVN_URL = "http://swtbot-examples.googlecode.com/svn";

    /**
     * VM test accounts
     */
    private final static String JID_ALICE_VM = ("testuser@saros-con.imp.fu-berlin.de");
    private final static String PASSWORD_ALICE_VM = "test";
    private final static String HOST_ALICE_VM = "192.168.66.71";
    private final static int PORT_ALICE_VM = 1099;
    private final static String JID_BOB_VM = ("testuser2@saros-con.imp.fu-berlin.de");
    private final static String PASSWORD_BOB_VM = "test";
    private final static String HOST_BOB_VM = "192.168.66.72";
    private final static int PORT_BOB_VM = 1099;
    private final static String JID_CARL_VM = ("unconfigured");
    private final static String PASSWORD_CARL_VM = "unconfigured";
    private final static String HOST_CARL_VM = "unconfigured";
    private final static int PORT_CARL_VM = 1099;

    // /**
    // * Local test accounts
    // */
    // private final static String JID_ALICE_LOCAL = ("alice1_fu@jabber.org/" +
    // Saros.RESOURCE);
    // private final static String PASSWORD_ALICE_LOCAL = "dddfffggg";
    // private final static String HOST_ALICE_LOCAL = "localhost";
    // private final static int PORT_ALICE_LOCAL = 12345;
    // private final static String JID_BOB_LOCAL = ("bob1_fu@jabber.org/" +
    // Saros.RESOURCE);
    // private final static String PASSWORD_BOB_LOCAL = "dddfffggg";
    // private final static String HOST_BOB_LOCAL = "localhost";
    // private final static int PORT_BOB_LOCAL = 12346;
    // private final static String JID_CARL_LOCAL = ("carl1_fu@jabber.org/" +
    // Saros.RESOURCE);
    // private final static String PASSWORD_CARL_LOCAL = "dddfffggg";
    // private final static String HOST_CARL_LOCAL = "localhost";
    // private final static int PORT_CARL_LOCAL = 1099;

    /**
     * Local test accounts
     */
    private final static String JID_ALICE_LOCAL = ("lin@saros-con.imp.fu-berlin.de/" + Saros.RESOURCE);
    private final static String PASSWORD_ALICE_LOCAL = "lin";
    // private final static String JID_ALICE_LOCAL = ("alice1_fu@jabber.ccc.de/"
    // + Saros.RESOURCE);
    // private final static String PASSWORD_ALICE_LOCAL = "dddfffggg";
    private final static String HOST_ALICE_LOCAL = "localhost";
    private final static int PORT_ALICE_LOCAL = 12345;

    private final static String JID_BOB_LOCAL = ("lin2@saros-con.imp.fu-berlin.de/" + Saros.RESOURCE);
    private final static String PASSWORD_BOB_LOCAL = "lin2";
    // private final static String JID_BOB_LOCAL = ("bob1_fu@jabber.ccc.de/" +
    // Saros.RESOURCE);
    // private final static String PASSWORD_BOB_LOCAL = "dddfffggg";
    private final static String HOST_BOB_LOCAL = "localhost";
    private final static int PORT_BOB_LOCAL = 12346;

    private final static String JID_CARL_LOCAL = ("lin3@saros-con.imp.fu-berlin.de/" + Saros.RESOURCE);
    private final static String PASSWORD_CARL_LOCAL = "lin3";
    // private final static String JID_CARL_LOCAL = ("carl1_fu@jabber.ccc.de/" +
    // Saros.RESOURCE);
    // private final static String PASSWORD_CARL_LOCAL = "dddfffggg";
    private final static String HOST_CARL_LOCAL = "localhost";
    private final static int PORT_CARL_LOCAL = 12347;

    /**
     * test accounts
     */
    public final static String JID_ALICE;
    public final static String PASSWORD_ALICE;
    public final static String HOST_ALICE;
    public final static int PORT_ALICE;
    public final static String JID_BOB;
    public final static String PASSWORD_BOB;
    public final static String HOST_BOB;
    public final static int PORT_BOB;
    public final static String JID_CARL;
    public final static String PASSWORD_CARL;
    public final static String HOST_CARL;
    public final static int PORT_CARL;

    static {
        if (DEVELOPMODE) {
            JID_ALICE = JID_ALICE_LOCAL;
            PASSWORD_ALICE = PASSWORD_ALICE_LOCAL;
            HOST_ALICE = HOST_ALICE_LOCAL;
            PORT_ALICE = PORT_ALICE_LOCAL;
            JID_BOB = JID_BOB_LOCAL;
            PASSWORD_BOB = PASSWORD_BOB_LOCAL;
            HOST_BOB = HOST_BOB_LOCAL;
            PORT_BOB = PORT_BOB_LOCAL;
            JID_CARL = JID_CARL_LOCAL;
            PASSWORD_CARL = PASSWORD_CARL_LOCAL;
            HOST_CARL = HOST_CARL_LOCAL;
            PORT_CARL = PORT_CARL_LOCAL;
        } else {
            JID_ALICE = JID_ALICE_VM;
            PASSWORD_ALICE = PASSWORD_ALICE_VM;
            HOST_ALICE = HOST_ALICE_VM;
            PORT_ALICE = PORT_ALICE_VM;
            JID_BOB = JID_BOB_VM;
            PASSWORD_BOB = PASSWORD_BOB_VM;
            HOST_BOB = HOST_BOB_VM;
            PORT_BOB = PORT_BOB_VM;
            JID_CARL = JID_CARL_VM;
            PASSWORD_CARL = PASSWORD_CARL_VM;
            HOST_CARL = HOST_CARL_VM;
            PORT_CARL = PORT_CARL_VM;
        }
    }
}
