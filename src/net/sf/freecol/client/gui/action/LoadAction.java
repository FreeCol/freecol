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

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;


/**
 * An action for filling the holds of the currently selected unit.
 */
public class LoadAction extends MapboardAction {

    public static final String id = "loadAction";


    /**
     * Creates this action.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public LoadAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        final Unit carrier = getGUI().getActiveUnit();
        return super.shouldBeEnabled()
            && carrier != null
            && carrier.isCarrier()
            && carrier.hasSpaceLeft();
    }    


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final Unit unit = getGUI().getActiveUnit();
        if (unit == null) return;

        final Colony colony = unit.getColony();
        if (colony == null) return;

        for (Goods goods : unit.getCompactGoodsList()) {
            final GoodsType type = goods.getType();
            int loadable = unit.getLoadableAmount(type);
            int present = colony.getGoodsCount(type);
            if (loadable > 0 && present > 0) {
                int n = Math.min(Math.min(loadable, present),
                                          GoodsContainer.CARGO_SIZE);
                igc().loadCargo(new Goods(goods.getGame(), colony, type, n),
                                unit);
            }
        }
    }
}
