/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import cz.autel.dmi.HIGLayout;

/**
* Asks the user if he's sure he wants to quit.
*/
public final class QuitDialog extends FreeColDialog implements ActionListener {
    private static final Logger logger = Logger.getLogger(QuitDialog.class.getName());

    
    private static final int    OK = 0,
                                CANCEL = 1;

    private final Canvas    parent;
    private JButton         ok = new JButton(Messages.message("yes"));
    private JButton         cancel;
    
    
    
    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public QuitDialog(Canvas parent) {
        this.parent = parent;

        int[] widths = {0};
        int[] heights = {0, margin, 0};
        setLayout(new HIGLayout(widths, heights));

        cancel = new JButton( Messages.message("no") );
        JLabel qLabel = new JLabel( Messages.message("areYouSureYouWantToQuit") );
        qLabel.setHorizontalAlignment(SwingConstants.CENTER);

        ok.setActionCommand(String.valueOf(OK));
        cancel.setActionCommand(String.valueOf(CANCEL));
        
        ok.addActionListener(this);
        cancel.addActionListener(this);

        ok.setMnemonic('y');
        cancel.setMnemonic('n');
    

        FreeColPanel.enterPressesWhenFocused(cancel);
        FreeColPanel.enterPressesWhenFocused(ok);

        setCancelComponent(cancel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        add(qLabel, higConst.rc(1, 1));
        add(buttonPanel, higConst.rc(3, 1));

        setSize(getPreferredSize());
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
