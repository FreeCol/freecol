
package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import net.sf.freecol.client.gui.i18n.Messages;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.FreeColClient;

/**
* This panel gets displayed to the player who have won the game.
*/
public final class VictoryPanel extends FreeColPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final Logger logger = Logger.getLogger(VictoryPanel.class.getName());
    private static final int    OK = 0;

    private final Canvas    parent;
    private final FreeColClient freeColClient;
    private JButton         ok = new JButton(Messages.message("victory.yes"));




    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public VictoryPanel(Canvas parent, FreeColClient freeColClient) {
        super(new FlowLayout(FlowLayout.CENTER, 1000, 10));
        this.parent = parent;
        this.freeColClient = freeColClient;
        
        setCancelComponent(ok);

        JLabel victoryLabel = new JLabel(Messages.message("victory.text"));
        Font font = (Font) UIManager.get("HeaderFont");
        victoryLabel.setFont(font.deriveFont(0, 48));
        add(victoryLabel);

        Image tempImage = (Image) UIManager.get("VictoryImage");
        JLabel imageLabel;
        
        if (tempImage != null) {
            imageLabel = new JLabel(new ImageIcon(tempImage));
            add(imageLabel);
        } else {
            imageLabel = new JLabel("");
        }

        add(ok);

        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);

        setSize(victoryLabel.getPreferredSize().width + 20, victoryLabel.getPreferredSize().height +
                                                            imageLabel.getPreferredSize().height +
                                                            ok.getPreferredSize().height + 50);
    }

    
    public void requestFocus() {
        ok.requestFocus();
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
                    freeColClient.quit();
                    break;
                default:
                    logger.warning("Invalid ActionCommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
