package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Monarch;
import cz.autel.dmi.HIGLayout;

/**
 * This panel is used to show information about a tile.
 */
public final class MonarchPanel extends FreeColDialog implements ActionListener {

    public static final String COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(MonarchPanel.class.getName());

    private static final int OK = 0;

    private static final int CANCEL = 1;

    private final Canvas parent;

    private final JLabel header;

    private final JLabel imageLabel;

    private JTextArea textArea;

    private final JButton okButton;

    private final JButton cancelButton;


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent panel.
     */
    public MonarchPanel(Canvas parent) {
        this.parent = parent;

        header = new JLabel("", SwingConstants.CENTER);
        header.setFont(mediumHeaderFont);
        header.setText(Messages.message("aMessageFromTheCrown"));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        imageLabel = new JLabel();

        okButton = new JButton(Messages.message("ok"));
        okButton.setActionCommand(String.valueOf(OK));
        okButton.addActionListener(this);
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        cancelButton = new JButton();
        cancelButton.setActionCommand(String.valueOf(CANCEL));
        cancelButton.addActionListener(this);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);

    }

    public void requestFocus() {
        okButton.requestFocus();
    }

    /**
     * Initializes the information that is being displayed on this panel. The
     * information displayed will be based on the monarch action.
     * 
     * @param action The monarch action.
     * @param replace The data to be used when displaying i18n-strings.
     */
    public void initialize(int action, String[][] replace) {
        removeAll();

        int nation = parent.getClient().getMyPlayer().getNation();
        imageLabel.setIcon(parent.getGUI().getImageLibrary().getMonarchImageIcon(nation));

        int[] widths = { -3, 3 * margin, -1 };
        int[] heights = { 0, margin, 0, margin, 0 };
        setLayout(new HIGLayout(widths, heights));

        int row = 1;
        int imageColumn = 1;
        int textColumn = 3;
        add(header, higConst.rcwh(row, imageColumn, widths.length, 1));
        row += 2;

        add(imageLabel, higConst.rc(row, imageColumn, "r"));

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

        textArea = getDefaultTextArea(Messages.message(messageID, replace));
        add(textArea, higConst.rc(row, textColumn));
        row += 2;

        okButton.setText(Messages.message(okText));

        if (cancelText == null) {
            add(okButton, higConst.rcwh(row, imageColumn, widths.length, 1, ""));
        } else {
            cancelButton.setText(Messages.message(cancelText));
            add(okButton, higConst.rc(row, imageColumn, "r"));
            add(cancelButton, higConst.rc(row, textColumn, "l"));
        }

        setSize(getPreferredSize());
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
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
