/*
 *  MainPanel.java - The main panel (new game, load game, settings, quit, ...).
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

import net.sf.freecol.client.gui.Canvas;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.UIManager;

/**
* A panel filled with 'main' items.
*/
public final class MainPanel extends JPanel implements ActionListener {
    private static final Logger logger = Logger.getLogger(MainPanel.class.getName());
    
    public static final int     NEW = 0,
                                OPEN = 1,
                                QUIT = 2;
    
    private final Canvas parent;
    private JButton newButton;
    

    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public MainPanel(Canvas parent) {
        setLayout(new BorderLayout());

        this.parent = parent;

        JButton         openButton = new JButton("Open"),
                        quitButton = new JButton("Quit");
        
        newButton = new JButton("New");
        
        newButton.setActionCommand(String.valueOf(NEW));
        openButton.setActionCommand(String.valueOf(OPEN));
        quitButton.setActionCommand(String.valueOf(QUIT));

        newButton.addActionListener(this);
        openButton.addActionListener(this);
        quitButton.addActionListener(this);

        ImageIcon tempImage = (ImageIcon) UIManager.get("TitleImage");

        if (tempImage != null) {
            add(new JLabel(tempImage), BorderLayout.CENTER);
        }

        JPanel buttons = new JPanel(new GridLayout(3, 1, 50, 5));

        buttons.add(newButton);
        buttons.add(openButton);
        buttons.add(quitButton);

        buttons.setBorder(new EmptyBorder(5, 25, 20, 25));        
        buttons.setOpaque(false);

        add(buttons, BorderLayout.SOUTH);

        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        } catch(Exception e) {
            logger.warning("EXCEPTION: " + e);
        }

        //setSize(100, 115);
        setSize(getPreferredSize());
    }

    public void requestFocus() {
        newButton.requestFocus();
    }


    /**
    * Sets whether or not this component is enabled. It also does this for
    * its children.
    * @param enabled 'true' if this component and its children should be
    * enabled, 'false' otherwise.
    */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Component components[] = getComponents();
        for (int i = 0; i < components.length; i++) {
            components[i].setEnabled(enabled);
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
                case NEW:
                    parent.showNewGamePanel();
                    parent.remove(this);
                    break;
                case OPEN:
                    parent.showOpenGamePanel();
                    break;
                case QUIT:
                    parent.quit();
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
