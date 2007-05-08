package net.sf.freecol.client.gui.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.filechooser.FileFilter;
import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FreeColMenuBar;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * An action for declaring independence.
 */
public class SelectLanguageAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SelectLanguageAction.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    public static final String ID = "selectLanguageAction";


    /**
     * Creates a new <code>SelectLanguageAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    SelectLanguageAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.game.selectLanguage", null, null);
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return true if this action should be enabled.
     */
    protected boolean shouldBeEnabled() {
        return true;
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "selectLanguageAction"
     */
    public String getId() {
        return ID;
    }

    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        try {
            FileFilter ff = new FileFilter() {
                    public boolean accept(File file) {
                        return (file.getName().startsWith(Messages.FILE_PREFIX) &&
                                file.getName().endsWith(Messages.FILE_SUFFIX));
                    }

                    public String getDescription() {
                        return "resource filter";
                    }

                };

            File resourceFile = freeColClient.getCanvas().showLoadDialog(new File(Messages.DATA_DIR),
                                                                         new FileFilter[] { ff });
            String[] pieces = resourceFile.getName().split("[_\\.]");
            if (pieces[0].equals(Messages.FILE_PREFIX)) {
                String language = "";
                String country = "";
                String variant = "";
                if (!pieces[1].equals(Messages.FILE_SUFFIX)) {
                    language = pieces[1];
                }
                if (!pieces[2].equals(Messages.FILE_SUFFIX)) {
                    country = pieces[2];
                }
                if (!pieces[3].equals(Messages.FILE_SUFFIX)) {
                    variant = pieces[3];
                }
                Messages.setMessageBundle(language, country, variant);
            } else {
                Messages.loadResources(resourceFile);
            }
            freeColClient.getActionManager().initializeActions();
            freeColClient.getCanvas().setJMenuBar(new FreeColMenuBar(freeColClient));
        } catch (Exception ex) {
            System.out.println("Failed to load resource bundle");
        }
    }
}
