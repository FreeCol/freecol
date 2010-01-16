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

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;

/**
 * An action for unloading the currently selected unit.
 */
public class UnloadAction extends MapboardAction {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(UnloadAction.class.getName());

    public static final String id = "unloadAction";

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    public UnloadAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.orders.unload", null, KeyStroke.getKeyStroke('U', 0));
    }

    /**
    * Returns the id of this <code>Option</code>.
    * @return "unloadAction"
    */
    public String getId() {
        return id;
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if there is a carrier active
     */
    protected boolean shouldBeEnabled() {
        if (super.shouldBeEnabled()) {
            GUI gui = getFreeColClient().getGUI();
            if (gui != null) {
                Unit unit = getFreeColClient().getGUI().getActiveUnit();
                return (unit != null && unit.isCarrier() && unit.getGoodsCount() > 0);
            }
        }
        return false;
    }    
    
    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        Unit unit = getFreeColClient().getGUI().getActiveUnit();
        if (unit != null) {
            if (!unit.isInEurope() && unit.getColony() == null) {
                if (getFreeColClient().getCanvas().showConfirmDialog("dumpAllCargo", "yes", "no")) {
                    unloadAllCargo(unit);
                    MapControls controls = ((MapControlsAction) getFreeColClient().getActionManager().getFreeColAction(MapControlsAction.id)).getMapControls();
                    if (controls != null) {
                        controls.update();
                    }
                }
            } else {
                unloadAllCargo(unit);
                unloadAllUnits(unit);
                MapControls controls = ((MapControlsAction) getFreeColClient().getActionManager().getFreeColAction(MapControlsAction.id)).getMapControls();
                if (controls != null) {
                    controls.update();
                }
            }
        }
    }

    private void unloadAllUnits(Unit unit) {
        Iterator<Unit> unitIterator = unit.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit newUnit = unitIterator.next();
            getFreeColClient().getInGameController().leaveShip(newUnit);
        }
    }

    private void unloadAllCargo(Unit unit) {
        Iterator<Goods> goodsIterator = unit.getGoodsIterator();
        while (goodsIterator.hasNext()) {
            Goods goods = goodsIterator.next();
            Boolean dump = unit.getColony() == null;
            getFreeColClient().getInGameController().unloadCargo(goods, dump);
        }
    }

}
