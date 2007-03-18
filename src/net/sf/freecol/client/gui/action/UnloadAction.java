

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;


/**
* An action for unloading the currently selected unit.
*/
public class UnloadAction extends MapboardAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(UnloadAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final String ID = "unloadAction";

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    UnloadAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.orders.unload", null, KeyStroke.getKeyStroke('U', 0));
    }

    /**
    * Returns the id of this <code>Option</code>.
    * @return 
    */
    public String getId() {
        return ID;
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if there is a carrier active
     */
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) return false;
        GUI gui = getFreeColClient().getGUI();
        if (gui == null) return false;
    
        Unit unit = getFreeColClient().getGUI().getActiveUnit();
        return (unit != null && unit.isCarrier() && unit.canUnload());
    }    
    
    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        Unit unit = getFreeColClient().getGUI().getActiveUnit();
        if (unit != null) {
            Iterator<Goods> goodsIterator = unit.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = goodsIterator.next();
                getFreeColClient().getInGameController().unloadCargo(goods);
            }
            Iterator<Unit> unitIterator = unit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit newUnit = unitIterator.next();
                getFreeColClient().getInGameController().leaveShip(newUnit);
            }
        }
    }
}
