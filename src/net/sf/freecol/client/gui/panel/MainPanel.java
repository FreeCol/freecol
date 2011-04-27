/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.resources.ResourceManager;

/**
* A panel filled with 'main' items.
*/
public final class MainPanel extends FreeColPanel implements ActionListener {

    private static final Logger logger = Logger.getLogger(MainPanel.class.getName());

    public static final int     NEW = 0,
                                OPEN = 1,
                                MAP_EDITOR = 2,
                                OPTIONS = 3,
                                QUIT = 4;

    private JButton newButton;


    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public MainPanel(Canvas parent) {
        super(parent, new BorderLayout());

        JButton openButton = new JButton( Messages.message("openAction.name") );
        JButton mapEditorButton = new JButton( Messages.message("mainPanel.editor") );
        JButton optionsButton = new JButton( Messages.message("clientOptions.name") );
        JButton quitButton = new JButton( Messages.message("quitAction.name") );

        setCancelComponent(quitButton);
        newButton = new JButton( Messages.message("newAction.name") );

        newButton.setActionCommand(String.valueOf(NEW));
        mapEditorButton.setActionCommand(String.valueOf(MAP_EDITOR));
        openButton.setActionCommand(String.valueOf(OPEN));
        optionsButton.setActionCommand(String.valueOf(OPTIONS));
        quitButton.setActionCommand(String.valueOf(QUIT));

        newButton.addActionListener(this);
        mapEditorButton.addActionListener(this);
        openButton.addActionListener(this);
        optionsButton.addActionListener(this);
        quitButton.addActionListener(this);

        enterPressesWhenFocused(newButton);
        enterPressesWhenFocused(mapEditorButton);
        enterPressesWhenFocused(openButton);
        enterPressesWhenFocused(optionsButton);
        enterPressesWhenFocused(quitButton);

        Image tempImage = ResourceManager.getImage("TitleImage");

        if (tempImage != null) {
            JLabel logoLabel = new JLabel(new ImageIcon(tempImage));
            logoLabel.setBorder(new CompoundBorder(new EmptyBorder(2,2,0,2), new BevelBorder(BevelBorder.LOWERED)));
            add(logoLabel, BorderLayout.CENTER);
        }

        JPanel buttons = new JPanel(new GridLayout(0, 1, 50, 10));

        buttons.add(newButton);
        buttons.add(openButton);
        buttons.add(mapEditorButton);
        buttons.add(optionsButton);
        buttons.add(quitButton);

        buttons.setBorder(new EmptyBorder(25, 25, 25, 25));
        buttons.setOpaque(false);

        add(buttons, BorderLayout.SOUTH);

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
        Component[] components = getComponents();
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
                    getCanvas().remove(this);
                    getCanvas().showPanel(new NewPanel(getCanvas()));
                    break;
                case OPEN:
                    getClient().getConnectController().loadGame();
                    break;
                case MAP_EDITOR:
                    getClient().getMapEditorController().startMapEditor();
                    break;
                case OPTIONS:
                    getCanvas().showClientOptionsDialog();
                    break;
                case QUIT:
                    getCanvas().quit();
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
