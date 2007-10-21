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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.mission.TransportMission;


/**
* Allows the user to obtain more info about a certain tile
* or to activate a specific unit on the tile.
*/
public final class TilePopup extends JPopupMenu implements ActionListener {
    private static final Logger logger = Logger.getLogger(TilePopup.class.getName());


    private final Tile tile;
    private final FreeColClient freeColClient;
    private final Canvas canvas;
    private final GUI gui;
    private boolean hasAnItem = false;






    /**
    * The constructor that will insert the MenuItems.
    * 
    * @param tile The <code>Tile</code> to create a popup for.
    *       The popup menu also appears near this <code>Tile</code>.
    * @param freeColClient The main controller object for the client.
    * @param canvas The component containing the map.
    * @param gui An object with methods used for making the popup.
    */
    public TilePopup(Tile tile, FreeColClient freeColClient, Canvas canvas, GUI gui) {
        super("Tile (" + tile.getX() + ", " + tile.getY() + ")");

        this.tile = tile;
        this.freeColClient = freeColClient;
        this.canvas = canvas;
        this.gui = gui;

        if (gui.getActiveUnit() != null) {
            //final Image gotoImage = (Image) UIManager.get("cursor.go.image");
            //JMenuItem gotoMenuItem = new JMenuItem(Messages.message("gotoThisTile"), new ImageIcon(gotoImage));
            JMenuItem gotoMenuItem = new JMenuItem(Messages.message("gotoThisTile"));
            gotoMenuItem.setActionCommand("GOTO" + tile.getId());
            gotoMenuItem.addActionListener(this);
            add(gotoMenuItem);
            hasAnItem = true;
            addSeparator();
        }

        Iterator<Unit> unitIterator = tile.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = unitIterator.next();

            addUnit(u, !u.isUnderRepair(), false);

            Iterator<Unit> childUnitIterator = u.getUnitIterator();
            while (childUnitIterator.hasNext()) {
                addUnit(childUnitIterator.next(), true, true);
            }
            
            Iterator<Goods> goodsIterator = u.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                addGoods(goodsIterator.next(), false, true);
            }
        }
        
        if (tile.getUnitCount() > 0) {
            addSeparator();
        }

        Settlement settlement = tile.getSettlement();
        if (settlement != null) {
            if (settlement.getOwner() == freeColClient.getMyPlayer()) {
                addColony(((Colony) settlement));
            } else if (settlement instanceof IndianSettlement) {
                addIndianSettlement((IndianSettlement) settlement);
            }
            if (hasItem()) {
                addSeparator();
            }          
        }        

        addTile(tile);
        
        // START DEBUG
        if (FreeCol.isInDebugMode() 
                && freeColClient.getFreeColServer() != null) {
            addSeparator();
            JMenu takeOwnership = new JMenu("Take ownership");
            takeOwnership.setOpaque(false);
            boolean notEmpty = false;
            Iterator<Unit> it = tile.getUnitIterator();
            while (it.hasNext()) {
                Unit u = it.next();
                JMenuItem toMenuItem = new JMenuItem(u.toString());
                toMenuItem.setActionCommand("TO" + u.getId());
                toMenuItem.addActionListener(this);
                takeOwnership.add(toMenuItem);
                notEmpty = true;
                if (u.isCarrier()) {
                    AIUnit au = (AIUnit) freeColClient.getFreeColServer().getAIMain().getAIObject(u);                
                    if (au.getMission() != null && au.getMission() instanceof TransportMission) {
                        JMenuItem menuItem = new JMenuItem("Transport list for: " + u.toString());
                        menuItem.setActionCommand("TL" + Unit.getXMLElementTagName() + u.getId());
                        menuItem.addActionListener(this);
                        add(menuItem);
                    }
                }
            }
            if (tile.getSettlement() != null) {
                if (!notEmpty) {
                    takeOwnership.addSeparator();
                }
                JMenuItem toMenuItem = new JMenuItem(tile.getSettlement().toString());
                toMenuItem.setActionCommand("TO" + tile.getSettlement().getId());
                toMenuItem.addActionListener(this);
                takeOwnership.add(toMenuItem);
                notEmpty = true;
            }
            if (notEmpty) {
                add(takeOwnership);
                hasAnItem = true;
            }
        }
        // END DEBUG
    }

    /**
     * Adds a unit entry to this popup.
     * @param unit The unit that will be represented on the popup.
     * @param enabled The initial state for the menu item.
     * @param indent Should be <code>true</code> if the text should be
     *      indented on the menu.
     */
    private void addUnit(Unit unit, boolean enabled, boolean indent) {
        String text;
        if(unit.getState() == Unit.IMPROVING) {
            text = ((indent ? "    " : "") + 
                    Messages.message("model.unit.nationUnit", 
                            "%nation%", unit.getOwner().getNationAsString(),
                            "%unit%", unit.getName()) +
                            " ( " + unit.getOccupationIndicator() + ": " + unit.getWorkLeft() +  " turns )");

        } else {
            text = ((indent ? "    " : "") + 
                    Messages.message("model.unit.nationUnit", 
                            "%nation%", unit.getOwner().getNationAsString(),
                            "%unit%", unit.getName()) +
                            " ( " + unit.getOccupationIndicator() + " )");
        }
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setActionCommand(Unit.getXMLElementTagName() + unit.getId());
        menuItem.addActionListener(this);
        if (indent) {
            menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
        }
        if (!enabled) {
            menuItem.setEnabled(false);
        }
        add(menuItem);
        hasAnItem = true;
    }

    /**
     * Adds a goods entry to this popup.
     * @param goods The goods that will be represented on the popup.
     * @param enabled The initial state for the menu item.
     * @param indent Should be <code>true</code> if the text should be
     *      indented on the menu.
     */
    private void addGoods(Goods goods, boolean enabled, boolean indent) {
        String text = (indent ? "    " : "") + goods.toString();
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setActionCommand(Goods.getXMLElementTagName());
        menuItem.addActionListener(this);
        if (indent) {
            menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC));
        }
        if (!enabled) {
            menuItem.setEnabled(false);
        }
        add(menuItem);
        hasAnItem = true;
    }

    /**
    * Adds a colony entry to this popup.
    * @param colony The colony that will be represented on the popup.
    */
    private void addColony(Colony colony) {
        JMenuItem menuItem = new JMenuItem(colony.toString());
        menuItem.setActionCommand(Colony.getXMLElementTagName());
        menuItem.addActionListener(this);
        add(menuItem);
        hasAnItem = true;
    }


    /**
    * Adds an indian settlement entry to this popup.
    * @param settlement The Indian settlement that will be represented on the popup.
    */
    private void addIndianSettlement(IndianSettlement settlement) {
        JMenuItem menuItem = new JMenuItem(settlement.getLocationName());
        menuItem.setActionCommand(IndianSettlement.getXMLElementTagName());
        menuItem.addActionListener(this);
        add(menuItem);
        hasAnItem = true;
    }

    /**
     * Adds a tile entry to this popup.
     * @param tile The tile that will be represented on the popup.
     */
    private void addTile(Tile tile) {
        JMenuItem menuItem = new JMenuItem(tile.getName());
        menuItem.setActionCommand(Tile.getXMLElementTagName());
        menuItem.addActionListener(this);
        add(menuItem);
        /**
         * Don't set hasAnItem to true, we want the tile panel to open
         * automatically whenever there is no other item on the list.
         */        
        // hasAnItem = true;
    }    

    /**
    * Returns true if this popup has at least one menuitem so that we know that we can
    * show it to the user. Returns false if there are no menuitems.
    * @return true if this popup has at least one menuitem, false otherwise.
    */
    public boolean hasItem() {
        return hasAnItem;
    }


    /**
    * This function analyses an event and calls the right methods to take
    * care of the user's requests.
    * @param event The incoming ActionEvent.
    */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.startsWith(Unit.getXMLElementTagName())) {
            String unitId = null;

            try {
                unitId = command.substring(Unit.getXMLElementTagName().length());
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }

            gui.setActiveUnit((Unit) freeColClient.getGame().getFreeColGameObject(unitId));
        } else if (command.equals(Colony.getXMLElementTagName())) {
            canvas.showColonyPanel((Colony) tile.getSettlement());
        } else if (command.equals(IndianSettlement.getXMLElementTagName())) {
            canvas.showIndianSettlementPanel((IndianSettlement) tile.getSettlement());
        } else if (command.equals(Tile.getXMLElementTagName())) {
            canvas.showTilePanel(tile);
            // START DEBUG
        } else if (command.startsWith("TL" + Unit.getXMLElementTagName())) {
            String unitID = command.substring(("TL"+Unit.getXMLElementTagName()).length());
            AIUnit au = (AIUnit) freeColClient.getFreeColServer().getAIMain().getAIObject(unitID);
            canvas.showInformationMessage(au.getMission().toString());
        } else if (command.startsWith("TO")) {
            String id = command.substring(("TO").length());
            Ownable o = (Ownable) freeColClient.getFreeColServer().getGame().getFreeColGameObject(id);
            Player mp = (Player) freeColClient.getFreeColServer().getGame().getFreeColGameObject(freeColClient.getMyPlayer().getId());
            o.setOwner(mp);
            if (o instanceof Unit) {
                Iterator<Unit> it = ((Unit) o).getUnitIterator();
                while (it.hasNext()) {
                    it.next().setOwner(mp);
                }
            }
            if (o instanceof Location) {
                freeColClient.getFreeColServer().getModelController().update(((Location) o).getTile());
            } else if (o instanceof Locatable) {
                freeColClient.getFreeColServer().getModelController().update(((Locatable) o).getTile());
            }
            // END DEBUG
        } else if (command.startsWith("GOTO")) {
            String tileID = command.substring(("GOTO").length());
            Tile gotoTile = (Tile) freeColClient.getGame().getFreeColGameObject(tileID);
            if (gotoTile != null && gui.getActiveUnit() != null) {
                freeColClient.getInGameController().setDestination(gui.getActiveUnit(), gotoTile);
                if (freeColClient.getGame().getCurrentPlayer() == freeColClient.getMyPlayer()) {
                    freeColClient.getInGameController().moveToDestination(gui.getActiveUnit());
                }
            }
        } else {
            logger.warning("Invalid actioncommand.");
        }
    }
}
