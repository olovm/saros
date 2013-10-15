package de.fu_berlin.inf.dpp.ui.wizards;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.picocontainer.annotations.Inject;

import de.fu_berlin.inf.dpp.SarosPluginContext;
import de.fu_berlin.inf.dpp.net.JID;
import de.fu_berlin.inf.dpp.project.ISarosSessionManager;
import de.fu_berlin.inf.dpp.ui.ImageManager;
import de.fu_berlin.inf.dpp.ui.Messages;
import de.fu_berlin.inf.dpp.ui.util.CollaborationUtils;
import de.fu_berlin.inf.dpp.ui.views.SarosView;
import de.fu_berlin.inf.dpp.ui.wizards.pages.ContactSelectionWizardPage;
import de.fu_berlin.inf.dpp.ui.wizards.pages.ProjectSelectionWizardPage;

/**
 * Wizard for sharing project resources.
 * <p>
 * Starts sharing the selected resource(s) with the selected buddy(s) on finish.
 * 
 * @author bkahlert
 * @author kheld
 */
public class ShareProjectWizard extends Wizard {

    public static final String TITLE = Messages.ShareProjectWizard_title;
    public static final ImageDescriptor IMAGE = ImageManager.WIZBAN_SHARE_PROJECT_OUTGOING;

    @Inject
    protected ISarosSessionManager sarosSessionManager;

    protected ProjectSelectionWizardPage projectSelectionWizardPage = new ProjectSelectionWizardPage();
    protected ContactSelectionWizardPage buddySelectionWizardPage = new ContactSelectionWizardPage(
        true);

    public ShareProjectWizard() {
        SarosPluginContext.initComponent(this);
        setWindowTitle(TITLE);
        setDefaultPageImageDescriptor(IMAGE);
        setNeedsProgressMonitor(true);
        setHelpAvailable(false);
    }

    /**
     * Remove any open notifications on page change in the wizard, in case the
     * user restored a selection in the ResourceSelectionComposite
     */
    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        SarosView.clearNotifications();
        return super.getNextPage(page);
    }

    @Override
    public void addPages() {
        addPage(projectSelectionWizardPage);
        addPage(buddySelectionWizardPage);
    }

    /**
     * @JTourBusStop 2, Invitation Process:
     * 
     *               The chosen resources are put into collections to be sent to
     *               the chosen buddies.
     * 
     *               As a slight detour, notice that the call to
     *               CollaborationUtils.shareResourcesWith includes
     *               sarosSessionManager as an argument. However, when you look
     *               through this class you should find this variable is
     *               declared but never initialized! It is not null however.
     * 
     *               Notice that "@Inject" annotation above the
     *               sarosSessionManager declaration? That means that our
     *               PicoContainer has taken care of initializing the variable
     *               for us. Look up PicoContainer to find out more about this.
     */
    @Override
    public boolean performFinish() {

        List<IResource> selectedResources = projectSelectionWizardPage
            .getSelectedResources();

        List<JID> selectedBuddies = buddySelectionWizardPage
            .getSelectedContacts();

        if (selectedResources == null || selectedBuddies == null)
            return false;

        if (selectedResources.isEmpty())
            return false;

        projectSelectionWizardPage.rememberCurrentSelection();

        SarosView.clearNotifications();

        CollaborationUtils.shareResourcesWith(sarosSessionManager,
            selectedResources, selectedBuddies);

        return true;
    }
}
