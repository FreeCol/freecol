
package net.sf.freecol.client.gui.panel;

import net.sf.freecol.client.gui.Canvas;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.NumberFormatException;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

/**
 * This is the panel that pops up when an error needs to be reported.
 */
public final class ErrorPanel extends JPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final Logger logger = Logger.getLogger(ErrorPanel.class.getName());
    
    private static final int OK = 0;
    
    private final Canvas parent;
    
    private JLabel errorLabel;
    private JButton errorButton;
    
    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public ErrorPanel(Canvas parent) {
        this.parent = parent;
        
        setLayout(null);

        errorButton = new JButton("OK");
        errorButton.setSize(80, 20);
        errorButton.setActionCommand(String.valueOf(OK));
        errorButton.addActionListener(this);

        errorLabel = null;
        
        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        }
        catch(Exception e) {
        }

        add(errorButton);
    }
    
    /**
    * Adapts the appearance of this ErrorPanel to the given error message.
    * @param message The error message to display in this error panel.
    */
    public void initialize(String message) {
        //TODO: size of labels and panel depend on length of message
        
        if (errorLabel != null) {
            remove(errorLabel);
        }

        errorLabel = new JLabel(message);
        errorLabel.setSize(320, 20);
        errorLabel.setLocation(10, 2);

        errorButton.setLocation(130, 25);

        add(errorLabel);
        setSize(340, 50);
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
                    parent.closeErrorPanel();
                    break;
                default:
                    logger.warning("Invalid Actioncommand: invalid number.");
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
