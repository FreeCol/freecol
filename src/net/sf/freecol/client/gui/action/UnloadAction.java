/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;


/**
 * An action for unloading a unit.
 */
public class UnloadAction extends MapboardAction {

    public static final String id = "unloadAction";

    private Unit unit = null;


    /**
     * Creates an action for unloading the currently selected unit.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public UnloadAction(FreeColClient freeColClient) {
        this(freeColClient, null);
    }

    /**
     * Creates an action for unloading the {@code Unit}
     * provided.  If the {@code Unit} is {@code null}, then
     * the currently selected unit is used instead.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param unit The {@code Unit} to unload.
     * @see net.sf.freecol.client.gui.MapViewer#getActiveUnit()
     */
    public UnloadAction(FreeColClient freeColClient, Unit unit) {
        super(freeColClient, id);

        this.unit = unit;
    }


    private Unit getUnit() {
        return (unit != null) ? unit : getGUI().getActiveUnit();
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        final Unit carrier = getUnit();
        final Player player = freeColClient.getMyPlayer();
        return carrier != null && carrier.isCarrier()
            && carrier.getCargoSpaceTaken() > 0
            && player != null && player.owns(carrier)
            && super.shouldBeEnabled();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        Unit carrier = getUnit();
        if (carrier != null) igc().unload(carrier);
    }
}
