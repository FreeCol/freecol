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

import net.sf.freecol.common.model.Ability;
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
public class ChangeWorkImprovementTypeMessage extends DOMMessage {

    /** The identifier of the unit that is working. */
    private final String unitId;

    /** The identifier of the improvement type. */
    private final String improvementId;


    /**
     * Create a new <code>ChangeWorkImprovementTypeMessage</code> with the
     * supplied unit and improvement type.
     *
     * @param unit The <code>Unit</code> that is working.
     * @param type The new <code>TileImprovementType</code>.
     */
    public ChangeWorkImprovementTypeMessage(Unit unit,
                                            TileImprovementType type) {
        super(getXMLElementTagName());

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
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.improvementId = element.getAttribute("improvementType");
    }


    /**
     * Handle a "changeWorkImprovementType"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the changed unit, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Tile tile = unit.getTile();
        if (tile == null) {
            return DOMMessage.clientError("Unit is not on the map: " + unitId);
        } else if (!unit.hasAbility(Ability.IMPROVE_TERRAIN)) {
            return DOMMessage.clientError("Unit can not improve tiles: "
                + unitId);
        }

        TileImprovementType type = server.getSpecification()
            .getTileImprovementType(improvementId);
        TileImprovement improvement;
        if (type == null) {
            return DOMMessage.clientError("Not a tile improvement type: "
                + improvementId);
        } else if (type.isNatural()) {
            return DOMMessage.clientError("ImprovementType must not be natural: "
                + improvementId);
        } else if (!type.isTileTypeAllowed(tile.getType())) {
            return DOMMessage.clientError("ImprovementType not allowed on tile: "
                + improvementId);
        } else if ((improvement = tile.getTileImprovement(type)) == null) {
            if (!type.isWorkerAllowed(unit)) {
                return DOMMessage.clientError("Unit can not create improvement: "
                    + improvementId);
            }
        } else { // Has improvement, check if worker can contribute to it
            if (!improvement.isWorkerAllowed(unit)) {
                return DOMMessage.clientError("Unit can not work on improvement: "
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
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "unit", unitId,
            "improvementType", improvementId);
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
