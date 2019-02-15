/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPopupMenu.Separator;
import javax.swing.TransferHandler;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.label.AbstractGoodsLabel;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.client.gui.panel.TradeRouteInputPanel.TradeRouteCargoLabel;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.OSUtils;


/**
 * A DragListener should be attached to Swing components that have a
 * TransferHandler attached.  The DragListener will make sure that the
 * Swing component to which it is attached is draggable (moveable to
 * be precise).
 */
public final class DragListener extends MouseAdapter {

    private static final Logger logger = Logger.getLogger(DragListener.class.getName());

    /**
     * The maximum numbers of pixels of the user's screen height
     * before triggering the small flag.
     */
    private static final int maxWindowHeight = 768;

    /** The user's screen height. */
    private static final int windowHeight
        = (int)Math.floor(Toolkit.getDefaultToolkit().getScreenSize().getHeight());

    /**
     * Whether the user's screen height is smaller than the maximum
     * height allowed
     */
    private static final boolean small = windowHeight < maxWindowHeight;

    /** Enable windows workaround. */
    private final boolean windows = OSUtils.onWindows();

    /** The enclosing client. */
    private final FreeColClient freeColClient;

    /** The panel within which the drag occurs. */
    private final FreeColPanel parentPanel;


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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
        final JComponent comp = (JComponent)e.getSource();

        // Special case for the menu.
        if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
            if (!this.parentPanel.isEditable()) return;

            final QuickActionMenu menu
                = new QuickActionMenu(this.freeColClient, this.parentPanel)
                    .addMenuItems(comp);
            final int lastIdx = menu.getComponentCount() - 1;
            if ((lastIdx >= 0)
                    && (menu.getComponent(lastIdx) instanceof Separator))
                menu.remove(lastIdx);
            if (menu.getComponentCount() <= 0) return;

            /*
            FIXME: Cleanup implementation
            Work-arounds:
            This moves the popup up when in full screen mode
            and when the screen size is too small to fit. JRE
            on Windows is unable to display popup menus that
            extend beyond the canvas. Targeted for users with
            smaller screens such as netbooks.
            */
            final GUI gui = this.freeColClient.getGUI();
            if ((gui.isWindowed() && windows)
                || (small && !gui.isWindowed())) {
                menu.show(gui.getCanvas(), menu.getLocation().x, 0);
            } else {
                menu.show(comp, e.getX(), e.getY());
            }
            return;
        }

        if (comp instanceof AbstractGoodsLabel) {
            AbstractGoodsLabel label = (AbstractGoodsLabel)comp;
            if (e.isShiftDown() && e.isAltDown()) {
                Component[] cArr = comp.getParent().getComponents();
                int sum = 0;
                for (int i = 0; i < cArr.length; i++) {
                    if (cArr[i] instanceof AbstractGoodsLabel) {
                        AbstractGoodsLabel abGoods = (AbstractGoodsLabel) cArr[i];
                        if (abGoods.getAbstractGoods().getType().equals(label.getAbstractGoods().getType())) {
                            sum += abGoods.getAmount();
                        }
                    }
                }
                label.setSuperFullChosen(true);
                label.setAmount(sum);
            } else if (e.isShiftDown()) {
                label.setSuperFullChosen(false);
                label.setPartialChosen(true);
            } else if (e.isControlDown()) {
                label.setSuperFullChosen(false);
                label.setFullChosen(true);
            } else {
                label.setSuperFullChosen(false);
                label.setPartialChosen(false);
                label.setDefaultAmount();
            }
        } else if (comp instanceof UnitLabel) {
            final UnitLabel label = (UnitLabel) comp;
            final Unit u = label.getUnit();
            if (u.isCarrier()
                && !u.isAtSea()
                && this.parentPanel instanceof PortPanel) {
                ((PortPanel)this.parentPanel).setSelectedUnitLabel(label);
            }
        } else if (comp instanceof TradeRouteCargoLabel) {
            ; // Do nothing, TradeRouteInputPanel handles this
        } else {
            System.err.println("DragListener did not recognize:" + comp);
        }

        final TransferHandler handler = comp.getTransferHandler();
        if (handler != null) {
            handler.exportAsDrag(comp, e, TransferHandler.COPY);
        }
    }
}
