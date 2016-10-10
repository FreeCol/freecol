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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when a missionary establishes/denounces a mission.
 */
public class MissionaryMessage extends TrivialMessage {

    public static final String TAG = "missionary";
    private static final String DENOUNCE_TAG = "denounce";
    private static final String DIRECTION_TAG = "direction";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code MissionaryMessage} with the
     * supplied name.
     *
     * @param unit The missionary {@code Unit}.
     * @param direction The {@code Direction} to the settlement.
     * @param denounce True if this is a denunciation.
     */
    public MissionaryMessage(Unit unit, Direction direction,
                             boolean denounce) {
        super(TAG, UNIT_TAG, unit.getId(),
              DIRECTION_TAG, String.valueOf(direction),
              DENOUNCE_TAG, String.valueOf(denounce));
    }

    /**
     * Create a new {@code MissionaryMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public MissionaryMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              DIRECTION_TAG, getStringAttribute(element, DIRECTION_TAG),
              DENOUNCE_TAG, getStringAttribute(element, DENOUNCE_TAG));
    }


    /**
     * Handle a "missionary"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param player The {@code Player} the message applies to.
     * @param connection The {@code Connection} message was received on.
     * @return An element containing the result of the mission
     *     operation, or an error {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final String unitId = getAttribute(UNIT_TAG);
        final String directionString = getAttribute(DIRECTION_TAG);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(directionString);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        IndianSettlement is = tile.getIndianSettlement();
        if (is == null) {
            return serverPlayer.clientError("There is no native settlement at: "
                + tile.getId())
                .build(serverPlayer);
        }

        Unit missionary = is.getMissionary();
        boolean denounce = getBooleanAttribute(DENOUNCE_TAG);
        if (denounce) {
            if (missionary == null) {
                return serverPlayer.clientError("Denouncing an empty mission at: "
                    + is.getId())
                    .build(serverPlayer);
            } else if (missionary.getOwner() == player) {
                return serverPlayer.clientError("Denouncing our own missionary at: "
                    + is.getId())
                    .build(serverPlayer);
            } else if (!unit.hasAbility(Ability.DENOUNCE_HERESY)) {
                return serverPlayer.clientError("Unit lacks denouncement ability: "
                    + unitId)
                    .build(serverPlayer);
            }
        } else {
            if (missionary != null) {
                return serverPlayer.clientError("Establishing extra mission at: "
                    + is.getId())
                    .build(serverPlayer);
            } else if (!unit.hasAbility(Ability.ESTABLISH_MISSION)) {
                return serverPlayer.clientError("Unit lacks establish mission ability: "
                    + unitId)
                    .build(serverPlayer);
            }
        }

        MoveType type = unit.getMoveType(is.getTile());
        if (type != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY) {
            return serverPlayer.clientError("Unable to enter " + is.getName()
                + ": " + type.whyIllegal())
                .build(serverPlayer);
        }

        // Valid, proceed to denounce/establish.
        return ((denounce)
            ? server.getInGameController()
                .denounceMission(serverPlayer, unit, is)
            : server.getInGameController()
                .establishMission(serverPlayer, unit, is))
            .build(serverPlayer);
    }
}
