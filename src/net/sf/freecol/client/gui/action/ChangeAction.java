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
import java.util.Iterator;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * An action for changing the view. This action will:
 *
 *  - Open a colony panel if the active unit is located on a tile with
 *    a colony.
 *  - If aboard a carrier then the carrier will be the active unit.
 *  - In other cases: switch to another unit on the same tile.
 */
public class ChangeAction extends UnitAction {

    public static final String id = "changeAction";


    /**
     * Creates this action.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ChangeAction(FreeColClient freeColClient) {
        super(freeColClient, id);

        update();
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled() && getGUI().getActiveUnit().hasTile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void update() {
        super.update();

        final Unit unit = getGUI().getActiveUnit();
        if (unit != null && unit.hasTile()) {
            if (unit.getColony() != null) {
                putValue(NAME, Messages.getName("changeAction.enterColony"));
            } else if (unit.isOnCarrier()) {
                putValue(NAME, Messages.getName("changeAction.selectCarrier"));
            } else {
                putValue(NAME, Messages.getName("changeAction.nextUnitOnTile"));
            }
        }
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final Unit unit = getGUI().getActiveUnit();
        final Tile tile = unit.getTile();

        if (tile.getColony() != null) {
            getGUI().showColonyPanel(tile.getColony(), unit);
        } else if (unit.isOnCarrier()) {
            getGUI().setActiveUnit(unit.getCarrier());
        } else {
            Iterator<Unit> unitIterator = tile.getUnitIterator();
            boolean activeUnitFound = false;
            while (unitIterator.hasNext()) {
                Unit u = unitIterator.next();
                if (u == unit) {
                    activeUnitFound = true;
                } else if (activeUnitFound
                    && u.getState() == Unit.UnitState.ACTIVE
                    && u.getMovesLeft() > 0) {
                    getGUI().setActiveUnit(u);
                    return;
                }
            }
            unitIterator = tile.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit u = unitIterator.next();
                if (u == unit) {
                    return;
                } else if (u.getState() == Unit.UnitState.ACTIVE
                    && u.getMovesLeft() > 0) {
                    getGUI().setActiveUnit(u);
                    return;
                }
            }
        }
    }
}
