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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when the client requests building of a colony.
 */
public class BuildColonyMessage extends DOMMessage {

    /** The name of the new colony. */
    private final String colonyName;

    /** The unit that is building the colony. */
    private final String builderId;


    /**
     * Create a new <code>BuildColonyMessage</code> with the supplied name
     * and building unit.
     *
     * @param colonyName The name for the new colony.
     * @param builder The <code>Unit</code> to do the building.
     */
    public BuildColonyMessage(String colonyName, Unit builder) {
        super(getXMLElementTagName());

        this.colonyName = colonyName;
        this.builderId = builder.getId();
    }

    /**
     * Create a new <code>BuildColonyMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public BuildColonyMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.colonyName = element.getAttribute("name");
        this.builderId = element.getAttribute("unit");
    }


    /**
     * Handle a "buildColony"-message.
     *
     * @param server The <code>FreeColServer</code> handling the request.
     * @param player The <code>Player</code> building the colony.
     * @param connection The <code>Connection</code> the message is from.
     * @return An update <code>Element</code> defining the new colony
     *     and updating its surrounding tiles, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = server.getGame();

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(builderId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }
        if (!unit.canBuildColony()) {
            return DOMMessage.clientError("Unit " + builderId
                + " can not build colony.");
        }

        if (colonyName == null) {
            return DOMMessage.clientError("Null colony name");
        } else if (Player.ASSIGN_SETTLEMENT_NAME.equals(colonyName)) {
            ; // ok
        } else if (game.getSettlementByName(colonyName) != null) {
            return DOMMessage.clientError("Non-unique colony name "
                + colonyName);
        }

        Tile tile = unit.getTile();
        if (!player.canClaimToFoundSettlement(tile)) {
            return DOMMessage.clientError("Can not build colony on tile "
                + tile + ": " + player.canClaimToFoundSettlementReason(tile));
        }

        // Build can proceed.
        return server.getInGameController()
            .buildSettlement(serverPlayer, unit, colonyName);
    }

    /**
     * Convert this BuildColonyMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "name", colonyName,
            "unit", builderId);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "buildColony".
     */
    public static String getXMLElementTagName() {
        return "buildColony";
    }
}
