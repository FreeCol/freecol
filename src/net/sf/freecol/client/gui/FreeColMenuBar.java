package net.sf.freecol.client.gui;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.action.SelectableAction;
import net.sf.freecol.client.gui.panel.FreeColImageBorder;

/**
 * The menu bar that is displayed on the top left corner of the
 * <code>Canvas</code>.
 * 
 * @see Canvas#setJMenuBar
 */
public abstract class FreeColMenuBar extends JMenuBar {
    private static final Logger logger = Logger.getLogger(FreeColMenuBar.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

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

    protected final FreeColImageBorder outerBorder;

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

        Image menuborderN = (Image) UIManager.get("menuborder.n.image");
        Image menuborderNW = (Image) UIManager.get("menuborder.nw.image");
        Image menuborderNE = (Image) UIManager.get("menuborder.ne.image");
        Image menuborderW = (Image) UIManager.get("menuborder.w.image");
        Image menuborderE = (Image) UIManager.get("menuborder.e.image");
        Image menuborderS = (Image) UIManager.get("menuborder.s.image");
        Image menuborderSW = (Image) UIManager.get("menuborder.sw.image");
        Image menuborderSE = (Image) UIManager.get("menuborder.se.image");
        Image menuborderShadowSW = (Image) UIManager.get("menuborder.shadow.sw.image");
        Image menuborderShadowS = (Image) UIManager.get("menuborder.shadow.s.image");
        Image menuborderShadowSE = (Image) UIManager.get("menuborder.shadow.se.image");
        final FreeColImageBorder innerBorder = new FreeColImageBorder(menuborderN, menuborderW, menuborderS,
                menuborderE, menuborderNW, menuborderNE, menuborderSW, menuborderSE);
        outerBorder = new FreeColImageBorder(null, null, menuborderShadowS, null, null, null, menuborderShadowSW,
                menuborderShadowSE);
        setBorder(new CompoundBorder(outerBorder, innerBorder));
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
     * Updates this <code>FreeColMenuBar</code>.
     */
    public void update() {

    }

    /**
     * Returns the opaque height of this menubar.
     * 
     * @return The height of this menubar including all the borders except the
     *         ones being transparent.
     */
    public int getOpaqueHeight() {
        return getHeight() - outerBorder.getBorderInsets(this).bottom;
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

            Image tempImage = (Image) UIManager.get("BackgroundImage");

            final Shape originalClip = g.getClip();
            g.setClip(insets.left, insets.top, width, height);
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
            g.setClip(originalClip);
        }
    }    
}
