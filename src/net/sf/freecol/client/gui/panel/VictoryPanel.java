/*
 *  VictoryPanel.java - This panel gets displayed to the player who have won the game.
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
import java.awt.FlowLayout;
import java.awt.Image;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;

import net.sf.freecol.client.gui.i18n.Messages;

import net.sf.freecol.client.gui.Canvas;

/**
* This panel gets displayed to the player who have won the game.
*/
public final class VictoryPanel extends JPanel implements ActionListener {
    private static final Logger logger = Logger.getLogger(VictoryPanel.class.getName());
    private static final int    OK = 0;

    private final Canvas    parent;
    private JButton         ok = new JButton(Messages.message("victory.yes"));




    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public VictoryPanel(Canvas parent) {
        super(new FlowLayout(FlowLayout.CENTER, 1000, 15));
        this.parent = parent;

        JLabel victoryLabel = new JLabel(Messages.message("victory.text"));
        victoryLabel.setFont(victoryLabel.getFont().deriveFont(0, 36));
        add(victoryLabel);

        Image tempImage = (Image) UIManager.get("VictoryImage");

        if (tempImage != null) {
            add(new JLabel(new ImageIcon(tempImage)));
        }

        add(ok);

        ok.setActionCommand(String.valueOf(OK));
        ok.addActionListener(this);

        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        } catch(Exception e) {}

        setSize(400, 400);
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
                    parent.reallyQuit();
                    break;
                default:
                    logger.warning("Invalid ActionCommand: invalid number.");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid Actioncommand: not a number.");
        }
    }
}
