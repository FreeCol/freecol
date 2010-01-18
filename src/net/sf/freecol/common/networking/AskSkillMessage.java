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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.PlayerExploredTile;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when asking for the skill taught at a settlement.
 */
public class AskSkillMessage extends Message {
    /**
     * The id of the unit that is asking.
     */
    private String unitId;

    /**
     * The direction the unit is asking in.
     */
    private String directionString;

    /**
     * Create a new <code>AskSkillMessage</code> with the
     * supplied unit and direction.
     *
     * @param unit The <code>Unit</code> that is asking.
     * @param direction The <code>Direction</code> the unit is looking.
     */
    public AskSkillMessage(Unit unit, Direction direction) {
        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>AskSkillMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public AskSkillMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unitId");
        this.directionString = element.getAttribute("direction");
    }

    /**
     * Handle a "askSkill"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An <code>Element</code> to update the originating player
     *         with the result of the query.
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
        if (unit.getMovesLeft() == 0) {
            return Message.clientError("Unit has no moves left: " + unitId);
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
        MoveType type = unit.getSimpleMoveType(settlement.getTile());
        if (type != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST) {
            return Message.clientError("Unable to enter "
                                       + settlement.getName()
                                       + ": " + type.whyIllegal());
        }

        // Update the skill
        PlayerExploredTile pet = tile.getPlayerExploredTile(player);
        pet.setVisited();
        pet.setSkill(((IndianSettlement) settlement).getLearnableSkill());
        unit.setMovesLeft(0);

        // Return an update of the tile with the skill, and unit move change.
        Element reply = Message.createNewRootElement("update");
        Document doc = reply.getOwnerDocument();
        reply.appendChild(tile.toXMLElement(player, doc, false, false));
        reply.appendChild(unit.toXMLElementPartial(doc, "movesLeft"));
        return reply;
    }

    /**
     * Convert this AskSkillMessage to XML.
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
     * @return "askSkill".
     */
    public static String getXMLElementTagName() {
        return "askSkill";
    }
}
