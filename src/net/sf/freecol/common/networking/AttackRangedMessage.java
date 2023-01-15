/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when attacking.
 */
public class AttackRangedMessage extends AttributeMessage {

    public static final String TAG = "attackRanged";
    private static final String UNIT_TAG = "unit";
    private static final String TARGET_TAG = "target";


    /**
     * Create a new {@code AttackMessage} for the supplied unit and
     * direction.
     *
     * @param unit The {@code Unit} attacking.
     * @param target The targeted {@code Tile} of the attack.
     */
    public AttackRangedMessage(Unit unit, Tile target) {
        super(TAG, UNIT_TAG, unit.getId(),
                TARGET_TAG, target.getId());
    }

    /**
     * Create a new {@code AttackMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public AttackRangedMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, TARGET_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String unitId = getStringAttribute(UNIT_TAG);
        final String targetId = getStringAttribute(TARGET_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        
        if (unit.getMovesLeft() <= 0) {
            return serverPlayer.clientError("No moves left.");
        }

        Tile tile;
        try {
            tile = freeColServer.getGame().getFreeColGameObject(targetId, Tile.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        if (unit.canAttackRanged(tile)) {
            ; // OK
        } else {
            return serverPlayer.clientError("Illegal ranged attack for: "
                + unitId
                + " from: " + unit.getLocation().getId()
                + " to: " + tile.getId());
        }

        Unit defender = tile.getDefendingUnit(unit);
        if (defender == null) {
            return serverPlayer.clientError("Could not find defender"
                + " in tile: " + tile.getId()
                + " from: " + unit.getLocation().getId());
        }

        // Proceed to attack.
        return igc(freeColServer)
            .combat(serverPlayer, unit, defender, null);
    }
}
