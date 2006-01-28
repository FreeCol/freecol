
package net.sf.freecol.client.gui.panel;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Monarch;


/**
 * This panel is used to show information about a tile.
 */
public final class MonarchPanel extends FreeColDialog implements ActionListener {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(MonarchPanel.class.getName());

    private static final int OK = 0;
    private static final int CANCEL = 1;
    private final Canvas canvas;

    private final JLabel header;
    private final JLabel imageLabel;

    private final JLabel textLabel;
    private final JButton okButton;
    private final JButton cancelButton;
    private final JPanel buttonPanel;
    
    /**
    * The constructor that will add the items to this panel.
    */
    public MonarchPanel(Canvas parent) {
        canvas = parent;

        setLayout(new BorderLayout());
        
        header = new JLabel("", SwingConstants.CENTER);
        header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 36));
        header.setText(Messages.message("aMessageFromTheCrown"));
        
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(header, BorderLayout.PAGE_START);

        imageLabel = new JLabel();
        Image image = (Image) UIManager.get("MonarchImage");
        imageLabel.setIcon(new ImageIcon(image));
        add(imageLabel, BorderLayout.LINE_START);

        textLabel = new JLabel("", SwingConstants.CENTER);
        textLabel.setPreferredSize(new Dimension(180, 380));
        add(textLabel, BorderLayout.LINE_END);

        okButton = new JButton(Messages.message("ok"));
        okButton.setActionCommand(String.valueOf(OK));
        okButton.addActionListener(this);
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        cancelButton = new JButton();
        cancelButton.setActionCommand(String.valueOf(CANCEL));
        cancelButton.addActionListener(this);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        add(buttonPanel, BorderLayout.PAGE_END);
        
        setSize(450, 450);
    }


    public void requestFocus() {
        okButton.requestFocus();
    }


    /**
    * Initializes the information that is being displayed on this panel.
    * The information displayed will be based on the monarch action.
    *
    * @param action The monarch action.
    */
    public void initialize(int action, String [][] replace) {
        buttonPanel.remove(okButton);
        buttonPanel.remove(cancelButton);
        String messageID;
        String okText = "ok";
        String cancelText = null;
        switch (action) {
        case Monarch.RAISE_TAX:
            messageID = "model.monarch.raiseTax";
            okText = "model.monarch.acceptTax";
            cancelText = "model.monarch.rejectTax";
            break;
        case Monarch.ADD_TO_REF:
            messageID = "model.monarch.addToREF";
            break;
        case Monarch.DECLARE_WAR:
            messageID = "model.monarch.declareWar";
            break;
        case Monarch.SUPPORT_SEA:
            messageID = "model.monarch.supportSea";
            cancelText = "display";
            break;
        case Monarch.SUPPORT_LAND:
            messageID = "model.monarch.supportLand";
            cancelText = "display";
            break;
        case Monarch.WAIVE_TAX:
            messageID = "model.monarch.waiveTax";
            break;
        case Monarch.OFFER_MERCENARIES:
            messageID = "model.monarch.offerMercenaries";
            okText = "model.monarch.acceptMercenaries";
            cancelText = "model.monarch.rejectMercenaries";
            break;
        default:
            messageID = "Unknown monarch action: " + action;
        }
        textLabel.setText("<html><body><p>" + 
                          Messages.message(messageID, replace) +
                          "</p></body></html>");
        okButton.setText(Messages.message(okText));
        buttonPanel.add(okButton);
        if (cancelText != null) {
            cancelButton.setText(Messages.message(cancelText));
            buttonPanel.add(cancelButton);
        }
    }


    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
            case OK:
                setResponse(new Boolean(true));
                break;
            case CANCEL:
                setResponse(new Boolean(false));
                break;
            default:
                logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }

}
