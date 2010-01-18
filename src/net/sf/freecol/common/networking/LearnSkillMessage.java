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
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when learning for the skill taught at a settlement.
 */
public class LearnSkillMessage extends Message {
    /**
     * The id of the unit that is learning.
     */
    private String unitId;

    /**
     * The direction the unit is learning in.
     */
    private String directionString;

    /**
     * Create a new <code>LearnSkillMessage</code> with the
     * supplied unit and direction.
     *
     * @param unit The <code>Unit</code> that is learning.
     * @param direction The <code>Direction</code> the unit is looking.
     */
    public LearnSkillMessage(Unit unit, Direction direction) {
        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>LearnSkillMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public LearnSkillMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unitId");
        this.directionString = element.getAttribute("direction");
    }

    /**
     * Handle a "learnSkill"-message.
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

        // Learn the skill if possible.
        // Bit of a mess building the reply given the multiple results.
        InGameController igc = server.getInGameController();
        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        IndianSettlement indianSettlement = (IndianSettlement) settlement;
        Tension tension = indianSettlement.getAlarm(player);
        switch (tension.getLevel()) {
        case HATEFUL: // Killed
            unit.dispose();

            Element remove = doc.createElement("remove");
            reply.appendChild(remove);
            unit.addToRemoveElement(remove);
            break;
        case ANGRY: // Learn nothing, not even a pet update
            unit.setMovesLeft(0);

            Element updateFail = doc.createElement("update");
            reply.appendChild(updateFail);
            updateFail.appendChild(unit.toXMLElementPartial(doc, "movesLeft"));
            break;
        default: // Unit clear to try to learn
            try {
                igc.learnFromIndianSettlement(unit, indianSettlement);
            } catch (Exception e) {
                return Message.clientError(e.getMessage());
            }

            Element updateSuccess = doc.createElement("update");
            reply.appendChild(updateSuccess);
            updateSuccess.appendChild(unit.toXMLElement(player, doc));
            updateSuccess.appendChild(tile.toXMLElement(player, doc));
            break;
        }
        return reply;
    }

    /**
     * Convert this LearnSkillMessage to XML.
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
     * @return "learnSkill".
     */
    public static String getXMLElementTagName() {
        return "learnSkill";
    }
}
