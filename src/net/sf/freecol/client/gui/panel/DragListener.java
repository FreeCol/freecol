/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.common.model.Unit;


/**
 * A DragListener should be attached to Swing components that have a
 * TransferHandler attached.  The DragListener will make sure that the
 * Swing component to which it is attached is draggable (moveable to
 * be precise).
 */
public final class DragListener extends MouseAdapter {

    private static final Logger logger = Logger.getLogger(DragListener.class.getName());

    private final FreeColPanel parentPanel;

    private final FreeColClient freeColClient;


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param parentPanel The layered pane that contains the
     *     components to which a DragListener might be attached.
     */
    public DragListener(FreeColClient freeColClient,
                        FreeColPanel parentPanel) {
        this.freeColClient = freeColClient;
        this.parentPanel = parentPanel;
    }


    /**
     * Gets called when the mouse was pressed on a Swing component
     * that has this object as a MouseListener.
     *
     * @param e The event that holds the information about the mouse click.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        JComponent comp = (JComponent)e.getSource();
        // Does not work on some platforms:
        // if (e.isPopupTrigger() && (comp instanceof UnitLabel)) {

        if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
            if (!parentPanel.isEditable()) { // No panel when not editable
                logger.warning("Button3 disabled on non-editable panel: "
                    + parentPanel);
                return;
            }
            QuickActionMenu menu
                = new QuickActionMenu(freeColClient, parentPanel)
                .addMenuItems(comp);
            int lastIdx = menu.getComponentCount() - 1;
            if (lastIdx >= 0
                && menu.getComponent(lastIdx) instanceof JPopupMenu.Separator)
                menu.remove(lastIdx);
            if (menu.getComponentCount() <= 0) return;

            final SwingGUI gui = (SwingGUI)freeColClient.getGUI();
            boolean windows = System.getProperty("os.name").startsWith("Windows");
            boolean small = Toolkit.getDefaultToolkit()
                .getScreenSize().getHeight() < 768;
            if (gui.isWindowed() && windows) {
                // Work-around: JRE on Windows is unable to
                // display popup menus that extend beyond the canvas.
                menu.show(gui.getCanvas(), menu.getLocation().x, 0);
            } else if (!gui.isWindowed() && small) {
                // Move popup up when in full screen mode and when
                // the screen size is too small to fit.  Similar
                // to above workaround, but targeted for users
                // with smaller screens such as netbooks.
                menu.show(gui.getCanvas(), menu.getLocation().x, 0);
            } else {
                menu.show(comp, e.getX(), e.getY());
            }

        } else {
            if (comp instanceof AbstractGoodsLabel) {
                AbstractGoodsLabel label = (AbstractGoodsLabel)comp;
                if (e.isShiftDown()) {
                    label.setPartialChosen(true);
                } else if (e.isControlDown()) {
                    label.setFullChosen(true);
                } else {
                    label.setPartialChosen(false);
                    label.setDefaultAmount();
                }
            } else if (comp instanceof UnitLabel) {
                UnitLabel label = (UnitLabel)comp;
                Unit u = label.getUnit();
                if (u.isCarrier()
                    && !u.isAtSea()
                    && parentPanel instanceof PortPanel) {
                    ((PortPanel)parentPanel).setSelectedUnitLabel(label);
                }
            }

            TransferHandler handler = comp.getTransferHandler();
            if (handler != null) {
                handler.exportAsDrag(comp, e, TransferHandler.COPY);
            }
        }
    }
}
