
package net.sf.freecol.client.gui.panel;

import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import cz.autel.dmi.HIGLayout;

/**
 * This panel is displayed when an imporantant event in the game has happened.
 */
public final class EventPanel extends FreeColDialog implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final Logger logger = Logger.getLogger(EventPanel.class.getName());
    
    public static final int FIRST_LANDING = 0;
    public static final int MEETING_NATIVES = 1;
    public static final int MEETING_EUROPEANS = 2;
    public static final int MEETING_AZTEC = 3;
    public static final int MEETING_INCA = 4;

    private static final int OK = 0;
    
    private final FreeColClient freeColClient;
    @SuppressWarnings("unused")
    private final Canvas parent;
    
    private JLabel header;
    private JLabel imageLabel;
    private JButton okButton;

    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    * @param freeColClient The main controller object for the client.
    */
    public EventPanel(Canvas parent, FreeColClient freeColClient) {
        this.parent = parent;
        this.freeColClient = freeColClient;

        int[] w = {0};
        int[] h = {0, 10, 0, 10, 0};
        setLayout(new HIGLayout(w, h));

        header = new JLabel("", JLabel.CENTER);
        header.setFont(mediumHeaderFont);

        imageLabel = new JLabel();

        okButton = new JButton( Messages.message("ok") );
        okButton.setActionCommand(String.valueOf(OK));
        okButton.addActionListener(this);


        add(header, higConst.rc(1, 1));
        add(imageLabel, higConst.rc(3, 1, ""));
        add(okButton, higConst.rc(5, 1));
    }
    
    
    public void requestFocus() {
        okButton.requestFocus();
    }


    public void initialize(int eventID) {
        if (eventID == FIRST_LANDING) {
            Image image = (Image) UIManager.get("EventImage.firstLanding");
            imageLabel.setIcon(new ImageIcon(image));
            header.setText(Messages.message("event.firstLanding",
                                            new String[][] {{"%name%", freeColClient.getMyPlayer().getNewLandName()}}));
        } else if(eventID == MEETING_NATIVES) {
            Image image = (Image) UIManager.get("EventImage.meetingNatives");
            imageLabel.setIcon(new ImageIcon(image));
            header.setText(Messages.message("event.meetingNatives"));
        } else if(eventID == MEETING_EUROPEANS) {
            Image image = (Image) UIManager.get("EventImage.meetingEuropeans");
            imageLabel.setIcon(new ImageIcon(image));
            header.setText(Messages.message("event.meetingEuropeans"));
        } else if(eventID == MEETING_AZTEC) {
            Image image = (Image) UIManager.get("EventImage.meetingAztec");
            imageLabel.setIcon(new ImageIcon(image));
            header.setText(Messages.message("event.meetingAztec"));
        } else if(eventID == MEETING_INCA) {
            Image image = (Image) UIManager.get("EventImage.meetingInca");
            imageLabel.setIcon(new ImageIcon(image));
            header.setText(Messages.message("event.meetingInca"));
        } else {
            setResponse(new Boolean(false));
        }
        setSize(getPreferredSize());
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
