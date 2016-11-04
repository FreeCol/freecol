/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when putting a unit outside a colony.
 */
public class PutOutsideColonyMessage extends AttributeMessage {

    public static final String TAG = "putOutsideColony";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code PutOutsideColonyMessage} with the
     * supplied unit.
     *
     * @param unit The {@code Unit} to put outside.
     */
    public PutOutsideColonyMessage(Unit unit) {
        super(TAG, UNIT_TAG, unit.getId());
    }

    /**
     * Create a new {@code PutOutsideColonyMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public PutOutsideColonyMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG));
    }


    /**
     * Handle a "putOutsideColony"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param serverPlayer The {@code ServerPlayer} the message applies to.
     * @return An update encapsulating the change, or an error
     *     {@code Element} on failure.
     */
    public Element handle(FreeColServer server, ServerPlayer serverPlayer) {
        final String unitId = getAttribute(UNIT_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        if (!unit.hasTile()) {
            return serverPlayer.clientError("Unit is not on the map: " + unitId)
                .build(serverPlayer);
        } else if (unit.getColony() == null) {
            return serverPlayer.clientError("Unit is not in a colony: " + unitId)
                .build(serverPlayer);
        }

        // Proceed to put outside.
        return server.getInGameController()
            .putOutsideColony(serverPlayer, unit)
            .build(serverPlayer);
    }
}
