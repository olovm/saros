package de.fu_berlin.inf.dpp.stf.server.rmiSwtbot.eclipse.noExportedPages;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;

import de.fu_berlin.inf.dpp.stf.sarosSWTBot.SarosSWTBot;
import de.fu_berlin.inf.dpp.stf.server.rmiSwtbot.eclipse.RmiSWTWorkbenchBot;

public class TableObject {
    private static final transient Logger log = Logger
        .getLogger(TableObject.class);
    private RmiSWTWorkbenchBot rmiBot;
    private WaitUntilObject wUntil;
    private SarosSWTBot bot;

    public TableObject(RmiSWTWorkbenchBot rmiBot) {
        this.rmiBot = rmiBot;
        this.bot = RmiSWTWorkbenchBot.delegate;
        this.wUntil = rmiBot.wUntilObject;

    }

    public void clickContextMenuOfTable(String itemName, String contextName) {
        bot.table().getTableItem(itemName).contextMenu(contextName).click();
    }

    public SWTBotTableItem selectTableItemWithLabel(SWTBotTable table,
        String label) {
        try {
            wUntil.waitUntilTableItemExisted(table, label);
            return table.getTableItem(label);

        } catch (WidgetNotFoundException e) {
            log.warn("table item " + label + " not found.", e);
        }
        return null;
    }

    public void selectCheckBoxInTable(String text) {
        for (int i = 0; i < bot.table().rowCount(); i++) {
            if (bot.table().getTableItem(i).getText(0).equals(text)) {
                bot.table().getTableItem(i).check();
                log.debug("found invitee: " + text);
                return;
            }
        }
    }

    public void selectCheckBoxsInTable(List<String> invitees) {
        for (int i = 0; i < bot.table().rowCount(); i++) {
            String next = bot.table().getTableItem(i).getText(0);
            if (invitees.contains(next)) {
                bot.table().getTableItem(i).check();
            }
        }
    }

    public boolean isTableItemExist(SWTBotTable table, String itemName) {
        try {
            // waitUntilTableItemExsited(table, itemName);
            table.getTableItem(itemName);
            return true;
        } catch (WidgetNotFoundException e) {
            log.warn("table item " + itemName + " not found.", e);
        }
        return false;
    }
}
