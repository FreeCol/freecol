

package net.sf.freecol.client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.freecol.common.model.*;
import net.sf.freecol.client.FreeColClient;


/**
* Allows the user to obtain more info about a certain tile
* or to activate a specific unit on the tile.
*/
public final class TilePopup extends JPopupMenu implements ActionListener {
    private static final Logger logger = Logger.getLogger(TilePopup.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private final Tile tile;
    private final FreeColClient freeColClient;
    private final Canvas canvas;
    private final GUI gui;
    private boolean hasAnItem = false;






    /**
    * The constructor that will insert the MenuItems.
    * @param tile The tile at which the popup must appear.
    */
    public TilePopup(Tile tile, FreeColClient freeColClient, Canvas canvas, GUI gui) {
        super("Tile (" + tile.getX() + ", " + tile.getY() + ")");

        this.tile = tile;
        this.freeColClient = freeColClient;
        this.canvas = canvas;
        this.gui = gui;


        Iterator unitIterator = tile.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = (Unit) unitIterator.next();
            if (!u.isUnderRepair()) {
                addUnit(u);
            }

            Iterator childUnitIterator = u.getUnitIterator();
            while (childUnitIterator.hasNext()) {
                addUnit((Unit) childUnitIterator.next());
            }
        }

        Settlement settlement = tile.getSettlement();
        if (settlement != null) {
            if (settlement.getOwner() == freeColClient.getMyPlayer()) {
                addColony(((Colony) settlement));
            }
            else if (settlement instanceof IndianSettlement) {
                addIndianSettlement((IndianSettlement) settlement);
            }
        }
    }





    /**
    * Adds a unit entry to this popup.
    * @param unit The unit that will be represented on the popup.
    */
    private void addUnit(Unit unit) {
        JMenuItem menuItem = new JMenuItem(unit.toString());
        menuItem.setActionCommand("unit " + unit.getID());
        menuItem.addActionListener(this);
        add(menuItem);
        hasAnItem = true;
    }


    /**
    * Adds a colony entry to this popup.
    * @param colony The colony that will be represented on the popup.
    */
    private void addColony(Colony colony) {
        JMenuItem menuItem = new JMenuItem(colony.toString());
        menuItem.setActionCommand("colony");
        menuItem.addActionListener(this);
        add(menuItem);
        hasAnItem = true;
    }


    /**
    * Adds an indian settlement entry to this popup.
    * @param settlement The Indian settlement that will be represented on the popup.
    */
    private void addIndianSettlement(IndianSettlement settlement) {
        JMenuItem menuItem = new JMenuItem(
                settlement.getOwner().getNationAsString() + " settlement");
        menuItem.setActionCommand("indianSettlement");
        menuItem.addActionListener(this);
        add(menuItem);
        hasAnItem = true;
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
        if (command.startsWith("unit ")) {
            String unitId = null;

            try {
                unitId = command.substring(5, command.length());
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }

            gui.setActiveUnit((Unit) freeColClient.getGame().getFreeColGameObject(unitId));
        } else if (command == "colony") {
            canvas.showColonyPanel((Colony) tile.getSettlement());
        } else if (command == "indianSettlement") {
            canvas.showIndianSettlementPanel((IndianSettlement) tile.getSettlement());
        } else {
            logger.warning("Invalid actioncommand.");
        }
    }
}
