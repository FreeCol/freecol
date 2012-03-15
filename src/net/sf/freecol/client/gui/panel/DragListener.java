/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;


/**
 * A DragListener should be attached to Swing components that have a
 * TransferHandler attached. The DragListener will make sure that the Swing
 * component to which it is attached is draggable (moveable to be precise).
 */
public final class DragListener extends MouseAdapter {

    private static final Logger logger = Logger.getLogger(DragListener.class.getName());

    private final FreeColPanel parentPanel;

    private FreeColClient freeColClient;

    private GUI gui;

    /**
     * The constructor to use.
     * @param freeColClient
     *
     * @param parentPanel The layered pane that contains the components to which
     *            a DragListener might be attached.
     */
    public DragListener(FreeColClient freeColClient, GUI gui, FreeColPanel parentPanel) {
        this.freeColClient = freeColClient;
        this.gui = gui;
        this.parentPanel = parentPanel;
    }

    /**
     * Gets called when the mouse was pressed on a Swing component that has this
     * object as a MouseListener.
     *
     * @param e The event that holds the information about the mouse click.
     */
    public void mousePressed(MouseEvent e) {
        JComponent comp = (JComponent) e.getSource();
        // Does not work on some platforms:
        // if (e.isPopupTrigger() && (comp instanceof UnitLabel)) {
        if ((e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger())) {
            // Popup mustn't be shown when panel is not editable
            if (parentPanel.isEditable()) {
                QuickActionMenu menu = null;
                if (comp instanceof UnitLabel) {
                    menu = new QuickActionMenu(freeColClient, gui, parentPanel);
                    menu.createUnitMenu((UnitLabel) comp);
                } else if (comp instanceof GoodsLabel) {
                    menu = new QuickActionMenu(freeColClient, gui, parentPanel);
                    menu.createGoodsMenu((GoodsLabel) comp);
                } else if (comp instanceof MarketLabel
                           && parentPanel instanceof EuropePanel) {
                    GoodsType goodsType = ((MarketLabel) comp).getType();
                    if (freeColClient.getInGameController()
                        .payArrears(goodsType)) {
                        ((EuropePanel) parentPanel).revalidate();
                        ((EuropePanel) parentPanel).refresh();
                    }
                }
                if (menu != null) {
                    int elements = menu.getSubElements().length;
                    if (elements > 0) {
                        int lastIndex = menu.getComponentCount() - 1;
                        if (menu.getComponent(lastIndex) instanceof JPopupMenu.Separator) {
                            menu.remove(lastIndex);
                        }
                        if (gui.isWindowed() && System.getProperty("os.name").startsWith("Windows")) {
                            // work-around: JRE on Windows is unable
                            // to display popup menus that extend
                            // beyond the canvas
                            menu.show(gui.getCanvas(), menu.getLocation().x, 0);
                        } else if(!gui.isWindowed()
                                  && Toolkit.getDefaultToolkit()
                                  .getScreenSize().getHeight() < 768) {
                            /*
                             * Move popup up when in full screen mode
                             * and when the screen size is too small
                             * to fit. Similar to above workaround,
                             * but targeted for users with smaller
                             * screens such as netbooks
                             */
                            menu.show(gui.getCanvas(), menu.getLocation().x, 0);
                        } else {
                            menu.show(comp, e.getX(), e.getY());
                        }
                    }
                }
            }
        } else {
            TransferHandler handler = comp.getTransferHandler();

            if (e.isShiftDown()) {
                if (comp instanceof GoodsLabel) {
                    ((GoodsLabel) comp).setPartialChosen(true);
                } else if (comp instanceof MarketLabel) {
                    ((MarketLabel) comp).setPartialChosen(true);
                }
            } else if(e.isAltDown()){
                if (comp instanceof GoodsLabel) {
                    ((GoodsLabel) comp).toEquip(true);
                } else if (comp instanceof MarketLabel) {
                    ((MarketLabel) comp).toEquip(true);
                }
            } else {
                if (comp instanceof GoodsLabel) {
                    ((GoodsLabel) comp).setPartialChosen(false);
                } else if (comp instanceof MarketLabel) {
                    ((MarketLabel) comp).setPartialChosen(false);
                    ((MarketLabel) comp).setAmount(GoodsContainer.CARGO_SIZE);
                }
            }

            if ((comp instanceof UnitLabel) && (((UnitLabel) comp).getUnit().isCarrier())) {
                Unit u = ((UnitLabel) comp).getUnit();
                if (parentPanel instanceof EuropePanel) {
                    if (!u.isAtSea()) {
                        ((EuropePanel) parentPanel).setSelectedUnitLabel((UnitLabel) comp);
                    }
                } else if (parentPanel instanceof ColonyPanel) {
                    ColonyPanel colonyPanel = (ColonyPanel) parentPanel;
                    if(colonyPanel.getSelectedUnit() != u){
                        colonyPanel.setSelectedUnit(u);
                        colonyPanel.updateInPortPanel();
                    }
                }
            }

            if (handler != null) {
                handler.exportAsDrag(comp, e, TransferHandler.COPY);
            }
        }
    }


}