package net.sf.freecol.client.gui.panel;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import cz.autel.dmi.HIGLayout;

/**
 * This panel gets displayed to the player who have won the game.
 */
public final class VictoryPanel extends FreeColPanel implements ActionListener {
    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(VictoryPanel.class.getName());

    private static final int OK = 0, CONTINUE = 1;

    @SuppressWarnings("unused")
    private final Canvas parent;

    private final FreeColClient freeColClient;

    private Box buttonsBox = Box.createHorizontalBox();
            
    private JButton ok = new JButton(Messages.message("victory.yes"));

    private JButton continueButton = new JButton(Messages.message("victory.continue"));

    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     * @param freeColClient The main controller object for the client
     */
    public VictoryPanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;
        
        int[] widths = { 0 };
        int[] heights = { 0, margin, 0, margin, 0 };

        setLayout(new HIGLayout(widths, heights));
        setCancelComponent(ok);

        JLabel victoryLabel = getDefaultHeader(Messages.message("victory.text"));

        Image tempImage = (Image) UIManager.get("VictoryImage");
        JLabel imageLabel;

        if (tempImage != null) {
            imageLabel = new JLabel(new ImageIcon(tempImage));
        } else {
            imageLabel = new JLabel("");
        }

        int row = 1;
        int column = 1;
        
        add(victoryLabel, higConst.rc(row, column));
        row += 2;
        add(imageLabel, higConst.rc(row, column));
        row += 2;
        add(buttonsBox, higConst.rc(row, column));

        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);
        continueButton.setActionCommand(String.valueOf(CONTINUE));
        continueButton.addActionListener(this);
    }
    
    public void initialize() {
        buttonsBox.removeAll();
        if (freeColClient.isSingleplayer()) {
            buttonsBox.add(continueButton);
            buttonsBox.add(Box.createGlue());
        }
        buttonsBox.add(ok);

        setSize(getPreferredSize());
    }

    public void requestFocus() {
        ok.requestFocus();
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
                freeColClient.quit();
                break;
            case CONTINUE:
                freeColClient.continuePlaying();
                parent.remove(this);
                break;
            default:
                logger.warning("Invalid ActionCommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
