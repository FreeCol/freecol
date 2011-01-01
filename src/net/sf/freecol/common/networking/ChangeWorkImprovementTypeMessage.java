/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when changing a work improvement type.
 */
public class ChangeWorkImprovementTypeMessage extends Message {

    /**
     * The id of the unit that is working.
     */
    private String unitId;

    /**
     * The id of the improvement type.
     */
    private String improvementId;

    /**
     * Create a new <code>ChangeWorkImprovementTypeMessage</code> with the
     * supplied unit and improvement type.
     *
     * @param unit The <code>Unit</code> that is working.
     * @param type The new <code>TileImprovementType</code>.
     */
    public ChangeWorkImprovementTypeMessage(Unit unit,
                                            TileImprovementType type) {
        this.unitId = unit.getId();
        this.improvementId = type.getId();
    }

    /**
     * Create a new <code>ChangeWorkImprovementTypeMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ChangeWorkImprovementTypeMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.improvementId = element.getAttribute("improvementType");
    }

    /**
     * Handle a "changeWorkImprovementType"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the changeWorkImprovementTyped unit,
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
        Tile tile = unit.getTile();
        if (tile == null) {
            return Message.clientError("Unit is not on the map: " + unitId);
        }
        if (!unit.hasAbility("model.ability.improveTerrain")) {
            return Message.clientError("Unit can not improve tiles: " + unitId);
        }

        TileImprovementType type = server.getSpecification()
            .getTileImprovementType(improvementId);
        TileImprovement improvement;
        if (type == null) {
            return Message.clientError("Not a tile improvement type: "
                                       + improvementId);
        } else if (type.isNatural()) {
            return Message.clientError("ImprovementType must not be natural: "
                                       + improvementId);
        } else if (!type.isTileTypeAllowed(tile.getType())) {
            return Message.clientError("ImprovementType not allowed on tile: "
                                       + improvementId);
        } else if ((improvement = tile.findTileImprovementType(type)) == null) {
            // TODO: This does not check if the tile (not TileType
            // accepts the improvement).
            if (!type.isWorkerAllowed(unit)) {
                return Message.clientError("Unit can not to create improvement: "
                                           + improvementId);
            }
        } else { // Has improvement, check if worker can contribute to it
            if (!improvement.isWorkerAllowed(unit)) {
                return Message.clientError("Unit can not work on improvement: "
                                           + improvementId);
            }
        }

        // Proceed to change.
        return server.getInGameController()
            .changeWorkImprovementType(serverPlayer, unit, type);
    }

    /**
     * Convert this ChangeWorkImprovementTypeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("improvementType", improvementId);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "changeWorkImprovementType".
     */
    public static String getXMLElementTagName() {
        return "changeWorkImprovementType";
    }
}
