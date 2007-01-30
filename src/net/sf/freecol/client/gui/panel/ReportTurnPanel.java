package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.MissingResourceException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;

import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Turn Report.
 */
public final class ReportTurnPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportTurnPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.turn"));

    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize(ArrayList<ModelMessage> messages) {
        Player player = parent.getClient().getMyPlayer();

        // Display Panel
        reportPanel.removeAll();

        int[] widths = new int[] {0, margin, 0};
        int[] heights = new int[2 * messages.size() - 1];

        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }

        reportPanel.setLayout(new HIGLayout(widths, heights));

        int row = 1;
        int imageColumn = 1;
        int textColumn = 3;
        for (ModelMessage message : messages) {
            if (message.getDisplay() != null) {
                reportPanel.add(new JLabel(parent.getImageIcon(message.getDisplay())),
                                higConst.rc(row, imageColumn));
            }
            try {
                String text = Messages.message(message.getMessageID(),
                                               message.getData());
                reportPanel.add(new JLabel(text),
                                higConst.rc(row, textColumn));
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + message.getMessageID() + ".");
            }

            row += 2;
        }

        // source should be the same for all messages
        /*
        FreeColGameObject source = modelMessages[0].getSource();
        if ((source instanceof Europe && !europePanel.isShowing()) ||
            (source instanceof Colony || source instanceof WorkLocation) && 
            !colonyPanel.isShowing()) {

            FreeColDialog confirmDialog = FreeColDialog.createConfirmDialog(messageText, messageIcon, okText, cancelText);
            addCentered(confirmDialog, MODEL_MESSAGE_LAYER);
            confirmDialog.requestFocus();

            if (!confirmDialog.getResponseBoolean()) {
                remove(confirmDialog);
                if (source instanceof Europe) {
                    showEuropePanel();
                } else if (source instanceof Colony) {
                    showColonyPanel((Colony) source);
                } else if (source instanceof WorkLocation) {
                    showColonyPanel(((WorkLocation) source).getColony());
                }
            } else {
                remove(confirmDialog);
                if (!getColonyPanel().isShowing() && !getEuropePanel().isShowing())
                    freeColClient.getInGameController().nextModelMessage();
            }
        } else {
            FreeColDialog informationDialog = FreeColDialog.createInformationDialog(messageText, messageIcon);
            addCentered(informationDialog, MODEL_MESSAGE_LAYER);
            informationDialog.requestFocus();

            informationDialog.getResponse();
            remove(informationDialog);

            if (!getColonyPanel().isShowing() && !getEuropePanel().isShowing())
                freeColClient.getInGameController().nextModelMessage();
        }
        */
    }
}

