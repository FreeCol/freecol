
package net.sf.freecol.client.gui.panel;

import net.sf.freecol.client.gui.Canvas;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.NumberFormatException;
import java.util.logging.Logger;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;

/**
 * This is the panel that pops up when an error needs to be reported.
 */
public final class ErrorPanel extends FreeColDialog implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final Logger logger = Logger.getLogger(ErrorPanel.class.getName());
    
    private static final int OK = 0;
    
    private static final int lineWidth = 320;
    
    private final Canvas parent;
    
    private LinkedList errorLabels; // A LinkedList of JLabel objects.
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

        errorLabels = null;

        add(errorButton);
    }
    
    
    public void requestFocus() {
        errorButton.requestFocus();
    }


    /**
    * Adapts the appearance of this ErrorPanel to the given error message.
    * If the error message is wider than lineWidth then the height of this panel
    * will be adjusted.
    * @param message The error message to display in this error panel.
    */
    public void initialize(String message) {
        LinkedList lines = new LinkedList();
        while (getFontMetrics(getFont()).getStringBounds(message, getGraphics()).getWidth()
                > lineWidth) {
            int spaceIndex = message.indexOf(' ');
            int previousIndex = -1;
            while (getFontMetrics(getFont()).getStringBounds(message.substring(0, spaceIndex),
                    getGraphics()).getWidth() <= lineWidth) {
                previousIndex = spaceIndex;
                if ((spaceIndex + 1) >= message.length()) {
                    spaceIndex = 0;
                    break;
                }
                spaceIndex = message.indexOf(' ', spaceIndex + 1);
                if (spaceIndex == -1) {
                    spaceIndex = 0;
                    break;
                }
            }
            
            if ((previousIndex >= 0) && (spaceIndex >= 0)) {
                lines.add(message.substring(0, previousIndex));
                if (previousIndex + 1 < message.length()) {
                    message = message.substring(previousIndex + 1);
                }
                else {
                    break;
                }
            }
            else {
                lines.add(message);
                lines.add("Internal error in ErrorPanel");
                break;
            }
        }
        
        if (message.trim().length() > 0) {
            lines.add(message);
        }
        
        if (errorLabels != null) {
            for (int i = 0; i < errorLabels.size(); i++) {
                remove((JLabel)errorLabels.get(i));
            }
            
            errorLabels.clear();
        }
        else {
            errorLabels = new LinkedList();
        }

        for (int i = 0; i < lines.size(); i++) {
            JLabel label = new JLabel((String)lines.get(i));
            label.setSize(lineWidth, 20);
            label.setLocation(10, 2 + i * 20);
            add(label);
            errorLabels.add(label);
        }

        errorButton.setLocation(130, 25 + (lines.size() - 1) * 20);
        add(errorButton);
        
        setSize(340, 50 + (lines.size() - 1) * 20);
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
                    //parent.closeErrorPanel();
                    setResponse(new Boolean(true));
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
