
package net.sf.freecol.client.gui.panel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;

import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.NumberFormatException;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.JButton;
import javax.swing.JLabel;

/**
 * This panel is displayed when an imporantant event in the game has happened.
 */
public final class EventPanel extends FreeColDialog implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final Logger logger = Logger.getLogger(EventPanel.class.getName());
    
    public static final int FIRST_LANDING = 0;

    private static final int OK = 0;
    
    private final FreeColClient freeColClient;
    private final Canvas parent;
    
    private JLabel header;
    private JLabel imageLabel;
    private JButton okButton;

    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public EventPanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;
        
        setLayout(new BorderLayout());

        header = new JLabel("", JLabel.CENTER);
        header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 36));
        header.setBorder(new EmptyBorder(10, 0, 10, 0));

        imageLabel = new JLabel();
        imageLabel.setBorder(new EmptyBorder(0, 10, 0, 10));

        JPanel p1 = new JPanel(new BorderLayout(0, 0));
        p1.setBorder(new EmptyBorder(10, 20, 10, 20));

        okButton = new JButton("OK");
        okButton.setActionCommand(String.valueOf(OK));
        okButton.addActionListener(this);
        p1.add(okButton);

        add(header, BorderLayout.NORTH);
        add(imageLabel, BorderLayout.CENTER);
        add(p1, BorderLayout.SOUTH);
    }
    
    
    public void requestFocus() {
        okButton.requestFocus();
    }


    public void initialize(int eventID) {
        if (eventID == FIRST_LANDING) {
            Image image = (Image) UIManager.get("EventImage.firstLanding");
            imageLabel.setIcon(new ImageIcon(image));

            header.setText(Messages.message("event.firstLanding").replaceAll("%name%", freeColClient.getMyPlayer().getNewLandName()));
            setSize(getPreferredSize());
        } else {
            setResponse(new Boolean(false));
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
                default:
                    logger.warning("Invalid Actioncommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
