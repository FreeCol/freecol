/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when scouting a native settlement.
 */
public class ScoutIndianSettlementMessage extends Message {
    /**
     * The id of the unit that is learning.
     */
    private String unitId;

    /**
     * The direction the unit is learning in.
     */
    private String directionString;

    /**
     * Create a new <code>ScoutIndianSettlementMessage</code> with the
     * supplied unit and direction.
     *
     * @param unit The <code>Unit</code> that is learning.
     * @param direction The <code>Direction</code> the unit is looking.
     */
    public ScoutIndianSettlementMessage(Unit unit, Direction direction) {
        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>ScoutIndianSettlementMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ScoutIndianSettlementMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unitId");
        this.directionString = element.getAttribute("direction");
    }

    /**
     * Handle a "scoutIndianSettlement"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An element containing the result of the scouting action,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (unit.getTile() == null) {
            return Message.clientError("Unit is not on the map: " + unitId);
        }
        Direction direction = Enum.valueOf(Direction.class, directionString);
        Game game = serverPlayer.getGame();
        Map map = game.getMap();
        Tile tile = map.getNeighbourOrNull(direction, unit.getTile());
        if (tile == null) {
            return Message.clientError("Could not find tile"
                                       + " in direction: " + direction
                                       + " from unit: " + unitId);
        }
        Settlement settlement = tile.getSettlement();
        if (settlement == null || !(settlement instanceof IndianSettlement)) {
            return Message.clientError("There is no native settlement at: "
                                       + tile.getId());
        }

        // Valid request, do the scouting.
        IndianSettlement indianSettlement = (IndianSettlement) settlement;
        InGameController igc = server.getInGameController();
        String result;
        try {
            result = igc.scoutIndianSettlement(unit, indianSettlement);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }

        // Build the reply, either a remove or an update.
        // Always return the result string to help the client display
        // something informative.
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        reply.setAttribute("result", result);
        if (unit.isDisposed()) {
            Element remove = doc.createElement("remove");
            reply.appendChild(remove);
            unit.addToRemoveElement(remove);
        } else {
            Element update = doc.createElement("update");
            reply.appendChild(update);
            // Always update the tile, as the settlement is now visited.
            update.appendChild(tile.toXMLElement(player, doc));
            // Update any new tiles the unit can see from the settlement,
            // which can include an enhanced radius from tales.
            int radius = unit.getLineOfSight();
            for (Tile t : map.getSurroundingTiles(tile, radius)) {
                if (!player.canSee(t)) {
                    update.appendChild(t.toXMLElement(player, doc));
                }
            }
            if ("tales".equals(result) && radius <= IndianSettlement.TALES_RADIUS) {
                for (Tile t : map.getSurroundingTiles(tile, radius+1, IndianSettlement.TALES_RADIUS)) {
                    if ((t.isLand() || t.isCoast()) && !player.canSee(t)) {
                        update.appendChild(t.toXMLElement(player, doc));
                    }
                }
            }

            // Update the gold if it was given.
            if ("beads".equals(result)) {
                update.appendChild(player.toXMLElementPartial(doc, "gold", "score"));
            }
            // Update the whole unit if it upgraded, otherwise just
            // the moves left.
            if ("expert".equals(result)) {
                update.appendChild(unit.toXMLElement(player, doc));
            } else {
                update.appendChild(unit.toXMLElementPartial(doc, "movesLeft"));
            }
        }
        return reply;
    }

    /**
     * Convert this ScoutIndianSettlementMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unitId", unitId);
        result.setAttribute("direction", directionString);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "scoutIndianSettlement".
     */
    public static String getXMLElementTagName() {
        return "scoutIndianSettlement";
    }
}
