/*
 *  QuitPanel.java - Asks the user if he's sure he wants to quit.
 *
 *  Copyright (C) 2002  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import net.sf.freecol.client.gui.Canvas;

/**
* Asks the user if he's sure he wants to quit.
*/
public final class QuitDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(QuitDialog.class.getName());
    private static final int    OK = 0,
                                CANCEL = 1;

    private final Canvas    parent;
    private JButton         ok = new JButton("Yes");

    
    
    
    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public QuitDialog(Canvas parent) {
        this.parent = parent;

        JButton         cancel = new JButton("No");
        JLabel          qLabel = new JLabel("Are you sure you want to Quit?");

        qLabel.setSize(200, 20);
        ok.setSize(60, 20);
        cancel.setSize(60, 20);

        qLabel.setLocation(10, 10);
        ok.setLocation(30, 40);
        cancel.setLocation(130, 40);

        setLayout(null);
        
        ok.setActionCommand(String.valueOf(OK));
        cancel.setActionCommand(String.valueOf(CANCEL));
        
        ok.addActionListener(this);
        cancel.addActionListener(this);
        
        add(qLabel);
        add(ok);
        add(cancel);
        
        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        }
        catch(Exception e) {
        }

        setSize(220, 70);
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
                    parent.remove(this);
                    setResponse(new Boolean(true));
                    //parent.quit();
                    break;
                case CANCEL:
                    parent.remove(this);
                    setResponse(new Boolean(false));
                    break;
                default:
                    logger.warning("Invalid ActionCommand: invalid number.");
            }
        }
        catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
