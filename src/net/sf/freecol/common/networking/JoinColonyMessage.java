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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when a unit joins a colony.
 */
public class JoinColonyMessage extends AttributeMessage {

    public static final String TAG = "joinColony";
    private static final String COLONY_TAG = "colony";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code JoinColonyMessage} with the supplied name
     * and building unit.
     *
     * @param colony a {@code Colony} value
     * @param builder The {@code Unit} to do the building.
     */
    public JoinColonyMessage(Colony colony, Unit builder) {
        super(TAG, COLONY_TAG, colony.getId(), UNIT_TAG, builder.getId());
    }

    /**
     * Create a new {@code JoinColonyMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public JoinColonyMessage(Game game, Element element) {
        super(TAG, COLONY_TAG, getStringAttribute(element, COLONY_TAG),
              UNIT_TAG, getStringAttribute(element, UNIT_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String colonyId = getAttribute(COLONY_TAG);
        final String unitId = getAttribute(UNIT_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Colony colony;
        try {
            colony = serverPlayer.getOurFreeColGameObject(colonyId, Colony.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        // Try to buy.
        return freeColServer.getInGameController()
            .joinColony(serverPlayer, unit, colony);
    }
}
