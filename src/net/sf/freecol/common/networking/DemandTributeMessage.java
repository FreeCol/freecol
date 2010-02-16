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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when demanding tribute from a native settlement.
 */
public class DemandTributeMessage extends Message {

    /**
     * The id of the object demanding.
     */
    private String unitId;

    /**
     * The direction the demand is made.
     */
    private String directionString;

    /**
     * Create a new <code>DemandTributeMessage</code> with the
     * supplied unit and direction.
     *
     * @param unit The <code>Unit</code> that is demanding.
     * @param direction The <code>Direction</code> the unit is looking.
     */
    public DemandTributeMessage(Unit unit, Direction direction) {
        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>DemandTributeMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DemandTributeMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.directionString = element.getAttribute("direction");
    }

    /**
     * Handle a "demandTribute"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> that sent the message.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An <code>Element</code> to update the originating player
     *         with the result of the demand.
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
        if (!unit.isArmed() && unit.getRole() != Unit.Role.SCOUT) {
            return Message.clientError("Unit is neither armed nor a scout: "
                                       + unitId);
        }
        Direction direction = Enum.valueOf(Direction.class, directionString);
        Tile tile = serverPlayer.getGame().getMap()
            .getNeighbourOrNull(direction, unit.getTile());
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

        // Do the demand
        IndianSettlement indianSettlement = (IndianSettlement) settlement;
        int gold = server.getInGameController().demandTribute(player,
                                                              indianSettlement);
        unit.setMovesLeft(0);

        // Build the reply.  Update the unit (no moves left), and the
        // player gold if the tribute succeeded, and add a suitable
        // message.
        Element reply = createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        Element update = doc.createElement("update");
        reply.appendChild(update);
        update.appendChild(unit.toXMLElementPartial(doc, "movesLeft"));
        if (gold > 0) {
            update.appendChild(player.toXMLElementPartial(doc, "gold"));
        }
        Element messages = doc.createElement("addMessages");
        reply.appendChild(messages);
        String messageId = (gold > 0) ? "scoutSettlement.tributeAgree"
            : "scoutSettlement.tributeDisagree";
        ModelMessage m = new ModelMessage(ModelMessage.MessageType.FOREIGN_DIPLOMACY,
                                          messageId, unit, indianSettlement)
            .addAmount("%amount%", gold);

        messages.appendChild(m.toXMLElement(player, doc));
        return reply;
    }

    /**
     * Convert this DemandTributeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("direction", directionString);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "demandTribute".
     */
    public static String getXMLElementTagName() {
        return "demandTribute";
    }
}
