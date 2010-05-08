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
import java.util.ArrayList;


import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.MapControls;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Unit;

/**
 * An action for unloading the currently selected unit.
 */
public class UnloadAction extends UnitAction {

    public static final String id = "unloadAction";

    /**
     * Creates this action.
     * @param freeColClient The main controller object for the client.
     */
    public UnloadAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>true</code> if there is a carrier active
     */
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled()
            && getFreeColClient().getGUI().getActiveUnit().isCarrier()
            && getFreeColClient().getGUI().getActiveUnit().getGoodsCount() > 0;
    }    
    
    /**
     * Applies this action.
     * @param e The <code>ActionEvent</code>.
     */    
    public void actionPerformed(ActionEvent e) {
        Unit unit = getFreeColClient().getGUI().getActiveUnit();
        if (unit != null) {
            if (!unit.isInEurope() && unit.getColony() == null) {
                if (getFreeColClient().getCanvas().showConfirmDialog(unit.getTile(), "dumpAllCargo", "yes", "no")) {
                    unloadAllCargo(unit);
                    MapControls controls = ((MapControlsAction) getFreeColClient().getActionManager()
                                            .getFreeColAction(MapControlsAction.id)).getMapControls();
                    if (controls != null) {
                        controls.update();
                    }
                }
            } else {
                unloadAllCargo(unit);
                unloadAllUnits(unit);
                MapControls controls = ((MapControlsAction) getFreeColClient().getActionManager()
                                        .getFreeColAction(MapControlsAction.id)).getMapControls();
                if (controls != null) {
                    controls.update();
                }
            }
        }
    }

    /**
     * Unload all units on a carrier.
     *
     * @param carrier A <code>Unit</code> to unload units off.
     */
    private void unloadAllUnits(Unit carrier) {
        for (Unit unit : new ArrayList<Unit>(carrier.getUnitList())) {
            getFreeColClient().getInGameController().leaveShip(unit);
        }
    }

    /**
     * Unload all goods on a carrier.
     *
     * @param carrier A <code>Unit</code> to unload goods off.
     */
    private void unloadAllCargo(Unit carrier) {
        Boolean dump = carrier.getColony() == null;
        for (Goods goods : new ArrayList<Goods>(carrier.getGoodsList())) {
            getFreeColClient().getInGameController().unloadCargo(goods, dump);
        }
    }

}
