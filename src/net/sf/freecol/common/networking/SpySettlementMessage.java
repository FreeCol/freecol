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

import org.w3c.dom.Element;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when spying on a settlement.
 */
public class SpySettlementMessage extends Message {
    /**
     * The id of the object doing the spying.
     */
    private String unitId;

    /**
     * The direction the spy is looking.
     */
    private String directionString;

    /**
     * Create a new <code>SpySettlementMessage</code> with the
     * supplied unit and direction.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param direction The <code>Direction</code> the unit is looking.
     */
    public SpySettlementMessage(Unit unit, Direction direction) {
        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>SpySettlementMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SpySettlementMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.directionString = element.getAttribute("direction");
    }

    /**
     * Handle a "spySettlement"-message.
     *
     * @param server The <code>FreeColServer</code> that is handling the message.
     * @param connection The <code>Connection</code> the message was received on.
     *
     * @return An <code>Element</code> containing the settlement being spied upon,
     *         and any units at that position.
     * @throws IllegalStateException if there is problem with the message arguments,
     *         including not finding a settlement where the spy is looking.
     */
    public Element handle(FreeColServer server, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Unit unit = server.getUnitSafely(unitId, serverPlayer);
        if (unit.getTile() == null) {
            throw new IllegalArgumentException("Unit is not on the map: " + unitId);
        }
        Direction direction = Enum.valueOf(Direction.class, directionString);
        Tile newTile = serverPlayer.getGame().getMap().getNeighbourOrNull(direction, unit.getTile());
        if (newTile == null) {
            throw new IllegalArgumentException("Could not find tile"
                                               + " in direction: " + direction
                                               + " from unit: " + unitId);
        }
        Settlement settlement = newTile.getSettlement();
        if (settlement == null) {
            throw new IllegalArgumentException("There is no settlement at: " + newTile.getId());
        }

        Element reply = Message.createNewRootElement("foreignColony");
        if (settlement instanceof Colony) {
            reply.appendChild(((Colony) settlement).toXMLElement(serverPlayer, reply.getOwnerDocument(), true, false));
        } else if (settlement instanceof IndianSettlement) {
            reply.appendChild(((IndianSettlement) settlement).toXMLElement(serverPlayer, reply.getOwnerDocument(), true, false));
        }
        for(Unit foreignUnit : newTile.getUnitList()) {
            reply.appendChild(foreignUnit.toXMLElement(serverPlayer, reply.getOwnerDocument(), true, false));
        }
        return reply;
    }

    /**
     * Convert this SpySettlementMessage to XML.
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
     * @return "spySettlement".
     */
    public static String getXMLElementTagName() {
        return "spySettlement";
    }
}
