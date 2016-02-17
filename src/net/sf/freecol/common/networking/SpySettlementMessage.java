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
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when spying on a settlement.
 */
public class SpySettlementMessage extends DOMMessage {

    public static final String TAG = "spySettlement";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the object doing the spying. */
    private final String unitId;

    /** The identifier of the settlement to spy on. */
    private final String settlementId;

    /**
     * A copy of the tile with the settlement on it, but including all
     * the extra (normally invisible) information.
     */
    private Tile spyTile;


    /**
     * Create a new <code>SpySettlementMessage</code> request with the
     * supplied unit and settlement
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param settlement The <code>Settlement</code> the unit is looking at.
     */
    public SpySettlementMessage(Unit unit, Settlement settlement) {
        super(getTagName());

        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.spyTile = null;
    }

    /**
     * Create a new <code>SpySettlementMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SpySettlementMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.settlementId = getStringAttribute(element, SETTLEMENT_TAG);
        this.spyTile = getChild(game, element, 0, false, Tile.class);
    }


    // Public interface

    public Tile getSpyTile() {
        return this.spyTile;
    }

    public Unit getUnit(Player player) {
        return player.getOurFreeColGameObject(this.unitId, Unit.class);
    }

    public Colony getColony(Game game) {
        return game.getFreeColGameObject(this.settlementId, Colony.class);
    }


    /**
     * Handle a "spySettlement"-message.
     *
     * @param server The <code>FreeColServer</code> that is handling
     *     the message.
     * @param connection The <code>Connection</code> the message was
     *     received on.
     * @return An <code>Element</code> containing a representation of
     *     the settlement being spied upon and any units at that
     *     position, or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = serverPlayer.getGame();

        Unit unit;
        try {
            unit = getUnit(serverPlayer);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        if (!unit.hasAbility(Ability.SPY_ON_COLONY)) {
            return serverPlayer.clientError("Unit lacks ability"
                + " to spy on colony: " + this.unitId)
                .build(serverPlayer);
        }

        Colony colony = getColony(game);
        if (colony == null) {
            return serverPlayer.clientError("Not a colony: "
                + this.settlementId)
                .build(serverPlayer);
        }
        Tile tile = colony.getTile();
        if (!unit.getTile().isAdjacent(tile)) {
            return serverPlayer.clientError("Unit " + this.unitId
                + " not adjacent to colony: " + this.settlementId)
                .build(serverPlayer);
        }

        MoveType type = unit.getMoveType(tile);
        if (type != MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT) {
            return serverPlayer.clientError("Unable to enter at: "
                + colony.getName() + ": " + type.whyIllegal())
                .build(serverPlayer);
        }

        // Spy on the settlement
        return server.getInGameController()
            .spySettlement(serverPlayer, unit, colony)
            .build(serverPlayer);
    }

    /**
     * Convert this SpySettlementMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            SETTLEMENT_TAG, this.settlementId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "spySettlement".
     */
    public static String getTagName() {
        return TAG;
    }
}
