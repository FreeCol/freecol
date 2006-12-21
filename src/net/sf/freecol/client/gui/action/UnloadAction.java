

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Goods;


/**
* An action for unloading the currently selected unit.
*/
public class UnloadAction extends MapboardAction {
    private static final Logger logger = Logger.getLogger(UnloadAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "unloadAction";


    /**
     * Creates a new <code>UnloadAction</code>.
     * @param freeColClient The main controller object for the client.
     */
    UnloadAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.orders.unload", null, KeyEvent.VK_U, KeyStroke.getKeyStroke('U', 0));
    }
    

    /**
    * Returns the id of this <code>Option</code>.
    * @return "unloadAction"
    */
    public String getId() {
        return ID;
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if the player has access to Europe.
     */
    protected boolean shouldBeEnabled() { 
        Unit unit = getFreeColClient().getGUI().getActiveUnit();
        return (unit != null && unit.isCarrier());
    }    
    
    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        Unit unit = getFreeColClient().getGUI().getActiveUnit();
        if (unit != null) {
            Iterator goodsIterator = unit.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = (Goods) goodsIterator.next();
                getFreeColClient().getInGameController().unloadCargo(goods);
            }
            Iterator unitIterator = unit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit newUnit = (Unit) unitIterator.next();
                getFreeColClient().getInGameController().leaveShip(newUnit);
            }
        }
    }
}
