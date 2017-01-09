/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;

import org.w3c.dom.Element;


/**
 * The message sent updating a unit's current stop.
 */
public class SetCurrentStopMessage extends AttributeMessage {

    public static final String TAG = "setCurrentStop";
    private static final String INDEX_TAG = "index";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code SetCurrentStopMessage} for the
     * supplied unit.
     *
     * @param unit A {@code Unit} whose stop is to be setd.
     * @param index The stop index.
     */
    public SetCurrentStopMessage(Unit unit, int index) {
        super(TAG, UNIT_TAG, unit.getId(), INDEX_TAG, String.valueOf(index));
    }

    /**
     * Create a new {@code SetCurrentStopMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public SetCurrentStopMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              INDEX_TAG, getStringAttribute(element, INDEX_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String unitId = getAttribute(UNIT_TAG);
        final String indexString = getAttribute(INDEX_TAG);

        ServerUnit serverUnit;
        try {
            serverUnit = serverPlayer.getOurFreeColGameObject(unitId, ServerUnit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        TradeRoute tr = serverUnit.getTradeRoute();
        if (tr == null) {
            return serverPlayer.clientError("Unit has no trade route: "
                + unitId);
        }

        int count;
        try {
            count = Integer.parseInt(indexString);
        } catch (NumberFormatException nfe) {
            return serverPlayer.clientError("Stop index is not an integer: " +
                indexString);
        }
        if (count < 0 || count > tr.getStops().size()) {
            return serverPlayer.clientError("Invalid stop index: "
                + indexString);
        }

        // Valid, set.
        return freeColServer.getInGameController()
            .setCurrentStop(serverPlayer, serverUnit, count);
    }
}
