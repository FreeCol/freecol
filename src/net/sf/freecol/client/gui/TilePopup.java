

package net.sf.freecol.client.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import java.util.Iterator;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.sf.freecol.common.model.*;
import net.sf.freecol.client.control.*;
import net.sf.freecol.client.FreeColClient;


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
            addUnit(u);
            
            Iterator childUnitIterator = u.getUnitIterator();
            while (childUnitIterator.hasNext()) {
                addUnit((Unit) childUnitIterator.next());
            }
        }

        if (tile.getSettlement() != null && (tile.getSettlement().getOwner() == freeColClient.getMyPlayer())) {
            addColony(((Colony) tile.getSettlement()));
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
        } else {
            logger.warning("Invalid actioncommand.");
        }
    }
}
