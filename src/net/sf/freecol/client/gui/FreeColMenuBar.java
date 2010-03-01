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

package net.sf.freecol.client.gui;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.action.SelectableAction;
import net.sf.freecol.client.gui.panel.FreeColImageBorder;
import net.sf.freecol.common.resources.ResourceManager;

/**
 * The menu bar that is displayed on the top left corner of the
 * <code>Canvas</code>.
 */
public abstract class FreeColMenuBar extends JMenuBar {

    private static final Logger logger = Logger.getLogger(FreeColMenuBar.class.getName());

    public static final int UNIT_ORDER_WAIT = 0;

    public static final int UNIT_ORDER_FORTIFY = 1;

    public static final int UNIT_ORDER_SENTRY = 2;

    public static final int UNIT_ORDER_CLEAR_ORDERS = 3;

    public static final int UNIT_ORDER_BUILD_COL = 5;

    public static final int UNIT_ORDER_PLOW = 6;

    public static final int UNIT_ORDER_BUILD_ROAD = 7;

    public static final int UNIT_ORDER_SKIP = 9;

    public static final int UNIT_ORDER_DISBAND = 11;

    protected final FreeColClient freeColClient;

    JMenuItem reportsTradeMenuItem = null;

    protected ActionManager am;


    /**
     * Creates a new <code>FreeColMenuBar</code>. This menu bar will include
     * all of the submenus and items.
     * 
     * @param f The main controller.
     */
    protected FreeColMenuBar(FreeColClient f) {

        // TODO: FreeColClient should not have to be passed in to this class.
        // This is only a menu bar, it doesn't need
        // a reference to the main controller. The only reason it has one now is
        // because DebugMenu needs it. And DebugMenu
        // needs it because it is using inner classes for ActionListeners and
        // those inner classes use the reference.
        // If those inner classes were in seperate classes, when they were
        // created, they could use the FreeColClient
        // reference of the ActionManger. So DebugMenu needs to be refactored to
        // remove inner classes so that this
        // MenuBar can lose its unnecessary reference to the main controller.
        // See FreeColMenuTest.
        //
        // Okay, I lied.. the update() and paintComponent() methods in this
        // MenuBar use freeColClient, too. But so what.
        // Move those to another class too. :)

        super();

        setOpaque(false);

        this.freeColClient = f;

        this.am = f.getActionManager();

        setBorder(FreeColImageBorder.imageBorder);
    }

    
    /**
     * Resets this menu bar.
     * 
     * <br><br>
     * <b>For subclasses:</b>
     * This method should reset both the texts and
     * the accelerator keys used by the menu items.
     */
    public abstract void reset();
    
    /**
     * Creates a default FreeCol JMenuItem.
     * 
     * @param actionID The ID given to the
     *      {@link ActionManager#getFreeColAction(String) action manager}.
     * @return The menu item.
     */
    protected JMenuItem getMenuItem(String actionID) {
        JMenuItem rtn = null;

        FreeColAction action = am.getFreeColAction(actionID);

        if (action != null) {
            rtn = new JMenuItem();
            rtn.setAction(action);
            rtn.setOpaque(false);

            if (action.getMnemonic() != FreeColAction.NO_MNEMONIC) {
                rtn.addMenuKeyListener(action.getMenuKeyListener());
            }
        } else {
            logger.finest("Could not create menu item. [" + actionID + "] not found.");
        }
        return rtn;
    }

    /**
     * Creates a default FreeCol JMenuItem.
     * 
     * @param actionID The ID given to the
     *      {@link ActionManager#getFreeColAction(String) action manager}.
     * @param actionListener An <code>ActionListener</code> that will be
     *      added to the menu item.
     * @return The menu item with the <code>ActionListener</code> added.
     */
    protected JMenuItem getMenuItem(String actionID, ActionListener actionListener) {
        JMenuItem rtn = getMenuItem(actionID);

        rtn.addActionListener(actionListener);

        return rtn;
    }

    /**
     * Creates a default FreeCol <code>JCheckBoxMenuItem</code>.
     * 
     * @param actionID The ID given to the
     *      {@link ActionManager#getFreeColAction(String) action manager}.
     * @return The menu item.
     */
    protected JCheckBoxMenuItem getCheckBoxMenuItem(String actionID) {

        JCheckBoxMenuItem rtn = null;
        FreeColAction action = am.getFreeColAction(actionID);

        if (action != null) {
            rtn = new JCheckBoxMenuItem();
            rtn.setAction(action);
            rtn.setOpaque(false);

            rtn.setSelected(((SelectableAction) am.getFreeColAction(actionID)).isSelected());
        } else
            logger.finest("Could not create menu item. [" + actionID + "] not found.");

        return rtn;
    }

    /**
     * Creates a default FreeCol <code>JRadioButtonMenuItem</code>.
     * 
     * @param actionID The ID given to the
     *      {@link ActionManager#getFreeColAction(String) action manager}.
     * @param group The <code>ButtonGroup</code> to add this item to
     * @return The menu item.
     */
    protected JRadioButtonMenuItem getRadioButtonMenuItem(String actionID,
                                                          ButtonGroup group) {
        JRadioButtonMenuItem rtn = null;
        FreeColAction action = am.getFreeColAction(actionID);

        if (action != null) {
            rtn = new JRadioButtonMenuItem();
            rtn.setAction(action);
            rtn.setOpaque(false);

            rtn.setSelected(((SelectableAction) am.getFreeColAction(actionID)).isSelected());
            group.add(rtn);
        } else {
            logger.finest("Could not create menu item. [" + actionID + "] not found.");
        }
        return rtn;
    }


    /**
     * Updates this <code>FreeColMenuBar</code>.
     */
    public void update() {
        repaint();
    }

    /**
     * When a <code>FreeColMenuBar</code> is disabled, it does not show the
     * "in game options".
     */
    public void setEnabled(boolean enabled) {
        // Not implemented (and possibly not needed).

        update();
    }

    /**
     * Paints the background and borders of the menubar.
     */
    public void paintComponent(Graphics g) {
        if (isOpaque()) {
            super.paintComponent(g);
        } else {
            Insets insets = getInsets();
            int width = getWidth() - insets.left - insets.right;
            int height = getHeight() - insets.top - insets.bottom;

            Image tempImage = ResourceManager.getImage("BackgroundImage");

            if (tempImage != null) {
                for (int x = 0; x < width; x += tempImage.getWidth(null)) {
                    for (int y = 0; y < height; y += tempImage.getHeight(null)) {
                        g.drawImage(tempImage, insets.left + x, insets.top + y, null);
                    }
                }
            } else {
                g.setColor(getBackground());
                g.fillRect(insets.left, insets.top, width, height);
            }
        }
    }    
}
